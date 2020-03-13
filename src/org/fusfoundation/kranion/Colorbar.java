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

import java.awt.Color;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CURRENT_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_COLOR;
import static org.lwjgl.opengl.GL11.GL_POLYGON_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glTexCoord1f;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL20.glUseProgram;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author jsnell
 */
public class Colorbar extends GUIControl {
    
    private LookupTable lut = null;
    
    private boolean mouseDrag = false;
    private Vector2f mouseStart, locationStart;
    
    private float selectedValue = Float.NEGATIVE_INFINITY;
    
    public Colorbar() {
        mouseStart = new Vector2f();
        locationStart = new Vector2f();
    }
    
    public void setSelectedValue(float value) {
        selectedValue = value;
        setIsDirty(true);
    }

    public void setLookupTable(LookupTable lookuptable) {
        lut = lookuptable;
        setIsDirty(true);
    }
    
    @Override
    public void render() {
        if (this.getVisible() && lut != null) {
            
            
            Main.glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_COLOR_BUFFER_BIT);
            
//                glUseProgram(0);
                

                glDisable(GL_BLEND);
                glDisable(GL_LIGHTING);


                
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

                glColor4d(0, 0, 0, 1);
                glBegin(GL_QUADS);
                    glTexCoord1f(0);
                    glVertex2f(bounds.x, bounds.y);
                    glTexCoord1f(0);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glTexCoord1f(1);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glTexCoord1f(1);
                    glVertex2f(bounds.x, bounds.y+bounds.height);        
                glEnd();
                
                lut.setupTexture(0);
                
                glEnable(GL_BLEND);
                glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_COLOR, GL_ZERO, GL_ONE);
                glEnable(GL_TEXTURE_1D);
                glDisable(GL_DEPTH_TEST);

                glBegin(GL_QUADS);
                    glTexCoord1f(0);
                    glVertex2f(bounds.x, bounds.y);
                    glTexCoord1f(0);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glTexCoord1f(1);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glTexCoord1f(1);
                    glVertex2f(bounds.x, bounds.y+bounds.height);        
                glEnd();
                
                
                glBindTexture(GL_TEXTURE_1D, 0);
                glActiveTexture(GL_TEXTURE0);
                glDisable(GL_TEXTURE_1D);
                
                glColor4d(0.2, 0.2, 0.2, 1);
                
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                glLineWidth(2);
                
                glBegin(GL_QUADS);
                    glNormal3f(0f, 0f, 1f);
                    glTexCoord1f(0);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glNormal3f(0f, 0f, 1f);
                    glTexCoord1f(1);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);        
                glEnd();
                
             Main.glPopAttrib();
             
            if (lut.getUnitsName() != null) {
                Rectangle textBounds = new Rectangle(bounds.x - 5, bounds.y + bounds.height, bounds.width + 10, 25);
                renderText(lut.getUnitsName(), textBounds, null, new Color(1f, 1f, 1f, 1f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_CENTER);                
            }           
                               
//            Rectangle textBounds = new Rectangle(bounds.x + bounds.width + 5, bounds.y + bounds.height - 12, 300, 25);
//            renderText(String.format("%3.1f", lut.getMaxValue()), textBounds, null, new Color(1f, 1f, 1f, 1f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT);
            drawAxisTickMark(lut.getMaxValue());
            
//            textBounds = new Rectangle(bounds.x + bounds.width + 5, bounds.y - 12, 300, 25);
//            renderText(String.format("%3.1f", lut.getMinValue()), textBounds, null, new Color(1f, 1f, 1f, 1f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT);
            drawAxisTickMark(lut.getMinValue());
            
            float valueRange = lut.getMaxValue() - lut.getMinValue();
            int valueIncrement = (int)(valueRange/3);
            valueIncrement = (int)Math.pow(10.0, Math.ceil(Math.log10(valueIncrement))) / 2;
            
            float startValue = ((int)((lut.getMinValue() / valueIncrement)) + 1) * valueIncrement;
            for (float v = startValue; v<lut.getMaxValue(); v+=valueIncrement) {
                drawAxisTickMark(v);
            }
            
            if (selectedValue != Float.NEGATIVE_INFINITY) {
                drawAxisTickMark(selectedValue, false);
            }
            
        }

        
    }
    
    private void drawAxisTickMark(float value) {
        drawAxisTickMark(value, true);
    }
    
    private void drawAxisTickMark(float value, boolean onRight) {
            Main.glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_CURRENT_BIT);
            
                glDisable(GL_DEPTH_TEST);
                
                float valueRange = lut.getMaxValue() - lut.getMinValue();
                float ypos = Math.max(0f, Math.min(1f, (value-lut.getMinValue())/valueRange))*bounds.height + bounds.y;

                glColor4f(0.8f, 0.8f, 0.8f, 1f);
                glLineWidth(3);
                
                if (onRight) {
                    glBegin(GL_LINES);
                        glVertex2f(bounds.x + bounds.width - 3, ypos);
                        glVertex2f(bounds.x + bounds.width + 3, ypos);
                    glEnd();

                    Rectangle textBounds = new Rectangle(bounds.x + bounds.width + 5, ypos - 12, 100, 25);
                    renderText(String.format("%3.0f", value), textBounds, null, new Color(1f, 1f, 1f, 1f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT);
                }
                else {
                    glColor4f(0.5f, 0.5f, 0.5f, 1f);
                    glBegin(GL_LINES);
                        glVertex2f(bounds.x - 3, ypos);
                        glVertex2f(bounds.x + bounds.width + 3, ypos);
                    glEnd();

                    Rectangle textBounds = new Rectangle(bounds.x - 105, ypos - 12, 100, 25);
                    renderText(String.format("%3.1f", value), textBounds, null, new Color(1f, 1f, 1f, 1f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_RIGHT);
                }
                
            Main.glPopAttrib();
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (this.isVisible) {
            if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
                return true;
            }
            else {
                if (mouseInside && button1down && !mouseDrag) {
                    mouseDrag=true;
                    mouseStart.x = x;
                    mouseStart.y = y;
                    locationStart.x = bounds.x;
                    locationStart.y = bounds.y;
                    this.grabMouse(x, y);
                    return true;
                }
                else if (!button1down && mouseDrag) {
                    mouseDrag=false;
                    this.ungrabMouse();
                    return true;
                }
                
                if (mouseDrag) {
                    bounds.x = locationStart.x + (x - mouseStart.x);
                    bounds.y = locationStart.y + (y - mouseStart.y);
                    setIsDirty(true);
                    return true;
                }
                
                if (mouseInside && button2down) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
}
