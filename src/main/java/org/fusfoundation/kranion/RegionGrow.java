/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.fusfoundation.kranion.model.image.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_DEPTH;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.GL_R16;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL42.GL_ATOMIC_COUNTER_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL30.GL_R16UI;
import static org.lwjgl.opengl.GL30.GL_R8UI;
import static org.lwjgl.opengl.GL43.GL_TEXTURE_BUFFER_SIZE;
/**
 *
 * @author john
 */
public class RegionGrow {
    private ImageVolume4D src;
    private int maskVolumeTexture = 0;
    private int maskChannel = -1;
    
    private LinkedList<Seed> growQ = new LinkedList<>();
    
    private static ShaderProgram shader, shader2, shader3, dilateShader;  
    
    private boolean useGpuAcceleration = true;
    
    private class Seed {
        public int x, y, z;
        public Seed(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    public RegionGrow() {
        initShader();
    }
    
    public RegionGrow(ImageVolume4D image) {
        setSourceImage(image);
    }
    
    public void setUseGPUAcceleration(boolean value) {
        useGpuAcceleration = value;
    }
    public boolean getUseGPUAcceleration() {
        return useGpuAcceleration;
    }
    
    public void setSourceImage(ImageVolume4D image) {
        src = image;
        
//        if (src != null) {
//            src.addChannel(ImageVolume.UBYTE_VOXEL);
//        }
        
//        growQ.add(thing); // push
//        thing = growQ.remove() // pop

    }
    
    public void grow(int x, int y, int z) {
                        
        if (useGpuAcceleration) {
            gpu_calculate();
            return;
        }
        
        growQ.addLast(new Seed(x, y, z));
         
        maskChannel = src.addChannel(org.fusfoundation.kranion.model.image.ImageVolume.UBYTE_VOXEL);
        
        short[] voxels = (short[])src.getData(0);
        byte[] mask = (byte[])src.getData(maskChannel);
        
        int xsize = src.getDimension(0).getSize();
        int ysize = src.getDimension(1).getSize();
        int zsize = src.getDimension(2).getSize();
        int framesize = xsize*ysize;
        
        Float intercept = (Float)src.getAttribute("RescaleIntercept");
        if (intercept == null) { intercept = 0f; }
        
        Float slope = (Float)src.getAttribute("RescaleSlope");
        if (slope == null) { slope = 1f; }
        
        System.out.println("RegionGrow: " + slope + ", " + intercept);
        
        System.out.println("RegionGrow: growing " + xsize + "x" + ysize + "x" + zsize);
        
        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;
        
        int count = 0;
        while (!growQ.isEmpty()) {
            Seed current = (Seed) growQ.removeFirst();
            
            if ((count++ % 1000) == 0) {
                System.out.println("queue sizse = " + growQ.size() + "(" + current.x + ", " + current.y + ", " + current.z + ")");
            }

            if ( current.x >= 0 && current.x < xsize
              && current.y >= 0 && current.y < ysize
              && current.z >= 0 && current.z < zsize) {
                
                int offset = current.x + current.y * xsize + current.z * framesize;
                
                float value = slope * (voxels[offset] + intercept);// & 0x0fff;
                int maskVal = mask[offset] & 0xff;
                
            if (((count-1) % 1000) == 0) {
                System.out.println("seed val = " + value);
            }
                
                if (value < minVal) minVal = value;
                if (value > maxVal) maxVal = value;
                
                if (maskVal == 0 && value > -250f) {
                    mask[offset] = 1;
                    
                    growQ.addLast(new Seed(current.x + 1, current.y, current.z));
                    growQ.addLast(new Seed(current.x - 1, current.y, current.z));
                    growQ.addLast(new Seed(current.x, current.y + 1, current.z));
                    growQ.addLast(new Seed(current.x, current.y - 1, current.z));
                    growQ.addLast(new Seed(current.x, current.y, current.z + 1));
                    growQ.addLast(new Seed(current.x, current.y, current.z - 1));
                }
            }
        }
        System.out.println("RegionGrow: range " + minVal + " - " + maxVal);
        
        System.out.println("RegionGrow: dilating mask");
        
        for (int xc = 1; xc < xsize-1; xc++) {
            for (int yc = 1; yc < ysize-1; yc++) {
                for (int zc = 1; zc < zsize-1; zc++) {
                    
                    int offset = xc + yc * xsize + zc * framesize;
                    
                    int value = mask[offset] & 0xff;
                    if ((value & 0x01) != 0) {
                        mask[offset+1] |= 0x02;
                        mask[offset-1] |= 0x02;
                        mask[offset+xsize] |= 0x02;
                        mask[offset-xsize] |= 0x02;
                        mask[offset+framesize] |= 0x02;
                        mask[offset-framesize] |= 0x02;
                    }
                }
            }
        }
        for (int xc = 1; xc < xsize-1; xc++) {
            for (int yc = 1; yc < ysize-1; yc++) {
                for (int zc = 1; zc < zsize-1; zc++) {
                    
                    int offset = xc + yc * xsize + zc * framesize;
                    
                    int value = mask[offset] & 0xff;
                    if ((value & 0x03) != 0) {
                        mask[offset+1] |= 0x04;
                        mask[offset-1] |= 0x04;
                        mask[offset+xsize] |= 0x04;
                        mask[offset-xsize] |= 0x04;
                        mask[offset+framesize] |= 0x04;
                        mask[offset-framesize] |= 0x04;
                    }
                }
            }
        }
        
        System.out.println("RegionGrow: clearing background");
        for (int i = 0; i < framesize * zsize; i++) {
            int value = mask[i] & 0xff;
            if (value == 0) {
                voxels[i] = 0;
            }
        }
                
        // clean up
        ImageVolumeUtil.releaseTexture(src);
        
        growQ.clear();
        
        src.freeChannel(maskChannel);
        
        System.out.println("RegionGrow: done");
    }
        
    private void initShader() {
        if (shader == null) {
            shader = new ShaderProgram();
            shader.addShader(GL_COMPUTE_SHADER, "shaders/RegionGrowIterate.cs.glsl");
            shader.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
        if (shader2 == null) {
            shader2 = new ShaderProgram();
            shader2.addShader(GL_COMPUTE_SHADER, "shaders/RegionGrowIterate2.cs.glsl");
            shader2.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
        if (shader3 == null) {
            shader3 = new ShaderProgram();
            shader3.addShader(GL_COMPUTE_SHADER, "shaders/RegionGrowIterate3.cs.glsl");
            shader3.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
        if (dilateShader == null) {
            dilateShader = new ShaderProgram();
            dilateShader.addShader(GL_COMPUTE_SHADER, "shaders/RegionGrowDilate.cs.glsl");
            dilateShader.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
    }
    
    public void gpu_calculate() {
        if (src == null) return;
        
        initShader();
                
        int iWidth = src.getDimension(0).getSize();
        int iHeight = src.getDimension(1).getSize();
        int iDepth = src.getDimension(2).getSize();
        
        Integer tn = (Integer) src.getAttribute("textureName");
        
        if (tn == null) {
            ImageVolumeUtil.buildTexture(src);
            tn = (Integer) src.getAttribute("textureName");
        }
        
        if (tn == null) {
            System.out.println("RegioGrow.gpu_calculate: textureName not found.");
            return;
        } //TODO: should prob throw exception
        
        // build mask texture
        this.buildTexture(src);
        
        maskChannel = src.addChannel(org.fusfoundation.kranion.model.image.ImageVolume.UBYTE_VOXEL);
        
        if (Main.OpenGLVersion < 4f) return;
                
        // bind input image textures for image and mask
        setupImageTexture(src, 0, 1);
        
        // bind an atomic counter so we can tell how many mask voxels changed
        IntBuffer counterBuffer = BufferUtils.createIntBuffer(1);
        counterBuffer.put(0);
        counterBuffer.flip();
        
        int counterBuf = glGenBuffers();
        glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, 2, counterBuf);
        glBufferData(GL_ATOMIC_COUNTER_BUFFER, counterBuffer, GL_DYNAMIC_DRAW);
        
        
        // hounsfield scaling parameters
        Float intercept = (Float)src.getAttribute("RescaleIntercept");
        if (intercept == null) { intercept = 0f; }
        
        Float slope = (Float)src.getAttribute("RescaleSlope");
        if (slope == null) { slope = 1f; }
        
//        shader.start();
//                        
//        int uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_intercept");
//        glUniform1f(uloc, intercept);
//        uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_slope");
//        glUniform1f(uloc, slope);
//
//        // run compute shader
//        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//        org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
//        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//                
//        // Clean up
//        shader.stop();

        int changeCount = 0;
        do {
            
            // reset mask change counter
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, counterBuf);
            glBufferData(GL_SHADER_STORAGE_BUFFER, counterBuffer, GL_DYNAMIC_DRAW);

//            shader2.start();
//
//            // run compute shader
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//            org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//
//            // Clean up
//            shader2.stop();

            shader.start();

            int uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_intercept");
            glUniform1f(uloc, intercept);
            uloc = glGetUniformLocation(shader.getShaderProgramID(), "ct_rescale_slope");
            glUniform1f(uloc, slope);

            // run compute shader
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
            org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

            // Clean up
            shader.stop();

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, counterBuf);
            ByteBuffer bbuf = glMapBuffer(GL_SHADER_STORAGE_BUFFER, GL_READ_ONLY, null);
            IntBuffer maskChangeCount = bbuf.asIntBuffer();

            changeCount = maskChangeCount.get(0);
            System.out.println("Mask change count = " + changeCount);

            glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        
        } while (changeCount > 0);
        
        // dilate the region grown mask so it will volume render smoothly
        dilateShader.start();

            // run compute shader
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
            org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

            // Clean up
         dilateShader.stop();
         
        dilateShader.start();

            // run compute shader
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
            org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

            // Clean up
         dilateShader.stop();
        
        shader3.start();

        // run compute shader
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute((iWidth+7)/8, (iHeight+7)/8, (iDepth+7)/8);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);

        // Clean up
        shader3.stop();

        glActiveTexture(GL_TEXTURE0 + 0);
        glBindTexture(GL_TEXTURE_3D, 0);
        glDisable(GL_TEXTURE_3D);
        
        // copy result textures back to cpu
        updateTexture2Image();
        updateTexture2Mask();
        
        // release channel 0 so it will get rebuilt
        ImageVolumeUtil.releaseTexture(src);
        ImageVolumeUtil.releaseTexture(src, "maskTexName");

        // release channel 1 so it will get rebuilt
        this.releaseTexture();
        src.freeChannel(maskChannel);

    }
    
    private void updateMask2Texture() {
        if (this.src == null) return;
        
        buildTexture(src);
        
        int width = src.getDimension(0).getSize();
        int height = src.getDimension(1).getSize();
        int depth = src.getDimension(2).getSize();
    
        Integer tn = (Integer) src.getAttribute("maskTexName");
        glBindTexture(GL_TEXTURE_3D, tn);
        ByteBuffer voxelData = src.getByteBuffer(1, true); // mask channel TODO: need a lookup mechanism, prob by name "Mask"
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, width, height, depth, 0, GL_RED, GL_UNSIGNED_BYTE, voxelData);
        glBindTexture(GL_TEXTURE_3D, tn);

    }
    
    private void updateTexture2Image() {
        if (this.src == null) return;
        
        Integer tn = (Integer) src.getAttribute("textureName");
        
        if (tn == null) return;
        
        int width = src.getDimension(0).getSize();
        int height = src.getDimension(1).getSize();
        int depth = src.getDimension(2).getSize();
           
        glBindTexture(GL_TEXTURE_3D, tn);
        
                int value;
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_WIDTH);
                System.out.println("Texture Width = " + value);
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_HEIGHT);
                System.out.println("Texture Height = " + value);
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_DEPTH);
                System.out.println("Texture Depth = " + value);
        
        ShortBuffer voxelData = src.getByteBuffer(0, true).asShortBuffer(); // mask channel TODO: need a lookup mechanism, prob by name "Mask"
        
        voxelData.clear();
        
        glPixelStorei(GL_PACK_ALIGNMENT, 1); // Super important or fetched data might be larger than buffer -> crash
        glGetTexImage(GL_TEXTURE_3D, 0, GL_RED, GL_UNSIGNED_SHORT, voxelData);
        
        short[] imgData = (short[])src.getData(0);
        
        for (int i=0; i<imgData.length; i++) {
            imgData[i] = (short)(voxelData.get() & 0xffff);
        }

        glBindTexture(GL_TEXTURE_3D, 0);

    }
 
    private void updateTexture2Mask() {
        if (this.src == null) return;
        
        Integer tn = (Integer) src.getAttribute("maskTexName");
        
        if (tn == null) return;
        
        int width = src.getDimension(0).getSize();
        int height = src.getDimension(1).getSize();
        int depth = src.getDimension(2).getSize();
           
        glBindTexture(GL_TEXTURE_3D, tn);
        
        ByteBuffer voxelData = src.getByteBuffer(1, true); // mask channel TODO: need a lookup mechanism, prob by name "Mask"
        
        if (voxelData == null) {
            System.out.println("RegionGrow: there was now mask channel in the src!");
            return;
        }
        
        voxelData.clear();
        
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glGetTexImage(GL_TEXTURE_3D, 0, GL_RED, GL_UNSIGNED_BYTE, voxelData);
        
        byte[] imgData = (byte[])src.getData(maskChannel);
        
        for (int i=0; i<imgData.length; i++) {
            imgData[i] = voxelData.get();
        }

        glBindTexture(GL_TEXTURE_3D, 0);

    }
    
    private void buildTexture(ImageVolume image) {

        if (image != null) {
            //System.out.println("ImageCanvas3D::build texture..");

            Integer tn = (Integer) image.getAttribute("maskTexName");

//            System.out.println("   textureName = " + tn);
            if (tn != null && tn > 0) {
                System.out.println("Got previously built mask texture = " + tn);
            } else {

                //System.out.println("build new texture");
                ByteBuffer buf = ByteBuffer.allocateDirect(4);
                IntBuffer texName = buf.asIntBuffer();

                releaseTexture();

                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                glGenTextures(texName);
                int textureName = texName.get(0);

                glBindTexture(GL_TEXTURE_3D, textureName);

                image.setAttribute("maskTexName", textureName, true); // transient texture name attribute

                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

                int pixelType = image.getVoxelType();
                int width = image.getDimension(0).getSize();
                int height = image.getDimension(1).getSize();
                int depth = image.getDimension(2).getSize();

                System.out.println("  building 8bit mask texture");

                //ShortBuffer pixelBuf = (tmp.asShortBuffer());
                //glTexImage3D(GL_TEXTURE_3D, 0, GL_INTENSITY16, texWidth, texHeight, texDepth, 0, GL_LUMINANCE, GL_SHORT, pixelBuf);
                glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, width, height, depth, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer)null);
                
                //glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA16F, width, height, depth);
            }
            int value;
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_WIDTH);
            System.out.println("Mask Texture Width = " + value);
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_HEIGHT);
            System.out.println("Mask Texture Height = " + value);
            value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_DEPTH);
            System.out.println("Mask Texture Depth = " + value);

            glBindTexture(GL_TEXTURE_3D, 0);
        }

    }
        
    private void setupImageTexture(ImageVolume image, int imageTextureUnit, int maskTextureUnit) {
        if (image == null) return;
        
        Integer imageTextureName = (Integer) image.getAttribute("textureName");
        
        if (imageTextureName == null) return;
        
        Integer maskTextureName = (Integer) image.getAttribute("maskTexName");
        
        if (maskTextureName == null) return;
           
        glBindImageTexture(imageTextureUnit, imageTextureName, 0, true, 0, GL_READ_WRITE, GL_R16);
        glBindImageTexture(maskTextureUnit, maskTextureName, 0, true, 0, GL_READ_WRITE, GL_R8);
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);
        
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
        
        Main.glPopAttrib();
    }
    
    private void releaseTexture() {
        if (maskVolumeTexture != 0) {
            glDeleteTextures(maskVolumeTexture);
            maskVolumeTexture = 0;               
        }
    }
    
    public void release() {
        releaseTexture();
        if (shader != null) {
            shader.release();
            shader = null;
        }
        if (shader2 != null) {
            shader2.release();
            shader2 = null;
        }
        if (shader3 != null) {
            shader3.release();
            shader3 = null;
        }
        if (dilateShader != null) {
            dilateShader.release();
            dilateShader = null;
        }
    }
}
