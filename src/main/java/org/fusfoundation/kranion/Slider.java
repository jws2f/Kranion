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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Vector4f;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.kranion.model.Model;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 *
 * @author john
 */
public class Slider extends GUIControl implements GUIControlModelBinding, ActionListener {
    private Vector4f color = new Vector4f(0.35f, 0.35f, 0.35f, 1f);

    public enum SliderState {DISABLED, ENABLED};  
    private SliderState state = SliderState.ENABLED;
    private BufferedImage labelImage;
    private int fontSize = 16;
    private float handleLength = 25;
    private float troughLength = 150;
    private float handlePos = 20;
    private float labelWidth = 100;
    private String format = new String("%4.0f");
    private String units = new String("");
    private float labelScale = 1f;
    private boolean mouseButton1down = false;
    private long previousClickTime;
    
    private boolean persistAsString = false;
        
    private float min=0f, max=100f, current=0f, grabbedCurrent=0f;
    
    private org.fusfoundation.kranion.Cylinder trough_cylinder = new org.fusfoundation.kranion.Cylinder(1f, 1f, 0f, 1f);
    private org.fusfoundation.kranion.Hemisphere trough_hemisphere = new org.fusfoundation.kranion.Hemisphere(1f, 1f);
    
    private org.fusfoundation.kranion.Cylinder handle_cylinder = new org.fusfoundation.kranion.Cylinder(1f, 1f, 0f, 1f);
    private org.fusfoundation.kranion.Hemisphere handle_hemisphere = new org.fusfoundation.kranion.Hemisphere(1f, 1f);
    
    private TextBox valueTextBox = null;

    public Slider() {
        setTitle("Slider");
        
        valueTextBox = new TextBox();
        valueTextBox.setIsNumeric(true);
        valueTextBox.setVisible(false);
        this.addChild(valueTextBox);
        valueTextBox.addActionListener(this);
        
        setAcceptsKeyboardFocus(true);
    }
    
    public Slider(int x, int y, int width, int height) {
        valueTextBox = new TextBox();
        valueTextBox.setIsNumeric(true);
        valueTextBox.setVisible(false);
        this.addChild(valueTextBox);
        valueTextBox.addActionListener(this);
        
        setBounds(x, y, width, height);
        setTitle("Slider");
        setCurrentValue(this.current); 
        
        setAcceptsKeyboardFocus(true);
    }
    
    public Slider(int x, int y, int width, int height, ActionListener listener) {
        valueTextBox = new TextBox();
        valueTextBox.setIsNumeric(true);
        valueTextBox.setVisible(false);
        this.addChild(valueTextBox);
        valueTextBox.addActionListener(this);
        
        setBounds(x, y, width, height);
        setTitle("Slider");
        addActionListener(listener);
        setCurrentValue(this.current);

        setAcceptsKeyboardFocus(true);
    }
        
    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        troughLength = bounds.width - labelWidth - 16;
        
        valueTextBox.setBounds(labelWidth, 0, troughLength, bounds.height);
    }
    
    public void setPersistAsString() {
        this.persistAsString = true;
    }
    public void setPersistAsFloat() {
        this.persistAsString = false;
    }
    
    public void setState(SliderState state) { this.state = state; }
    public SliderState getState() { return state; }
    
    public void setMinMax(float min, float max) {
        this.min = min;
        this.max = max;
        setCurrentValue(this.current);
    }
    
    public void setCurrentValue(float value) {
        valueTextBox.setVisible(false);
        
        float old = current;
        this.current = Math.max(this.min, Math.min(value, this.max));
        
        // try to round with specified precision
        try {
            String tmp = String.format(format, current);
            this.current = Float.parseFloat(tmp);
        }
        catch(Exception e) {}
        
        generateLabel();
        setIsDirty(true);
        
        if (old != current) {
            setIsDirty(true);
            fireActionEvent();
        }
    }
    
    public float getCurrentValue() { return current; }
    
    public String getFormatString() { return format; }
    public void setFormatString(String format) {
        this.format = new String(format);
        generateLabel();
    }
    
    public String getUnitsString() { return units; }
    public void setUnitsString(String u) {
        units = new String(u);
        generateLabel();
    }
    
    public void setLabelWidth(int width) {
        labelWidth = width;
        troughLength = bounds.width - labelWidth - 16;
        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        generateLabel();
    }
    
    private boolean mouseInHandle(float x, float y) {
        x = x - bounds.x;
        y = y - bounds.y;
        if (x >= labelWidth+handlePos-4.5f && x < labelWidth+handlePos+handleLength+4.5f &&
                y>=2 && y<bounds.height-2f) {
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
             
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) return true;
        
        if (state != SliderState.DISABLED) {
            
            // handle double click
            if (MouseIsInside(x, y)) {
                if (button1down && !this.mouseButton1down) {
                    this.mouseButton1down = true; // a press
                    
                    if (valueTextBox.getVisible()) {
                        valueTextBox.setVisible(false);
                        valueTextBox.lostKeyboardFocus();
                    }
                    
                    long now = System.currentTimeMillis();
                    if (previousClickTime > 0 && now - previousClickTime < 300) { // double click
                        
                        valueTextBox.setVisible(true);
                        valueTextBox.setIsEnabled(true);
                        valueTextBox.setTextEditable(true);
                        valueTextBox.setText(String.format(format, this.current).trim());
                        if (!valueTextBox.hasKeyboardFocus()) {
                            valueTextBox.acquireKeyboardFocus();
                        }
                    
                        this.setIsDirty(true);
                        
                        this.previousClickTime = -1;
                        return true;
                    }
                    else { //single click
                        acquireKeyboardFocus();
//                        lostKeyboardFocus();
                    }
                    
                    previousClickTime = now;
                }
                else if (!button1down && this.mouseButton1down) {
                    this.mouseButton1down = false; // a release
                }
            }
            
            if (MouseIsInside(x, y) || hasGrabbed()) {
                
                if (!hasGrabbed() && mouseInHandle(x, y) && button1down) {
                    grabMouse(x, y);
                    grabbedCurrent = current;
                }                
                else if (hasGrabbed() && !button1down) {
                    ungrabMouse();
                }
                
                if (hasGrabbed()) {
                    float old = current;
                    
                    // Vernier control with shift key
                    float scale = 1f;
                    if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                        Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                        
                        scale = 10f;
                    }
                    
                    float newVal = grabbedCurrent + (float)((x - xgrab)/scale)/(float)(troughLength - handleLength) * (max - min);
                                        
                    xgrab = x;
                    grabbedCurrent = newVal;
                    
//                    System.out.println("Slider val: " + x + " " + xgrab + " " + current);
                    setCurrentValue(newVal);
                    
                }
                
                return true;
            }
        }
                
        return false;
    }
    
    @Override
    public void render() {
        
        setIsDirty(false);
        
        float scale = getGuiScale();
        if (scale != labelScale) {
            labelScale = scale;
            generateLabel();
        }
        
        handlePos = Math.round((current - min)/(max - min) * (troughLength - handleLength));
        
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
                }
                
                glBegin(GL_QUADS);
                    glNormal3f(0f, 1f, 0.4f);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);        
                glEnd();
                               
                renderLabel();
                

                    glEnable(GL_BLEND);
                    glEnable(GL_DEPTH_TEST);

                    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 30.0f); // sets shininess
                    glLightModeli(GL_LIGHT_MODEL_LOCAL_VIEWER, GL_FALSE);
                    
//                    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
//                    lightPosition.put(Display.getWidth()/2).put(Display.getHeight()/2).put(10000.0f).put(1f).flip();
//                    glLight(GL_LIGHT0, GL_POSITION, lightPosition);	// sets light position

                    glMatrixMode(GL_MODELVIEW);
                    Main.glPushMatrix();

                    DoubleBuffer eqnBuf = BufferUtils.createDoubleBuffer(4);
                    eqnBuf.put(0.0f).put(0.0f).put(-1.0f).put(0.1f);
                    eqnBuf.flip();
                    glClipPlane(GL_CLIP_PLANE0, eqnBuf);
                    glEnable(GL_CLIP_PLANE0);

                    glDisable(GL_DEPTH_TEST);
glEnable(GL_CULL_FACE);        
glCullFace(GL_FRONT);
                        trough_hemisphere.setColor(0.15f, 0.15f, 0.15f, 0.8f);
                        trough_cylinder.invertNormals(true);
                        trough_hemisphere.invertNormals(true);
                               
                        drawHemisphereHorz(trough_hemisphere, labelWidth, bounds.height/2, 9, 180f);
                        drawHemisphereHorz(trough_hemisphere, labelWidth+troughLength, bounds.height/2, 9, 0f);
                        
                        trough_cylinder.setColor(0.15f, 0.15f, 0.15f, 0.8f);
                        drawCylinderHorz(trough_cylinder, labelWidth, bounds.height/2, 9, troughLength);
                        
                    glDisable(GL_CLIP_PLANE0);

glEnable(GL_CULL_FACE);        
glCullFace(GL_BACK);
                        handle_hemisphere.setColor(0.1f, 0.9f, 0.1f, 0.9f);
                        drawHemisphereHorz(handle_hemisphere, labelWidth+handlePos, bounds.height/2f, 7, 180f);
                        drawHemisphereHorz(handle_hemisphere, labelWidth+handlePos+handleLength, bounds.height/2f, 7, 0f);
                        
                        handle_cylinder.setColor(0.1f, 0.9f, 0.1f, 0.9f);
                        drawCylinderHorz(handle_cylinder, labelWidth+handlePos, bounds.height/2f, 7, handleLength);
                        
                    Main.glPopMatrix();
                    
glDisable(GL_CULL_FACE);

        this.renderChildren();
                        
        Main.glPopAttrib();
    }
        
    private void drawCylinderHorz(Cylinder c, float x, float y, float radius, float length) {
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
            glTranslatef(bounds.x+x, bounds.y+y, 0f);
            glRotatef(90f, 0f, -1f, 0f);
            
            glScalef(radius, radius, length);
            
            c.render();
        glMatrixMode(GL_MODELVIEW);
        Main.glPopMatrix();
    }
    
    private void drawCylinderVert(Cylinder c, float x, float y, float radius, float length) {
//        glPushMatrix();
//            glTranslatef(bounds.x+x, bounds.y+y, 0f);
//            glRotatef(90f, 1f, 0f, 0f);
//            
//            glScalef(radius, radius, length);
//            
//            c.render();
//        glPopMatrix();
    }
    
    private void drawHemisphereHorz(Hemisphere h, float x, float y, float radius, float rotate) {
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
            glTranslatef(bounds.x+x, bounds.y+y, 0f);
            glRotatef(rotate, 0f, 0f, 1f);
            glRotatef(90f, 0f, -1f, 0f);
            
            glScalef(radius, radius, radius);
            h.render();
        glMatrixMode(GL_MODELVIEW);
        Main.glPopMatrix();
    }
    
    private void generateLabel() {

        Font font = stdfont;//new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, Math.round(fontSize * labelScale));        
        
        labelImage = new BufferedImage(Math.round(bounds.width*labelScale), Math.round(bounds.height*2f*labelScale), BufferedImage.TYPE_4BYTE_ABGR);

        String handleText = String.format(format, current);
        if (units.length() > 0) {
            handleText = handleText + units;
        }
        
        handlePos = Math.round((current - min)/(max - min) * (troughLength - handleLength));
        
        Graphics2D gc = (Graphics2D) labelImage.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        FontRenderContext frc = gc.getFontRenderContext();                
        FontMetrics metrics = gc.getFontMetrics(font);
        
        Rectangle2D textBound = font.getStringBounds(getTitle(), frc);
        
        float titleWidth = metrics.stringWidth(getTitle());
        float handleTextWidth = metrics.stringWidth(handleText);
        float textHeight = (float)textBound.getHeight();//metrics.getMaxAscent() + metrics.getMaxDescent();
        float textVPos = bounds.height*labelScale  - (bounds.height*labelScale - textHeight)/2f;
        textVPos = bounds.height*labelScale - textHeight  + (bounds.height*labelScale - (float)textBound.getHeight() - metrics.getDescent())/2f + metrics.getAscent();
        
        float textHPos = 8 * labelScale;
        float handleTextHPos = labelScale * (labelWidth + handlePos + handleLength/2) - handleTextWidth/2;
        //handleTextHPos *= labelScale;
        //handleTextHPos = Math.min(handleTextHPos, labelWidth + troughLength + handleLength/2- handleTextWidth);

        
//        if (newWidth > labelWidth) {
//            labelWidth = newWidth;
//            labelImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_4BYTE_ABGR);            
//        }

        gc.setFont(font);
        gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        gc.fillRect(0, 0, labelImage.getWidth(), labelImage.getHeight());

        gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
        gc.drawString(getTitle(), textHPos+1*labelScale, textVPos+1*labelScale + textHeight);
        
        gc.drawString(handleText, handleTextHPos+1*labelScale, textVPos+1*labelScale);
        
        if (this.state != SliderState.DISABLED) {
            gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
        }
        else {
            gc.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));            
        }
        gc.drawString(getTitle(), textHPos, textVPos + textHeight);
        
        gc.drawString(handleText, handleTextHPos, textVPos);
        
//        gc.drawRect((int)(textBound.getX() + textHPos), (int)(textBound.getY() + textVPos + bounds.height * labelScale), (int)textBound.getWidth(), (int)textBound.getHeight());
//        gc.drawRect((int)textHPos, (int)textVPos + 3*metrics.getDescent(), (int)titleWidth, (int)textHeight - 2*metrics.getDescent());
                
    }

    public void renderLabel() {
        
        if (labelImage == null) {
            generateLabel();
        }
        
        if (labelImage != null) {
//            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);
//
//            byte buf[] = (byte[]) labelImage.getRaster().getDataElements(0, 0, labelImage.getWidth(), labelImage.getHeight(), null);
//
//
//        //            glEnable(GL_BLEND);
//        //            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//                    glPixelZoom(1.0f, -1.0f);
//                    //glRasterPos2d(-1.0, 1.0);
//
//                    glDisable(GL_CLIP_PLANE0);
//                    glDisable(GL_CLIP_PLANE1);
//                    glDisable(GL_DEPTH_TEST);
//
//                    ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
//                    bbuf.put(buf, 0, buf.length);
//                    bbuf.flip();
//                    glRasterPos2f(bounds.x, bounds.y+labelImage.getHeight()/labelScale);
//                    glDrawPixels(labelImage.getWidth(), labelImage.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
//
//                    //      }
//                    //     glPopMatrix();
//                    //  }
//                    //  glMatrixMode(GL_PROJECTION);
//                    //   glPopMatrix();
////                    glDisable(GL_BLEND);
//
//            Main.glPopAttrib();
            
            this.renderBufferedImageViaTexture(labelImage,
                    new Rectangle(bounds.x, bounds.y,
                                    labelImage.getWidth(), labelImage.getHeight())
                );
        }        
    }
    
    @Override
    public void update(Object newValue) {
        try {
            if (newValue instanceof Float) {
                setCurrentValue((Float)newValue);
            }
            else if (newValue instanceof String) {
                setCurrentValue(Float.parseFloat((String)newValue));
            }
            else {
                throw new Exception("Wrong type");
            }
        }
        catch(Exception e) {
            Logger.getGlobal().log(Level.WARNING, this + " " + this.getTitle() +  ": Wrong or NULL new value. (" + newValue.toString() + ")");
        }
    }

    @Override
    public void release() {
        this.trough_cylinder.release();
        this.handle_cylinder.release();
        this.trough_hemisphere.release();
        this.handle_hemisphere.release();
        super.release(); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void doBinding(Model model) {
        if (model != null && !getCommand().isBlank()) {
            if (persistAsString) {
                model.setAttribute(this.getCommand(), String.format(format, this.getCurrentValue()));
            }
            else {
                model.setAttribute(this.getCommand(), this.getCurrentValue());
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e!=null) {
            switch(e.getActionCommand()) {
                case "ENTER_KEY":
                    try {
                        float val = Float.parseFloat(valueTextBox.getText());
                        this.setCurrentValue(val);
                    }
                    catch(Exception ex) {}
                    
                    valueTextBox.setVisible(false);
                    valueTextBox.lostKeyboardFocus();
                    setIsDirty(true);
                    break;
                case "ESCAPE_KEY":
                    valueTextBox.setVisible(false);
                    valueTextBox.lostKeyboardFocus();
                    break;
                case "TEXTBOX_LOST_FOCUS":
                    valueTextBox.setVisible(false);
                    break;
            }
        }
    }

   
}
