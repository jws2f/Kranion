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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author john
 */
public class ImageHistogram {
    
//    public static int shaderprogram = 0;
    ShaderProgram shaderprogram;
    
    private ImageVolume image = null;
    private int outSSBo=0;
    
    
    public ImageHistogram() {
        
    }
    
    private void init() {
                
        release();
        
        initShader();
        
        // Create a buffer of ints to hold output histogram
        IntBuffer intOutBuffer = ByteBuffer.allocateDirect(4096*4).order(ByteOrder.nativeOrder()).asIntBuffer();
        for (int i=0; i<4096; i++) {
            //color
            intOutBuffer.put(0);
        }       
        intOutBuffer.flip();
        
        outSSBo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, outSSBo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, intOutBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }
    
    public void calculate() {
        if (image == null) return;
        
        init();
        
        int iWidth = image.getDimension(0).getSize();
        int iHeight = image.getDimension(1).getSize();
        int iDepth = image.getDimension(2).getSize();
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) {
            ImageVolumeUtil.buildTexture(image);
            tn = (Integer) image.getAttribute("textureName");
        }
        
        if (tn == null) return; //TODO: should prob throw exception
                
        setupImageTexture(image, 0);
        
        shaderprogram.start();
        
        // Pass texture info
//        int texLoc = glGetUniformLocation(shaderprogram, "image_tex");
//        glUniform1i(texLoc, 0);
        
//        int uloc = glGetUniformLocation(shaderprogram, "width");
//        glUniform1f(uloc, iWidth);
//        uloc = glGetUniformLocation(shaderprogram, "height");
//        glUniform1f(uloc, iHeight);
//        uloc = glGetUniformLocation(shaderprogram, "depth");
//        glUniform1f(uloc, iDepth);

        // Bind our output buffer
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        
        //unbind buffer and image
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        
        // Clean up
        shaderprogram.stop();
        
        glActiveTexture(GL_TEXTURE0 + 0);
        glBindTexture(GL_TEXTURE_3D, 0);
        glDisable(GL_TEXTURE_3D);
       
//        // Get the output histogram data from the shader buffer object
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER,outSSBo);
//        ByteBuffer histogram = glMapBuffer(GL_SHADER_STORAGE_BUFFER,GL_READ_ONLY,null);
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
//        
//        IntBuffer histogramBins = histogram.asIntBuffer();
//
//        int bin = 0;
//        int total = 0;
//        while (histogramBins.hasRemaining())
//        {
//        	int value = histogramBins.get();
//                //if (value > 0) {
//                    System.out.println(bin + "\t" + value);
//                //}
//                bin++;
//                total += value;
//        }
//        
//        System.out.println("\nTotal histogram votes: " + total);
//        System.out.println("Should be: " + (iWidth * iHeight * iDepth));
        
    }
    
    public IntBuffer getData() {
        if (outSSBo != 0) {
            // Get the output histogram data from the shader buffer object
            glBindBuffer(GL_SHADER_STORAGE_BUFFER,outSSBo);
            ByteBuffer histogram = glMapBuffer(GL_SHADER_STORAGE_BUFFER,GL_READ_ONLY,null);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            if (histogram != null) {            
                return histogram.asIntBuffer();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    public void release() {
        if (outSSBo != 0) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(outSSBo);
            outSSBo = 0;
        }
        
        if (shaderprogram != null) {
            shaderprogram.deleteShaderProgram();
            shaderprogram = null;
        }
    }
    
    public void setImage(ImageVolume image) { this.image = image; }
    
    private void initShader() {
        
        // only need to compile shader once
        if (shaderprogram != null) return;
        
        shaderprogram = new ShaderProgram();
        shaderprogram.addShader(GL_COMPUTE_SHADER, "shaders/Histogram.cs.glsl");
        shaderprogram.compileShaderProgram();         
    }
    
    private void setupImageTexture(ImageVolume image, int textureUnit) {
        if (image == null) return;
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return;
        
        int textureName = tn.intValue();
       
        glBindImageTexture(0, textureName, 0, true, 0, GL_READ_ONLY, GL_R16);
        
//        glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);
//        
//        glActiveTexture(GL_TEXTURE0 + textureUnit);
//        glEnable(GL_TEXTURE_3D);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
//        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//        
//       
//        glBindTexture(GL_TEXTURE_3D, textureName);  
//        
//        // Build transformation of Texture matrix
//        ///////////////////////////////////////////
//        glMatrixMode(GL_TEXTURE);
//        glLoadIdentity();
//
//            
//        glMatrixMode(GL_MODELVIEW);
//        
//        glPopAttrib();
    }    
}
