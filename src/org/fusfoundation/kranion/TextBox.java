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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import java.nio.*;
import static org.lwjgl.opengl.GL14.glWindowPos2f;

/**
 *
 * @author john
 */
public class TextBox extends GUIControl implements Animator {
    
    private String text = "";
    private Vector4f color = new Vector4f(0.25f, 0.25f, 0.25f, 1f);
    private Vector4f textColor = new Vector4f(1f, 01f, 1f, 1f);
    private BufferedImage labelImage;
    private int fontSize = 16;
    private float labelScale = 1f;
    private float textwidth;
    private long caretBlinkStartTime;
    private boolean showCaret = true;
    private boolean showBackgroundBox = true;
    private HPosFormat textHPos = HPosFormat.HPOSITION_LEFT;
    private Font textfont = stdfont;
    
    private int cursorPos = 0;
    
    public TextBox() {
        textfont = stdfont;
        this.setAcceptsKeyboardFocus(true);
    }
    
    public TextBox(float x, float y, float width, float height, String text) {
        super.setBounds(x, y, width, height);
        textfont = stdfont;
        this.setText(text);
        this.setAcceptsKeyboardFocus(true);
    }
    
    public TextBox(float x, float y, float width, float height, String text, ActionListener listener) {
        super.setBounds(x, y, width, height);
        textfont = stdfont;
        this.setText(text);
        this.addActionListener(listener);
        this.setAcceptsKeyboardFocus(true);
    }
    
    public void setText(String text) {
        if (text == null) {
            this.text = "";
        }
        else {
            this.text = new String(text);
        }
    }
    
    public void setTextFont(Font font) {
        textfont = font;
    }
    
    public void setTextHorzAlignment(HPosFormat hpos){
        textHPos = hpos;
        setIsDirty(true);
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
        if (red   != color.x ||
            green != color.y ||
            blue  != color.z ||
            alpha != color.w) {
            
            textColor.set(red, green, blue, alpha);
            setIsDirty(true);
        }
    }
    
    public String getText() {
        return text;
    }
    
    public void showBackgroundBox(boolean show) {
        this.showBackgroundBox = show;
        setIsDirty(true);
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (this.MouseIsInside(x, y) && (button1down || button2down)) {
            this.acquireKeyboardFocus();
            this.caretBlinkStartTime = System.currentTimeMillis();
            this.showCaret = true;
            this.cursorPos = this.calculateCaretPos(text, bounds, textfont, x, y, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT, this.cursorPos);
//            System.out.println("cursor pos = " + cursorPos);
            return true;
        }
        return super.OnMouse(x, y, button1down, button2down, dwheel);
    }

    @Override
    public void OnKeyboard(int keyCode, char keyChar, boolean isKeyDown) {
        
        if (!this.getTextEditable()) return;
        
        if (isKeyDown) {
//            System.out.println("Key: " + keyChar);
            
            
            if (cursorPos > -1) {
                if (keyCode == Keyboard.KEY_LEFT) {
                    cursorPos--;
                    cursorPos = Math.min(text.length(), Math.max(cursorPos, 0));
                }
                else if (keyCode == Keyboard.KEY_RIGHT) {
                    cursorPos++;
                    cursorPos = Math.min(text.length(), Math.max(cursorPos, 0));
                }
                else if (keyCode == Keyboard.KEY_BACK) {
                    if (cursorPos > 0) {
                        this.text = text.substring(0, cursorPos-1) + text.substring(cursorPos);
                        cursorPos--;
                        cursorPos = Math.min(text.length(), Math.max(cursorPos, 0));
                        fireActionEvent();
                    }
                }
                else if (keyCode == Keyboard.KEY_DELETE) {
                    if (cursorPos < text.length()) {
                        this.text = text.substring(0, cursorPos) + text.substring(cursorPos+1);
                        cursorPos = Math.min(text.length(), Math.max(cursorPos, 0));
                        fireActionEvent();
                    }
                }
                else if (keyCode == Keyboard.KEY_LSHIFT ||
                        keyCode == Keyboard.KEY_RSHIFT ||
                        keyCode == Keyboard.KEY_LCONTROL ||
                        keyCode == Keyboard.KEY_RCONTROL ||
                        keyCode == Keyboard.KEY_LMETA ||
                        keyCode == Keyboard.KEY_RMETA ||
                        keyCode == Keyboard.KEY_LMENU ||
                        keyCode == Keyboard.KEY_RMENU ||
                        keyCode == Keyboard.KEY_UP ||
                        keyCode == Keyboard.KEY_DOWN ||
                        keyCode == Keyboard.KEY_CAPITAL 
                        ) {
                    // do nothing
                }
                else {
                    cursorPos = Math.min(text.length(), Math.max(cursorPos, 0));
                    this.text = text.substring(0, cursorPos) + keyChar + text.substring(cursorPos);
                    cursorPos++;

                    fireActionEvent();
                }

                setIsDirty(true);
            }
        }
    }
    
    @Override
    public void render() {
        
        if (!this.getVisible()) return;
        
        float scale = getGuiScale();
        if (scale != labelScale) {
            labelScale = scale;
//            generateLabel();
        }
        
            Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT | GL_COLOR_BUFFER_BIT);

            if (isEnabled) {
                glColor4f(color.x, color.y, color.z, color.w);
                if (this.mouseInside) {
                    glColor4f(color.x * 1.15f, color.y * 1.15f, color.z * 1.15f, color.w);
                } else {
                    glColor4f(color.x, color.y, color.z, color.w);
                }
            } else {
                glColor4f(color.x, color.y, color.z, 0.5f);
            }
            
        if (showBackgroundBox) {

            glBegin(GL_QUADS);
            glNormal3f(0f, 0f, 1f);
            glVertex2f(bounds.x, bounds.y);
            glVertex2f(bounds.x + bounds.width, bounds.y);
            glNormal3f(0f, 0.4f, 0.4f);
            glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
            glVertex2f(bounds.x, bounds.y + bounds.height);
            glEnd();

            glEnable(GL_BLEND);
            //glBlendFunc(GL_SRC_ALPHA, GL_ONE);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glLineWidth(1f);
            glColor4f(0.1f, 0.1f, 0.1f, 1f);
            glNormal3f(0f, 0f, 1f);
            glTranslatef(1, -1, 0);
            glBegin(GL_LINES);
            glVertex2f(bounds.x, bounds.y + 2);
            glVertex2f(bounds.x, bounds.y + bounds.height);
            glVertex2f(bounds.x, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width - 2, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width - 2, bounds.y + 2);
            glEnd();

            glColor4f(0.35f, 0.35f, 0.35f, 1f);
            glNormal3f(0f, 0f, 1f);
            glTranslatef(-1, 1, 0);
            glBegin(GL_LINES);
            glVertex2f(bounds.x, bounds.y + 1);
            glVertex2f(bounds.x, bounds.y + bounds.height);
            glVertex2f(bounds.x, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
            glVertex2f(bounds.x + bounds.width, bounds.y + 1);
            glEnd();
        }

            //renderLabel();
            renderText(getText(), bounds, textHPos, this.showCaret, this.cursorPos);

            Main.glPopAttrib();
        
        renderText(getTitle(), new Rectangle(bounds.x - 150, bounds.y, 150, bounds.height), HPosFormat.HPOSITION_RIGHT, false, -1);
        
        setIsDirty(false);
    }
        
    private void renderText(String str, Rectangle rect, HPosFormat hpos, boolean showCaret, int cursPos) {
        renderText(str, rect.shrinkHorz(8f), textfont, new Color(textColor.x, textColor.y, textColor.z, textColor.w), true, VPosFormat.VPOSITION_CENTER, hpos, showCaret, cursPos);
    }
        
//    private void generateLabel() {
//
//        Font font = new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, Math.round(fontSize * labelScale));        
//        
//        labelImage = new BufferedImage(bounds.getIntWidth(), bounds.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);
//
//        Graphics2D gc = (Graphics2D) labelImage.getGraphics();
//        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//                
//        FontMetrics metrics = gc.getFontMetrics(font);
//        float newWidth = metrics.stringWidth(getText());
//        float textHeight = metrics.getHeight();
//        float textVPos = bounds.height/2 + textHeight/4;
//        
//        this.textwidth = newWidth;
//        
//        float textHPos = 5;
//        
//        gc.setFont(font);
//        gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
//        gc.fillRect(0, 0, labelImage.getWidth(), labelImage.getHeight());
//
//        gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
//        gc.drawString(getText(), textHPos+1, textVPos+1);
//        
//        if (isEnabled) {
//            gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
//            
//            gc.drawLine((int)15, 0, (int)15, labelImage.getHeight());
//       }
//        else {
//            gc.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));            
//        }
//        
//        gc.drawString(getText(), textHPos, textVPos);
//                
//    }

//    public void renderLabel() {
//        // Overlay demographics
//        
//        if (labelImage == null) {
//            generateLabel();
//        }
//        
//        if (labelImage != null) {
//            glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);
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
//                    glRasterPos2f(bounds.x, bounds.y+labelImage.getHeight());
//                    glDrawPixels(labelImage.getWidth(), labelImage.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
//
//                    //      }
//                    //     glPopMatrix();
//                    //  }
//                    //  glMatrixMode(GL_PROJECTION);
//                    //   glPopMatrix();
////                    glDisable(GL_BLEND);
//
//            glPopAttrib();
//        }        
//    }    

    @Override
    public void update(Object newValue) {
        if (newValue instanceof String) {
            setText((String)newValue);
            this.labelImage = null;
            setIsDirty(true);
        }
    }


    @Override
    public boolean isAnimationDone() {
        if (hasKeyboardFocus()) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void cancelAnimation() {
    }

    @Override
    public void advanceFrame() {
        if (this.hasKeyboardFocus()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - caretBlinkStartTime > 333) {
                caretBlinkStartTime = currentTime;
                showCaret = !showCaret;
                setIsDirty(true);
            }
        }
    }
}
