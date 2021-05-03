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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import org.lwjgl.util.vector.*;

/**
 *
 * @author john
 */
public class MutualInformation {
    private ImageVolume CTimage, MRimage;
    
    public FloatBuffer ctMatrixBuf = BufferUtils.createFloatBuffer(16);
    public FloatBuffer mrMatrixBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f ctTexMatrix = new Matrix4f();
    public Matrix4f mrTexMatrix = new Matrix4f();
    private float canvasFOV=250f;
    private int sampleCount=0;
    public float ha = 0f;
    public float hb = 0f;
    public float hab = 0f;
    
    private static ShaderProgram miShaderBlur = null;
    private static ShaderProgram miShader = null;
    
    private int samplePointSSBo=0; // vec4 sampling points
    private int histogramsSSBo=0; // int histogram bins
    
    // need arrays for joint and marginal histograms
    // bin count
    
    public MutualInformation(ImageVolume ct, ImageVolume mr) {
        setImageVolumes(ct, mr);
    }
    
    public void setImageVolumes(ImageVolume ct, ImageVolume mr) {
        CTimage = ct;
        MRimage = mr;
    }
    
    public void release() {
        if (samplePointSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(samplePointSSBo);
            samplePointSSBo = 0;            
        }
        if (histogramsSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(histogramsSSBo);
            histogramsSSBo = 0;            
        }
    }
    
    private void initBuffers(float percentSample) {
    
        if (CTimage == null || MRimage == null) return;
        
        // choose the smaller size image volume to pick percent sampling size
        
        int imagesize1 = ((short[])(CTimage.getData())).length;
        int imagesize2 = ((short[])(MRimage.getData())).length;
        
        int smallestSize = imagesize1 < imagesize2 ? imagesize1 : imagesize2;
        
        // input sampling point array
        int size = (int)(smallestSize * (percentSample / 100f)); // subsampling of the volume for building histograms
        
        if (sampleCount != size) {
            sampleCount = size;
            release();
        }
        
        if (samplePointSSBo == 0) {
//            System.out.println("Generating sampling points for joint histogram: " + sampleCount);
            Random rnd = new Random(314159);
            FloatBuffer floatPosBuffer = ByteBuffer.allocateDirect(sampleCount * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

            for (int i = 0; i < sampleCount; i++) {

                // randomly sample texture space -0.5 -> 0.5 in all three directions
                floatPosBuffer.put((float)(rnd.nextFloat() - 0.5));
                floatPosBuffer.put((float)(rnd.nextFloat() - 0.5));
                floatPosBuffer.put((float)(rnd.nextFloat() - 0.5));
                floatPosBuffer.put(1.0f);

            }
            floatPosBuffer.flip();

            samplePointSSBo = glGenBuffers();
                
            glBindBuffer(GL_ARRAY_BUFFER, samplePointSSBo);
            glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        }
        
        
        // 50x50 joint histogram and 2 marginal histograms
        int bufferSize = ( (50 * 50) + (2 * 50) );
        IntBuffer intHistogramBuffer = ByteBuffer.allocateDirect(4 * bufferSize).order(ByteOrder.nativeOrder()).asIntBuffer();
        
        for (int i = 0; i < bufferSize; i++) {

            intHistogramBuffer.put(0);

        }
        intHistogramBuffer.flip();

        if (histogramsSSBo == 0) {
            histogramsSSBo = glGenBuffers();
        }
        glBindBuffer(GL_ARRAY_BUFFER, histogramsSSBo);
        glBufferData(GL_ARRAY_BUFFER, intHistogramBuffer, GL_DYNAMIC_DRAW);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public float calcMI(float percentSample, boolean blur) {
        
        long startTime = System.currentTimeMillis();
        
        float result = 0f;
        // do stuff
        
        initShader();
        
        initBuffers(percentSample);
        
        setupImageTexture(CTimage, 0, ctTexMatrix, ctMatrixBuf);
        setupImageTexture(MRimage, 1, mrTexMatrix, mrMatrixBuf);
        
        ShaderProgram shaderToUse = blur ? miShaderBlur : miShader;
        
        shaderToUse.start();
        
        // Pass texture info
        int texLoc = glGetUniformLocation(shaderToUse.shaderProgramID, "ct_tex");
        glUniform1i(texLoc, 0);
        
        texLoc = glGetUniformLocation(shaderToUse.shaderProgramID, "mr_tex");
        glUniform1i(texLoc, 1);
        
        texLoc = glGetUniformLocation(shaderToUse.shaderProgramID, "sampleCount");
        glUniform1i(texLoc, sampleCount);
       

        int texMatLoc = glGetUniformLocation(shaderToUse.shaderProgramID, "ct_tex_matrix");
        glUniformMatrix4(texMatLoc, false, ctMatrixBuf);
        texMatLoc = glGetUniformLocation(shaderToUse.shaderProgramID, "mr_tex_matrix");
        glUniformMatrix4(texMatLoc, false, mrMatrixBuf);
        
        int width = CTimage.getDimension(0).getSize();
        int height = CTimage.getDimension(1).getSize();
        int depth = CTimage.getDimension(2).getSize();

        float xres = CTimage.getDimension(0).getSampleWidth(0);
        float yres = CTimage.getDimension(1).getSampleWidth(0);
        float zres = CTimage.getDimension(2).getSampleWidth(0);
        
        int uloc = glGetUniformLocation(shaderToUse.shaderProgramID, "voxelSize");
        glUniform3f(uloc, xres, yres, zres);
        uloc = glGetUniformLocation(shaderToUse.shaderProgramID, "imageVolumeSize");
        glUniform3f(uloc, width, height, depth);

        // Bind our output buffer
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, samplePointSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, histogramsSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(sampleCount / 512 + 1, 1, 1); // TODO: fix work group size
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        
        //unbind buffer and image
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        
        // Clean up
        shaderToUse.stop();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glBindTexture(GL_TEXTURE_3D, 0);
        glDisable(GL_TEXTURE_3D);
        
        glBindBuffer(GL_ARRAY_BUFFER, histogramsSSBo);
        ByteBuffer histograms = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_ONLY, null);
        IntBuffer histValues = histograms.asIntBuffer();

        ha = 0f;
        hb = 0f;
        hab = 0f;
        
        //Compute marginal histograms
        int am[] = new int[50];
        int bm[] = new int[50];
        for (int i=0; i<50; i++) {
            for (int j=0; j<50; j++) {
                am[i] += histValues.get(i + j*50);
                bm[i] += histValues.get(i*50 + j);
            }
        }
        
        
        for (int i=0; i<50; i++) {
//            System.out.println("marginal hist " + i + "\t" + histValues.get(50*50+i) + "\t" + histValues.get(50*50+50+i));
            float a = am[i];//histValues.get(50*50+i);
            float b = bm[i];//histValues.get(50*50+50+i);
            
            if (a>0) ha -= a*Math.log(a);
            if (b>0) hb -= b*Math.log(b);
            
            for (int j=0; j<50; j++) {
                a = histValues.get(i+j*50);
                if (a>0) {
                    hab -= a*Math.log(a);
                }
            }
        }
//        System.out.println("HA: " + -ha);
//        System.out.println("HB: " + -hb);
//        System.out.println("HAB: " + -hab);
//        System.out.println("normMI: " + (-ha +  -hb)/-hab);

        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        //result = ha + hb - hab; //(ha +  hb)/hab;
        result = -(ha +  hb)/hab;
        
        Logger.getGlobal().log(Level.INFO, "MI calc time = " + (System.currentTimeMillis() - startTime) + "ms");
        
        return result;
    }
    
    private void initShader() {
        if (miShaderBlur == null) {
            miShaderBlur = new ShaderProgram();
            miShaderBlur.addShader(GL_COMPUTE_SHADER, "shaders/MutualInformation.cs.glsl");
            miShaderBlur.compileShaderProgram();
        }
        if (miShader == null) {
            miShader = new ShaderProgram();
            miShader.addShader(GL_COMPUTE_SHADER, "shaders/MutualInformation2.cs.glsl");
            miShader.compileShaderProgram();
        }
    }

    private void setupImageTexture(ImageVolume image, int textureUnit, Matrix4f texMat, FloatBuffer texMatBuffer) {
        if (image == null) {
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

        if (tn != null) {

            int textureName = tn.intValue();
            glBindTexture(GL_TEXTURE_3D, textureName);

            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;

            //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
            //fix voxel scaling
            float xres = image.getDimension(0).getSampleWidth(0);
            float yres = image.getDimension(1).getSampleWidth(0);
            float zres = image.getDimension(2).getSampleWidth(0);

            glMatrixMode(GL_TEXTURE);
            glLoadIdentity(); // Not sure this is necessary for a compute shader
            
            // Translation to the texture volume center (convert 0 -> 1 value range to -0.5 -> 0.5)
            ///////////////////////////////////////////////////////////
            // glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);

//            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
            //canvasSize = 200f;
            float xzoom = canvasFOV / (xres * texWidth);
            float yzoom = canvasFOV / (yres * texHeight);
            float zzoom = -canvasFOV / (zres * texDepth);

            // Scale for in-plane/out-of-plane ratio
            ///////////////////////////////////////////////////////////
            //glScaled(1.0f, 1.0f, -zscaleFactor); // this assumes in-plane pixel dimesions are the same! //HACK
            // Canvas is 250mm wide
            //glScaled(1f/(xres*texWidth/200f), 1f/(yres*texHeight/200f), -zscaleFactor * 1f/(xres*texWidth/200f)); 
            //glScaled(xzoom, yzoom, zzoom); 
            Quaternion imageOrientation = (Quaternion) image.getAttribute("ImageOrientationQ");
            if (imageOrientation == null) {
                return;
            }
            
//            System.out.println("Image Orient: " + imageOrientation);

            FloatBuffer orientBuffer = BufferUtils.createFloatBuffer(16);
            Trackball.toMatrix4f(imageOrientation).store(orientBuffer);
            orientBuffer.flip();

            // Translation of center of rotation to origin (mm to texture coord values (0 -> 1))
            ////////////////////////////////////////////////////////////
            Vector3f imageTranslation = (Vector3f) image.getAttribute("ImageTranslation");
            if (imageTranslation == null) {
                imageTranslation = new Vector3f();
            }
            float[] imagePosition = (float[]) image.getAttribute("ImagePosition");
            if (imagePosition == null) {
                imagePosition = new float[3];
            }

            texMat.setIdentity();

            // Final translation to put origin in the center of texture space (0.5, 0.5, 0.5)
            Matrix4f.translate(new Vector3f(0.5f, 0.5f, 0.5f), texMat, texMat);

            // Scale for image resolution (normalize to texture coordinates 0-1
            Matrix4f.scale(new Vector3f(xzoom, yzoom, zzoom), texMat, texMat);

            // Rotation
            Matrix4f rotMat = new Matrix4f();
            rotMat.load(orientBuffer);
            Matrix4f.mul(texMat, rotMat, texMat);

            // Translation
            Matrix4f.translate(new Vector3f(
                    (imageTranslation.x /* + imagePosition[0] */)/canvasFOV,
                    (imageTranslation.y /* + imagePosition[1] */)/canvasFOV,
                    (imageTranslation.z /* + imagePosition[2] */)/canvasFOV
            ),
                    texMat, texMat);

            // store matrix for use later
            texMatBuffer.rewind();
            texMat.store(texMatBuffer);
            texMatBuffer.flip();

            glMatrixMode(GL_MODELVIEW);
        }
        else {
            Logger.getGlobal().log(Level.WARNING, "MutualInformation.setupImageTexture: textureName not found.");
        }

        Main.glPopAttrib();
    }
    
    public void updateTestImage(ImageVolume image) {
        glBindBuffer(GL_ARRAY_BUFFER, histogramsSSBo);
        ByteBuffer histograms = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_ONLY, null);
        IntBuffer histValues = histograms.asIntBuffer();

        float[] pixels = (float[])image.getData();
        for (int x=0; x<50; x++) {
            for (int y=0; y<50; y++) {
                pixels[x + y*50] = (float)(histValues.get(x + y*50)/32768.0f);
            }
        }
        
        ImageVolumeUtil.releaseTextures(image);
        
        glUnmapBuffer(GL_ARRAY_BUFFER);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

    }
    
    public static Quaternion toQuaternion(float pitch, float roll, float yaw) {
        Quaternion q = new Quaternion();

        // Abbreviations for the various angular functions
        float cy = (float)Math.cos(yaw * 0.5);
        float sy = (float)Math.sin(yaw * 0.5);
        float cr = (float)Math.cos(roll * 0.5);
        float sr = (float)Math.sin(roll * 0.5);
        float cp = (float)Math.cos(pitch * 0.5);
        float sp = (float)Math.sin(pitch * 0.5);

        q.w = cy * cr * cp + sy * sr * sp;
        q.x = cy * sr * cp - sy * cr * sp;
        q.y = cy * cr * sp + sy * sr * cp;
        q.z = sy * cr * cp - cy * sr * sp;
        
        return q;
    }
}
