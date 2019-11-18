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

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

/**
 *
 * @author john
 */
public class CrossHair extends Renderable 
{
    private Trackball trackball = null;
    private Vector3f offset = null;
    private int style = 0;
    
    private static int solidCircleVertsID=0, dashedCircleVertsID=0, crosshairVertsID=0;
            
    public CrossHair() {
        init();
    }
    public CrossHair(Trackball tb) {
        trackball = tb;
        init();
    }
    
    private void init() {
        FloatBuffer vertsBuffer1, vertsBuffer2, vertsBuffer3;
        
        vertsBuffer1 = BufferUtils.createFloatBuffer(51 * 6);
        for (int i = 0; i < 51; i++) {
            float cx = 1f * (float) Math.cos((float) i / 49f * 2f * 3.1415f);
            float cy = 1f * (float) Math.sin((float) i / 49f * 2f * 3.1415f);

            vertsBuffer1.put(cx);
            vertsBuffer1.put(cy);
            vertsBuffer1.put(-2);

            cx = 1f * (float) Math.cos((float) (i + 1) / 49f * 2f * 3.1415f);
            cy = 1f * (float) Math.sin((float) (i + 1) / 49f * 2f * 3.1415f);

            vertsBuffer1.put(cx);
            vertsBuffer1.put(cy);
            vertsBuffer1.put(-2);
        }        
        vertsBuffer1.flip();

        vertsBuffer2 = BufferUtils.createFloatBuffer(100 * 3);
        for (int i = 0; i < 100; i++) {
            float cx = 20f * (float) Math.cos((float) i / 98f * 2f * 3.1415f);
            float cy = 20f * (float) Math.sin((float) i / 98f * 2f * 3.1415f);
            vertsBuffer2.put(cx);
            vertsBuffer2.put(cy);
            vertsBuffer2.put(-2);
        }        
        vertsBuffer2.flip();
        
        vertsBuffer3 = BufferUtils.createFloatBuffer(12 * 3);
//                glVertex3f(-0.5f, 0f, -2f);
//                glVertex3f(0.5f, 0f, -2f);
//                glVertex3f(0f, -0.5f, -2f);
//                glVertex3f(0f, 0.5f, -2f);
                
            // small central cross hair
            vertsBuffer3.put(-0.5f);
            vertsBuffer3.put(0);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0.5f);
            vertsBuffer3.put(0);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0f);
            vertsBuffer3.put(-0.5f);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0f);
            vertsBuffer3.put(0.5f);
            vertsBuffer3.put(-2);
            
            // larger outer cross hair
//                glVertex3f(-30f, 0f, -2f);
//                glVertex3f(-5f, 0f, -2f);
//                glVertex3f(5f, 0f, -2f);
//                glVertex3f(30f, 0f, -2f);
            vertsBuffer3.put(-30f);
            vertsBuffer3.put(0);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(-5f);
            vertsBuffer3.put(0);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(5f);
            vertsBuffer3.put(0f);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(30f);
            vertsBuffer3.put(0f);
            vertsBuffer3.put(-2);

//                glVertex3f(0f, -30f, -2f);
//                glVertex3f(0f, -5f, -2f);
//                glVertex3f(0f, 30f, -2f);
//                glVertex3f(0f, 5f, -2f);
            vertsBuffer3.put(0f);
            vertsBuffer3.put(-30);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0f);
            vertsBuffer3.put(-5f);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0f);
            vertsBuffer3.put(30f);
            vertsBuffer3.put(-2);
            
            vertsBuffer3.put(0f);
            vertsBuffer3.put(5f);
            vertsBuffer3.put(-2);
            
        vertsBuffer3.flip();

        solidCircleVertsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, solidCircleVertsID);
        glBufferData(GL_ARRAY_BUFFER, vertsBuffer1, GL_STATIC_DRAW);

        dashedCircleVertsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, dashedCircleVertsID);
        glBufferData(GL_ARRAY_BUFFER, vertsBuffer2, GL_STATIC_DRAW);
        
        crosshairVertsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, crosshairVertsID);
        glBufferData(GL_ARRAY_BUFFER, vertsBuffer3, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

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
	    
            drawLineArray(solidCircleVertsID, 0, 200);
        }
        else {
            glColor4f(.12f, 0.72f, .12f, 0.9f);

             drawLineArray(crosshairVertsID, 0, 4);
            
            glColor4f(.1f, 0.6f, .1f, 0.85f);            
            glLineWidth(1.5f);

            drawLineArray(dashedCircleVertsID, 0, 100);
            
            glLineWidth(1f);
            glColor4f(.1f, 0.6f, .1f, 0.6f);

            drawLineArray(crosshairVertsID, 4, 8);

        }       
        Main.glPopAttrib();
        
        Main.glPopMatrix();
    }
    
    private void drawLineArray(int bufID, int vert_offset, int count) {
            glBindBuffer(GL_ARRAY_BUFFER, bufID);
            
	    glEnableClientState(GL_VERTEX_ARRAY);
	    glVertexPointer(3, GL_FLOAT, 0, vert_offset*3*Float.BYTES /* 3=num floats per vertex */);
            
	    glDrawArrays(GL_LINES, 0, count);
            
	    glDisableClientState(GL_VERTEX_ARRAY);
	    
	    // clean up
	    glBindBuffer(GL_ARRAY_BUFFER, 0);         
    }

    @Override
    public void release() {
        if (solidCircleVertsID != 0) {
            glDeleteBuffers(solidCircleVertsID);
            solidCircleVertsID = 0;
        }
        if (dashedCircleVertsID != 0) {
            glDeleteBuffers(dashedCircleVertsID);
            dashedCircleVertsID = 0;
        }
        if (crosshairVertsID != 0) {
            glDeleteBuffers(crosshairVertsID);
            crosshairVertsID = 0;
        }
    }
    
}
