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
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.fusfoundation.kranion.model.Model;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author john
 */
public class PullDownSelection extends GUIControl implements Animator, GUIControlModelBinding {
    private BufferedImage labelImage, itemImage;
    private Vector4f color = new Vector4f(0.35f, 0.35f, 0.35f, 1f);
    private int fontSize = 16;
    private float labelScale = 1f;
    private AnimatorUtil anim = new AnimatorUtil();
    private float flyScale = 0f;
    private float itemHeight = 25f;
    private float dx = 300f, dy = 10 * itemHeight;
    private int hoverItem = -1;
    private int selectedItem = -1;
    private long flyinDelay = -1;
    
    private boolean button1pressed = false;
    private boolean button1released = false;

    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<Object> attachments = new ArrayList<>();
    
    public PullDownSelection(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
        setTitle("Test");
    }
    
    public PullDownSelection(int x, int y, int width, int height, ActionListener listener) {
        setBounds(x, y, width, height);
        setTitle("Test");
        addActionListener(listener);
        
//        items.add("Sonication 1");
//        items.add("Sonication 2");
//        items.add("Sonication 3");
//        items.add("Sonication 4");
//        items.add("Sonication 5");
//        items.add("Sonication 6");
//        items.add("Sonication 7");
    }
    
    public void addItem(String item) {
        addItem(item, null);
    }
    
    public void addItem(String item, Object atmt) {
        items.add(item);
        attachments.add(atmt);
        setIsDirty(true);
    }
    
    public void setItem(int i, String item) {
        setItem(i, item, null);
    }
    
    public void setItem(int i, String item, Object atmt) {
        items.set(i, item);
        attachments.set(i, atmt);
        setIsDirty(true);
    }
    
    public void addItem(int i, String item) {
        addItem(i, item, null);
    }
    
    public void addItem(int i, String item, Object atmt) {
        items.add(i, item);
        attachments.add(i, atmt);
        setIsDirty(true);
    }
    
    public int getSelectionIndex() {
        return this.selectedItem;
    }
    
    public String getItem(int index) {
        try {
            return items.get(index);
        }
        catch(ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public int getItemIndex(String itemName) {
        return items.indexOf(itemName);
    }
    
    public Object getAttachment(int index) {
        try {
            return attachments.get(index);
        }
        catch(ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public void setSelectionIndex(int i) {
        if (i>=0 && i<items.size()) {
            if (selectedItem != i) {
                selectedItem = i;
                setTitle(items.get(i));
                labelImage = null;
                setIsDirty(true);
            }
        }
    }
    
    public void clear() {
        items.clear();
        attachments.clear();
        selectedItem = -1;
        hoverItem = -1;
        setTitle("");
        labelImage = null;
        setIsDirty(true);
    }
    
    @Override
    public void render() {
        
        float scale = getGuiScale();
        if (scale != labelScale) {
            labelScale = scale;
        }
        
        dx = this.bounds.width;
        dy = items.size() * itemHeight * labelScale;
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT | GL_COLOR_BUFFER_BIT);
        
                if (getIsEnabled()) {
                    glColor4f(color.x, color.y, color.z, color.w);
                    if (this.mouseInside) {
                        glColor4f(color.x * 1.15f, color.y * 1.15f, color.z * 1.15f, color.w);
                    } else {
                        glColor4f(color.x, color.y, color.z, color.w);
                    }
                } else {
                    glColor4f(color.x, color.y, color.z, 0.5f);
                }

                float nx = bounds.x;
                float ny = bounds.y;

                float nw = bounds.width;
                float nh = -(dy)*flyScale;
                        
                
                glBegin(GL_QUADS);
                    glNormal3f(0f, 1f, 0.4f);
                    glVertex2f(bounds.x, bounds.y);
                    glVertex2f(bounds.x+bounds.width, bounds.y);
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                    glVertex2f(bounds.x, bounds.y+bounds.height);                                        
                glEnd();
                                    
                Rectangle extents = new Rectangle(bounds);                
                extents.y = bounds.y + dy*flyScale;
                extents.height = dy*flyScale;
                                               
                if (this.flyScale > 0f) {
//                glScissor((int)extents.x, (int)extents.y, (int)extents.width, (int)extents.height);
//                glEnable(GL_SCISSOR_TEST);
                
            glStencilMask(0xff);
            
glDisable(GL_SCISSOR_TEST);

glEnable(GL_STENCIL_TEST);

glStencilFunc(GL_ALWAYS, 1, 0xff);
glStencilOp(GL_ZERO, GL_REPLACE, GL_REPLACE);
                    glDisable(GL_LIGHTING);
                    glBegin(GL_QUADS);
                        glColor4f(0.10f, 0.10f, 0.10f, 0.925f);                       
                        glVertex2f(nx, ny);
                        glVertex2f(nx+nw, ny);

                        glColor4f(0.05f, 0.05f, 0.05f, 0.925f);
                        glVertex2f(nx+nw, ny+nh);
                        glVertex2f(nx, ny+nh);
                    glEnd();
glStencilFunc(GL_EQUAL, 1, 0xff);
glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                    if (selectedItem > -1) {
                        glNormal3f(0f, 0f, -1f);                   
                        glColor4f(0.2f, 0.6f, 0.2f, 0.6f);

                        glBegin(GL_QUADS);
                            glVertex2f(nx, ny  - selectedItem * itemHeight + (1f-flyScale)*dy);
                            glVertex2f(nx+nw, ny - selectedItem * itemHeight + (1f-flyScale)*dy);

                            glVertex2f(nx+nw, ny  - (selectedItem+1) * itemHeight + (1f-flyScale)*dy);
                            glVertex2f(nx, ny - (selectedItem+1) * itemHeight + (1f-flyScale)*dy); 
                        glEnd();
                    }
                }
                
                        
                if (flyScale == 1f && hoverItem > -1) {
                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                    glLineWidth(2f);
                    glColor4f(0.5f, 0.5f, 0.5f, 0.4f);
                    
                    glBegin(GL_QUADS);

                        glVertex2f(nx, ny  - hoverItem * itemHeight);
                        glVertex2f(nx+nw, ny - hoverItem * itemHeight);

                        glVertex2f(nx+nw, ny  - (hoverItem+1) * itemHeight);
                        glVertex2f(nx, ny - (hoverItem+1) * itemHeight);                            
                   glEnd();
                }
                                
                if (flyScale > 0f) {
                    for (int i=0; i<items.size(); i++) {
                        renderItem(i);
                    }
                }
                
                glDisable(GL_STENCIL_TEST);
                
                renderText(getTitle(), bounds, null, new Color(1.0f, 1.0f, 1.0f, 1.0f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_CENTER);
                        
        Main.glPopAttrib();
        
        setIsDirty(false);
    }
    
    private void renderItem(int i) {
        
        
        Rectangle itemBounds = new Rectangle(
                bounds.x,
                bounds.y + (bounds.height - (i+2)*bounds.height - dy*(flyScale) + items.size()*bounds.height),
                bounds.width,
                bounds.height);
        
        itemBounds.x += 10;
        itemBounds.width -= 10;
        renderText(items.get(i), itemBounds, null, new Color(1.0f, 1.0f, 1.0f, 1.0f), true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT);
    }        
        

    @Override
    public boolean MouseIsInside(float x, float y) {
        
        Rectangle testMainRect = new Rectangle(bounds);
//        testMainRect.translate(Math.round(dx*flyScale)-dx, Math.round(dy*flyScale)-dy);
        testMainRect.width = dx;
        testMainRect.height = -dy;
                
        if (bounds.contains(x, y) || (flyScale == 1f && testMainRect.contains(x, y))) {           
            return true;
        }
        
        return false;
    }
    
    private void flyout() {
//        System.out.println("flyout" + flyScale);
        anim.set(flyScale, 1f, 0.35f);
    }
    
    private void flyin() {
//        System.out.println("flyin" + flyScale);
        anim.set(flyScale, 0f, 0.35f);
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (!anim.isAnimationDone()) return false;
        
        if (super.OnMouse((int)((x-bounds.x) + bounds.x), (int)((y-bounds.y) + bounds.y), button1down, button2down, dwheel)) {
            return true;
        }
        
        if (button1down) {
            if (!button1pressed) {
                button1pressed = true;
                button1released = false;
            }
        }
        else {
            if (button1pressed) {
                button1pressed = false;
                button1released = true;
            }
        }
        
        if (MouseIsInside(x, y)) {
            
            flyinDelay = -1;
            
            if (flyScale == 0f && button1released) {
                button1released = false;
                bringToTop();
                flyout();
            }
            else {
                int index = Math.round(Math.max(0, Math.min(items.size(), -(y-bounds.y)/itemHeight - 0.5f)));
                if (index != hoverItem) {
                    setIsDirty(true);
                    hoverItem = index;
                }
            }
 
            if (flyScale == 1f && button1pressed) {
                grabMouse(x, y);
                if (selectedItem != hoverItem &&
                    hoverItem >=0 && hoverItem < items.size()) {
                    
                    setIsDirty(true);
                    selectedItem = hoverItem;
                    setTitle(items.get(selectedItem));
                    labelImage = null;
                }
                
                return true;
            }
            else if (button1released) {
                ungrabMouse();
                flyin();
                this.fireActionEvent();
            }
            else {
                // reset this so we dont trigger more than once
                button1released = false;
                return true;
            }
        }
        else if (!hasGrabbed() && grabbedChild == null) {
            if (flyScale == 1f) {
//                flyin();
                flyinDelay = System.currentTimeMillis() + 400;
            }
        }
        
        // reset this so we dont trigger more than once
        button1released = false;
        
        return false;
    }
    
    @Override
    public void update(Object newValue) {
        if (newValue != null) {
            this.setSelectionIndex(((Integer)newValue).intValue());
        }
    }    

    @Override
    public boolean isAnimationDone() {
        return anim.isAnimationDone();
    }

    @Override
    public void cancelAnimation() {
        anim.cancelAnimation();
    }

    @Override
    public void advanceFrame() {
        if (isAnimationDone() && flyinDelay == -1) return;
        
        anim.advanceFrame();
        flyScale = anim.getCurrent();
        
        if (flyinDelay != -1 && (System.currentTimeMillis() > flyinDelay)) {
            flyinDelay = -1;
            flyin();
        }
    }
    
    @Override
    public boolean getIsDirty() {
        return super.getIsDirty() || !isAnimationDone();
    }

    @Override
    public void doBinding(Model model) {
        if (model != null && !getCommand().isBlank()) {
            model.setAttribute(this.getCommand(), this.getSelectionIndex());
        }
    }
    
}
