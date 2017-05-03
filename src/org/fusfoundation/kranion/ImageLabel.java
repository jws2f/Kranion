/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
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

import org.lwjgl.opengl.GL11;
import java.io.*;
import java.nio.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author john
 */
public class ImageLabel extends GUIControl {

    private int texName = 0;
    private String imageResource;
    private float zDepth = 0f;
    
    public ImageLabel(String imageResource, int x, int y, int width, int height) {
        this.imageResource = imageResource;
        setBounds(x, y, width, height);
        buildTexture(imageResource);
    }
    
    public void setZDepth(float depth)
    {
        zDepth = depth;    
    }
    
    @Override
    public void render() {
        if (!this.getVisible()) return;
        
        setIsDirty(false);
        
        glPushAttrib(GL_ENABLE_BIT | GL_POLYGON_BIT);
        
        glEnable(GL_TEXTURE_2D);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glBindTexture(GL_TEXTURE_2D, texName);
        
        glEnable(GL_BLEND);
        glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        
        glBegin(GL_QUADS);
            glNormal3f(0f, 1f, 0.4f);
            glTexCoord2f(0f, 1f);
            glVertex3f(bounds.x, bounds.y, zDepth);
            
            glTexCoord2f(1f, 1f);
            glVertex3f(bounds.x+bounds.width, bounds.y, zDepth);
            
            glNormal3f(0f, 0f, 1f);
            glTexCoord2f(1f, 0f);
            glVertex3f(bounds.x+bounds.width, bounds.y+bounds.height, zDepth);
            
            glTexCoord2f(0f, 0f);
            glVertex3f(bounds.x, bounds.y+bounds.height, zDepth);        
        glEnd();
        
        
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
        
        glPopAttrib();
    }

    @Override
    public void release() {
        deleteTexture();
    }
    
    private void buildTexture(String resource) {
        BufferedImage img = null;
        try {
            InputStream rstm = this.getClass().getResourceAsStream(resource);
            img = ImageIO.read(rstm);
        } catch (IOException e) {
            return;
        }
        
        try {
            //Create the PNGDecoder object and decode the texture to a buffer
            int width = img.getWidth(), height = img.getHeight();
            
            bounds.width = width;
            bounds.height = height;
            
            byte buf[] = (byte[]) img.getRaster().getDataElements(0, 0, width, height, null);

            ByteBuffer pixelData = ByteBuffer.allocateDirect(buf.length);
            pixelData.put(buf, 0, buf.length);
            pixelData.flip();
            
            if (texName != 0) {
                deleteTexture();
            }
                    
            //Generate and bind the texture
            texName = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texName);
            //Upload the buffer's content to the VRAM
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelData);
            //Apply filters
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void deleteTexture() {
        if (glIsTexture(texName)) {
            ByteBuffer texNameBuf = ByteBuffer.allocateDirect(4);
            texNameBuf.asIntBuffer().put(0, texName);
            texNameBuf.flip();
            glDeleteTextures(texNameBuf.asIntBuffer());
        }
    }
    
}
