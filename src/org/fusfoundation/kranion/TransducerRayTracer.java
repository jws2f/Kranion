/* 
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.kranion;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import javax.swing.JFileChooser;

import org.fusfoundation.kranion.model.image.ImageVolume;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import org.lwjgl.util.vector.*;

import org.fusfoundation.kranion.model.image.*;

/**
 *
 * @author john
 */
public class TransducerRayTracer extends Renderable {

    private Vector3f steering = new Vector3f(0f, 0f, 0f);
    
    private float boneThreshold = /*1024f +*/ 700f; // 700 HU threshold for bone
    private static ShaderProgram refractShader;
    private static ShaderProgram sdrShader;
    private static ShaderProgram pressureShader;
    
    private int posSSBo=0; // buffer of element starting center points
    private int colSSBo=0; // buffer of color data per element
    private int outSSBo=0; // buffer of 6 vertices per element indicating beam path (3 line segments)
    private int distSSBo=0; // buffer containing minimum distance from focal spot per element
    
    private int outDiscSSBo = 0; // buffer of triangle vertices for 'discs' on skull surface
    private int outRaysSSBo = 0; // buffer of lines from element start point to outside of skull
    
    private int envSSBo = 0; // buffer for treatment envelope data (21x21x21 volume of active element counts)
    
    private int sdrSSBo = 0; // buffer of houdsfield values along the path between outside and inside of skull per element
    private int pressureSSBo = 0; // buffer of pressure contributions per ray to a given sample point
    private int phaseSSBo =0;
    
    private Trackball trackball = null;
    private Vector3f centerOfRotation;
    private ImageVolume CTimage = null;
    public FloatBuffer matrixBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f ctTexMatrix = new Matrix4f();
    public FloatBuffer normMatrixBuf = BufferUtils.createFloatBuffer(16);
    
    private ImageVolume4D envelopeImage = null;
    
    private boolean showEnvelope = false;
    
    private int elementCount = 0;
    private int activeElementCount = 0;
    private float sdr = 0f;
    
    // Observable pattern
    private List<Observer> observers = new ArrayList<Observer>();
    public void addObserver(Observer observer) {
        observers.add(observer);
    }
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    public void removeAllObservers() {
        observers.clear();
    }
    private void updateObservers(PropertyChangeEvent e) {
        Iterator<Observer> i = observers.iterator();
        while(i.hasNext()) {
            Observer o = i.next();
            o.update(null, e);
        }
    }
            
    public int getActiveElementCount() { return activeElementCount; }
    
    public boolean getShowEnvelope() { return showEnvelope; }
    public void setShowEnvelope(boolean f) {
        if (showEnvelope != f) {
            setIsDirty(true);
        }
        showEnvelope = f;
    }
    
    private boolean clipRays = true;
    public void setClipRays(boolean clip) {
        if (clipRays != clip) {
            setIsDirty(true);
        }
        clipRays = clip;
    }
   
    
    private float rescaleIntercept = 0f;
    private float rescaleSlope = 1f;
    
    private float colormap_min, colormap_max;
    
    public void setImage(ImageVolume image) {
        if (CTimage != image) {
            setIsDirty(true);
        }
        CTimage = image;
        Float value = (Float) image.getAttribute("RescaleIntercept");
        if (value != null) rescaleIntercept = value;
        value = (Float) image.getAttribute("RescaleSlope");
        if (value != null) rescaleSlope = value;   
    }
    
    public void setTextureRotatation(Vector3f rotationOffset, Trackball tb) {
        if (trackball == null || !centerOfRotation.equals(rotationOffset) || trackball.getCurrent() != tb.getCurrent()) {
            setIsDirty(true);
        }
        centerOfRotation = rotationOffset;
        trackball = tb;
    }
    
    public float getSDR() { return sdr; }
    
    private float transducerTilt = 0f;

       // set tilt around x axis in degrees
    public void setTransducerTilt(float tilt) {
        if (transducerTilt != tilt) {
            setIsDirty(true);
        }
        transducerTilt = tilt;
    }
    
    private float boneSpeed = 2652f;//3500f;
    private float boneRefractionSpeed = 2652f;
    
    public void setTargetSteering(float x, float y, float z) { // (0, 0, 0) = no steering
        if (steering.x != x || steering.y != y || steering.z != z) {
            setIsDirty(true);
        }
        steering.set(x, y, z);
    }
    
    public Vector3f getTargetSteering() {
        return steering;
    }
    
    public void setBoneSpeed(float speed) {
        if (boneSpeed != speed) {
            setIsDirty(true);
        }
        boneSpeed = Math.min(3500, Math.max(1482, speed));
    }
    public void setBoneRefractionSpeed(float speed) {
        if (boneRefractionSpeed != speed) {
            setIsDirty(true);
        }
        boneRefractionSpeed = Math.min(3500, Math.max(1482, speed));        
    }
    
    public float getBoneSpeed() { return boneSpeed; }
    
    public float getBoneRefractionSpeed() { return boneRefractionSpeed; }
    
    public void setBoneThreshold(float v) {
        if (boneThreshold != v) {
            setIsDirty(true);
        }
        boneThreshold = v;
    }
    
    public float getBoneThreshold() { return boneThreshold; }
    
    public ImageVolume getEnvelopeImage() { return envelopeImage; }
     
    private void initShader() {
        if (refractShader == null) {
            refractShader = new ShaderProgram();
            refractShader.addShader(GL_COMPUTE_SHADER, "shaders/TransducerRayTracer5x5.cs.glsl");
            refractShader.compileShaderProgram();
        }
        if (sdrShader == null) {
            sdrShader = new ShaderProgram();
            sdrShader.addShader(GL_COMPUTE_SHADER, "shaders/sdrShader.cs.glsl");
            sdrShader.compileShaderProgram();
        }
        if (pressureShader == null) {
            pressureShader = new ShaderProgram();
            pressureShader.addShader(GL_COMPUTE_SHADER, "shaders/pressureShader.cs.glsl");
            pressureShader.compileShaderProgram();
        }
    }
    
    public void init(Transducer trans) {

        setIsDirty(true);
        
        release();
        
        initShader();
        
        elementCount = trans.getElementCount();

        FloatBuffer floatPosBuffer = ByteBuffer.allocateDirect(1024*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        int maxElemCount = elementCount > 1024 ? elementCount : 1024;
        for (int i=0; i<maxElemCount; i++) {
            Vector4f pos = new Vector4f(trans.getElement(i));
            pos.w = 1f;
            
            if (!trans.getElementActive(i)) {
                pos.w = -1f;
            }
            
            floatPosBuffer.put(pos.x);
            floatPosBuffer.put(pos.y);
            floatPosBuffer.put(pos.z);
            floatPosBuffer.put(pos.w);
            
            Vector3f norm = new Vector3f(pos.x, pos.y, pos.z);
            norm.negate(norm);
            norm.normalise();
            
            floatPosBuffer.put(norm.x);
            floatPosBuffer.put(norm.y);
            floatPosBuffer.put(norm.z);
            floatPosBuffer.put(0f);
            
        }
        floatPosBuffer.flip();
        
        
        posSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, posSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        
        
        floatPosBuffer = ByteBuffer.allocateDirect(1024 * 4*12 * 20*3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<1024*3*20; i++) {
            
            //positions
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
            Vector3f norm = new Vector3f(1f, 1f, 1f);
            norm.normalise();
            
            //normals
            floatPosBuffer.put(norm.x);
            floatPosBuffer.put(norm.y);
            floatPosBuffer.put(norm.z);
            floatPosBuffer.put(0f);
            
            //colors
            floatPosBuffer.put(1f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
        }
        floatPosBuffer.flip();
        
        
        outDiscSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_DYNAMIC_DRAW);
 
        // Rays between transducer surface and skull
        floatPosBuffer = ByteBuffer.allocateDirect(1024 * 4*8 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<1024*2; i++) {
            
            //positions
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
            //colors
            floatPosBuffer.put(1f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
        }
        floatPosBuffer.flip();
        
        
        outRaysSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, outRaysSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_DYNAMIC_DRAW);

/////
        // Envelope survey data
        floatPosBuffer = ByteBuffer.allocateDirect(/*11x11x11*/1331 * 4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<1331; i++) {
            
            //positions
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
            //colors
            floatPosBuffer.put(1f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(0f);
            floatPosBuffer.put(1f);
            
        }
        floatPosBuffer.flip();
        
        
        envSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_DYNAMIC_DRAW);
/////
        
//        // Create a buffer of vertices to hold the output of the raytrace procedure
//        FloatBuffer floatOutBuffer = ByteBuffer.allocateDirect(1024*4*4 * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
//        for (int i=0; i<1024 * 2; i++) {
//            //color
//            floatOutBuffer.put(0f);
//            floatOutBuffer.put(0f);
//            floatOutBuffer.put(0f);
//            floatOutBuffer.put(1f);
//        }       
//        floatOutBuffer.flip();
//        
//        outSSBo = glGenBuffers();
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, outSSBo);
//        glBufferData(GL_SHADER_STORAGE_BUFFER, floatOutBuffer, GL_STATIC_DRAW);
        
        
        // Create a buffer of vertices to hold the output of the raytrace procedure
        FloatBuffer floatOutBuffer = ByteBuffer.allocateDirect(1024*4*4 * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024 * 6; i++) {
            //color
            floatOutBuffer.put(0f);
            floatOutBuffer.put(0f);
            floatOutBuffer.put(0f);
            floatOutBuffer.put(1f);
        }       
        floatOutBuffer.flip();
        
        outSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatOutBuffer, GL_STATIC_DRAW);
        
        FloatBuffer floatColBuffer = ByteBuffer.allocateDirect(1024*4*4 * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024 * 6; i++) {
            //color
            floatColBuffer.put(1f);
            floatColBuffer.put(1f);
            floatColBuffer.put(1f);
            floatColBuffer.put(0.4f);
        }
        floatColBuffer.flip();

        colSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatColBuffer, GL_STATIC_DRAW);
        
        distSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, distSSBo);
        FloatBuffer distBuffer = ByteBuffer.allocateDirect(1024*4 *4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024; i++) {
            //distance from focus
            distBuffer.put(0f);
            // SDR value
            distBuffer.put(0f);
            // Incident angle value
            distBuffer.put(0f);
            // Skull thickness value
            distBuffer.put(0f);
        }
        distBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER,distBuffer,GL_STATIC_DRAW);
        
        sdrSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, sdrSSBo);
        FloatBuffer sdrBuffer = ByteBuffer.allocateDirect(1024*4 *(60 + 3)).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024; i++) {
            for (int j=0; j<60; j++) {
                //ct values along transit of skull per element
                sdrBuffer.put(0f);
            }
            // extrema values
            sdrBuffer.put(0f);
            sdrBuffer.put(0f);
            sdrBuffer.put(0f);
        }
        sdrBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER,sdrBuffer,GL_STATIC_DRAW);
        
        pressureSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, pressureSSBo);
        FloatBuffer pressureBuffer = ByteBuffer.allocateDirect(1024*4 * 1).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024; i++) {
                 //ct values along transit of skull per element
                pressureBuffer.put(0f);
        }
        pressureBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, pressureBuffer, GL_STATIC_DRAW);
        
        phaseSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, phaseSSBo);
        FloatBuffer phaseBuffer = ByteBuffer.allocateDirect(1024*4 * 1).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<1024; i++) {
                 //ct values along transit of skull per element
                phaseBuffer.put(0f);
        }
        phaseBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER,phaseBuffer, GL_STATIC_DRAW);        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private void doCalc() {
        refractShader.start();
        
        int shaderProgID = refractShader.getShaderProgramID();
        
        int texLoc = glGetUniformLocation(shaderProgID, "ct_tex");
        glUniform1i(texLoc, 0);
        int texMatLoc = glGetUniformLocation(shaderProgID, "ct_tex_matrix");
        glUniformMatrix4(texMatLoc, false, this.matrixBuf);
        
        int boneLoc = glGetUniformLocation(shaderProgID, "boneSpeed");
        glUniform1f(boneLoc, boneRefractionSpeed);
        int waterLoc = glGetUniformLocation(shaderProgID, "waterSpeed");
        glUniform1f(waterLoc, 1482f);
        int boneThreshLoc = glGetUniformLocation(shaderProgID, "ct_bone_threshold");
        glUniform1f(boneThreshLoc, boneThreshold);
        
        int rescaleLoc = glGetUniformLocation(shaderProgID, "ct_rescale_intercept");
        glUniform1f(rescaleLoc, this.rescaleIntercept);
        rescaleLoc = glGetUniformLocation(shaderProgID, "ct_rescale_slope");
        glUniform1f(rescaleLoc, this.rescaleSlope);
        
        int targetLoc = glGetUniformLocation(shaderProgID, "target");
        glUniform3f(targetLoc, steering.x, steering.y, steering.z);
        
        
//        int targetLoc = glGetUniformLocation(shaderprogram, "target");
//        glUniform3f(targetLoc, 0f, 0f, 300f);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, outDiscSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, outRaysSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, phaseSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(1024 / 256, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, 0);

        
        refractShader.stop();
        
        this.updateObservers(new PropertyChangeEvent(this, "rayCalc", null, null));        
    }
    
    public void doSDRCalc() {
         // Calculate per element SDR data
         //
         // doCalc() must be called first to compute the per element ray segments
         //
         
        sdrShader.start();
        int shaderProgID = sdrShader.getShaderProgramID();
        
        int texLoc = glGetUniformLocation(shaderProgID, "ct_tex");
        glUniform1i(texLoc, 0);
        int texMatLoc = glGetUniformLocation(shaderProgID, "ct_tex_matrix");
        glUniformMatrix4(texMatLoc, false, this.matrixBuf);
        int rescaleLoc = glGetUniformLocation(shaderProgID, "ct_rescale_intercept");
        glUniform1f(rescaleLoc, this.rescaleIntercept);
        rescaleLoc = glGetUniformLocation(shaderProgID, "ct_rescale_slope");
        glUniform1f(rescaleLoc, this.rescaleSlope);
        
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, sdrSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(1024 / 256, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        
        sdrShader.stop();
        
        this.updateObservers(new PropertyChangeEvent(this, "sdrCalc", null, null));        
    }
    
    public void doPressureCalc(Vector4f samplePoint) {
     // Calculate per element SDR data
         //
         // doCalc() must be called first to compute the per element ray segments
         //
         
        pressureShader.start();
        
        int shaderProgID = pressureShader.getShaderProgramID();
        int uniformLoc = glGetUniformLocation(shaderProgID, "sample_point");
        glUniform3f(uniformLoc, samplePoint.x, samplePoint.y, samplePoint.z);// + 150f);
        uniformLoc = glGetUniformLocation(shaderProgID, "boneSpeed");
        glUniform1f(uniformLoc, this.boneSpeed);
//TEST        glUniform1f(uniformLoc, 2640f);
        uniformLoc = glGetUniformLocation(shaderProgID, "waterSpeed");
        glUniform1f(uniformLoc, 1482f);
        
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, pressureSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, phaseSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(1024 / 256, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
        
        pressureShader.stop();
        
    }
    
    public void calcEnvelope() {
        calcEnvelope(null);
    }
    
    public void calcEnvelope(ProgressListener listener) {
        
//        if (envelopeImage == null) {
            if (envelopeImage != null) {
                ImageVolumeUtil.releaseTexture(envelopeImage);
            }
            
            envelopeImage = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, 21, 21, 21, 1);
            envelopeImage.getDimension(0).setSampleWidth(4f);
            envelopeImage.getDimension(1).setSampleWidth(6f);
            envelopeImage.getDimension(2).setSampleWidth(4f);
            
            envelopeImage.getDimension(0).setSampleSpacing(4f);
            envelopeImage.getDimension(1).setSampleSpacing(6f);
            envelopeImage.getDimension(2).setSampleSpacing(4f);
//        }
        
        Vector4f offset = new Vector4f();
        Matrix4f transducerTiltMat = new Matrix4f();
        transducerTiltMat.setIdentity();
        Matrix4f.rotate(-transducerTilt/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), transducerTiltMat, transducerTiltMat);

        
        if (CTimage == null) return;
        Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
        
        envelopeImage.setAttribute("ImageTranslation", new Vector3f());
        
        // 21x21x21 array of floats to hold sampled treatment envelope
        FloatBuffer envFloats = ByteBuffer.allocateDirect(9261*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        float[] voxels = (float[])envelopeImage.getData();
        
        for (int i = -10; i <= 10; i++) {
            for (int j = -10; j <= 10; j++) {
                if (listener != null) {
                    listener.percentDone("Treatment envelope", Math.round( ((i+10)*21f + (j+10))/440f*100f ) );
                }
                for (int k = -10; k <= 10; k++) {
                    
                    offset.set(i * 4f, j * 6f, k * 4f, 1f);
                    
                    int elemCount = calcElementsOn(new Vector3f(0f, 0f, 0f), offset);
                    
                    voxels[(i+10) + (-j+10)*21 + (-k+10)*21*21] = (short)(elemCount & 0xffff);
                    
//                    System.out.print("elemCount = " + elemCount + " ");
//                    envFloats.put(-centerOfRotation.x + imageTranslation.x + offset.x);
//                    envFloats.put(-centerOfRotation.x + imageTranslation.y + offset.y);
//                    envFloats.put(-centerOfRotation.x + imageTranslation.z + offset.z + 150f);

                    Matrix4f.transform(transducerTiltMat, offset,  offset);
                    
                    envFloats.put(offset.x);// + centerOfRotation.x);
                    envFloats.put(offset.y);// - centerOfRotation.y);
                    envFloats.put(-offset.z);//  -centerOfRotation.z);
                     envFloats.put(1f);
                    if (elemCount >= 700) {
                        envFloats.put(0f);
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    }
                    else if (elemCount >= 500) {
                        envFloats.put(1f);
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    } else {
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    }
                }
            }
//            System.out.println("");
        }
        if (listener != null) {
            listener.percentDone("Ready", -1);
        }

        envFloats.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glBufferData(GL_ARRAY_BUFFER, envFloats, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public void writeSkullMeasuresFile(File outFile) {

        if (outFile != null) {
            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
            FloatBuffer floatPhases = dists.asFloatBuffer();
            int count = 0;

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(outFile));
                writer.write("Skull parameters");
                writer.newLine();
                writer.write("NumberOfChannels = 1024");
                writer.newLine();
                writer.newLine();
                writer.write("channel\tsdr\tincidentAngle\tskull thickness");
                writer.newLine();
                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();

                    writer.write(count + "\t");
                    count++;
                    writer.write(String.format("%1.3f", sdr) + "\t");
                    writer.write(String.format("%3.3f", incidentAngle) + "\t");
                    writer.write(String.format("%3.3f", skullThickness) + "\t");
                    writer.newLine();
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }
    
    public void writeACTFile(File outFile) {
        if (outFile != null) {
            doCalc();
            doPressureCalc(new Vector4f());
            
            glBindBuffer(GL_ARRAY_BUFFER, phaseSSBo);
            ByteBuffer phases = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
            FloatBuffer floatPhases = phases.asFloatBuffer();
            int count = 0, zeroCount = 0;

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(outFile));
                writer.write("[AMPLITUDE_AND_PHASE_CORRECTIONS]");
                writer.newLine();
                writer.write("NumberOfChannels = 1024");
                writer.newLine();
                writer.newLine();
                while (floatPhases.hasRemaining()) {
                    double phase = (double) floatPhases.get() % (2.0 * Math.PI);
                    if (phase > Math.PI) {
                        phase -= 2.0 * Math.PI;
                    } else if (phase < -Math.PI) {
                        phase += 2.0 * Math.PI;
                    }
                    if (phase == 0.0) {
                        zeroCount++;
                    }
//                phase = -phase;
//double phase = (double)floatPhases.get(); // for outputing skull thickness if set in pressure shader

//                    System.out.println("Channel " + count + " = " + phase);
                    writer.write("CH" + count + "\t=\t");
                    count++;
                    if (phase == 0.0) {
                        writer.write("0\t");
                    } else {
                        writer.write("1\t");
                    }
                    writer.write(String.format("%1.4f", phase));
                    writer.newLine();
                }
                System.out.println("zero phase channels = " + zeroCount);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }
    
    public List<Double> getIncidentAngles() {

        List<Double> result = new ArrayList<>();
        
            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
            FloatBuffer floatPhases = dists.asFloatBuffer();
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    
                    if (incidentAngle >= 0f) {
                        result.add((double)incidentAngle);
                    }                    
                }
                
            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    public List<Double> getSDRs() {

        List<Double> result = new ArrayList<>();
        
            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
            FloatBuffer floatPhases = dists.asFloatBuffer();
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)sdr);
                    }
                }
                
            glUnmapBuffer(GL_ARRAY_BUFFER);
            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    
    public void calcPressureEnvelope() {
        
//        if (envelopeImage == null) {
            float voxelsize = 0.5f;
            
            int volumeHalfWidth = 10;
            int volumeWidth = 2*volumeHalfWidth+1;
            
            envelopeImage = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, volumeWidth, volumeWidth, volumeWidth, 1);
            envelopeImage.getDimension(0).setSampleWidth(voxelsize);
            envelopeImage.getDimension(1).setSampleWidth(voxelsize);
            envelopeImage.getDimension(2).setSampleWidth(voxelsize);
            
            envelopeImage.getDimension(0).setSampleSpacing(voxelsize);
            envelopeImage.getDimension(1).setSampleSpacing(voxelsize);
            envelopeImage.getDimension(2).setSampleSpacing(voxelsize);
//        }
        
        Vector4f offset = new Vector4f();
//        Matrix4f transducerTiltMat = new Matrix4f();
//        transducerTiltMat.setIdentity();
//        Matrix4f.rotate(-transducerTilt/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), transducerTiltMat, transducerTiltMat);

        
        if (CTimage == null) return;
//        Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
//        
//        envelopeImage.setAttribute("ImageTranslation", new Vector3f(-imageTranslation.x, -imageTranslation.y, -imageTranslation.z));
        envelopeImage.setAttribute("ImageTranslation", new Vector3f(-centerOfRotation.x, -centerOfRotation.y, -centerOfRotation.z));
        envelopeImage.setAttribute("ImageOrientationQ", new Quaternion());
        
                
        float[] voxels = (float[])envelopeImage.getData();
        
        this.colormap_min = Float.MAX_VALUE;
        this.colormap_max = -Float.MAX_VALUE;
                
        setupImageTexture(CTimage, 0, new Vector3f(centerOfRotation.x, centerOfRotation.y, centerOfRotation.z), new Vector4f(0f, 0f, 0f, 1f)/*offset*/);

        doCalc();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
        
        for (int i = -volumeHalfWidth; i <= volumeHalfWidth; i++) {
            for (int j = -volumeHalfWidth; j <= volumeHalfWidth; j++) {
                for (int k = -volumeHalfWidth; k <= volumeHalfWidth; k++) {
                    float pressure = 0f;
//if (k==0)   {                 
                    offset.set(i * voxelsize, j * voxelsize, k * voxelsize, 1f);

                    pressure = calcSamplePressure(new Vector3f(), offset) * 10f;
                    pressure *= pressure;
                    
                    if (i==0 && j==0 && k==0) {
/////////////////////////////////////////////
/////////////////////////////////////////////
//TODO: just for now, remove
        ///glMapBuffer
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, phaseSSBo);
//        ByteBuffer phases = glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_READ_WRITE, null);
//        FloatBuffer floatPhases = phases.asFloatBuffer();
//        int count = 0, zeroCount = 0;
//
//        BufferedWriter writer=null;
//        try {
//            writer = new BufferedWriter(new FileWriter("ACT.ini"));
//            writer.write("[AMPLITUDE_AND_PHASE_CORRECTIONS]");
//            writer.newLine();
//            writer.write("NumberOfChannels = 1024");
//            writer.newLine();
//            writer.newLine();
//            while (floatPhases.hasRemaining()) {
//                double phase = (double)floatPhases.get() % (2.0 * Math.PI);
//                if (phase > Math.PI) {
//                    phase -= 2.0 * Math.PI;
//                }
//                else if (phase < -Math.PI) {
//                    phase += 2.0 * Math.PI;
//                }
//                if (phase == 0.0) {
//                    zeroCount++;
//                }
////                phase = -phase;
////double phase = (double)floatPhases.get(); // for outputing skull thickness if set in pressure shader
//                
//                System.out.println("Channel " + count + " = " + phase);
//                writer.write("CH" + count + "\t=\t");
//                count++;
//                if (phase == 0.0) {
//                    writer.write("0\t");
//                }
//                else {
//                    writer.write("1\t");
//                }
//                writer.write(String.format("%1.4f", phase));
//                writer.newLine();
//            }
//            System.out.println("zero phase channels = " + zeroCount);
//            writer.close();
//        }
//        catch(IOException e) {
//            e.printStackTrace();
//        }
//        
//        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);        
/////////////////////////////////////////////
/////////////////////////////////////////////
                    }

                    if (pressure > colormap_max) {
                        colormap_max = pressure;
                    }
                    if (pressure < colormap_min) {
                        colormap_min = pressure;
                    }
//}
                    voxels[(i + volumeHalfWidth) + (-j + volumeHalfWidth) * volumeWidth + (k + volumeHalfWidth) * volumeWidth * volumeWidth] = pressure;
                    
//                    if (i==0) {
//                         if (k==-volumeHalfWidth) {
//                             System.out.println();
//                         }
//                        System.out.print(pressure + "   ");
//                    } // k == 0
                }
            }
        }
//        for (int i = -volumeHalfWidth; i <= volumeHalfWidth; i++) {
//            for (int j = -volumeHalfWidth; j <= volumeHalfWidth; j++) {
//                for (int k = -volumeHalfWidth; k <= volumeHalfWidth; k++) {
//
//                    float value = voxels[(i + volumeHalfWidth) + (-j + volumeHalfWidth) * volumeWidth + (-k + volumeHalfWidth) * volumeWidth * volumeWidth];
//                    voxels[(i + volumeHalfWidth) + (-j + volumeHalfWidth) * volumeWidth + (-k + volumeHalfWidth) * volumeWidth * volumeWidth] = (value - colormap_min) / (colormap_max - colormap_min);
//                }
//            }
//        }
       
        System.out.println("Pressure max = " + colormap_max + ", min = " + colormap_min);

    }
        
    public void calc2DEnvelope() {
        Vector4f offset = new Vector4f();
        Matrix4f transducerTiltMat = new Matrix4f();
        transducerTiltMat.setIdentity();
        Matrix4f.rotate(-transducerTilt/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), transducerTiltMat, transducerTiltMat);

        
        if (CTimage == null) return;
        Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
        
        // 21x21x21 array of floats to hold sampled treatment envelope
        FloatBuffer envFloats = ByteBuffer.allocateDirect(9261*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i = -10; i <= 10; i++) {
            for (int j = -10; j <= 10; j++) {
                    
                    offset.set(i * 6f, j * 6f, 0f, 1f);
                    
                    Matrix4f rotMat = Trackball.toMatrix4f(trackball.getCurrent());
//                    rotMat.rotate((float)Math.PI, new Vector3f(0f, 0f, 1f), rotMat);
                    Matrix4f.transform(rotMat, offset, offset);
                    
                    int elemCount = calcElementsOn(centerOfRotation, offset);
                    
                    System.out.print("" + elemCount + " ");
//                    envFloats.put(-centerOfRotation.x + imageTranslation.x + offset.x);
//                    envFloats.put(-centerOfRotation.x + imageTranslation.y + offset.y);
//                    envFloats.put(-centerOfRotation.x + imageTranslation.z + offset.z + 150f);

                    //Matrix4f.transform(transducerTiltMat, offset,  offset);
                    
                    envFloats.put(offset.x);// + centerOfRotation.x);
                    envFloats.put(offset.y);// - centerOfRotation.y);
                    envFloats.put(-offset.z);//  -centerOfRotation.z);
                     envFloats.put(1f);
                    if (elemCount >= 700) {
                        envFloats.put(0f);
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    }
                    else if (elemCount >= 500) {
                        envFloats.put(1f);
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    } else {
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                }
            }
            System.out.println("");
        }
        envFloats.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glBufferData(GL_ARRAY_BUFFER, envFloats, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private int calcElementsOn(Vector4f offset) {
        return calcElementsOn(centerOfRotation, offset);
    }
    
    private int calcElementsOn(Vector3f center, Vector4f offset) {
        if (CTimage == null) {
            return 0;
        }
        
        setupImageTexture(CTimage, 0, center, offset);

        doCalc();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);

        ///glMapBuffer
        glBindBuffer(GL_ARRAY_BUFFER, distSSBo);
        ByteBuffer distances = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
        FloatBuffer floatDistances = distances.asFloatBuffer();
        int numberOn = 0;
//        float sdrSum = 0f;
        while (floatDistances.hasRemaining()) {
            float value = floatDistances.get();
            float sdrval = floatDistances.get(); // TODO
            floatDistances.get(); // incidence angle
            floatDistances.get(); // skull thickness
            if (value > 0) {
                numberOn++;
//                sdrSum += sdr;
            }
        }
        
//        System.out.println("SDR = " + sdrSum/numberOn);
        
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        return numberOn;
    }
    
    private float calcSamplePressure(Vector3f center, Vector4f offset) {
        if (CTimage == null) {
            return 0;
        }
        
//        setupImageTexture(CTimage, 0, center, new Vector4f(0f, 0f, 0f, 1f)/*offset*/);
//
//        doCalc();
        doPressureCalc(offset);
        
//        glActiveTexture(GL_TEXTURE0 + 0);
//        glDisable(GL_TEXTURE_3D);

        ///glMapBuffer
        glBindBuffer(GL_ARRAY_BUFFER, pressureSSBo);
        ByteBuffer pressures = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
        FloatBuffer floatPressures = pressures.asFloatBuffer();
        float totalPressure = 0f;

        while (floatPressures.hasRemaining()) {
            totalPressure += floatPressures.get();
        }
        
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        return totalPressure;
    }
    
    @Override
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);
        
        if (CTimage == null) return;
        
        setupImageTexture(CTimage, 0, centerOfRotation);
        
        doCalc();
        doSDRCalc();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
        
        // Raytracer done, now render result
        
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER,sdrSSBo);
//        ByteBuffer sdrOutput = glMapBuffer(GL_SHADER_STORAGE_BUFFER,GL_READ_ONLY,null);
//        FloatBuffer sdrValues = sdrOutput.asFloatBuffer();
//        
//        float[][] tmpOut = new float[1024][60];
//        float[][] tmpIndices = new float[1024][3];
//        
//        for (int i=0; i<1024; i++) {
//            sdrValues.get(tmpOut[i]);
//            sdrValues.get(tmpIndices[i]);
//        }
//        
//        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        
        
//        System.out.println("///////////SDR data");
//        for (int i=0; i<60; i++) {
//            System.out.print(i);
//            for (int j=0; j<1024; j++) {
//                System.out.print(", " + tmpOut[j][i]);
//            }
//            System.out.println();
//        }
//        for (int i=0; i<3; i++) {
//            switch(i) {
//                case 0:
//                    System.out.print("LPk");
//                    break;
//                case 1:
//                    System.out.print("RPk");
//                    break;
//                case 2:
//                    System.out.print("Min");
//                    break;
//            }
//            for (int j=0; j<1024; j++) {
//                System.out.print(", " + tmpIndices[j][i]);
//            }
//            System.out.println();
//        }
//
//        System.out.println("/////////////");
        
        
        ///glMapBuffer
        glBindBuffer(GL_ARRAY_BUFFER,distSSBo);
        ByteBuffer distances = glMapBuffer(GL_ARRAY_BUFFER,GL_READ_WRITE,null);
        FloatBuffer floatDistances = distances.asFloatBuffer();
        float distanceSum = 0.0f;
        int distanceNum = 0;
        int numberOn = 0;
        while (floatDistances.hasRemaining())
        {
        	float value = floatDistances.get();
                float sdr = floatDistances.get();
                floatDistances.get(); // skip incidence angle
                floatDistances.get(); // skip skull thickness
        	if (value > 0)
        	{
                    distanceNum++;
                    distanceSum += value;
                    numberOn++;
        	}
        }
        float percentOn = numberOn/1024.0f;
        float mean = distanceSum/distanceNum;
        floatDistances.rewind();
        float diffSqSum = 0.0f;
        float sdrSum = 0.0f;
        while (floatDistances.hasRemaining())
        {
        	float value = floatDistances.get();
                float sdr = floatDistances.get();
                floatDistances.get();
                floatDistances.get();
        	if (value > 0)
        	{
        		diffSqSum += (float) Math.pow(value-mean,2);
                        sdrSum += sdr;
        	}
        }
        activeElementCount = numberOn;
        
        this.sdr = sdrSum/distanceNum;
        float stDev = (float)Math.sqrt(diffSqSum/distanceNum);
//        System.out.println("Average: "+mean);
//        System.out.println("Std Dev: "+stDev);
//        System.out.println("% Within 3 mm of focus: "+(percentOn*100) + "(" + numberOn + " of 1024)");
//        System.out.println("SDR: " + sdrSum/distanceNum);
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
//        glTranslatef(0, 0, 150);

        glTranslatef(-steering.x, -steering.y, -steering.z);
        
        glRotatef(this.transducerTilt, 1, 0, 0);
//        glTranslatef(0, 0, -150);
        
        glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
        glVertexPointer(4, GL_FLOAT, 16*6, 0);
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
        glColorPointer(4, GL_FLOAT, 16*6, 0);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);

        Main.glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT);
        
            glDisable(GL_LIGHTING);
            
            if (clipRays) {
                glEnable(GL_CLIP_PLANE0);
                glEnable(GL_CLIP_PLANE1);
            }
            else {
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);
            }
            
            //glEnable(GL_BLEND);
            //glColor4f(1f, 0f, 0f, 1f);
            glPointSize(6f);

            // Draw outer skull points
            if (clipRays) {
                glDrawArrays(GL_POINTS, 0, 1024);
            }
            
            // Draw inner skull points
        glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
        glVertexPointer(4, GL_FLOAT, 16*6, 16*3);
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
        glColorPointer(4, GL_FLOAT, 16*6, 16*3);
        
        if (clipRays) {
            glDrawArrays(GL_POINTS, 0, 1024);
        }
        
        // Draw envelope
        renderEnvelope();
//        calc2DEnvelope(); // TODO: doesn't work yet
//        render2DEnvelope();

       
            if (clipRays) {
            //
            // Draw normal flags
            /////////////////////////////////////////////
            glDisableClientState(GL_VERTEX_ARRAY);
            glDisableClientState(GL_COLOR_ARRAY);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        
            glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
            glVertexPointer(4, GL_FLOAT, 16, 0);
            
            glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
            glColorPointer(4, GL_FLOAT, 16, 0);
            
            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_COLOR_ARRAY);
        
            org.lwjgl.opengl.GL11.glLineWidth(1.2f);
            if (!showEnvelope) {
                glDrawArrays(GL_LINES, 0, 1024*6);
            }
            /////////////////////////////////////////////
            //
            }
            
            // Draw skull strike discs
            if (!clipRays) {

            Main.glPushAttrib(GL_LIGHTING_BIT);
            
                glEnable(GL_LIGHTING);
                FloatBuffer matSpecular = BufferUtils.createFloatBuffer(4);
                matSpecular.put(0.2f).put(0.2f).put(0.2f).put(1.0f).flip();
                glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular); // sets specular material color
                glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 30.0f);					// sets shininess

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glVertexPointer(4, GL_FLOAT, 12*4, 0);

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glNormalPointer(GL_FLOAT, 12*4, 16);

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glColorPointer(4, GL_FLOAT, 12*4, 32);

                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);
                glEnableClientState(GL_NORMAL_ARRAY);
                glDrawArrays(GL_TRIANGLES, 0, 1024*3*20);
                
            Main.glPopAttrib();
            
            
            }
            else {
                Main.glPushAttrib(GL_COLOR_BUFFER_BIT);
                
                    org.lwjgl.opengl.GL11.glLineWidth(1.0f);
                    
                    glBindBuffer(GL_ARRAY_BUFFER, outRaysSSBo);
                    glVertexPointer(4, GL_FLOAT, 8*4, 0);

                    glBindBuffer(GL_ARRAY_BUFFER, outRaysSSBo);
                    glColorPointer(4, GL_FLOAT, 8*4, 16);

                    glEnableClientState(GL_VERTEX_ARRAY);
                    glEnableClientState(GL_COLOR_ARRAY);

                    glEnable(GL_BLEND);
                    glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
                    glDrawArrays(GL_LINES, 0, 1024*2);
                
                Main.glPopAttrib();

            }
       
        Main.glPopAttrib();
               
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);   
    }
    
    private void renderEnvelope() {
            Main.glPushAttrib(GL_POINT_BIT | GL_ENABLE_BIT);
        
        glPointSize(8f);
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glVertexPointer(4, GL_FLOAT, 16*2, 0);
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo); 
        glColorPointer(4, GL_FLOAT, 16*2, 16);
        
        if (clipRays && showEnvelope) {
            //glEnable(GL_BLEND);
            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
            
            
//                    envFloats.put(offset.x + centerOfRotation.x);
//                    envFloats.put(offset.y - centerOfRotation.y);
//                    envFloats.put(150f - offset.z - centerOfRotation.z);
//            Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
//            glTranslatef(-imageTranslation.x, -imageTranslation.y, -imageTranslation.z);
            
//            glTranslatef(0f, 0f, 150f);
            
//            glRotatef(-(float)Math.PI, 1f, 0f, 0f);
            
            glRotatef(-transducerTilt, 1f, 0f, 0f);
            
            glTranslatef(-centerOfRotation.x, centerOfRotation.y, centerOfRotation.z);
            
            
            glDrawArrays(GL_POINTS, 0, 9261);
            
            Main.glPopMatrix();
        }
        Main.glPopAttrib();
    }
    
    private void render2DEnvelope() {
        Main.glPushAttrib(GL_POINT_BIT | GL_ENABLE_BIT);
        
        glPointSize(8f);
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glVertexPointer(4, GL_FLOAT, 16*2, 0);
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo); 
        glColorPointer(4, GL_FLOAT, 16*2, 16);
        
        if (clipRays && showEnvelope) {
            //glEnable(GL_BLEND);
            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();                      
            
//                    envFloats.put(offset.x + centerOfRotation.x);
//                    envFloats.put(offset.y - centerOfRotation.y);
//                    envFloats.put(150f - offset.z - centerOfRotation.z);
//            Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
//            glTranslatef(-imageTranslation.x, -imageTranslation.y, -imageTranslation.z);
            
            
//            glTranslatef(0f, 0f, 150f);
            
//            glRotatef(-(float)Math.PI, 1f, 0f, 0f);
            
//            glRotatef(-transducerTilt, 1f, 0f, 0f);
            
//            glTranslatef(-centerOfRotation.x, centerOfRotation.y, centerOfRotation.z);
            
            
            glDrawArrays(GL_POINTS, 0, 9261);
            
            Main.glPopMatrix();
        }
        Main.glPopAttrib();    
    }
    
    private void setupImageTexture(ImageVolume image, int textureUnit, Vector3f center) {
        setupImageTexture(image, textureUnit, center, new Vector4f(0f, 0f, 0f, 1f));
    }
    
    private void setupImageTexture(ImageVolume image, int textureUnit, Vector3f center, Vector4f offset) {
        if (image == null) return;
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);
        
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_3D);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return;
        
        int textureName = tn.intValue();
        glBindTexture(GL_TEXTURE_3D, textureName);  
        
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;

            // Build transformation of Texture matrix
            ///////////////////////////////////////////
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();

            //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
            //fix voxel scaling
            float xres = image.getDimension(0).getSampleWidth(0);
            float yres = image.getDimension(1).getSampleWidth(1);
            float zres = image.getDimension(2).getSampleWidth(2);
            
            // Translation to the texture volume center (convert 0 -> 1 value range to -0.5 -> 0.5)
            ///////////////////////////////////////////////////////////
//            glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);
//
//            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
//            
//            // Scale for in-plane/out-of-plane ratio
//            ///////////////////////////////////////////////////////////
////            glScaled(1.0f, 1.0f, -zscaleFactor); // this assumes in-plane pixel dimesions are the same! //HACK
//            glScaled(1f /(texWidth * xres), 1f/(texWidth * xres), 1f/(texDepth * zres));
//
//
//            // Translation of center of rotation to origin (mm to texture coord values (0 -> 1))
//            ////////////////////////////////////////////////////////////
            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            if (imageTranslation == null) {
                imageTranslation = new Vector3f();
            }
            
            float[] imagePosition = (float[])image.getAttribute("ImagePosition");
            if (imagePosition == null) {
                imagePosition = new float[3];
            }
            
//            glTranslatef(
////                    (imageTranslation.x)/ (xres * iWidth),
////                    (imageTranslation.y) / (yres * iHeight),
////                    (imageTranslation.z) / (zres * idepth * zscaleFactor));
//                    (-imageTranslation.x),
//                    (-imageTranslation.y),
//                    (-imageTranslation.z) - 150f);

            Quaternion imageOrientation = (Quaternion)image.getAttribute("ImageOrientationQ");
            if (imageOrientation == null) return;
            
            FloatBuffer orientBuffer = BufferUtils.createFloatBuffer(16);
		Trackball.toMatrix4f(imageOrientation).store(orientBuffer);
		orientBuffer.flip();
                
                         
            // Rotation for image orientation
            ////////////////////////////////////////////////////////////
		//glMultMatrix(orientBuffer);
            
            // Rotation for camera view
//            if (trackball != null) {
//                trackball.renderOpposite();
//            }

//            matrixBuf.rewind();
//            glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
//            ctTexMatrix.load(matrixBuf);
            
            ctTexMatrix.setIdentity();
            
            // Final translation to put origin in the center of texture space (0.5, 0.5, 0.5)
            Matrix4f.translate(new Vector3f(0.5f, 0.5f, 0.5f), ctTexMatrix, ctTexMatrix);
                       
            // Scale for image resolution (normalize to texture coordinates 0-1
            Matrix4f.scale(new Vector3f(1.0f/(texWidth*xres), 1.0f/(texHeight*yres), -1.0f/(texDepth*zres)), ctTexMatrix, ctTexMatrix);
                      
            // Rotation
            Matrix4f rotMat = new Matrix4f();
            rotMat.load(orientBuffer);
            Matrix4f.mul(ctTexMatrix, rotMat, ctTexMatrix);
            
            // Translation
            Matrix4f.translate(new Vector3f(
                        (center.x + imageTranslation.x + imagePosition[0]),
                        (center.y + imageTranslation.y + imagePosition[1]),
                        (center.z + imageTranslation.z + imagePosition[2])
                        ),
                    ctTexMatrix, ctTexMatrix);
            
            Matrix4f.rotate((float)Math.PI, new Vector3f(1f, 0f, 0f), ctTexMatrix, ctTexMatrix);
            //Matrix4f.rotate((float)Math.PI, new Vector3f(0f, 1f, 0f), ctTexMatrix, ctTexMatrix);
           
            // add in transducer tilt
            Matrix4f.rotate(transducerTilt/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), ctTexMatrix, ctTexMatrix);
            
            // Translate transducer origin to the transducer face
//            Matrix4f.translate(new Vector3f(0f, 0f, -150f), ctTexMatrix, ctTexMatrix);
            Matrix4f.translate(new Vector3f(offset.x, offset.y, -offset.z), ctTexMatrix, ctTexMatrix);
            
            // store matrix for use later
            matrixBuf.rewind();
            ctTexMatrix.store(matrixBuf);
            matrixBuf.flip();
            

            
            
//            System.out.println(ctTexMatrix);
//            System.out.println(Matrix4f.transform(ctTexMatrix, new Vector4f(0f, 0f, 150f, 1f), null));
//            System.out.println(Matrix4f.transform(ctTexMatrix, new Vector4f(0f, 0f, 0f, 1f), null));
//            System.out.println(Matrix4f.transform(ctTexMatrix, new Vector4f(150f, 0f, 0f, 1f), null));
            
            glMatrixMode(GL_MODELVIEW);
        
        Main.glPopAttrib();
    }
        
    @Override
    public void release()
    {    
        if (posSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(posSSBo);
            posSSBo = 0;
        }
        if (colSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(colSSBo);
            colSSBo = 0;
        }
        if (outSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(outSSBo);
            outSSBo = 0;
        }
        if (distSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(distSSBo);
            distSSBo = 0;
        }
        if (outDiscSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(outDiscSSBo);
            outDiscSSBo = 0;
        }
        if (outRaysSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(outDiscSSBo);
            outRaysSSBo = 0;
        }
        if (envSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(envSSBo);
            envSSBo = 0;
        }
        if (sdrSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(sdrSSBo);
            sdrSSBo = 0;
        }
        if (pressureSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(pressureSSBo);
            pressureSSBo = 0;
        }
        if (phaseSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(phaseSSBo);
            phaseSSBo = 0;
        }
        
        if (refractShader != null) {
            refractShader.release();
            refractShader = null;
        }
        
        if (sdrShader != null) {
            sdrShader.release();
            sdrShader = null;
        }
        
        if (pressureShader != null) {
            pressureShader.release();
            pressureShader = null;
        }
    }

}
