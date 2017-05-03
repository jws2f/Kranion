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

import java.nio.*;
import org.fusfoundation.kranion.model.image.*;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author john
 */
public class TransferFunctionDisplay extends GUIControl {
    private int[] bins = new int[4096];
//    private float[] alpha = new float[4096];
    private ImageVolume4D lut;
    private float[] lutValues;
    private int maxVal = 0;
    private int materialThreshold = 250;
    private int opacityThreshold = 1024;
    private Vector3f tissueColor = new Vector3f(0.6f, 0.4f, 0.25f), boneColor = new Vector3f(0.95f, 0.85f, 0.5f);
    private boolean mouseGrabbed = false;
    
    public TransferFunctionDisplay() {
        initVars();
    }
    
    public TransferFunctionDisplay(int x, int y, int width, int height) {
        initVars();
        setBounds(x, y, width, height);
    }
    
    public ImageVolume4D getTransferFunction() { return lut; }
    
    public int getOpacityThreshold() { return opacityThreshold; }
    public void setOpacityThreshold(int value) {
        if (opacityThreshold != value) {
            setIsDirty(true);
        }
        opacityThreshold = Math.min(4095, Math.max(0, value));

        float scale = 300f;

        float low = (sigmoid(0f, scale));
        float high = (sigmoid(1f, scale));

        for (int i=0; i<4095; i++) {
            float t = (i-opacityThreshold)/2048f + 0.5f;

            //setAlphaValue(i, (mouseY-(y+1f))/(height-2f) * (sigmoid(t, scale) - low) * 1f/(high - low));
            setAlphaValue(i, (sigmoid(t, scale) - low) * 1f/(high - low));
        }        
    }
    
    public int getMaterialThreshold() { return materialThreshold; }
    public void setMaterialThreshold(int value) {
        if (materialThreshold != value) {
            setIsDirty(true);
        }
        materialThreshold = Math.min(4095, Math.max(0, value));
       
        float scale = 650f;
        
        float low = (sigmoid(0f, scale));
        float high = (sigmoid(1f, scale));
            

        for (int i=0; i<4096; i++) {
            float t = (i-materialThreshold)/2048f + 0.5f;
            
            float blend = (sigmoid(t, scale) - low) * 1f/(high - low);
            
//            if (i-ct_threshold > -10) {
                lutValues[i*4+0] = 0.9f * blend + 0.48f * (1f - blend);
                lutValues[i*4+1] = 0.85f * blend + 0.16f * (1f - blend);
                lutValues[i*4+2] = 0.55f* blend + 0.08f * (1f - blend);
                //pixels[i*4+3] = (sigmoid(t, scale) - low) * 1f/(high - low) * 0.9f;            
//            }
//            else {
//                pixels[i*4+0] = 0.6f;
//                pixels[i*4+1] = 0.2f;
//                pixels[i*4+2] = 0.1f;               
//                //pixels[i*4+3] = (sigmoid(t, scale) - low) * 1f/(high - low) * 0.5f;            
//            }
                
            //pixels[i*4+3] = (sigmoid(t, scale) - low) * 1f/(high - low) * 0.9f;            
        }
    }
    
    public void setAlphaValue(int bin, float value) {
        if (bin<0 || bin >= bins.length) return;
        
        if (lutValues[bin*4+3] != value) {
            setIsDirty(true);
        }
        
        lutValues[bin*4+3] = Math.min(1f, Math.max(0f, value));
    }
    
//    public float[] getAlphaValues() {
//        return alpha;
//    }
    
    private void initVars() {
        
        lut = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, 4096, 4, 1, 1);
        lutValues = (float[])lut.getData();

//        for (int i=0; i<alpha.length; i++) {
//            alpha[i] = 1f;
//        }        
        for (int i=0; i<bins.length; i++) {
            bins[i] = 0;
        }

        setMaterialThreshold(145);
        setOpacityThreshold(165);
    }
    
    @Override
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);
        
        glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT);
        
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
            glLoadIdentity();

            org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, Display.getWidth(), 0.0f, Display.getHeight());

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
                glLoadIdentity();
                
                
                glDisable(GL_DEPTH_TEST);
                glDisable(GL_LIGHTING);
//                glEnable(GL_BLEND);
//                glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glLineWidth(2f);
                
               
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                glColor4f(0.5f, 0.5f, 0.5f, 0.6f);
                glBegin(GL_QUADS);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);
                glEnd();
                
                
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                
                                
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                glColor3f(0.3f, 0.3f, 0.3f);
                glBegin(GL_QUADS);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);
                glEnd();
                
                if (maxVal > 0f) {
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                    glColor4f(0.8f, 0.8f, 0.8f, 0.6f);
                    float x1 = bounds.x+1f;
                    float width1 = bounds.width-2f;
                    float height1 = bounds.height-2f;
                    glBegin(GL_LINES);
                        for (int i=1; i<bins.length/4; i++) {
                            int index = i*4;
                            glColor4f(lutValues[index], lutValues[index+1], lutValues[index+2], Math.max(lutValues[index+3], 0.3f) * 0.8f);
//                            if (i>this.materialThreshold) {
//                                glColor4f(boneColor.x, boneColor.y, boneColor.z, Math.max(alpha[i], 0.3f) * 0.8f);
//                            }
//                            else {
//                                glColor4f(tissueColor.x, tissueColor.y, tissueColor.z, Math.max(alpha[i], 0.3f) * 0.8f);                                
//                            }
                            
                            if (bins[i] > 0 && maxVal > 0f) {
                                glVertex2f(x1 + (float)i/(bins.length/4)*width1, (float)bounds.y+(float)Math.log(bins[i])/(float)Math.log(maxVal)*height1 );
                                //glVertex2f(x1 + (float)i/(bins.length/4)*width1, (float)y+(float)(bins[i])/(float)(maxVal)*height1 );
                            }
                            else {
                                glVertex2f(x1 + (float)i/(bins.length/4)*width1, 0f );                                
                            }
                            glVertex2f(x1 + (float)i/(bins.length/4)*width1, 0f );
                       }                        
                    glEnd();
                    glBegin(GL_LINE_STRIP);
                        for (int i=1; i<bins.length/4; i++) {
                            glColor4f(0.2f, 0.2f, 0.2f, 0.85f);
                            
                            if (bins[i] > 0 && maxVal > 0f) {
                                glVertex2f(x1 + (float)i/(bins.length/4)*width1, (float)bounds.y+(float)Math.log(bins[i])/(float)Math.log(maxVal)*height1 );
                            }
                            else {
                                glVertex2f(x1 + (float)i/(bins.length/4)*width1, 0f );                                
                            }
                       }                        
                    glEnd();                    
                }

            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
            
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        
        glMatrixMode(GL_MODELVIEW);
        
        glPopAttrib();
    }
    
    public void setHistogram(IntBuffer histogramBins) {
        setIsDirty(true);
        
        maxVal = 0;
        for (int i=1; i< bins.length; i++) {
                bins[i] = 0;
        }
        
        if (histogramBins == null) return;
        
        int bin = 0;
        while (histogramBins.hasRemaining() && bin < bins.length)
        {
        	int value = histogramBins.get();
                bins[bin/4] += value; // crush from 4096 to 1024 bins
                                
                bin++;
        }
        
        for (int i=1; i< bins.length-1; i++) {
                 if (bins[i] > maxVal) maxVal = bins[i];
//                 System.out.println(i + " = " + bins[i]);
        }
   }

    @Override
    public void release() {
        // TODO: release any resources here that we allocated, GL buffers, etc.
    }

    @Override
    public boolean OnMouse(float mouseX, float mouseY, boolean button1down, boolean button2down, int dwheel) {
        if (!mouseGrabbed) {
            if(!MouseIsInside(mouseX, mouseY))
                return false;
        }
        
        if (button1down ==false && button2down == false) {
            ungrabMouse();
            mouseGrabbed = false;
            return false;
        }
        else if (button1down) {
            grabMouse(mouseX, mouseY);
            mouseGrabbed = true;
            setMaterialThreshold((int)Math.round( ((float)mouseX - bounds.x)/bounds.width * 1024.0f) - 20);
            setOpacityThreshold((int)Math.round( ((float)mouseX - bounds.x)/bounds.width * 1024.0f));
//            System.out.println("Opacity thresh: " + (int)Math.round( ((float)mouseX - bounds.x)/bounds.width * 1024.0f));
            return true;
        }
        else if (button2down) {
            grabMouse(mouseX, mouseY);
            mouseGrabbed = true;
            
//            float scale = 300f;
//            
//            float low = (sigmoid(0f, scale));
//            float high = (sigmoid(1f, scale));
//            
//            int index = Math.round( ((float)mouseX - x)/width * 1024.0f);
//            for (int i=0; i<4095; i++) {
//                float t = (i-index)/2048f + 0.5f;
//                            
//                //setAlphaValue(i, (mouseY-(y+1f))/(height-2f) * (sigmoid(t, scale) - low) * 1f/(high - low));
//                setAlphaValue(i, (sigmoid(t, scale) - low) * 1f/(high - low));
//            }
            
            setOpacityThreshold((int)Math.round( ((float)mouseX - bounds.x)/bounds.width * 1024.0f));
            return true;
        }
        
        return false;
    }
    
    private float sigmoid(float t, float m) {
            return (1f / (1f + (float)Math.exp(-((t-0.5f)*m))));
    }
    
}
