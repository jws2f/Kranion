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

import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

/**
 *
 * @author john
 */
public class CrossHair extends Renderable 
{
    private Trackball trackball = null;
    private Vector3f offset = null;
    private int style = 0;
            
    public CrossHair() {}
    public CrossHair(Trackball tb) {
        trackball = tb;
    }
    
    public void setStyle(int style) {
        this.style = style;
    }
    
    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }
    
    @Override
    public void render() {
        
        if (!this.getVisible()) {
            return;
        }
        
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
        
        if (offset != null) {
            glTranslatef(-offset.x, -offset.y, -offset.z);
        }
        
        if (trackball != null) {
            trackball.renderOpposite();
        }
        
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_COLOR_BUFFER_BIT);
        
        glEnable(GL_BLEND);
        glDisable(GL_LIGHTING);
        glDisable(GL_CLIP_PLANE0);
//        glDisable(GL_DEPTH_TEST);
        //glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glLineWidth(1f);
        
        if (style == 0) {
            glColor4f(.12f, 0.52f, .52f, 0.95f);
            glLineWidth(1.5f);

            glBegin(GL_LINES);
            for (int i = 0; i < 51; i++) {
                float cx = 1f * (float) Math.cos((float) i / 49f * 2f * 3.1415f);
                float cy = 1f * (float) Math.sin((float) i / 49f * 2f * 3.1415f);
                glVertex3f(cx, cy, -2f);
                cx = 1f * (float) Math.cos((float) (i+1) / 49f * 2f * 3.1415f);
                cy = 1f * (float) Math.sin((float) (i+1) / 49f * 2f * 3.1415f);
                glVertex3f(cx, cy, -2f);
            }
            glEnd();

        }
        else {
        glColor4f(.12f, 0.72f, .12f, 0.9f);
            glBegin(GL_LINES);
                glVertex3f(-0.5f, 0f, -2f);
                glVertex3f(0.5f, 0f, -2f);
                glVertex3f(0f, -0.5f, -2f);
                glVertex3f(0f, 0.5f, -2f);
            glEnd();
            
            glColor4f(.1f, 0.6f, .1f, 0.85f);
            
            glLineWidth(1.5f);
            glBegin(GL_LINES);
            for (int i=0; i<100; i++) {
                float cx = 20f * (float)Math.cos((float)i/98f * 2f * 3.1415f);
                float cy = 20f * (float)Math.sin((float)i/98f * 2f * 3.1415f);
                glVertex3f(cx, cy, -2f);
            }
            glEnd();
            
            glLineWidth(1f);
            glColor4f(.1f, 0.6f, .1f, 0.6f);
            glBegin(GL_LINES);
                glVertex3f(-30f, 0f, -2f);
                glVertex3f(-5f, 0f, -2f);
                glVertex3f(5f, 0f, -2f);
                glVertex3f(30f, 0f, -2f);

                glVertex3f(0f, -30f, -2f);
                glVertex3f(0f, -5f, -2f);
                glVertex3f(0f, 30f, -2f);
                glVertex3f(0f, 5f, -2f);
            glEnd();

        }       
        Main.glPopAttrib();
        
        Main.glPopMatrix();
    }

    @Override
    public void release() {
    }
    
}
