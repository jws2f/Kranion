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


import com.sun.scenario.effect.impl.BufferUtil;
import java.nio.ByteBuffer;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import org.fusfoundation.kranion.model.image.ImageVolume;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author john
 */
public class ImageGradientVolume {
    private static ShaderProgram shader;  
    private int imageVolumeTexture=0;    
    private ImageVolume image = null;
    
    private int pixelOffsets[] = new int[27];
    
    
    public ImageGradientVolume() {
        initShader();
    }
    
    // 3x3x3 Sobel mask offsets and weights
    private static Vector3f offsets[] = {
        new Vector3f(-1, -1, -1), new Vector3f(-1, 0, -1), new Vector3f(-1, 1, -1),
        new Vector3f(-1, -1, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 1, 0),
        new Vector3f(-1, -1, 1), new Vector3f(-1, 0, 1), new Vector3f(-1, 1, 1),
        new Vector3f(0, -1, -1), new Vector3f(0, 0, -1), new Vector3f(0, 1, -1),
        new Vector3f(0, -1, 0), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0),
        new Vector3f(0, -1, 1), new Vector3f(0, 0, 1), new Vector3f(0, 1, 1),
        new Vector3f(1, -1, -1), new Vector3f(1, 0, -1), new Vector3f(1, 1, -1),
        new Vector3f(1, -1, 0), new Vector3f(1, 0, 0), new Vector3f(1, 1, 0),
        new Vector3f(1, -1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 1)
    };

    private static float sqr3 = (float) Math.sqrt(3.0) / 3;
    private static float sqr2 = (float) Math.sqrt(2.0) / 2;

    private static float xGradZH[] = {
        -sqr3, -sqr2, -sqr3,
        -sqr2, -1, -sqr2,
        -sqr3, -sqr2, -sqr3,
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        sqr3, sqr2, sqr3,
        sqr2, 1, sqr2,
        sqr3, sqr2, sqr3
    };

// 3x3x3 Zucker-Hummel weights
    private static float yGradZH[] = {
        -sqr3, 0, sqr3,
        -sqr2, 0, sqr2,
        -sqr3, 0, sqr3,
        -sqr2, 0, sqr2,
        -1, 0, 1,
        -sqr2, 0, sqr2,
        -sqr3, 0, sqr3,
        -sqr2, 0, sqr2,
        -sqr3, 0, sqr3
    };

    private static float zGradZH[] = {
        -sqr3, -sqr2, -sqr3,
        0, 0, 0,
        sqr3, sqr2, sqr3,
        -sqr2, -1, -sqr2,
        0, 0, 0,
        sqr2, 1, sqr2,
        -sqr3, -sqr2, -sqr3,
        0, 0, 0,
        sqr3, sqr2, sqr3
    };

//vec3 gradient_delta = vec3(0.003, 0.003, 0.00375);
    private static Vector3f gradient_delta = new Vector3f(0.0045f, 0.0045f, 0.00689f);

    Vector3f findNormal(short[] imageData, int position) {
        Vector3f grad = new Vector3f();
        for (int i = 0; i < 27; i++) {
            //Vector3f coord = position + (offset[i] * gradient_delta);
            //float s = texture(image_tex, coord).r * 4095.0;
            int indexOffset = position + pixelOffsets[i];
            float s = imageData[indexOffset] & 0xff - 1024;
            grad.x += s * xGradZH[i];
            grad.y += s * yGradZH[i];
            grad.z += s * zGradZH[i];
        }
        return grad;
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
        
        if (Main.OpenGLVersion < 4f) {
            calculateSW();
            return;
        }
                
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
    
    public void calculateSW() {
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
        
        for (int i=0; i<27; i++) {
            Vector3f offset = offsets[i];
            pixelOffsets[i] = (int)offset.x + (int)offset.y * iWidth + (int)offset.z * iWidth * iHeight;
        }
        
        float[] gradients = new float[iWidth * iHeight * iDepth * 4];
        
        FloatBuffer gradArray = BufferUtil.newFloatBuffer(iWidth * iHeight * iDepth * 4);
        
        short idata[] = (short[])image.getData();
        
        for (int x=1; x<iWidth-1; x++) {
            System.out.println("Gradient calc: x = " + x);
            int index = x;
            
            for (int y=1; y<iHeight-1; y++) {
                int index2 = index + y*iWidth;
                
                for (int z=1; z<iDepth-1; z++) {
                    int index3 = index2 + z*iWidth*iHeight;
                                       
                    Vector3f grad = findNormal(idata, index3);
                    
                    gradients[index3*4] = -grad.x;
                    gradients[index3*4+1] = -grad.y;
                    gradients[index3*4+2] = -grad.z;
                    gradients[index3*4+3] = (float)Math.sqrt(grad.x * grad.x + grad.y * grad.y + grad.z * grad.z);
                    
                }
            }
        }
        
        gradArray.put(gradients);
        System.out.println("gradArray size = " + gradArray.capacity());
        gradArray.flip();
        gradients = null;
        
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
                glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA16F, width, height, depth, 0, GL_RGBA, GL_FLOAT, gradArray);
                
                glBindTexture(GL_TEXTURE_3D, 0);
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
                glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA16F, width, height, depth, 0, GL_RGBA, GL_HALF_FLOAT, (ByteBuffer)null);
                
                //glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA16F, width, height, depth);
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
