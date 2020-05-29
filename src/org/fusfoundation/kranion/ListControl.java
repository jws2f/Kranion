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
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import static org.fusfoundation.kranion.Main.glPopAttrib;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_LIGHTING_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_POLYGON_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TRANSFORM_BIT;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glVertex2f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class ListControl extends GUIControl {
    private Vector4f color = new Vector4f(0.25f, 0.25f, 0.25f, 1f);
    private int vscroll = 0;
    private float normVscroll = 0;
    private int hoverItem = -1;
    private int selectedItem = -1;
    private boolean mouseButton1down = false;
    private long previousClickTime = -1;

    private class Pair implements Comparable<Pair>{
        public Pair(String key, Object val) {
            this.key = key;
            this.value = val;
        }
        public String key;
        public Object value;

        @Override
        public int compareTo(Pair o) {
            return key.compareTo(o.key); // Alphabetical by file name
        }
    }
    
    public ListControl(int x, int y, int width, int height, ActionListener listener) {
        setBounds(x, y, width, height);
        addActionListener(listener);
    }
    
    private final ArrayList<Pair> items = new ArrayList<>();
    
    public void setScroll(float val) {
        normVscroll = Math.min(1, Math.max(0, val));
        vscroll = Math.round(normVscroll * Math.max(0, size() - itemsDisplayed()) * 20);
        setIsDirty(true);
    }
    
    public float getScroll() {
        return normVscroll;
    }
    
    public int getSelected() {
        if (selectedItem != -1 && selectedItem < items.size()) {
            return selectedItem;
        }
        else {
            return -1;
        }
    }
    
    public Object getSelectedKey() {
        if (selectedItem != -1 && selectedItem < items.size()) {
            return items.get(selectedItem).key;
        }
        else {
            return null;
        }
    }

    public Object getSelectedValue() {
        if (selectedItem != -1 && selectedItem < items.size()) {
            return items.get(selectedItem).value;
        }
        else {
            return null;
        }
    }
    
    public void addItem(String name, Object value) {
        items.add(new Pair(name, value));
        setIsDirty(true);
    }
    
    public void addItem(int index, String name, Object value) {
        items.add(index, new Pair(name, value));
        setIsDirty(true);
    }
        
    public void sort() {
        Collections.sort(items);
    }
    
    public void removeItem(int index) {
        items.remove(index);
        setIsDirty(true);
    }
    
    public int itemsDisplayed() {
        return bounds.getIntHeight()/20;
    }
    
    public int size() { return items.size(); }
    
    public void clear() {
        items.clear();
        hoverItem = selectedItem = -1;
        setIsDirty(true);
}

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
            return true;
        }
        else if (isEnabled) {
            if (this.MouseIsInside(x, y)) {
                if (dwheel != 0) {
                    
                    float val = getScroll() - (dwheel/120)/Math.max(1f, (size() - itemsDisplayed()));
                    
                    setScroll(val);
                    
                    this.fireActionEvent("listWheel");
                    
                    return true;
                }
                else if (!mouseButton1down) {
                    int item = Math.round((bounds.height - y + vscroll) / 20) + 2;
                    
                    if (item != hoverItem) {
                        hoverItem = item;
                        setIsDirty(true);
                    }

                    if (button1down) {
                        grabMouse(x, y);
                        mouseButton1down = true; // item selected by left mouse button
                    }

                    return true;
                }
                else if (!button1down && mouseButton1down) {
                    mouseButton1down = false;
                    ungrabMouse();
                    
                    selectedItem = hoverItem;
                    hoverItem = -1;
                    
                    long now = System.currentTimeMillis();
                    if (previousClickTime > 0 && now - previousClickTime < 300) {
                        fireActionEvent("doubleClick");
                        System.out.println("dbClick interval: " + (now - previousClickTime));
                        this.previousClickTime = -1;
                    }
                    else {
                        fireActionEvent();
                        System.out.println("Click interval: " + (now - previousClickTime));
                        previousClickTime = now;
                    }
                    setIsDirty(true);
                    
                    return true;
                }
                else {
                    return true;
                }
            } else {
                if (mouseButton1down && !button1down) {
                    ungrabMouse();
                    mouseButton1down = false;
                    hoverItem = -1;
                    setIsDirty(true);
                    return true;
                }
                else {
                    if (hoverItem != -1) {
                        hoverItem = -1;
                        setIsDirty(true);
                        }
 
                    if (mouseButton1down) {
                        return true;
                    } else {
                        return false;
                    }
                }
                
            }
        } else {
            return false;
        }

    }

    @Override
    public void render() {
        if (!isVisible) return;
        
//        System.out.println("ListControl::render");
        
//        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT);
        
        setIsDirty(false);
        
        Rectangle itemBounds = new Rectangle();
        itemBounds.height = 20;
        itemBounds.width = bounds.width;
        itemBounds.x = bounds.x;
        itemBounds.y = bounds.height + bounds.y - 20 + vscroll;
//        
//                        if (this.mouseInside) {
//                            glColor4f(color.x*1.15f, color.y*1.15f, color.z*1.15f, color.w);                            
//                        }
//                        else {                           
                            glColor4f(color.x, color.y, color.z, color.w);                            
//                        }
                        
                GUIControl p = this.parent;
                Rectangle parentBounds = new Rectangle();
                if (p!=null) {
                    parentBounds = p.getBounds();
                }
                
                Vector2f screenOrig = getScreenCoords(0f, 0f, 0f);
                
                
                    glBegin(GL_QUADS);
//                        glNormal3f(0f, 1f, 0.4f);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(bounds.x, bounds.y);
                        glVertex2f(bounds.x+bounds.width, bounds.y);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(bounds.x+bounds.width, bounds.y+bounds.height);
                        glVertex2f(bounds.x, bounds.y+bounds.height);        
                    glEnd();
                    
                glEnable(GL_SCISSOR_TEST);
                glScissor((int)(bounds.x + screenOrig.x), (int)(bounds.y + screenOrig.y), (int)bounds.width, (int)bounds.height);
        
                    int selected = 0;           
                    for (Pair item: items) {
                        
                        if (itemBounds.intersection(bounds).height > 0) {


                            if (selected == hoverItem) {
                                glBegin(GL_QUADS);
                                glColor4f(0.3f, 0.3f, 0.3f, 1);
                                glNormal3f(0f, 0f, 1f);
                                glVertex2f(itemBounds.x, itemBounds.y);
                                glVertex2f(itemBounds.x + itemBounds.width, itemBounds.y);
                                glVertex2f(itemBounds.x + itemBounds.width, itemBounds.y + itemBounds.height);
                                glVertex2f(itemBounds.x, itemBounds.y + itemBounds.height);
                                glEnd();
                            }
                            
                            if (selected == selectedItem) {
                                glBegin(GL_QUADS);
                                glColor4f(0.2f, 0.6f, 0.2f, 0.6f);
                                glNormal3f(0f, 0f, 1f);
                                glVertex2f(itemBounds.x, itemBounds.y);
                                glVertex2f(itemBounds.x + itemBounds.width, itemBounds.y);
                                glVertex2f(itemBounds.x + itemBounds.width, itemBounds.y + itemBounds.height);
                                glVertex2f(itemBounds.x, itemBounds.y + itemBounds.height);
                                glEnd();
                            }
                            
                            this.renderText(item.key, itemBounds, stdfont, Color.white, null /*selected == 0 ? Color.GREEN : null*/, isEnabled, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT, false, 0, -1);
                        }
                        
                        itemBounds.y -= itemBounds.height;
                      
                        selected++;
                    }
                    
                glDisable(GL_SCISSOR_TEST);
        
//        glPopAttrib();
    }    
    
}
