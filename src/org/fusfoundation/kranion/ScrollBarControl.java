/*
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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

import java.awt.Color;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LIGHTING_BIT;
import static org.lwjgl.opengl.GL11.GL_LIGHT_MODEL_LOCAL_VIEWER;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POLYGON_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SHININESS;
import static org.lwjgl.opengl.GL11.GL_TRANSFORM_BIT;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLightModeli;
import static org.lwjgl.opengl.GL11.glMaterialf;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class ScrollBarControl extends GUIControl {
    
    private Vector4f color = new Vector4f(0.15f, 0.35f, 0.15f, 1f);
    private boolean thumbGrabbed = false;
    private enum ScrollState {DISABLED, ENABLED, ARMED, FIRED};
    private ScrollState state = ScrollState.ENABLED;
    
    private static float minValue = 0;
    private static float maxValue = 1;
    private float currentValue = 0;
    private float pageLength = 1;
    Rectangle thumbRect = new Rectangle();
    private float initialGrabbedValue = 0;
        
    public void setValue(float val) {
        currentValue = Math.max(Math.min(val, maxValue), minValue);
        updateThumb();
        setIsDirty(true);
    }
    
    public float getValue() { return currentValue; }
    
    public void setPageLength(float length) {
        pageLength = Math.max(0f, Math.min(1f, length));
//        System.out.println(this + " pagelength set = " + pageLength);
        updateThumb();
        setIsDirty(true);
    }
    
    public float getPageLenth() { return pageLength; }

    @Override
    public void render() {
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT);
        
                // Draw the gutter
                    glColor4f(0.15f, 0.15f, 0.15f, 1f);
                
                    glBegin(GL_QUADS);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(bounds.x, bounds.y);
                        glVertex2f(bounds.x+bounds.width, bounds.y);
                        glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                        glVertex2f(bounds.x, bounds.y+bounds.height);        
                    glEnd();
                                               
                switch(state) {
                    case ENABLED:
                        glColor4f(color.x, color.y, color.z, color.w);
                        if (this.mouseInside) {
                            glColor4f(color.x*1.15f, color.y*1.15f, color.z*1.15f, color.w);                            
                        }
                        else {                           
                            glColor4f(color.x, color.y, color.z, color.w);                            
                        }
                        break;
                    case DISABLED:
                        glColor4f(color.x, color.y, color.z, 0.5f);
                        break;
                    case ARMED:
                        glColor4f(color.x/1.1f, color.y/1.1f, color.z/1.1f, color.w);
                        break;
                }
                
                // draw the thumb
                
                
                glBegin(GL_QUADS);
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(thumbRect.x, thumbRect.y);
                    glVertex2f(thumbRect.x+thumbRect.width, thumbRect.y);
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(thumbRect.x+thumbRect.width, thumbRect.y+thumbRect.height);
                    glVertex2f(thumbRect.x, thumbRect.y+thumbRect.height);        
                glEnd();
                
                
                        
        Main.glPopAttrib();
        
        setIsDirty(false);
    }
    
    protected void updateThumb() {
                float thumbLength = Math.min(bounds.height, Math.max(bounds.width, bounds.height * pageLength));
                float thumbPos = bounds.height + currentValue * (bounds.height - thumbLength) + thumbLength;
                
                thumbRect = new Rectangle(bounds.x, bounds.y + bounds.height - thumbPos + bounds.height, 25, thumbLength);
//                System.out.println(thumbRect);
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
            return true;
        }
        
        
        if (state != ScrollState.DISABLED) {
            if (MouseIsInside(x, y) && button1down && !this.hasGrabbed()) {
                    grabMouse(x, y);
                    
                    if (thumbRect.contains(x, y)) {
                        thumbGrabbed = true;
                        state = ScrollState.ARMED;
                        initialGrabbedValue = currentValue;
                    }
            }
            else if (thumbGrabbed && !button1down) {
                ungrabMouse();
                thumbGrabbed = false;
                state = ScrollState.ENABLED;
                setIsDirty(true);
            }
            else if (hasGrabbed() && !button1down) {
                ungrabMouse();
                if (y > thumbRect.y+thumbRect.height) {
                    this.setValue(currentValue - pageLength);
                    fireActionEvent();
                }
                else if (y < thumbRect.y) {
                    this.setValue(currentValue + pageLength);
                    fireActionEvent();
                }
                setIsDirty(true);
            }
            else if (MouseIsInside(x, y) && dwheel != 0) {
                this.setValue(currentValue - pageLength * dwheel/120 / 10f);
                fireActionEvent();
            }
            
            if (thumbGrabbed) {
                float delta = ygrab - y;
//                System.out.println("thumb delta = " + delta);
                
                float deltaRange = delta/(bounds.height-thumbRect.height);
                
                currentValue = initialGrabbedValue + deltaRange;
                
                currentValue = Math.min(maxValue, Math.max(minValue, currentValue));
                
                if (pageLength >= 1f) {
                    currentValue = 0f;
                }
                
//                System.out.println("val = " + currentValue);
//                System.out.println("minVal = " + minValue);
//                System.out.println("maxVal = " + maxValue);
//                System.out.println("pl = " + pageLength);
                
                updateThumb();
                setIsDirty(true);
                
                this.fireActionEvent();
                
            }
            
            
        }
        
        return hasGrabbed();
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r); //To change body of generated methods, choose Tools | Templates.
        updateThumb();
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height); //To change body of generated methods, choose Tools | Templates.
        updateThumb();
    }
}
