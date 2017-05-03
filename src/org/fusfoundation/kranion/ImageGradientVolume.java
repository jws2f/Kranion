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

import java.nio.IntBuffer;
import org.fusfoundation.kranion.model.image.ImageVolume;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

/**
 *
 * @author john
 */
public class ImageGradientVolume {
    private static ShaderProgram shader;  
    private int imageVolumeTexture=0;    
    private ImageVolume image = null;
    
    
    public ImageGradientVolume() {
        initShader();
    }
    
    public void calculate() {
        if (image == null) return;
                
        int iWidth = image.getDimension(0).getSize();
        int iHeight = image.getDimension(1).getSize();
        int iDepth = image.getDimension(2).getSize();
        
        float xres = image.getDimension(0).getSampleWidth(0);
        float yres = image.getDimension(1).getSampleWidth(1);
        float zres = image.getDimension(2).getSampleWidth(2);
                
        Integer imageTextureName = (Integer) image.getAttribute("textureName");
        Integer gradientTextureName = (Integer) image.getAttribute("gradientTexName");
        
        if (imageTextureName == null) return; //TODO: should prob throw exception
        
        // we already have a gradient texture built, return
        if (gradientTextureName != null && gradientTextureName > 0) return;
                
        buildTexture(image);
        setupImageTexture(image, 0, 1);
        
        
        //glUseProgram(shaderprogram);
        int shaderID = shader.getShaderProgramID();
        shader.start();
        
        // Pass texture info
        int texLoc = glGetUniformLocation(shaderID, "image_tex");
        glUniform1i(texLoc, 0);
        
        int uloc = glGetUniformLocation(shaderID, "width");
        glUniform1f(uloc, iWidth);
        uloc = glGetUniformLocation(shaderID, "height");
        glUniform1f(uloc, iHeight);
        uloc = glGetUniformLocation(shaderID, "depth");
        glUniform1f(uloc, iDepth);
        
        uloc = glGetUniformLocation(shaderID, "voxelSize");
        glUniform3f(uloc, xres, yres, zres);
        

        // Bind our output buffer
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        
        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        
        
        // Clean up
        //glUseProgram(0);
        shader.stop();
        
        glActiveTexture(GL_TEXTURE0 + 1);
        glBindTexture(GL_TEXTURE_3D, 0);
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
    
    private void releaseTexture() {
        if (imageVolumeTexture != 0) {
            glDeleteTextures(imageVolumeTexture);
            imageVolumeTexture = 0;               
        }
    }
    
    public void release() {
        releaseTexture();
        shader.release();
        shader = null;
    }
    
    public void setImage(ImageVolume image) {
        if (this.image == image) return;
        this.image = image;
    }

    private void initShader() {
        if (shader == null) {
            shader = new ShaderProgram();
            shader.addShader(GL_COMPUTE_SHADER, "shaders/ImageGradientVolume.cs.glsl");
            shader.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
    }
    
    private void buildTexture(ImageVolume image) {

        if (image != null) {
            //System.out.println("ImageCanvas3D::build texture..");

            Integer tn = (Integer) image.getAttribute("gradientTexName");

//            System.out.println("   textureName = " + tn);
            if (tn != null && tn > 0) {
                System.out.println("Got previously built gradient texture = " + tn);
            } else {

                //System.out.println("build new texture");
                ByteBuffer buf = ByteBuffer.allocateDirect(4);
                IntBuffer texName = buf.asIntBuffer();

                releaseTexture();

                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                glGenTextures(texName);
                int textureName = texName.get(0);

                glBindTexture(GL_TEXTURE_3D, textureName);

                image.setAttribute("gradientTexName", textureName);

                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

                int pixelType = image.getVoxelType();
                int width = image.getDimension(0).getSize();
                int height = image.getDimension(1).getSize();
                int depth = image.getDimension(2).getSize();

                System.out.println("  building 16bit gradient texture");

                //ShortBuffer pixelBuf = (tmp.asShortBuffer());
                //glTexImage3D(GL_TEXTURE_3D, 0, GL_INTENSITY16, texWidth, texHeight, texDepth, 0, GL_LUMINANCE, GL_SHORT, pixelBuf);
                //glTexImage3D(GL_TEXTURE_3D, 0, GL_ALPHA16, texWidth, texHeight, texDepth, 0, GL_ALPHA, GL_SHORT, pixelBuf);
                glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA16F, width, height, depth);
            }
            int value;
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_WIDTH);
            System.out.println("Gradient Texture Width = " + value);
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_HEIGHT);
            System.out.println("Gradient Texture Height = " + value);
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_DEPTH);
            System.out.println("Gradient Texture Depth = " + value);

            glBindTexture(GL_TEXTURE_3D, 0);
        }

    }
        
    private void setupImageTexture(ImageVolume image, int imageTextureUnit, int gradientTextureUnit) {
        if (image == null) return;
        
        Integer imageTextureName = (Integer) image.getAttribute("textureName");
        
        if (imageTextureName == null) return;
        
        Integer gradientTextureName = (Integer) image.getAttribute("gradientTexName");
        
        if (gradientTextureName == null) return;
           
        glBindImageTexture(gradientTextureUnit, gradientTextureName, 0, true, 0, GL_WRITE_ONLY, GL_RGBA16F);
        
        glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);
        
            glActiveTexture(GL_TEXTURE0 + imageTextureUnit);
            glEnable(GL_TEXTURE_3D);
            glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);


            glBindTexture(GL_TEXTURE_3D, imageTextureName);  

            // Build transformation of Texture matrix
            ///////////////////////////////////////////
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glMatrixMode(GL_MODELVIEW);
        
        glPopAttrib();
    }        
}
