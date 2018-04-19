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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static org.fusfoundation.kranion.Main.glPopAttrib;
import static org.fusfoundation.kranion.Main.glPushAttrib;
import org.fusfoundation.kranion.view.DefaultView;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glVertex2f;

/**
 *
 * @author john
 */
public class TabbedPanel extends GUIControl implements ActionListener, Animator, Resizeable {
    private class Tab {
        public String label;
        public int id;
        public int width;
        public RenderList children = new RenderList();
        public String viewTagName = null;
        
        public Tab(int id, String label) {
            this.id = id;
            this.label = label;
            this.width = 100;
        }        
    }
    
    private int selectedTab = 0;
    private List<Tab> tabs = new ArrayList<>(1);
    
    public TabbedPanel() {
//            addTab("File");
//            addTab("View");
//            addTab("Transducer");        
    }
    
    public int addTab(String label, String viewNameToLink) {
        int id = tabs.size();
        
        Rectangle stringBounds = getStringBounds(label, null);
        
        Tab tab = new Tab(id, label);
        tab.viewTagName = viewNameToLink;
        tab.width = (int)(stringBounds.width) + 40;
        
        tabs.add(tab);
        
        // if this is the first tab created, redirect children to
        // tab's child list
        if (tabs.size() == 1) {
            children = tab.children.renderlist;
        }
        
        return id;
    }
    
    public int addTab(String label) {
        return addTab(label, null);
    }
    
    public void addChild(String tabLabel, Renderable child) {
            Iterator<Tab> iter = tabs.iterator();
            while (iter.hasNext()) {
                Tab tab = iter.next();
                if (tab.label.equalsIgnoreCase(tabLabel)) {
                    
                    if (child instanceof GUIControl) {
                        ((GUIControl)child).parent = this;
                    }
                    
                    tab.children.add(child);
                    setIsDirty(true);
                    
                    return;
                }
            }
            
            // tab didn't exist, make one
            addTab(tabLabel);
            addChild(tabLabel, child);
    }
    
    @Override
    public void render() {
        
        if (tabs.size() == 0) return;
        
        doLayout();
        
        glPushAttrib(GL_ENABLE_BIT);
            glEnable(GL_BLEND);
        
            glBegin(GL_QUADS);
                glNormal3f(0f, 1f, 0f);
                glColor4f(0.15f, 0.15f, 0.15f, 0.85f);

                glVertex2f(bounds.x,bounds.y +  bounds.height - 25);
                glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height - 25);
                glNormal3f(0f, 0f, 1f);
                glVertex2f(bounds.x + bounds.width, bounds.y + bounds.height);
                glVertex2f(bounds.x, bounds.y + bounds.height);
            glEnd();


            int tabCount = 0;
            int tabIndent = 0;
            int tabOffset = 0;
            Iterator<Tab> iter = tabs.iterator();
            while (iter.hasNext()) {
                Tab tab = iter.next();
                
                Rectangle rect = new Rectangle(bounds.x + tabOffset, bounds.y + bounds.height - 25, tab.width, 25);
                
                glBegin(GL_QUADS);
                    if (selectedTab == tab.id) {
                        tabIndent = 0;
                        glColor4f(0.25f, 0.25f, 0.25f, 0.85f);
                        
                        glNormal3f(0f, 1f, 0f);
                        glVertex2f(rect.x, rect.y);
                        glVertex2f(rect.x + rect.width, rect.y);

                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(rect.x + rect.width - tabIndent, rect.y + rect.height);
                        glVertex2f(rect.x + tabIndent, rect.y + rect.height);
                    
                        tabIndent = 5;
                        glColor4f(0.4f, 0.8f, 0.4f, 0.65f);                        
                    }
                    else {
                        tabIndent = 0;
                        glColor4f(0.25f, 0.25f, 0.25f, 0.85f);
                    }

                    glNormal3f(0f, 1f, 0f);
                    glVertex2f(rect.x, rect.y);
                    glVertex2f(rect.x + rect.width, rect.y);
                    
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(rect.x + rect.width - tabIndent, rect.y + rect.height);
                    glVertex2f(rect.x + tabIndent, rect.y + rect.height);
                glEnd();
            
                this.renderText(tab.label, rect, stdfont, Color.WHITE, true, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_CENTER);
                tabOffset += tab.width;
                tabCount++;
            }
        
            glDisable(GL_BLEND);
        glPopAttrib();
        
        renderChildren();
        
        setIsDirty(false);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public boolean isAnimationDone() {
        return true;
    }

    @Override
    public void cancelAnimation() {
        
    }

    @Override
    public void advanceFrame() {
        
    }

    @Override
    public void doLayout() {
        this.bounds = new Rectangle(0, 0, parent.bounds.width, parent.bounds.height);
        setIsDirty(true);
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (button1down && y > bounds.height-25 && y < bounds.height) {
            int tabOffset = 0;
            int tabCount = 0;
            Iterator<Tab> iter = tabs.iterator();
            while (iter.hasNext()) {
                Tab tab = iter.next();
                
                if (x > tabOffset && x < tabOffset + tab.width) {
                    this.selectedTab = tabCount;
                    children = tab.children.renderlist;
                    setIsDirty(true);
                    DefaultView df = (DefaultView)Renderable.lookupByTag("DefaultView");
                    if (df != null) {
                        df.setDoTransition(true, 0.2f);
                    }
                    return true;
                }
                
                tabCount++;
                tabOffset += tab.width;
            }
        }
        
        return super.OnMouse(x, y, button1down, button2down, dwheel); 

    }
    
}
