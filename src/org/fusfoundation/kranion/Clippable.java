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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.util.glu.GLU.*;
import org.lwjgl.util.vector.*;
import org.lwjgl.BufferUtils;
import java.nio.*;

/**
 *
 * @author john
 */
public abstract class Clippable extends Renderable {
    
    protected boolean isClipped = false;
    
    private static ShaderProgram shader = null;
    private Trackball clip_trackball = null;
    private float dolly = 0f; // TODO: we need a camera model to pass around
    private float cameraZ = 0f;
    
    private Vector4f[] corners = new Vector4f[4];
    private Vector4f[] world_corners = new Vector4f[4];
    
    Vector4f clipColor = new Vector4f(0.5f, 0.5f ,0.5f, 1f);
    
    public Clippable() {
        
        for (int i=0; i<4; i++) {
            corners[i] = new Vector4f();
            world_corners[i] = new Vector4f();
        }
        
        corners[0].set(-8000f, 5000f, 0.0f, 1.0f);
        corners[1].set(8000f, 5000f, 0.0f, 1.0f);
        corners[2].set(8000f, -5000f, 0.0f, 1.0f);
        corners[3].set(-8000f, -5000f, 0.0f, 1.0f);

//        corners[0].set(-250, 250, 0.0f, 1.0f);
//        corners[1].set(250, 250, 0.0f, 1.0f);
//        corners[2].set(250, -250, 0.0f, 1.0f);
//        corners[3].set(-250, -250, 0.0f, 1.0f);
   }
        
    public boolean getClipped() { return isClipped; }
    public void setClipped(boolean clipped) { 
        if (isClipped != clipped) {
            setIsDirty(true);
        }
        isClipped = clipped;
    }
    
    public void setClipColor(float r, float g, float b, float a) {
        if (clipColor.x != r || clipColor.y != g || clipColor.z != b || clipColor.w != a) {
            setIsDirty(true);
        }
        clipColor.set(r, g, b, a);
    }
        
    public void setTrackball(Trackball tb) {
        if (clip_trackball == null || clip_trackball.getCurrent() != tb.getCurrent()) {
            setIsDirty(true);
        }
        clip_trackball = tb;
    }
    
    public void setDolly(float cameraz, float dolly) {
        if (this.cameraZ != cameraz || this.dolly != dolly) {
            setIsDirty(true);
        }
        this.cameraZ = cameraz;
        this.dolly = dolly;
    }
    
    
    public void renderClipped() {
            
        glPushAttrib(GL_ENABLE_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_POLYGON_BIT);

            glEnable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);

            glEnable(GL_DEPTH_TEST);

            // 1st pass //////////////////
            glEnable(GL_CULL_FACE);       
            glCullFace(GL_BACK);

            render(); ////////////////////


            // Turn stencil buffer on
            glClearStencil(0);
            glClear(GL_STENCIL_BUFFER_BIT);
            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xff);

            // 2nd pass ///////////////////
            glCullFace(GL_FRONT);
            glStencilFunc(GL_ALWAYS, 1, 0xff);
            glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);

            render(); /////////////////////

            glDisable(GL_CULL_FACE);


            //End cap ///////////////////////
            glStencilFunc(GL_NOTEQUAL, 0, 0xff);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
            glEnable(GL_DEPTH_TEST);

            FloatBuffer c = BufferUtils.createFloatBuffer(4);
            c.put(new float[] { clipColor.x, clipColor.y, clipColor.z, clipColor.w }).flip();
            glMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, c);

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();

                glLoadIdentity();

                // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
                gluLookAt(0.0f, 0.0f, -600.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
                // camera dolly in/out
                glTranslatef(0.0f, 0.0f, dolly); //HACK - need to use the clip plane equation to position the end cap correctly in depth buffer

                if (clip_trackball != null)  {
                    clip_trackball.render();
                }

                if (shader == null) {
                    shader = new ShaderProgram();
                    shader.addShader(GL_VERTEX_SHADER, "/org/fusfoundation/kranion/shaders/Clippable.vs.glsl");
                    shader.addShader(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/Clippable.fs.glsl");
                    shader.compileShaderProgram();
                }

                Quaternion quat;
                if (clip_trackball != null) {
                    quat = clip_trackball.getCurrent().negate(null);
                }
                else {
                    quat = new Quaternion().setIdentity();
                }

                Matrix4f xfrm = Trackball.toMatrix4f(quat);

                DoubleBuffer planeEQ = org.lwjgl.BufferUtils.createDoubleBuffer(4);
                glGetClipPlane(GL_CLIP_PLANE0, planeEQ);
                float zoffset =  600 + dolly + (float)planeEQ.get(3);  // TODO: need a proper camera model object to pass around
                Matrix4f.translate(new Vector3f(0f, 0f, -zoffset), xfrm, xfrm);

                for (int i=0; i<4; i++) {
                    Matrix4f.transform(xfrm, corners[i], world_corners[i]);
                }

                Vector4f norm = new Vector4f(0f, 0f, -1f, 1f);
                Matrix4f.transform(xfrm, norm, norm);

                shader.start();
                glBegin(GL_QUADS);
                    glNormal3d(norm.x, norm.y, norm.z);

                    for (int i=0; i<4; i++) {
                        glVertex3d(world_corners[i].x, world_corners[i].y, world_corners[i].z);
                    }

                glEnd();
                shader.stop();

                glDisable(GL_STENCIL_TEST);

            glPopMatrix();

        glPopAttrib();
    }
    
}
