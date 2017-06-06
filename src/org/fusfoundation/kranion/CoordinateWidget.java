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

import java.io.IOException;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Quaternion;

import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE1;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopAttrib;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushAttrib;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.util.glu.GLU.gluLookAt;
import static org.lwjgl.util.glu.GLU.gluPerspective;

/**
 *
 * @author john
 */
public class CoordinateWidget extends GUIControl{

    private Trackball extTrackball;
    private Trackball intTrackball = new Trackball(1, 1, 1);
    private static PlyFileReader body = new PlyFileReader("/org/fusfoundation/kranion/meshes/humanbody.ply");

    public CoordinateWidget() {
        super();
        try {
            body.readObject();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }
    
    public void setTrackball(Trackball trackball) {
        this.extTrackball = trackball;
    }
    
    @Override
    public boolean getIsDirty() {
        Quaternion q1 = intTrackball.getCurrent();
        Quaternion q2 = extTrackball.getCurrent();
        isDirty =  q1.x != q2.x || q1.y != q2.y || q1.z != q2.z || q1.w != q2.w;
        return super.getIsDirty();
    }
    
    @Override
    public void render() {
                
        intTrackball.setCurrent(extTrackball.getCurrent());
        
        // Draw coordinate axis widget thing
        ////////////////////////////////////////
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
        glLoadIdentity();
        //gluPerspective(25.0f, 1f, 100.0f, 3000.0f);
        gluPerspective(25.0f, 1f, 100.0f, 100000.0f);
        
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
            glViewport(0, 0, 150, 150);
            glLoadIdentity();

            // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
            gluLookAt(0.0f, 0.0f, -800.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);

            // camera dolly in/out
            glTranslatef(0.0f, 0.0f, -400);
            intTrackball.render();

            Main.glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT);
                glDisable(GL_LIGHTING);
        //        glDisable(GL_BLEND);
        //        glDisable(GL_DEPTH_TEST);
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);

                Main.glPushMatrix();
                    glTranslatef(0f, 20f, 0f);
                    glLineWidth(2.5f);
            //            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                    glBegin(GL_LINES);
                        glColor3f(1f, 0f, 0f);
                        glVertex3f(0f, 0f, 0f);
                        glVertex3f(60, 0f, 0f);

                        glColor3f(0f, 1f, 0f);
                        glVertex3f(0f, 0f, 0f);
                        glVertex3f(0f, 60f, 0f);

                        glColor3f(0f, 0f, 1f);
                        glVertex3f(0f, 0f, 0f);
                        glVertex3f(0, 0f, 60f);
                    glEnd();


                Main.glPopMatrix();

                glEnable(GL_LIGHTING);
                Main.glPushMatrix();
                    glRotatef(90, 1f, 0f, 0f);
                    glTranslatef(0f, -100f, 0f);
                    body.setColor(0.6f, 0.6f, 0.8f, 1f);
                    body.render();
                Main.glPopMatrix();
                
              Main.glPopMatrix();

            Main.glPopAttrib();


            glViewport(0, 0, Display.getWidth(), Display.getHeight());
                    
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
        
        glMatrixMode(GL_MODELVIEW);
    }
    
    @Override
    public void release() {
        if (body != null) {
            body.release();
            body = null;
        }
    }
    
}
