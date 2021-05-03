/*
 * The MIT License
 *
 * Copyright 2020 Focused Ultrasound Foundation.
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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPLACE;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_MODE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glIsTexture;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexEnvf;
import static org.lwjgl.opengl.GL11.glTexImage1D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */

// The intent of this class is to provide a means of passing a lookup table to a shader,
// where a floating point value looks up an RGBA color. The shader will end up getting the
// lookup table as a GL_Texture_1D. Min and Max values map to texture coordinates S=[0, 1].

public class LookupTable {
    private int lutTextureName=0;
    private ImageVolume4D lut;
    private float[] lutValues;
    
    private float minValue;
    private float maxValue;
    private String unitsName;
    
    private boolean isDirty = true;
    
    public LookupTable(int nentries) {
        lut = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, nentries, 4, 1, 1);
        lutValues = (float[])lut.getData();        
    }
    
    public void setUnitsName(String name) {
        unitsName = name;
    }
    
    public String getUnitsName() {
        return unitsName;
    }
    
    public void setLUTvalue(int index, float red, float green, float blue, float alpha) {
        lutValues[index*4] = red;
        lutValues[index*4+1] = green;
        lutValues[index*4+2] = blue;
        lutValues[index*4+3] = alpha;
    }
    
    public void setLUTvalueRamp(float rangeStart, float rangeEnd, Vector4f colorStart, Vector4f colorEnd) {
        int startIndex = (int)((rangeStart - minValue)/(maxValue - minValue)*(lutValues.length/4-1));
        int endIndex = (int)((rangeEnd - minValue)/(maxValue - minValue)*(lutValues.length/4-1));
        int indexRange = endIndex - startIndex;
        
        for(int i=startIndex; i<=endIndex; i++) {
            float redValue =    colorStart.x + (float)(i-startIndex)/(float)indexRange*(colorEnd.x - colorStart.x);
            float greenValue =  colorStart.y + (float)(i-startIndex)/(float)indexRange*(colorEnd.y - colorStart.y);
            float blueValue =   colorStart.z + (float)(i-startIndex)/(float)indexRange*(colorEnd.z - colorStart.z);
            float alphaValue =  colorStart.w + (float)(i-startIndex)/(float)indexRange*(colorEnd.w - colorStart.w);
            
            if (i>=0 && i<lutValues.length/4) {
//                System.out.println("LUT index " + i + " -- " + redValue + ", " + greenValue + ", " + blueValue + ", " + alphaValue);
                setLUTvalue(i, redValue, greenValue, blueValue, alphaValue);
            }
        }
        
        isDirty = true;
    }
    
    public void setMinValue(float value) {
        minValue = value;
        isDirty = true;
    }
    
    public float getMinValue() {
        return minValue;
    }
    
    public void setMaxValue(float value) {
        maxValue = value;
        isDirty = true;
    }
    
    public float getMaxValue() {
        return maxValue;
    }
    
    public void buildLutTexture() {
        release();
        
        if (lutTextureName == 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(4);
            IntBuffer texName = buf.asIntBuffer();

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenTextures(texName);
            lutTextureName = texName.get(0);
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TEXTURE_BIT);
        
        glBindTexture(GL_TEXTURE_1D, lutTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
        if (lut != null) {
            ByteBuffer tmp = lut.getByteBuffer();
            //tmp.order(ByteOrder.BIG_ENDIAN);
            FloatBuffer pixelBuf = (tmp.asFloatBuffer());
            //glTexImage1D(GL_TEXTURE_1D, 0, org.lwjgl.opengl.GL11.GL_INTENSITY16, TransferFunction.getDimension(0).getSize(), 0, GL_LUMINANCE, GL_FLOAT, pixelBuf);
            glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA32F, lut.getDimension(0).getSize(), 0, GL_RGBA, GL_FLOAT, pixelBuf);
        }
        
        glBindTexture(GL_TEXTURE_1D, 0);
        
        Main.glPopAttrib();
        
        isDirty = false;
                    
    }
    
    private float sigmoid(float t, float m) {
            return (1f / (1f + (float)Math.exp(-((t-0.5f)*m))));
    }
    
    public void setupTexture(int textureUnit) {

        if (isDirty) {
            this.buildLutTexture();
        }
        
        if (lutTextureName == 0) {
            Logger.getGlobal().log(Level.WARNING, "ImageCanvas3D: lutTextureName == 0");
            return;
        }
        

        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_1D);
        
        glBindTexture(GL_TEXTURE_1D, lutTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                
    }
    
    public void release() {
        if (lutTextureName != 0) {
            if (glIsTexture(lutTextureName)) {
                glDeleteTextures(lutTextureName);
                lutTextureName = 0;
            }                
        }
    }
}
