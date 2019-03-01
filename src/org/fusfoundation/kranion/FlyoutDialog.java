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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.FloatBuffer;
import java.util.ListIterator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_LIGHT0;
import static org.lwjgl.opengl.GL11.GL_LIGHTING_BIT;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POLYGON_BIT;
import static org.lwjgl.opengl.GL11.GL_POSITION;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRANSFORM_BIT;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLight;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

/**
 *
 * @author jsnell
 */

// Modal dialog baseclass
public class FlyoutDialog extends FlyoutPanel {
    
    protected boolean isClosed = true;

    public FlyoutDialog() {
        this.children.clear();
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
                        glColor4f(0.15f, 0.15f, 0.15f, 0.95f);

                        glVertex2f(nx, ny);
                        glVertex2f(nx+bounds.width, ny);
                        glNormal3f(0f, 0f, 1f);
                        glVertex2f(nx+bounds.width, ny+bounds.height);
                        glVertex2f(nx, ny+bounds.height);
                    }
                    
//                    glNormal3f(0f, 1f, 0f);
//                    glColor4f(0.55f, 0.15f, 0.15f, 0.85f);
//                    
//                    glVertex2f(tx, ty);
//                    glVertex2f(tx+tabRect.width, ty);
//                    glNormal3f(0f, 0f, 1f);
//                    glVertex2f(tx+tabRect.width, ty+tabRect.height);
//                    glVertex2f(tx, ty+tabRect.height);
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
        
        glMatrixMode(GL_MODELVIEW);    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {

        if (flyScale < 1f) {
            return false;
        }
        
        boolean currentInside = MouseIsInside(x, y);
                
        if (mouseInside != currentInside) {
            mouseInside = currentInside;
            setIsDirty(true);
        }
//        if (currentInside && !mouseInside) {
//            mouseInside = true;
//            setIsDirty(true);
//        }
//        else if (!currentInside && mouseInside) {
//            mouseInside = false;
//            setIsDirty(true);
//        }
        
        if (getVisible()) {
            
            if (currentInside && button1down || button2down) {
                if (!this.acquireKeyboardFocus()) {
                    if (Renderable.getDefaultKeyboardFocus() != null) {
                        Renderable.getDefaultKeyboardFocus().acquireKeyboardFocus();
                    }
                }
            }

            if (this.grabbedChild != null) {
                return grabbedChild.OnMouse(x - bounds.getIntX(), y - bounds.getIntY(), button1down, button2down, dwheel);
            }
            else {
                // things are drawn in list order, so mouse order is reversed (things drawn last, get mouse first)
                ListIterator<Renderable> i = children.listIterator(children.size());
                while(i.hasPrevious()) {
                    Renderable child = i.previous();
                    if (child instanceof MouseListener) {
                        if ( ((MouseListener)child).OnMouse(x - bounds.getIntX(), y - bounds.getIntY(), button1down, button2down, dwheel) ) {
                            return true;
                        }
                    }
                }
            }
        }
                
        return true; // always modal behavior
    }
    

    
    public void show() {
        this.grabMouse(0f, 0f);
        bringToTop();
        flyout();
        isClosed = false;
        while(!isClosed || !anim.isAnimationDone()) {
            Main.processNextFrame();
        }
        this.ungrabMouse();
    }

    @Override
    public void doLayout() {
        if (flyDirection == direction.SOUTH) {
            bounds.x = Display.getWidth()/2 - bounds.width/2;
            bounds.y = Display.getHeight()-bounds.height;
            
            if (autoExpand) {
                bounds.x = 0;
                bounds.width = Display.getWidth();
            }
        }
        else if (flyDirection == direction.WEST) {
            bounds.x = Display.getWidth()-bounds.width;
            bounds.y = Display.getHeight()/2 - bounds.height/2;
            if (autoExpand) {
                bounds.y = 0;
                bounds.height = Display.getHeight();
            }
        }
        else if (flyDirection == direction.NORTH) {
            bounds.x = Display.getWidth()/2 - bounds.width/2;
            if (autoExpand) {
                bounds.x = 0;
                bounds.width = Display.getWidth();
            }
        }
        else if (flyDirection == direction.EAST) {
            bounds.x = 0;
            bounds.y = Display.getHeight()/2 - bounds.height/2;
            if (autoExpand) {
                bounds.y = 0;
                bounds.height = Display.getHeight();
            }
        }
        setFlyDirection(flyDirection);
    }
   
}
