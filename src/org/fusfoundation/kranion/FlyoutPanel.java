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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.FloatBuffer;
import java.util.List;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

/**
 *
 * @author john
 */
public class FlyoutPanel extends GUIControl implements ActionListener, Animator, Resizeable {

    public enum direction { NORTH, SOUTH, EAST, WEST };
    
    protected direction flyDirection = direction.EAST;
    protected Rectangle tabRect = new Rectangle();
    protected float dx, dy;
    protected float flyScale = 0f;
    protected float tabSize = 5;
    protected boolean autoExpand = false;
    protected AnimatorUtil anim = new AnimatorUtil();
    protected static float guiScale = 2f;
    private Button pinButton;
    protected TabbedPanel tabbedPanel;
    
    protected long flyinDelay = -1;
    

    public static void setGuiScale(float scale) {
        guiScale = Math.max(1f, scale);
    }

    public FlyoutPanel() {
            pinButton = new Button(Button.ButtonType.TOGGLE_BUTTON, 0, 0, 21, 21, this);
            pinButton.setTitle("");
            pinButton.setIndicatorRadius(8f);
            pinButton.setColor(0,0,0,0);
            
            addChild(tabbedPanel = new TabbedPanel());
            addChild(pinButton);            
    }

    public void addTab(String tabName) {
        tabbedPanel.addTab(tabName);
    }
    
    public void addChild(String tabName, Renderable child) {
        tabbedPanel.addChild(tabName, child);
    }
        
    public void setFlyDirection(direction dir) {
        setIsDirty(true);
        flyDirection = dir;
                
        switch (dir) {
            case NORTH:
                dx = 0;
                dy = bounds.height;
                
                tabRect.setBounds(bounds.x, bounds.y+bounds.height, bounds.width, tabSize);
                break;
            case SOUTH:
                dx = 0;
                dy = -bounds.height;
                
                tabRect.setBounds(bounds.x, bounds.y-tabSize, bounds.width, tabSize);
                break;
            case EAST:
                dx = bounds.width;
                dy = 0;
                
                tabRect.setBounds(bounds.x+bounds.width, bounds.y, tabSize, bounds.height);
                break;
            case WEST:
                dx = -bounds.width;
                dy = 0;
                tabRect.setBounds(bounds.x-tabSize, bounds.y, tabSize, bounds.height);
                break;
        }
        
        Rectangle pinBounds = pinButton.getBounds();
        pinButton.setBounds(bounds.width - pinBounds.width, bounds.height - pinBounds.height, pinBounds.width, pinBounds.height);
                
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
    }
    

    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width*guiScale, height*guiScale);
    }
    
    public void setAutoExpand(boolean flag) {
        setIsDirty(true);
        autoExpand = flag;
    }
    
    @Override
    public boolean getIsDirty() {
        return super.getIsDirty() || !isAnimationDone();
    }
    
    public void doLayout() {
        if (flyDirection == direction.SOUTH) {
            bounds.y = Display.getHeight()-bounds.height;
            
            if (autoExpand) {
                bounds.x = 0;
                bounds.width = Display.getWidth();
            }
        }
        else if (flyDirection == direction.WEST) {
            bounds.x = Display.getWidth()-bounds.width;
            if (autoExpand) {
                bounds.y = 0;
                bounds.height = Display.getHeight();
            }
        }
        else if (flyDirection == direction.NORTH) {
            if (autoExpand) {
                bounds.x = 0;
                bounds.width = Display.getWidth();
            }
        }
        else if (flyDirection == direction.EAST) {
            if (autoExpand) {
                bounds.y = 0;
                bounds.height = Display.getHeight();
            }
        }
        setFlyDirection(flyDirection);
    }
    
            
    protected void flyout() {
//        System.out.println("flyout" + flyScale);
        anim.set(flyScale, 1f, (1f - flyScale) * 0.7f);
    }
    
    protected void flyin() {
//        System.out.println("flyin" + flyScale);
        anim.set(flyScale, 0f,  flyScale * 0.7f);
    }

    @Override
    public void render() {
        setIsDirty(false);
                
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT);
        
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
            glLoadIdentity();

            //org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, Display.getWidth(), 0.0f, Display.getHeight());
            org.lwjgl.opengl.GL11.glOrtho(0.0f, Display.getWidth(), 0.0f, Display.getHeight(), -1000, 2000); // TODO: move this elsewhere, to overlay managment


            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
                glLoadIdentity();
                
                float nx = bounds.x + (dx)*flyScale - dx;
                float ny = bounds.y + (dy)*flyScale - dy;
                float tx = tabRect.x + (dx)*flyScale - dx;
                float ty = tabRect.y + (dy)*flyScale - dy;
                
                Rectangle extents = bounds.union(tabRect);
                
                glDisable(GL_DEPTH_TEST);
                glScissor((int)extents.x, (int)extents.y, (int)extents.width, (int)extents.height);
                glEnable(GL_SCISSOR_TEST);
                glEnable(GL_BLEND);
                glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                
                glBegin(GL_QUADS);
                    // Don't draw the main panel rect if the panel isn't out
                    if (this.flyScale > 0f) {
                        glNormal3f(0f, 1f, 0f);                   
                        glColor4f(0.15f, 0.15f, 0.15f, 0.85f);

                        glVertex2f(nx, ny);
                        glVertex2f(nx+bounds.width, ny);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(nx+bounds.width, ny+bounds.height);
                        glVertex2f(nx, ny+bounds.height);
                    }
                    
                    glNormal3f(0f, 1f, 0f);
                    glColor4f(0.55f, 0.15f, 0.15f, 0.85f);
                    
                    glVertex2f(tx, ty);
                    glVertex2f(tx+tabRect.width, ty);
                    glNormal3f(0f, 0f, 1f);
                    glVertex2f(tx+tabRect.width, ty+tabRect.height);
                    glVertex2f(tx, ty+tabRect.height);
                glEnd();
                
                // Don't render child controls if the panel isn't out
                if (this.flyScale > 0f) {
                    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
                    lightPosition.put(Display.getWidth() / 2).put(Display.getHeight() / 2).put(10000.0f).put(1f).flip();
                    glLight(GL_LIGHT0, GL_POSITION, lightPosition);	// sets light position
                    
                    glTranslatef(nx, ny, 0.1f);
                   
                    glScalef(guiScale,guiScale, 1);

                    renderChildren();
                }
                
                
            glMatrixMode(GL_MODELVIEW);
           Main.glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
                
        Main.glPopAttrib();
        
        glMatrixMode(GL_MODELVIEW);
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
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
//        if (!anim.isAnimationDone()) return false;
        
        if (flyScale == 1 && super.OnMouse((int)((x-bounds.x)/guiScale + bounds.x), (int)((y-bounds.y)/guiScale + bounds.y), button1down, button2down, dwheel)) {
            return true;
        };

        if (MouseIsInside(x, y, true)) {
            
            flyinDelay = -1;
            
            if (flyScale == 0f) {
                flyout();
            }
            else {
                super.OnMouse((int)((x-bounds.x)/guiScale + bounds.x), (int)((y-bounds.y)/guiScale + bounds.y), button1down, button2down, dwheel);
            }
 
            if (button1down || button2down) {
                return true;
            }
        }
        else if (!MouseIsInside(x, y, true) && grabbedChild == null) {
            if (flyScale == 1f && !pinButton.getIndicator() && flyinDelay == -1) {
                //flyin();
                flyinDelay = System.currentTimeMillis() + 500; // half second delay
            }
        }
        
        if (hasGrabbed() || grabbedChild != null) return true;
        
        
        return false;
    }
    
    public boolean MouseIsInside(float x, float y, boolean withTab) {
        
        Rectangle testMainRect = new Rectangle(bounds);
        testMainRect.translate(Math.round(dx*flyScale)-dx, Math.round(dy*flyScale)-dy);
        
        Rectangle testTabRect = new Rectangle(tabRect);
        testTabRect.translate(Math.round(dx*flyScale)-dx, Math.round(dy*flyScale)-dy);
        
        if (flyScale == 1f) {
            Iterator<Renderable> iter = this.children.iterator();
            while(iter.hasNext()) {
                Renderable r = iter.next();
                if (r instanceof GUIControl) {
                    if (((GUIControl)r).MouseIsInside(x, y)) {
                        return true;
                    }
                }
            }
        }
        
        if (testMainRect.contains(x, y)) {           
            return true;
        }
        if (withTab && testTabRect.contains(x, y)) {            
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean MouseIsInside(float x, float y) {
        return MouseIsInside(x, y, false);
    }
            
}
