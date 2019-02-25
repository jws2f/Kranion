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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.Sphere;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;
import org.lwjgl.util.vector.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.glu.GLU;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.Observable;
import org.fusfoundation.kranion.model.Model;


/**
 *
 * @author John Snell
 */
public class Button extends GUIControl implements GUIControlModelBinding  {
    private Vector4f color = new Vector4f(0.35f, 0.35f, 0.35f, 1f);
    private Vector4f textcolor = new Vector4f(1f, 1f, 1f, 1f);
    private BufferedImage labelImage;
    private int fontSize = 16;
    private float labelScale = 1f;
    private Sphere sphere = new Sphere();
    private float indicatorRadius = 10f;
    private boolean indicatorOn = false; // for radio and checkboxes
    private boolean drawBackground = true;

    private enum ButtonState {DISABLED, ENABLED, ARMED, FIRED};
    private ButtonState state = ButtonState.ENABLED;
    
    public enum ButtonType {BUTTON, RADIO_BUTTON, TOGGLE_BUTTON}
    private ButtonType type = ButtonType.BUTTON;
    
    public Button() {type = ButtonType.BUTTON;}
    
    public Button(ButtonType type, int x, int y, int width, int height) {
        this.type = type;
        setBounds(x, y, width, height);
        setTitle("Test");
    }
    
    public Button(ButtonType type, int x, int y, int width, int height, ActionListener listener) {
        this.type = type;
        setBounds(x, y, width, height);
        setTitle("Test");
        addActionListener(listener);
    }
    
    @Override
    public void setIsEnabled(boolean enabled) {
        super.setIsEnabled(enabled);
        
        if (enabled) {
            state = ButtonState.ENABLED;
        }
        else {
            state = ButtonState.DISABLED;
        }
        setIsDirty(true); // redundant?
    }
    
    public Button setIndicator(boolean flag) {
        indicatorOn = flag;
        
        setIsDirty(true);
        
        return this;
    }
    
    public boolean getIndicator() {
        return indicatorOn;
    }
    
    public Button setIndicatorRadius(float radius) {
        if (radius != indicatorRadius) {
            indicatorRadius = radius;
            setIsDirty(true);
        }
        return this;
    }
    
    public Button setDrawBackground(boolean drawbg) {
        if (drawbg != drawBackground) {
            drawBackground = drawbg;
            setIsDirty(true);
        }                
        return this;
    }
    
    public Button setType(ButtonType type) {
        this.type = type;
        
        setIsDirty(true);
        
        return this;
    }
    
    public GUIControl setTitle(String title) {
        super.setTitle(title);
        setIsDirty(true);
        return this;
    }
    
    public void setColor(float red, float green, float blue, float alpha) {
        if (red   != color.x ||
            green != color.y ||
            blue  != color.z ||
            alpha != color.w) {
            
            color.set(red, green, blue, alpha);
            setIsDirty(true);
        }
    }
    
    public void setTextColor(float red, float green, float blue, float alpha) {
        if (red   != textcolor.x ||
            green != textcolor.y ||
            blue  != textcolor.z ||
            alpha != textcolor.w) {
            
            textcolor.set(red, green, blue, alpha);
            setIsDirty(true);
        }
    }
    
    public void setOpacity(float alpha) {
        if (alpha != color.w) {
            color.w = alpha;
            setIsDirty(true);
        }
    }
    
    public Vector4f getColor() {
        return color;
    }
    
    @Override
    public void render() {
                        
        float scale = getGuiScale();
        if (scale != labelScale) {
            labelScale = scale;
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT);
        
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
                
                if (drawBackground) {
                    glBegin(GL_QUADS);
                        glNormal3f(0f, 1f, 0.4f);
                        glVertex2f(bounds.x, bounds.y);
                        glVertex2f(bounds.x+bounds.width, bounds.y);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                        glVertex2f(bounds.x, bounds.y+bounds.height);        
                    glEnd();
                }
                               
                Rectangle textBounds = new Rectangle(bounds);
                textBounds.y+=3;
                textBounds.height-=3;
                
                if (type != ButtonType.BUTTON) {
                    textBounds.x += this.indicatorRadius * 3;
                   //textBounds.width -= this.indicatorRadius * 3;
                    renderText(getTitle(), textBounds, null, new Color(textcolor.x, textcolor.y, textcolor.z, textcolor.w), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT);
                }
                else {
                    renderText(getTitle(), textBounds, null, new Color(textcolor.x, textcolor.y, textcolor.z, textcolor.w), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_CENTER);
                }
                
                if (type != ButtonType.BUTTON) {

                    glEnable(GL_BLEND);

                    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 80.0f);					// sets shininess
                    glLightModeli(GL_LIGHT_MODEL_LOCAL_VIEWER, GL_FALSE);

                    glMatrixMode(GL_MODELVIEW);
                    Main.glPushMatrix();
                        glTranslatef(bounds.x+12, bounds.y+bounds.height/2f, 10f);

                        sphere.setDrawStyle(GLU.GLU_FILL);
                        sphere.setNormals(GLU.GLU_SMOOTH);

                        glColor4f(0.2f, 0.2f, 0.2f, 0.2f);
                        //glColor4f(0.2f, 0.2f, 0.2f, 1f);
                        sphere.setOrientation(GLU.GLU_OUTSIDE);
                        
            glEnable(GL_CULL_FACE);       
            glCullFace(GL_FRONT);
                        sphere.draw(indicatorRadius, 32, 16);
                        glDisable(GL_CULL_FACE);

                        if (indicatorOn) {
                            glColor4f(0.1f, 0.9f, 0.1f, 0.8f);
                            //glColor4f(0.1f, 0.9f, 0.1f, 1f);
                        }
                        else {
                            glColor4f(0.3f, 0.4f, 0.3f, 0.8f);                            
                            //glColor4f(0.3f, 0.4f, 0.3f, 1f);                            
                        }
                        sphere.setOrientation(GLU.GLU_INSIDE);
                        sphere.draw(indicatorRadius-2f, 32, 16);
                    Main.glPopMatrix();
                }
                        
        Main.glPopAttrib();
        
        setIsDirty(false);
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
            return true;
        }
        
        if (state != ButtonState.DISABLED) {
            if (MouseIsInside(x, y) && button1down) {
                    grabMouse(x, y);
            }
            else if (!button1down) {
                ungrabMouse();
            }
            
            if (MouseIsInside(x, y) || hasGrabbed()) {
                if (button1down && state != ButtonState.ARMED) {
                    state = ButtonState.ARMED;
                    setIsDirty(true);
                }
                else if (state == ButtonState.ARMED && !button1down && MouseIsInside(x, y)) {
                    FireButtonEvent();
                    state = ButtonState.ENABLED;
                    setIsDirty(true);
                }                
                
                return true;
            }
            else  {
                if (state == ButtonState.ARMED) {
                    state = ButtonState.ENABLED;
                }
            }
        }
                
        return false;
    }
    
    private void FireButtonEvent() {
        if (type != ButtonType.BUTTON) {
            indicatorOn = !indicatorOn;
        }
        
        fireActionEvent();
    }
    
    @Override
    public void update(Object newValue) {
        try {
            if (newValue == null) {
                newValue = new Boolean(false);
            }
            setIndicator((Boolean)newValue);
//            System.out.println("Button.update " + getTitle() + ") = " + (Boolean)newValue);
        }
        catch(Exception e) {
            System.out.println(this + "Name=" + this.getTitle() + " command=" + this.getCommand() + " Wrong or NULL new value: " + newValue.toString());
        }
    }
    
    @Override
    public void doBinding(Model model) {
        if (model != null && (type == ButtonType.TOGGLE_BUTTON || type == ButtonType.RADIO_BUTTON)) {
            model.setAttribute(this.getCommand(), this.getIndicator());
        }
    }
    
}
