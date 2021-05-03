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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.fusfoundation.kranion.model.Model;

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
import org.fusfoundation.util.FastFourierTransform;

/**
 *
 * @author john
 */
public class TransducerRayTracer extends Renderable implements Pickable {

    PropertyChangeSupport propertyChangeSupport;
    
    private Vector3f steering = new Vector3f(0f, 0f, 0f);
    
    private float boneThreshold = /*1024f +*/ 700f; // 700 HU threshold for bone
    private static ShaderProgram refractShader;
    private static ShaderProgram sdrShader;
    private static ShaderProgram pressureShader;
    private static ShaderProgram pickShader;
    
    private int posSSBo=0; // buffer of element starting center points
    private int colSSBo=0; // buffer of color data per element
    private int outSSBo=0; // buffer of 6 vertices per element indicating beam path (3 line segments)
    private int distSSBo=0; // buffer containing minimum distance from focal spot per element
    
    private int outDiscSSBo = 0; // buffer of triangle vertices for 'discs' on skull surface
    private int outRaysSSBo = 0; // buffer of lines from element start point to outside of skull
    
    // skull floor strike data
    private int skullFloorRaysSSBo = 0;
    private int skullFloorDiscSSBo = 0;
    
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
    
    private int selectedElement = -1;
    
    private ImageVolume4D envelopeImage = null;
    
    private boolean showEnvelope = false;
    private boolean showSkullFloorStrikes = false;
    
    private int elementCount = 0;
    private int activeElementCount = 0;
    private float sdr = 0f;
    private float avgTransmitCoeff = 0f;
    private float frequency = 650f;
    
    private boolean showPressureEnvelope = false; // show pressure waveform or envelope?
    private float phaseCorrectAmount = 1f; // to vary the amount of phase correction 0 to 1
    
    private boolean needsCalc = true;
    private boolean needsSDRCalc = true;
    private boolean needsPressureCalc = true;
    
    private float normDiscDiam = 1f;
    
    protected Thread myThread;
    
    public TransducerRayTracer() {
        myThread = Thread.currentThread();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    // Observable pattern

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListeners() {
        PropertyChangeListener listeners[] = this.propertyChangeSupport.getPropertyChangeListeners();
        for (int i=0; i<listeners.length; i++) {
            removePropertyChangeListener(listeners[i]);
        }
    }

    private void updateObservers(PropertyChangeEvent e) {
        propertyChangeSupport.firePropertyChange(e);
    }
            
    public void setFrequency(float f_kHz) {
        if (frequency != f_kHz * 1000.0f) {
            frequency = f_kHz * 1000.0f;
            setIsDirty(true);
        }
    }
    
    public int getActiveElementCount() { return activeElementCount; }
    
    public void setPhaseCorrectionAmount(float amount) {
        if (amount != phaseCorrectAmount) {
            phaseCorrectAmount = amount;
            setIsDirty(true);
        }
    }
    
    public boolean getShowEnvelope() { return showEnvelope; }
    public void setShowEnvelope(boolean f) {
        if (showEnvelope != f) {
            setIsDirty(true);
        }
        showEnvelope = f;
    }
    
    public boolean getShowPressureEnvelope() { return showPressureEnvelope; }
    public void setShowPressureEnvelope(boolean f) {
        if (showPressureEnvelope != f) {
            setIsDirty(true);
        }
        showPressureEnvelope = f;
    }
    
    public boolean getShowSkullFloorStrikes() { return showEnvelope; }
    public void setShowSkullFloorStrikes(boolean f) {
        if (showSkullFloorStrikes != f) {
            setIsDirty(true);
        }
        showSkullFloorStrikes = f;
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
//        if (CTimage != image) {
            setIsDirty(true);
//        }
        CTimage = image;
        Float value = (Float) image.getAttribute("RescaleIntercept");
        if (value != null) rescaleIntercept = value;
        value = (Float) image.getAttribute("RescaleSlope");
        if (value != null) rescaleSlope = value;
        
        if (envelopeImage != null) {
            ImageVolumeUtil.releaseTextures(envelopeImage);
            envelopeImage = null;
        }
        
        needsCalc = true;
        needsSDRCalc = true;
    }
    
    public void setTextureRotation(Vector3f rotationOffset, Trackball tb) {
        if (trackball == null || !centerOfRotation.equals(rotationOffset) || trackball.getCurrent() != tb.getCurrent()) {
            setIsDirty(true);
        }
        centerOfRotation = new Vector3f(rotationOffset);
        trackball = tb;
        
        needsCalc = true;
        needsSDRCalc = true;
    }
    
    public float getSDR() { return sdr; }
    public float getAvgTransmitCoeff() { return this.avgTransmitCoeff; }
    public float getBEAMvalue() {
        // from Dylan Beam @ Ohio State
        // These scaling values are derived from the min and max
        // penetration coefficients calculated from the Ohio State
        // patient cohort
        float MinPen = 0.0120f;
        float MaxPen = 0.1049f;
        float BeamValue = (avgTransmitCoeff-MinPen)*(99/(MaxPen-MinPen))+1;
        return BeamValue;
    }
    
    private float transducerTiltX = 0f;
    private float transducerTiltY = 0f;

       // set tilt around x axis in degrees
    public void setTransducerTiltX(float tilt) {
        if (transducerTiltX != tilt) {
            setIsDirty(true);
        }
        transducerTiltX = tilt;
        
        needsCalc = true;
        needsSDRCalc = true;
    }
    
    public void setTransducerTiltY(float tilt) {
        if (transducerTiltY != tilt) {
            setIsDirty(true);
        }
        transducerTiltY = tilt;
        
        needsCalc = true;
        needsSDRCalc = true;
    }
    
    private float boneSpeed = 2652f;//3500f;
    private float boneRefractionSpeed = 2900f;
    
    public void setTargetSteering(float x, float y, float z) { // (0, 0, 0) = no steering
        if (steering.x != x || steering.y != y || steering.z != z) {
            setIsDirty(true);
        }
        steering.set(x, y, z);
        
        needsPressureCalc = true;
    }
    
    public Vector3f getTargetSteering() {
        return steering;
    }
    
    public void setBoneSpeed(float speed) {
        if (boneSpeed != speed) {
            setIsDirty(true);
        }
        boneSpeed = Math.min(3500, Math.max(1482, speed));
        
        needsCalc = true;
    }
    public void setBoneRefractionSpeed(float speed) {
        if (boneRefractionSpeed != speed) {
            setIsDirty(true);
        }
        boneRefractionSpeed = Math.min(3500, Math.max(1482, speed));

        needsCalc = true;      
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
    
    public void clearEnvelopeImage() {
        if (envelopeImage != null) {
            ImageVolumeUtil.releaseTextures(envelopeImage);
            envelopeImage = null;
        }
    }
    
    public void setSelectedElement(int nelement) {
        selectedElement = nelement;
    }
    
    public int getSelectedElement() {
        return selectedElement;
    }
     
    private void initShader() {
        if (refractShader == null) {
            refractShader = new ShaderProgram();
            refractShader.addShader(GL_COMPUTE_SHADER, "shaders/TransducerRayTracer5x5skullfloor.cs.glsl");
            refractShader.compileShaderProgram();
        }
        if (pickShader == null) {
            pickShader = new ShaderProgram();
            pickShader.addShader(GL_COMPUTE_SHADER, "shaders/TransducerRayTracerPick.cs.glsl");
            pickShader.compileShaderProgram();
        }
        if (sdrShader == null) {
            sdrShader = new ShaderProgram();
            sdrShader.addShader(GL_COMPUTE_SHADER, "shaders/sdrAreaShader2.cs.glsl"); // no refraction, only outer skull peak used
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
        
        this.selectedElement = -1;
        
        initShader();
        
        elementCount = trans.getElementCount();

        FloatBuffer floatPosBuffer = ByteBuffer.allocateDirect(elementCount*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        double radiusSum = 0;
        
        for (int i=0; i<elementCount; i++) {
            Vector4f pos;
            pos = new Vector4f(trans.getElement(i));
            
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

            radiusSum += Math.sqrt(trans.getElementArea(i) / Math.PI / 2.0);
        }
        
        this.normDiscDiam = (float)(radiusSum / elementCount / 2); // using half of average element radius for normal disc representation size. TODO: per element radius?
        floatPosBuffer.flip();
        
        
        posSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, posSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        
        
        floatPosBuffer = ByteBuffer.allocateDirect(elementCount * 4*12 * 20*3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<elementCount*3*20; i++) {
            
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
        floatPosBuffer = ByteBuffer.allocateDirect(elementCount * 4*8 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<elementCount*2; i++) {
            
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
        FloatBuffer floatOutBuffer = ByteBuffer.allocateDirect(elementCount*4*4 * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount * 6; i++) {
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
        
        FloatBuffer floatColBuffer = ByteBuffer.allocateDirect(elementCount*4*4 * 6).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount * 6; i++) {
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
        FloatBuffer distBuffer = ByteBuffer.allocateDirect(elementCount*4 *7).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount; i++) {
            //distance from focus
            distBuffer.put(0f);
            // SDR value
            distBuffer.put(0f);
            // Incident angle value
            distBuffer.put(0f);
            // Skull path length value
            distBuffer.put(0f);
            // SDR 2 value
            distBuffer.put(0f);
            // Normal skull thickness value
            distBuffer.put(0f);
            // transmission coeff
            distBuffer.put(0f);
        }
        distBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER,distBuffer,GL_STATIC_DRAW);
        
        sdrSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, sdrSSBo);
        FloatBuffer sdrBuffer = ByteBuffer.allocateDirect(elementCount*4 *(60 + 3)).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount; i++) {
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
        FloatBuffer pressureBuffer = ByteBuffer.allocateDirect(elementCount*4 * 1).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount; i++) {
                 //ct values along transit of skull per element
                pressureBuffer.put(0f);
        }
        pressureBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, pressureBuffer, GL_STATIC_DRAW);
        
        phaseSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, phaseSSBo);
        FloatBuffer phaseBuffer = ByteBuffer.allocateDirect(elementCount*4 * 1).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i<elementCount; i++) {
                 //ct values along transit of skull per element
                phaseBuffer.put(0f);
        }
        phaseBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER,phaseBuffer, GL_STATIC_DRAW);        
        glBindBuffer(GL_ARRAY_BUFFER, 0);


        // Skull floor strike geometry data
        /////
        floatPosBuffer = ByteBuffer.allocateDirect(elementCount * 4*12 * 20*3).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<elementCount*3*20; i++) {
            
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
        
        
        skullFloorDiscSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skullFloorDiscSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_DYNAMIC_DRAW);
 
        // Rays between final beam point and skull floor
        floatPosBuffer = ByteBuffer.allocateDirect(elementCount * 4*8 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        for (int i=0; i<elementCount*2; i++) {
            
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
        
        
        this.skullFloorRaysSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skullFloorRaysSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private void doCalc() {
        
        needsCalc = false;
        
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Void call() {
                    TransducerRayTracer.this.doCalc();
                    return null;
                }
            };

            Main.callCrossThreadCallable(c);

            return;
        }
        
        if (CTimage == null) return;
        
        setupImageTexture(CTimage, 0, centerOfRotation);

//        System.out.println("TransducerRayTracer::doCalc() " + Thread.currentThread().getId() + " - " + myThread.getId());
    
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
        //glUniform3f(targetLoc, steering.x, steering.y, steering.z);
        glUniform3f(targetLoc, 0, 0, 0); // just natural focus
        
//        System.out.println("doCalc: steering = " + steering);
        
        texLoc = glGetUniformLocation(shaderProgID, "selectedElement");
        glUniform1i(texLoc, this.selectedElement);
        
        texLoc = glGetUniformLocation(shaderProgID, "elementCount");
        glUniform1i(texLoc, this.elementCount);
        
        texLoc = glGetUniformLocation(shaderProgID, "normDiscDiam");
        glUniform1f(texLoc, normDiscDiam);
       
        
//        int targetLoc = glGetUniformLocation(shaderprogram, "target");
//        glUniform3f(targetLoc, 0f, 0f, 300f);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, outDiscSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, outRaysSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, phaseSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, skullFloorRaysSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 8, skullFloorDiscSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(elementCount / 256 + 1, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 8, 0);

        
        refractShader.stop();
        
        this.updateObservers(new PropertyChangeEvent(this, "rayCalc", null, true));        
    }
    
    private void doPickCalc() {
        pickShader.start();
        
        int shaderProgID = pickShader.getShaderProgramID();
        
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
        
        texLoc = glGetUniformLocation(shaderProgID, "elementCount");
        glUniform1i(texLoc, this.elementCount);
        
        texLoc = glGetUniformLocation(shaderProgID, "normDiscDiam");
        glUniform1f(texLoc, normDiscDiam);
       
//        int targetLoc = glGetUniformLocation(shaderprogram, "target");
//        glUniform3f(targetLoc, 0f, 0f, 300f);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, colSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, outDiscSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, outRaysSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, phaseSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, skullFloorRaysSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 8, skullFloorDiscSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(elementCount / 256 + 1, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 5, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 6, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 7, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 8, 0);

        
        pickShader.stop();
        
//        this.updateObservers(new PropertyChangeEvent(this, "rayCalc", null, null));        
    }
    
   public void doSDRCalc() {
        // Calculate per element SDR data
        //
        // doCalc() must be called first to compute the per element ray segments
        //

        needsSDRCalc = false;
        
        if (needsCalc) {
            doCalc();
        }
        
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Void call() {
                    TransducerRayTracer.this.doSDRCalc();
                    return null;
                }
            };

            Main.callCrossThreadCallable(c);

            return;
        }
        
//        System.out.println("TransducerRayTracer::doSDRCalc() " + Thread.currentThread().getId() + " - " + myThread.getId());
        
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
        texLoc = glGetUniformLocation(shaderProgID, "elementCount");
        glUniform1i(texLoc, this.elementCount);
        
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, sdrSSBo);
        
        // have the SDR shader recolor rays and skull strike discs for any elements that hit air
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, colSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, outDiscSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(elementCount / 256 + 1, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, 0);
        
        sdrShader.stop();
        
        this.updateObservers(new PropertyChangeEvent(this, "sdrCalc", null, true));        
    }
    
    public void doPressureCalc(Vector4f samplePointOffset) {
     // Calculate per element SDR data
         //
         //doCalc(); // must be called first to compute the per element ray segments
         //
         
         if (needsCalc) {
             doCalc();
         }
         
        pressureShader.start();
        
        int shaderProgID = pressureShader.getShaderProgramID();
        int uniformLoc = glGetUniformLocation(shaderProgID, "sample_point");
        glUniform3f(uniformLoc, samplePointOffset.x, samplePointOffset.y, samplePointOffset.z);// + 150f);
        
        uniformLoc = glGetUniformLocation(shaderProgID, "steering");
        glUniform3f(uniformLoc, steering.x, steering.y, steering.z);// + 150f);
        
        uniformLoc = glGetUniformLocation(shaderProgID, "boneSpeed");
        glUniform1f(uniformLoc, this.boneSpeed);
//TEST        glUniform1f(uniformLoc, 2640f);
        uniformLoc = glGetUniformLocation(shaderProgID, "waterSpeed");
        glUniform1f(uniformLoc, 1482f);
        int paramLoc = glGetUniformLocation(shaderProgID, "elementCount");
        glUniform1i(paramLoc, this.elementCount);
        
        paramLoc = glGetUniformLocation(shaderProgID, "phaseCorrectAmount");
        glUniform1f(paramLoc, phaseCorrectAmount);
        
        paramLoc = glGetUniformLocation(shaderProgID, "frequency");
        glUniform1f(paramLoc, frequency);
                
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, distSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, pressureSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 4, phaseSSBo);
                    
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(elementCount / 256 + 1, 1, 1);
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
    
    
    public void calcSkullFloorDensity() {
        if (this.CTimage == null) return;

        int xsize = CTimage.getDimension(0).getSize() / 2;
        int ysize = CTimage.getDimension(1).getSize() / 2;
        int zsize = CTimage.getDimension(2).getSize() / 2;

        float xres = CTimage.getDimension(0).getSampleWidth(0) * 2f;
        float yres = CTimage.getDimension(1).getSampleWidth(1) * 2f;
        float zres = CTimage.getDimension(2).getSampleWidth(2) * 2f;

        Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
        Quaternion imageRotation = (Quaternion)CTimage.getAttribute("ImageOrientationQ");

        if (envelopeImage != null) {
            ImageVolumeUtil.releaseTexture(envelopeImage);
        }

        envelopeImage = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, xsize, ysize, zsize, 1);
        envelopeImage.getDimension(0).setSampleWidth(xres);
        envelopeImage.getDimension(1).setSampleWidth(yres);
        envelopeImage.getDimension(2).setSampleWidth(zres);

        envelopeImage.getDimension(0).setSampleSpacing(xres);
        envelopeImage.getDimension(1).setSampleSpacing(yres);
        envelopeImage.getDimension(2).setSampleSpacing(zres);

        envelopeImage.setAttribute("ImageTranslation", new Vector3f(imageTranslation));
        envelopeImage.setAttribute("ImageOrientationQ", new Quaternion(imageRotation));
        
        // setup textures
        
        // need to compile shader in init

// TODO: work on integration of skull floor footprints        
//            shader.start();
//
//            int uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_intercept");
//            glUniform1f(uloc, this.rescaleIntercept);
//            uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_slope");
//            glUniform1f(uloc, this.rescaleSlope);
//
//            // run compute shader
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//            org.lwjgl.opengl.GL43.glDispatchCompute((xsize+7)/8, (ysize+7)/8, (zsize+7)/8);
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//
//            // Clean up
//            shader.stop();
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
        Matrix4f.rotate(-transducerTiltX/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), transducerTiltMat, transducerTiltMat);
        Matrix4f.rotate(transducerTiltY/180f*(float)Math.PI, new Vector3f(0f, 1f, 0f), transducerTiltMat, transducerTiltMat);

        
        if (CTimage == null) return;
        Vector3f imageTranslation = (Vector3f)CTimage.getAttribute("ImageTranslation");
        
        envelopeImage.setAttribute("ImageTranslation", new Vector3f());
        
        // 21x21x21 array of floats to hold sampled treatment envelope
        FloatBuffer envFloats = ByteBuffer.allocateDirect(9261*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        float[] voxels = (float[])envelopeImage.getData();
        
        int totalElements = this.elementCount;
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
                     
                     float activeRatio = (float)elemCount/totalElements;
                    if (activeRatio >= 0.68f /*700*/) {
                        envFloats.put(0f);
                        envFloats.put(1f);
                        envFloats.put(0f);
                        envFloats.put(0.6f);
                    }
                    else if (activeRatio >= 0.49f /*500*/) {
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
        
        if (envSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(envSSBo);
            envSSBo = glGenBuffers();
        }
        
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glBufferData(GL_ARRAY_BUFFER, envFloats, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public float[] getChannelSkullSamples(int channel) {
//            glBindBuffer(GL_ARRAY_BUFFER, this.sdrSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatSamples = dists.asFloatBuffer();
            FloatBuffer floatSamples = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, sdrSSBo, GL_READ_WRITE);
            int count = 0;
            
            float[] huOut = new float[63];
            
            for (int i=0; i<huOut.length; i++) {
                huOut[i] = floatSamples.get(channel * 63 + i);
            }
        
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
            
            return huOut;
    } 
    
    public void writeSkullMeasuresFile(File outFile) {

        if (outFile != null) {
            List<Double> speeds = getSpeeds();
            
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            int count = 0;

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(outFile));
                writer.write("Skull parameters");
                writer.newLine();
                writer.write("NumberOfChannels = " + elementCount);
                writer.newLine();
                writer.newLine();
                writer.write("channel\tsdr\tsdr5x5\tincidentAngle\tskull_thickness\trefracted_skull_path_length\tSOS\ttransCoeff");
                writer.newLine();
                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    float speed = CTSoundSpeed.lookupSpeed(speeds.get(count).floatValue() + 1000f); // add 1000 to get apparent density

                    writer.write(count + "\t");
                    writer.write(String.format("%1.3f", sdr) + "\t");
                    writer.write(String.format("%1.3f", sdr2) + "\t");
                    writer.write(String.format("%3.3f", incidentAngle) + "\t");
                    writer.write(String.format("%3.3f", normSkullThickness) + "\t");
                    writer.write(String.format("%3.3f", skullThickness) + "\t");
                    writer.write(String.format("%3.3f", speed) + "\t");
                    writer.write(String.format("%3.6f", transmCoeff) + "\t");
                    writer.newLine();
                    count++;
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
    }
    
    private FloatBuffer getFloatBufferThreadSafe(int bufferType, int bufferName, int access) {
        if (Thread.currentThread() == myThread) {
            glBindBuffer(bufferType, bufferName);
            ByteBuffer byteBuffer = glMapBuffer(bufferType, access, null);
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            glUnmapBuffer(bufferType);
            glBindBuffer(bufferType, 0);
            
            return floatBuffer;
        }
        else {
            
            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                  public FloatBuffer call() {
                    return  TransducerRayTracer.this.getFloatBufferThreadSafe(bufferType, bufferName, access);
                  }
            };
            
            return (FloatBuffer)Main.callCrossThreadCallable(c);

        }        
    }
    
    public FloatBuffer getChannelPhases() {
            // force pressure calc so we have phase information
            doPressureCalc(new Vector4f(steering.x, steering.y, steering.z, 1));
                        
            return getFloatBufferThreadSafe(GL_ARRAY_BUFFER, phaseSSBo, GL_READ_WRITE);
    }
    
    public FloatBuffer getChannelActive() {
            // force raytracer calc so we have valid data
            doCalc();
                        
            return getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE); // -1 values mean inactive
    }
    
    public void writeACTFile(File outFile) {
        if (outFile != null) {
            doCalc();
            doPressureCalc(new Vector4f());
//            doPressureCalc(new Vector4f(steering.x, steering.y, steering.z, 1));
            
//            glBindBuffer(GL_ARRAY_BUFFER, phaseSSBo);
//            ByteBuffer phases = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = phases.asFloatBuffer();
            int count = 0, zeroCount = 0;
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
            
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, phaseSSBo, GL_READ_WRITE);

//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatDist = dists.asFloatBuffer();
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
            
            FloatBuffer floatDist = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
           
            
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(outFile));
                writer.write("[AMPLITUDE_AND_PHASE_CORRECTIONS]");
                writer.newLine();
                writer.write("NumberOfChannels = " + elementCount);
                writer.newLine();
                writer.newLine();
                while (floatPhases.hasRemaining()) {
                    double phase = (double) floatPhases.get() % (2.0 * Math.PI);
                    
                    float dist = floatDist.get();
                    float sdr = floatDist.get();
                    float incidentAngle = floatDist.get();
                    float skullThickness = floatDist.get();
                    float sdr2 = floatDist.get();
                    float normSkullThickness = floatDist.get();
                    float transmCoeff = floatDist.get();

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
                    if (dist == -1) {
                        writer.write("0\t");
                    } else {
                        writer.write("1\t");
                    }
                    writer.write(String.format("%1.4f", phase));
                    writer.newLine();
                }
//                System.out.println("zero phase channels = " + zeroCount);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public void writeACTFileForWorkstation(File outFile) {
        if (outFile != null) {
            doCalc();
            doPressureCalc(new Vector4f());
//            doPressureCalc(new Vector4f(steering.x, steering.y, steering.z, 1));
            
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, phaseSSBo, GL_READ_WRITE);
            
            FloatBuffer floatDist = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);

            int count = 0, zeroCount = 0;
            
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(outFile));
                writer.write("[Table Header]");
                writer.newLine();
                writer.write("Name=KranionGeneratedACT");
                writer.newLine();
                writer.write("ID=1");
                writer.newLine();
                writer.write("XdSerialNumber=7026");
                writer.newLine();
                writer.write(";LoadedType=' optional value: '0' for overwrite ACT, '1' for turned off elements, '2' for scaling final power\n");
                writer.newLine();
                writer.write("LoadedType=0");
                writer.newLine();
                writer.write("NumberOfElements=" + elementCount);
                writer.newLine();
                writer.write("[Elements]");
                writer.newLine();
                while (floatPhases.hasRemaining()) {
                    double phase = (double) floatPhases.get() % (2.0 * Math.PI);
                    
                    float dist = floatDist.get();
                    float sdr = floatDist.get();
                    float incidentAngle = floatDist.get();
                    float skullThickness = floatDist.get();
                    float sdr2 = floatDist.get();
                    float normSkullThickness = floatDist.get();
                    float transmCoeff = floatDist.get();
                    
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
                    writer.write("Element" + count + "=");
                    count++;
                    if (dist == -1.0f) {
                        writer.write("0.000000\t");
                    } else {
                        writer.write("1.000000\t");
                    }
                    writer.write(String.format("%1.6f", phase));
                    writer.newLine();
                }
//                System.out.println("zero phase channels = " + zeroCount);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    
    public class RayData {
        public Vector3f outerNormal;
        public Vector3f rayVerts[];
    }
    
    public RayData[] getRayData() {
        doCalc();
        
        RayData result[] = new RayData[this.elementCount];
        
//        glBindBuffer(GL_ARRAY_BUFFER, this.outSSBo);
//        ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//        FloatBuffer floatValues = dists.asFloatBuffer();
//        glUnmapBuffer(GL_ARRAY_BUFFER);
//        glBindBuffer(GL_ARRAY_BUFFER, 0);        
          FloatBuffer floatValues = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, outSSBo, GL_READ_ONLY);

//        glBindBuffer(GL_ARRAY_BUFFER, this.outDiscSSBo);
//        ByteBuffer buf = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//        FloatBuffer floatDiscValues = buf.asFloatBuffer();
//        glUnmapBuffer(GL_ARRAY_BUFFER);
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
        FloatBuffer floatDiscValues = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, outDiscSSBo, GL_READ_ONLY);  
        
//        System.out.println("TransducerRayTracer::getRayData()");
//        System.out.println(floatValues.capacity());
//        System.out.println(floatDiscValues.capacity());
        
        for (int i=0; i<this.elementCount; i++) {
            
            // get first triangle in surface strike disc to compute normal
            int offset = i*12*3*20; // 12 floats per vertex * 3 per triangle * 20 triangles per disk
            Vector3f center = new Vector3f(floatDiscValues.get(offset + 0), floatDiscValues.get(offset + 1), floatDiscValues.get(offset + 2));
            Vector3f first = new Vector3f(floatDiscValues.get(offset + 12 + 0), floatDiscValues.get(offset + 12 + 1), floatDiscValues.get(offset + 12 + 2));
            Vector3f second = new Vector3f(floatDiscValues.get(offset + 24 + 0), floatDiscValues.get(offset + 24 + 1), floatDiscValues.get(offset + 24 + 2));
            
            Vector3f normal = Vector3f.cross(Vector3f.sub(second, center, null), Vector3f.sub(first, center, null), null);
            if (normal.lengthSquared() > 0f) {
                normal.normalise();
            }                    
            
            result[i] = new RayData();
            result[i].outerNormal = normal;
            result[i].rayVerts = new Vector3f[4];
            
            // element center
            result[i].rayVerts[0] = new Vector3f(floatValues.get(), floatValues.get(), floatValues.get());
            floatValues.get();
            
            // outer skull surface intercept
            result[i].rayVerts[1] = new Vector3f(floatValues.get(), floatValues.get(), floatValues.get());
            floatValues.get();
            
            //skip repeated outer skull surface intercept
            floatValues.get();
            floatValues.get();
            floatValues.get();
            floatValues.get();
            
            // inner skull surface intercept
            result[i].rayVerts[2] = new Vector3f(floatValues.get(), floatValues.get(), floatValues.get());
            floatValues.get();
            
            //skip repeated inner skull surface intercept
            floatValues.get();
            floatValues.get();
            floatValues.get();
            floatValues.get();
            
            // nearest focal point intercept
            result[i].rayVerts[3] = new Vector3f(floatValues.get(), floatValues.get(), floatValues.get());
            floatValues.get();
        }
        
        
        return result;
    }
    
    public List<Double> getIncidentAngles() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);       

            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist > 0 && incidentAngle >= 0f) {
                        result.add((double)incidentAngle);
                    }                    
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    
    public List<Double> getIncidentAnglesFull() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);  
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    result.add((double)incidentAngle);
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }

    
    public List<Double> getSDRs() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)sdr);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    
    // include -1 for all inactive elements
    public List<Double> getSDRsFull() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)sdr);
                    }
                    else {
                        result.add(-1.0);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }

    public List<Double> getSDR2s() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)sdr2);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }

    @Override
    public boolean getIsDirty() {
        boolean result = super.getIsDirty(); //To change body of generated methods, choose Tools | Templates.
       
        return result;
    }
    
    public List<Double> getSkullThicknesses() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)skullThickness);
                    }
                    else {
                        result.add(-1.0);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    public List<Double> getNormSkullThicknesses() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)normSkullThickness);
                    }
                    else {
                        result.add(-1.0);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
        
    // this list will contain sdr = -1 for inactive elements
    public List<Double> getSDR2sFull() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)sdr2);
                    }
                    else {
                        result.add(-1.0);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    // this list will contain sdr = -1 for inactive elements
    public List<Double> getTransmissionCoeff() {

        List<Double> result = new ArrayList<>();
        
//            glBindBuffer(GL_ARRAY_BUFFER, this.distSSBo);
//            ByteBuffer dists = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//            FloatBuffer floatPhases = dists.asFloatBuffer();
            FloatBuffer floatPhases = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);
            
            int count = 0;

                while (floatPhases.hasRemaining()) {
                    float dist = floatPhases.get();
                    float sdr = floatPhases.get();
                    float incidentAngle = floatPhases.get();
                    float skullThickness = floatPhases.get();
                    float sdr2 = floatPhases.get();
                    float normSkullThickness = floatPhases.get();
                    float transmCoeff = floatPhases.get();
                    
                    if (dist >= 0f) {
                        result.add((double)transmCoeff);
                    }
                    else {
                        result.add(-1.0);
                    }
                }
                
//            glUnmapBuffer(GL_ARRAY_BUFFER);
//            glBindBuffer(GL_ARRAY_BUFFER, 0);        
        
        return result;
    }
    
    public List<Double> getSpeeds() {

        List<Double> result = new ArrayList<>();
        
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER,sdrSSBo);
//        ByteBuffer sdrOutput = glMapBuffer(GL_SHADER_STORAGE_BUFFER,GL_READ_ONLY,null);
//        FloatBuffer huValues = sdrOutput.asFloatBuffer();
        FloatBuffer huValues = this.getFloatBufferThreadSafe(GL_SHADER_STORAGE_BUFFER, sdrSSBo, GL_READ_WRITE);
        
        float[] huOut = new float[60];
        float[] huIndices = new float[3];
        
        for (int i=0; i<elementCount; i++) {
            huValues.get(huOut);
            huValues.get(huIndices);
            
            int left = 0;
            int right = 59;
            
            for (int x=0; x<59; x++) {
                if (huOut[x] >= 700f) {
                    left = x;
                    break;
                }
            }
            
            for (int x=59; x>=0; x--) {
                if (huOut[x] >= 700f) {
                    right = x;
                    break;
                }
            }
            
            int count = 0;
            float sum = 0f;
            for (int x=left; x<=right; x++) {
                count++;
                sum += huOut[x];
            }
            
            if (count > 0) {
                result.add((double)sum/(double)count);
            }
            else {
                result.add(-1.0);
            }
        }
        
//        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        
        return result;
    }
    
    public void calcPressureEnvelope(Quaternion pressureFieldOrientation) {
        
        if (CTimage == null) return;
        
        float voxelsize = 1f;

        int volumeHalfWidthX = 15;
        int volumeHalfWidthY = 15;
        int volumeHalfWidthZ = 0;
        int volumeWidthX = 2*volumeHalfWidthX+1;
        int volumeWidthY = 2*volumeHalfWidthY+1;
        int volumeWidthZ = 2*volumeHalfWidthZ+1;
        
        if (envelopeImage != null) {
            ImageVolumeUtil.releaseTextures(envelopeImage);
        }
            
//        if (envelopeImage == null) {
            
            envelopeImage = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, volumeWidthX, volumeWidthY, volumeWidthZ, 1);
            envelopeImage.getDimension(0).setSampleWidth(voxelsize);
            envelopeImage.getDimension(1).setSampleWidth(voxelsize);
            envelopeImage.getDimension(2).setSampleWidth(1f);
            
            envelopeImage.getDimension(0).setSampleSpacing(voxelsize);
            envelopeImage.getDimension(1).setSampleSpacing(voxelsize);
            envelopeImage.getDimension(2).setSampleSpacing(1f);
//        }
        
        Vector4f offset = new Vector4f();
        
        envelopeImage.setAttribute("ImageTranslation", new Vector3f(-centerOfRotation.x - steering.x, -centerOfRotation.y + steering.y, -centerOfRotation.z + steering.z));
        if (pressureFieldOrientation == null) {
            pressureFieldOrientation = new Quaternion();
        }
        
        Quaternion r1 = new Quaternion();
        r1.setFromAxisAngle(new Vector4f(1, 0, 0, 3.14159f)); // flip around the x-axis
        Quaternion r2 = new Quaternion();
        r2.setFromAxisAngle(new Vector4f(0, 1, 0, 3.14159f)); // flip around the y-axis
 
        envelopeImage.setAttribute("ImageOrientationQ", pressureFieldOrientation);
        
        Quaternion tmpq = new Quaternion(pressureFieldOrientation);
        
        Quaternion rotX = new Quaternion();
        rotX.setFromAxisAngle(new Vector4f(1, 0, 0, transducerTiltX/180f*(float)Math.PI));
        Quaternion.mul(tmpq, rotX, tmpq);
        
        Quaternion rotY = new Quaternion();
        rotY.setFromAxisAngle(new Vector4f(0, 1, 0, transducerTiltY/180f*(float)Math.PI));
        Quaternion.mul(tmpq, rotY, tmpq);
        
        Matrix4f pfoMat = Trackball.toMatrix4f(tmpq.negate(null));
        
                
        float[] voxels = (float[])envelopeImage.getData();
        
        this.colormap_min = Float.MAX_VALUE;
        this.colormap_max = -Float.MAX_VALUE;
                
//        setupImageTexture(CTimage, 0, new Vector3f(centerOfRotation.x, centerOfRotation.y, centerOfRotation.z), new Vector4f(0f, 0f, 0f, 1f)/*offset*/);
        setupImageTexture(CTimage, 0, centerOfRotation);

        doCalc();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
        
        for (int i = -volumeHalfWidthX; i <= volumeHalfWidthX; i++) {
            for (int j = -volumeHalfWidthY; j <= volumeHalfWidthY; j++) {
                for (int k = -volumeHalfWidthZ; k <= volumeHalfWidthZ; k++) {
                    float pressure = 0f;

                    offset.set(-i * voxelsize, -j * voxelsize, -k * voxelsize, 1f);
                                        
                    Matrix4f.transform(pfoMat, new Vector4f(offset), offset);
                    
                    //TODO: seems fishy. check coord system and xfrm
//                    offset.z = -offset.z;
                    offset.x = -offset.x;
                    
                    offset.x += steering.x;
                    offset.y += steering.y;
                    offset.z += steering.z;
//                                        
//                        pressure = calcSamplePressure(new Vector3f(0, 0, 0), offset);
                        pressure = calcSamplePressure(offset);
    //                    pressure = Math.abs(pressure);
    //                    pressure *= pressure;
                    

                    if (pressure > colormap_max) {
                        colormap_max = pressure;
                    }
                    if (pressure < colormap_min) {
                        colormap_min = pressure;
                    }

                    voxels[(i + volumeHalfWidthX) + (j + volumeHalfWidthY) * volumeWidthX + (k + volumeHalfWidthZ) * volumeWidthX * volumeWidthY] = pressure;
                    
                }
            }
        }
        
        if (showPressureEnvelope) {
            //// Hilbert transform madness
            ///////////////////////////////

            double[] realRow = new double[volumeWidthX];
            double[] imagRow = new double[volumeWidthX];

            double realImage[] = new double[volumeWidthX * volumeWidthY];
            double imagImage[] = new double[volumeWidthX * volumeWidthY];

            // transform rows
            for (int y = 0; y < volumeWidthY; y++) {
                for (int x = 0; x < volumeWidthX; x++) {
                    realRow[x] = voxels[y * volumeWidthX + x];
                    imagRow[x] = 0.0;
                }
                FastFourierTransform.transform(realRow, imagRow);
                for (int x = 0; x < volumeWidthX; x++) {
                    realImage[y * volumeWidthX + x] = realRow[x];
                    imagImage[y * volumeWidthX + x] = imagRow[x];
                }
            }

            double[] realCol = new double[volumeWidthY];
            double[] imagCol = new double[volumeWidthY];

            // transform cols
            for (int x = 0; x < volumeWidthX; x++) {
                for (int y = 0; y < volumeWidthY; y++) {
                    realCol[y] = realImage[y * volumeWidthX + x];
                    imagCol[y] = imagImage[y * volumeWidthX + x];
                }
                FastFourierTransform.transform(realCol, imagCol);
                for (int y = 0; y < volumeWidthY; y++) {
                    realImage[y * volumeWidthX + x] = realCol[y];
                    imagImage[y * volumeWidthX + x] = imagCol[y];
                }
            }

            // hilbert transform (really Analytic Signal mask)
            // Assumes width and height are odd valued for now
            for (int x = 0; x < volumeWidthX; x++) {
                for (int y = 0; y < volumeWidthY; y++) {

                    int index = y * volumeWidthX + x;

                    // Mask values
                    double real_coeff = 0.0;
                    double imag_coeff = 0.0;

// //Best result so far with spectrum truncation
//                if (x==0 && y==0) {
//                    real_coeff = 1.0;
//                }
//                else if (x<(volumeWidthX+1)/2 && y<(volumeWidthY+1)/2) {
//                    real_coeff = 2.0;
//                }
//                else if (x>=(volumeWidthX+1)/2 && y<(volumeWidthY+1)/2) {
//                    real_coeff = 2.0;
//                }
                    // spiral signum operator
                    //
                    // Larkin, Bone, Oldfield, "Natural demodulation of two-dimension fringe
                    // patterns. I. General background of the spiral phase quadrature transform",
                    // J. Opt. Soc. Am. A/Vol. 18 No. 8/August 2001.
                    //
                    double dx = x < (volumeWidthX + 1) / 2 ? x : x - volumeWidthX;
                    double dy = y < (volumeWidthY + 1) / 2 ? y : y - volumeWidthY;
                    double smag = Math.sqrt(dx * dx + dy * dy);

                    if (x == 0 && y == 0) {
                        real_coeff = 0;
                        imag_coeff = 0;
                    } else {
                        real_coeff = dx / smag;
                        imag_coeff = dy / smag;
                    }

                    // FFT values
                    double r = realImage[index];
                    double i = imagImage[index];

                    // complex multiply mask value with FFT value
                    realImage[index] = (real_coeff * r - imag_coeff * i);
                    imagImage[index] = (real_coeff * i + imag_coeff * r);
                }
            }

            // inv transform rows
            for (int y = 0; y < volumeWidthY; y++) {
                for (int x = 0; x < volumeWidthX; x++) {
                    realRow[x] = realImage[y * volumeWidthX + x];
                    imagRow[x] = imagImage[y * volumeWidthX + x];
                }
                FastFourierTransform.inverseTransform(realRow, imagRow);
                for (int x = 0; x < volumeWidthX; x++) {
                    realImage[y * volumeWidthX + x] = realRow[x] / volumeWidthX;
                    imagImage[y * volumeWidthX + x] = imagRow[x] / volumeWidthX;
                }
            }

            // inv transform cols
            for (int x = 0; x < volumeWidthX; x++) {
                for (int y = 0; y < volumeWidthY; y++) {
                    realCol[y] = realImage[y * volumeWidthX + x];
                    imagCol[y] = imagImage[y * volumeWidthX + x];
                }
                FastFourierTransform.inverseTransform(realCol, imagCol);
                for (int y = 0; y < volumeWidthY; y++) {
                    realImage[y * volumeWidthX + x] = realCol[y] / volumeWidthY;
                    imagImage[y * volumeWidthX + x] = imagCol[y] / volumeWidthY;
                }
            }

            this.colormap_min = Float.MAX_VALUE;
            this.colormap_max = -Float.MAX_VALUE;

            for (int x = 0; x < volumeWidthX; x++) {
                for (int y = 0; y < volumeWidthY; y++) {
                    int index = y * volumeWidthX + x;

                    double realVal = realImage[index];
                    double imagVal = imagImage[index];

                    double sigval = voxels[index];

//                 float mag = (float)Math.sqrt(sigval*sigval + hilbertval*hilbertval);
                    float mag = (float) Math.abs(sigval) + (float) Math.sqrt(realVal * realVal + imagVal * imagVal);
//                 float mag = (float)hilbertval;
//mag = (float)(sigval);

                    if (mag > colormap_max) {
                        colormap_max = mag;
                    } else if (mag < colormap_min) {
                        colormap_min = mag;
                    }
//                else {
//                    System.out.println("Bad mag = " + mag);
//                }

                    voxels[index] = mag;
                }
            }

            ///////////////////////////////
            //
        }
        
        // normalize the pressure field
        float colormap_range = (colormap_max - colormap_min);// / 500000f;
System.out.println("pressure maximum = " + colormap_range);
//colormap_range = 45f;
        for (int index=0; index<voxels.length; index++) {

                    float value = voxels[index];
                    voxels[index] = (value - colormap_min) / colormap_range;
        }
       
        System.out.println("Pressure max = " + colormap_max + ", min = " + colormap_min);

    }
        
    public void calc2DEnvelope() {
        Vector4f offset = new Vector4f();
        Matrix4f transducerTiltMat = new Matrix4f();
        transducerTiltMat.setIdentity();
        Matrix4f.rotate(-transducerTiltX/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), transducerTiltMat, transducerTiltMat);
        Matrix4f.rotate(transducerTiltY/180f*(float)Math.PI, new Vector3f(0f, 1f, 0f), transducerTiltMat, transducerTiltMat);

        
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
                    
//                    System.out.print("" + elemCount + " ");
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
//            System.out.println("");
        }
        envFloats.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, envSSBo);
        glBufferData(GL_ARRAY_BUFFER, envFloats, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private int calcElementsOn() {
        return calcElementsOn(centerOfRotation, new Vector4f());
    }
    
    private int calcElementsOn(Vector4f offset) {
        return calcElementsOn(centerOfRotation, offset);
    }
    
    private int calcElementsOn(Vector3f center, Vector4f offset) {
        
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Integer call() {
                    return (Integer)TransducerRayTracer.this.calcElementsOn(center, offset);
                }
            };

            return (int)Main.callCrossThreadCallable(c);
        }

        if (CTimage == null) {
            return 0;
        }
        
        setupImageTexture(CTimage, 0, center, offset);

        doCalc();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);

        ///glMapBuffer
//        glBindBuffer(GL_ARRAY_BUFFER, distSSBo);
//        ByteBuffer distances = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//        FloatBuffer floatDistances = distances.asFloatBuffer();
        FloatBuffer floatDistances = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, distSSBo, GL_READ_WRITE);

            int numberOn = 0;
//        float sdrSum = 0f;
        while (floatDistances.hasRemaining()) {
            float value = floatDistances.get();
            float sdrval = floatDistances.get(); // TODO
            floatDistances.get(); // incidence angle
            floatDistances.get(); // skull thickness
            float sdrval2 = floatDistances.get(); // TODO
            float normSkullThickness = floatDistances.get();
            float transmCoeff = floatDistances.get();
            if (value > 0) {
                numberOn++;
//                sdrSum += sdr;
            }
        }
        
//        System.out.println("SDR = " + sdrSum/numberOn);
        
//        glUnmapBuffer(GL_ARRAY_BUFFER);
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        return numberOn;
    }
    
    private float calcSamplePressure(Vector4f offset) {
        if (CTimage == null) {
            return 0;
        }

//        setupImageTexture(CTimage, 0, center, new Vector4f(0f, 0f, 0f, 1f)/*offset*/);
//
//        doCalc();
        float totalPressure = 0f;

        doPressureCalc(offset);

//        glBindBuffer(GL_ARRAY_BUFFER, pressureSSBo);
//        ByteBuffer pressures = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
//        FloatBuffer floatPressures = pressures.asFloatBuffer();
        FloatBuffer floatPressures = this.getFloatBufferThreadSafe(GL_ARRAY_BUFFER, pressureSSBo, GL_READ_WRITE);

        while (floatPressures.hasRemaining()) {
            totalPressure += floatPressures.get();
        }
        
//        glUnmapBuffer(GL_ARRAY_BUFFER);
//        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return totalPressure;
    }
    
    @Override
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);
                
        if (CTimage == null) return;
        
//        setupImageTexture(CTimage, 0, centerOfRotation);

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
//        
//        
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
//        System.out.print("CH, ");
//        for (int i=0; i<1024; i++) {
//            System.out.print(i);
//            if (i<1023) {
//                System.out.print(", ");
//            }
//        }
//        System.out.println();
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
                float sdr2 = floatDistances.get();
                float normSkullThickness = floatDistances.get();
                float transmCoeff = floatDistances.get();
        	if (value > 0)
        	{
                    distanceNum++;
                    distanceSum += value;
                    numberOn++;
        	}
        }
        float percentOn = numberOn/(float)elementCount;
        float mean = distanceSum/distanceNum;
        floatDistances.rewind();
        float diffSqSum = 0.0f;
        float sdrSum = 0.0f;
        float sdrCount = 0.0f;
        float   transmitCoeffSum = 0f;
        int     transmitCoeffCount = 0;
        while (floatDistances.hasRemaining())
        {

        	float value = floatDistances.get();
                float sdr = floatDistances.get();
                float incidentAngle = floatDistances.get();
                float skullThickness = floatDistances.get();
                float sdr2 = floatDistances.get();
                float normSkullThickness = floatDistances.get();
                float transmitCoeff = floatDistances.get();
        	if (value > 0)
        	{
        		diffSqSum += (float) Math.pow(value-mean,2);
                        sdrSum += sdr2;
                        sdrCount += 1.0f;
        	}
                if (value > 0 && incidentAngle < 20f) {
                    transmitCoeffCount++;
                    transmitCoeffSum += transmitCoeff;
                }
                else {
                    transmitCoeffCount++; // transmitCoeff is zero for this element
                }
        }
        activeElementCount = numberOn;
        
        this.sdr = sdrSum/sdrCount; //distanceNum;
        this.avgTransmitCoeff = transmitCoeffSum/(float)transmitCoeffCount;
        float stDev = (float)Math.sqrt(diffSqSum/distanceNum);
//        System.out.println("Average: "+mean);
//        System.out.println("Std Dev: "+stDev);
//        System.out.println("% Within 3 mm of focus: "+(percentOn*100) + "(" + numberOn + " of 1024)");
//        System.out.println("SDR: " + sdrSum/distanceNum);
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
//        glTranslatef(0, 0, 150);

        glTranslatef(-steering.x, -steering.y, -steering.z);
        
        glRotatef(this.transducerTiltX, 1, 0, 0);
        glRotatef(-this.transducerTiltY, 0, 1, 0);
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
                glDrawArrays(GL_POINTS, 0, elementCount);
            }
            
            // Draw inner skull points
        glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
        glVertexPointer(4, GL_FLOAT, 16*6, 16*3);
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
        glColorPointer(4, GL_FLOAT, 16*6, 16*3);
        
        if (clipRays) {
            glDrawArrays(GL_POINTS, 0, elementCount);
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
                glDrawArrays(GL_LINES, 0, elementCount*6);
//                glDrawArrays(GL_LINES, 0, 6); // to draw one element ray path for testing
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
                glDrawArrays(GL_TRIANGLES, 0, elementCount*3*20);
                
                
                // skull floor normal discs
                if (showSkullFloorStrikes) {
                    glBindBuffer(GL_ARRAY_BUFFER, skullFloorDiscSSBo);
                    glVertexPointer(4, GL_FLOAT, 12*4, 0);

                    glBindBuffer(GL_ARRAY_BUFFER, skullFloorDiscSSBo);
                    glNormalPointer(GL_FLOAT, 12*4, 16);

                    glBindBuffer(GL_ARRAY_BUFFER, skullFloorDiscSSBo);
                    glColorPointer(4, GL_FLOAT, 12*4, 32);

                    glEnableClientState(GL_VERTEX_ARRAY);
                    glEnableClientState(GL_COLOR_ARRAY);
                    glEnableClientState(GL_NORMAL_ARRAY);
                    glDrawArrays(GL_TRIANGLES, 0, elementCount*3*20);
                }
                
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
                    glDrawArrays(GL_LINES, 0, elementCount*2);
                
                if (showSkullFloorStrikes) {
                    // skull floor strike rays
                    ////////
                    glBindBuffer(GL_ARRAY_BUFFER, this.skullFloorRaysSSBo);
                    glVertexPointer(4, GL_FLOAT, 8*4, 0);

                    glBindBuffer(GL_ARRAY_BUFFER, skullFloorRaysSSBo);
                    glColorPointer(4, GL_FLOAT, 8*4, 16);

                    glEnableClientState(GL_VERTEX_ARRAY);
                    glEnableClientState(GL_COLOR_ARRAY);

                    glEnable(GL_BLEND);
                    glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
                    glDrawArrays(GL_LINES, 0, elementCount*2);
                }
                    
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
            
            glRotatef(-transducerTiltX, 1f, 0f, 0f);
            glRotatef(-transducerTiltY, 0f, 1f, 0f);
            
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
        if (image == null) {
            
            // if there is no image for this texture unit, zero it out
            glEnable(GL_TEXTURE_3D);
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            
            glBindTexture(GL_TEXTURE_3D, 0);
            
            glActiveTexture(GL_TEXTURE0 + 0);
            glDisable(GL_TEXTURE_3D);

            return;
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);
        
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_3D);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) {
            ImageVolumeUtil.buildTexture(image);
            tn = (Integer) image.getAttribute("textureName");
            if (tn == null) {
                Logger.getGlobal().log(Level.WARNING, "TransducerRayTracer.setupImageTexture: textureName not found.");
                return;
            }
        } // TODO: this should be handled when the 3D texture doesn't exist for some reason (i.e. after some image filter)
        
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
                        (center.x + imageTranslation.x /* + imagePosition[0] */),
                        (center.y + imageTranslation.y /* + imagePosition[1] */),
                        (center.z + imageTranslation.z /* + imagePosition[2] */)
                        ),
                    ctTexMatrix, ctTexMatrix);
            
            Matrix4f.rotate((float)Math.PI, new Vector3f(1f, 0f, 0f), ctTexMatrix, ctTexMatrix);
            //Matrix4f.rotate((float)Math.PI, new Vector3f(0f, 1f, 0f), ctTexMatrix, ctTexMatrix);
           
            // add in transducer tilt
            Matrix4f.rotate(transducerTiltX/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), ctTexMatrix, ctTexMatrix);
            Matrix4f.rotate(-transducerTiltY/180f*(float)Math.PI, new Vector3f(0f, 1f, 0f), ctTexMatrix, ctTexMatrix);
            
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
    
    public Matrix4f getCTMatrix() {
        return ctTexMatrix;
    }
    
    public FloatBuffer getCTMatrixFloatBuffer() {
        return matrixBuf;
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
        if (skullFloorDiscSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(skullFloorDiscSSBo);
            skullFloorDiscSSBo = 0;
        }
        if (skullFloorRaysSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(skullFloorRaysSSBo);
            skullFloorRaysSSBo = 0;
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
        
        clearEnvelopeImage();
    }

    @Override
    public void renderPickable() {
        
        if (!getVisible()) return;
                
        if (CTimage == null) return;
        
        setupImageTexture(CTimage, 0, centerOfRotation);
        
        doPickCalc();
        
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
        
        


        glTranslatef(-steering.x, -steering.y, -steering.z);
        
        glRotatef(this.transducerTiltX, 1, 0, 0);
        glRotatef(-this.transducerTiltY, 0, 1, 0);
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
                glDrawArrays(GL_POINTS, 0, elementCount);
            }
            
            // Draw inner skull points
        glBindBuffer(GL_ARRAY_BUFFER, outSSBo);
        glVertexPointer(4, GL_FLOAT, 16*6, 16*3);
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
        glColorPointer(4, GL_FLOAT, 16*6, 16*3);
        
        if (clipRays) {
            glDrawArrays(GL_POINTS, 0, elementCount);
        }

       
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
                glDrawArrays(GL_LINES, 0, elementCount*6);
            }
            /////////////////////////////////////////////
            //
            }
            
            // Draw skull strike discs
            if (!clipRays) {

            Main.glPushAttrib(GL_LIGHTING_BIT);
            
                glDisable(GL_LIGHTING);

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glVertexPointer(4, GL_FLOAT, 12*4, 0);

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glNormalPointer(GL_FLOAT, 12*4, 16);

                glBindBuffer(GL_ARRAY_BUFFER, outDiscSSBo);
                glColorPointer(4, GL_FLOAT, 12*4, 32);

                glEnableClientState(GL_VERTEX_ARRAY);
                glEnableClientState(GL_COLOR_ARRAY);
                glEnableClientState(GL_NORMAL_ARRAY);
                glDrawArrays(GL_TRIANGLES, 0, elementCount*3*20);
                
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

                    glDisable(GL_BLEND);
                    glDrawArrays(GL_LINES, 0, elementCount*2);
                
                Main.glPopAttrib();

            }
       
        Main.glPopAttrib();
               
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);   
    }

}
