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
    
    private static Vector4f[] corners = null;
    private static Vector4f[] world_corners = null;
    
    private Vector4f clipColor = new Vector4f(0.5f, 0.5f ,0.5f, 1f);
    
    public Clippable() {
        
        // only do once for all Clippable objects
        if (corners == null || world_corners == null) {
            corners = new Vector4f[4];
            world_corners = new Vector4f[4];
            
            for (int i=0; i<4; i++) {
                corners[i] = new Vector4f();
                world_corners[i] = new Vector4f();
            }

            corners[0].set(-8000f, 5000f, 0.0f, 1.0f);
            corners[1].set(8000f, 5000f, 0.0f, 1.0f);
            corners[2].set(8000f, -5000f, 0.0f, 1.0f);
            corners[3].set(-8000f, -5000f, 0.0f, 1.0f);
        }

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
    
    public Vector4f getClipColor() {
        return new Vector4f(clipColor);
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
            
        // assumes each clipped mesh is a manifold with no degenerate topology
        // otherwise the stencil mask that is created for the clipped end cap
        // won't be sensible. The object is rendered twice with the clip plane
        // enabled, alternately culling front faces, then back faces and incrementing
        // or decrementing the the stencil buffer respectively. Resulting non-zero
        // stencil buffer values indicate the cut surface.
        
        // Could be optimized by using two-sided stencil test I suppose
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_POLYGON_BIT | GL_TRANSFORM_BIT | GL_COLOR_BUFFER_BIT | GL_LIGHTING_BIT);

            glEnable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);

            glEnable(GL_DEPTH_TEST);

            glEnable(GL_STENCIL_TEST);
            
            glClearStencil(0);
            glClear(GL_STENCIL_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);
            
            // don't update the color or depth buffer values
            // during the stencil update operations
            glColorMask(false, false, false, false);
            glDepthMask(false);
            
            //first pass
            glStencilFunc(GL_ALWAYS, 0, 0);
            glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
            glEnable(GL_CULL_FACE);       
            glCullFace(GL_FRONT);
            
            // set flag to render unclipped
            isClipped = false;
            render();
            
            //second pass
            glStencilOp(GL_KEEP, GL_KEEP, GL_DECR);
            glCullFace(GL_BACK);
            render();


            //End cap ///////////////////////
            
            glStencilFunc(GL_NOTEQUAL, 0, ~0);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
            
            //back to normal framebuffer mode
            glDisable(GL_CULL_FACE);       
            glColorMask(true, true, true, true);
            glDepthMask(true);
            glEnable(GL_DEPTH_TEST);

            // set the color and draw the stenciled clip plane with
            // 3D perlin noise shader.
            FloatBuffer c = BufferUtils.createFloatBuffer(4);
            c.put(new float[] { clipColor.x, clipColor.y, clipColor.z, clipColor.w }).flip();
            glMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, c);
            glColor4f(clipColor.x, clipColor.y, clipColor.z, clipColor.w);

            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();

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
 
            Main.glPopMatrix();
            
            glEnable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);

            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glEnable(GL_DEPTH_TEST);
            
            // end cap is rendered, now render the rest of the unclipped geometry normally
            render();
            
            // restore flag to render clipped
            isClipped = true;

        Main.glPopAttrib();
        
    }
    
}
