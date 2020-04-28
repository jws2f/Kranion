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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.Display;
import static  org.lwjgl.opengl.GL11.*;

/**
 *
 * @author john
 */
public class ProgressBar extends GUIControl {

    private float min, max, value;
    private BufferedImage labelImage;
    private int labelWidth = 85;
    private int labelHeight = 40;
    private int fontSize = 32;
    private String format = new String("%3.0f");
    
    private float greenLevel = 0.7f;
    private float yellowLevel = 0.5f;
    
    public ProgressBar() {
        min = 0f;
        max = 100f;
        value = -1f;
        
        setBounds(550, 50, 600, 40);
    }
    
    public void setGreenLevel(float level) {
        if (greenLevel != level) {
            setIsDirty(true);
        }
        greenLevel = level;
    }
    
    public void setYellowLevel(float level) {
        if (yellowLevel != level) {
            setIsDirty(true);
        }
        yellowLevel = level;
    }
    
    public void setMinMax(float low, float high) {
        if (min != low || max != high) {
            setIsDirty(true);
        }
        min = low;
        max = high;
    }
    
    public void setFormatString(String fmt) {format = fmt;}
    public void setFontSize(int size) {
        if (fontSize != size) {
            setIsDirty(true);
        }
        fontSize = size > 5 ? size : 5;
    }
    
    public void setValue(float val) { 
        if (value != val) {
            setIsDirty(true);
        }
        value = val;
        generateLabel();
    }
    
    @Override
    public void setBounds(float x, float y, float width, float height)
    {
        setIsDirty(true);
        super.setBounds(x, y, width, height);
        labelHeight = Math.round(height) + 2;
    }
    
    @Override
    public void render() {
        setIsDirty(false);

        if (value < 0 || !getVisible()) {
            return;
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT);
        
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
            glLoadIdentity();

            org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, Display.getWidth(), 0.0f, Display.getHeight());

            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
                glLoadIdentity();
                
                
                glDisable(GL_DEPTH_TEST);
                glDisable(GL_LIGHTING);
//                glEnable(GL_BLEND);
//                glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glLineWidth(1f);
                
                generateLabel();
               
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                glColor4f(0.10f, 0.10f, 0.10f, 0.6f);
                glBegin(GL_QUADS);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);
                glEnd();
                
                float ratio = Math.min(1f, Math.max((value-min)/(max-min), 0f));
                float barOffset = (bounds.width-labelWidth-1) * ratio + labelWidth;
                
                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                
                if (ratio > greenLevel) {
                    glColor3f(0.1f, 0.6f, 0.1f);
                }
                else if (ratio > yellowLevel) {
                    glColor3f(0.75f, 0.75f, 0.1f);
                }
                else {
                    glColor3f(0.6f, 0.1f, 0.1f);
                }
                
                glBegin(GL_QUADS);
                    glVertex2f(bounds.x+labelWidth+1, bounds.y);
                    glVertex2f(bounds.x+barOffset+1, bounds.y);
                    glVertex2f(bounds.x+barOffset+1, bounds.y+bounds.height);
                    glVertex2f(bounds.x+labelWidth+1, bounds.y+bounds.height);
                glEnd();
                
                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                glColor4f(0.2f, 0.2f, 0.2f, 0.8f);
                glBegin(GL_QUADS);
                    glVertex2f(bounds.x+labelWidth, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x+labelWidth, bounds.y+bounds.height);
                glEnd();
                
                renderLabel();

            glMatrixMode(GL_MODELVIEW);
            Main.glPopMatrix();
            
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
        
        Main.glPopAttrib();
        
        glMatrixMode(GL_MODELVIEW);
        
        setIsDirty(false);
        
    }

    @Override
    public void release() {
        
    }
    private synchronized void generateLabel() {

        Font font = new Font("Helvetica", Font.BOLD | Font.TRUETYPE_FONT, fontSize);
      
        String svalue = String.format(format, value);
        
        
        labelImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) labelImage.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        FontMetrics metrics = gc.getFontMetrics(font);
        int newWidth = metrics.stringWidth(svalue) + 12;
        int textHeight = metrics.getHeight();
        int textVPos = labelHeight/2 + textHeight/4;
        
        if (newWidth > labelWidth) {
            labelWidth = newWidth;
            labelImage = new BufferedImage(labelWidth, labelHeight, BufferedImage.TYPE_4BYTE_ABGR);            
        }

        gc.setFont(font);
        gc.setColor(new Color(0.25f, 0.25f, 0.25f, 0.9f));
        gc.fillRect(0, 0, labelImage.getWidth(), labelImage.getHeight());
//        gc.setColor(new Color(0.2f, 0.2f, 0.2f, 0.5f));
//        gc.drawRect(0, 0, labelImage.getWidth() - 1, labelImage.getHeight() - 1);

        gc.setColor(new Color(0.1f, 0.1f, 0.1f, 0.6f));
        gc.drawString(svalue, 6, textVPos+1);
        gc.setColor(new Color(0.8f, 0.8f, 0.8f, 1.0f));
        gc.drawString(svalue, 5, textVPos);
                
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
            return true;
        }
        else if (this.MouseIsInside(x, y) && (value>=0 && isVisible) && (button1down || button2down)) {
            // eat all mouse clicks if we are visible
            return true;
        }
        else {
            return false;
        }
    }
    
    public synchronized void renderLabel() {
        // Overlay demographics
        
        if (labelImage == null) {
            generateLabel();
        }
        
        if (labelImage != null) {
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);

            byte buf[] = (byte[]) labelImage.getRaster().getDataElements(0, 0, labelImage.getWidth(), labelImage.getHeight(), null);


        //            glEnable(GL_BLEND);
        //            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glPixelZoom(1.0f, -1.0f);
                    //glRasterPos2d(-1.0, 1.0);

                    glDisable(GL_CLIP_PLANE0);
                    glDisable(GL_CLIP_PLANE1);
                    glDisable(GL_DEPTH_TEST);

                    ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
                    bbuf.put(buf, 0, buf.length);
                    bbuf.flip();
                    glRasterPos2f(bounds.x, bounds.y+labelHeight-1);
                    glDrawPixels(labelImage.getWidth(), labelImage.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

                    //      }
                    //     glPopMatrix();
                    //  }
                    //  glMatrixMode(GL_PROJECTION);
                    //   glPopMatrix();
                    glDisable(GL_BLEND);

            Main.glPopAttrib();
        }        
    }

}
