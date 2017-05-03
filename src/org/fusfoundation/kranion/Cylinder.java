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

//import static org.lwjgl.opengl.GL12.*;
//import static org.lwjgl.opengl.GL13.*;
//import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
//import static org.lwjgl.opengl.GL20.*;
//import static org.lwjgl.util.glu.GLU.*;
//import org.lwjgl.opengl.GL30;

import org.lwjgl.util.vector.*;
import org.lwjgl.BufferUtils;

import java.nio.*;

public class Cylinder extends Renderable {
        
    // changing this to one static copy of the geometry
    // with unit dimensions so each instance can scale and translate
    // using the same geometry data
    private static int vertsID, normsID;
    private static int refCount = 0;

    // Hemispheric  variables
    private float radius, length, zoffset, normdir;
    private float tesselation = 180;
    private float incrAngle = 360.0f / tesselation;
    private int angleStepCount = (int) (360.0f / incrAngle);

    private Vector4f color = new Vector4f(0.5f, 0.5f, 0.5f, 1f);
    
    private StandardShader shader = new StandardShader();

    public Cylinder(float outerRadius, float cylinder_length, float zOffset, float dir) {

        // housing data
        radius = outerRadius;
        length = cylinder_length;
        zoffset = zOffset;
        normdir = Math.signum(dir);
        
        if (refCount == 0) {
            FloatBuffer vertsBuffer, normsBuffer;
            vertsBuffer = BufferUtils.createFloatBuffer((int) (angleStepCount + 1) * 3 * 2);
            normsBuffer = BufferUtils.createFloatBuffer((int) (angleStepCount + 1) * 3 * 2);

            // probably unnecessary
            vertsBuffer.rewind();
            normsBuffer.rewind();

            // build unit radius/length cyclinder
            for (int i = 0; i <= angleStepCount; i++) {
                float dx = (float) Math.cos(dir * (double) i / angleStepCount * 2.0 * Math.PI);
                float dy = (float) Math.sin(dir * (double) i / angleStepCount * 2.0 * Math.PI);

                float dz = 0;

                // Add vertex
                vertsBuffer.put(dx);
                vertsBuffer.put(dy);
                vertsBuffer.put(dz);

                // Add normal
                Vector3f norm = new Vector3f(dx, dy, 0);
                norm.normalise();
                normsBuffer.put(norm.x);
                normsBuffer.put(norm.y);
                normsBuffer.put(norm.z);

                dz -= 1f;

                // Add vertex
                vertsBuffer.put(dx);
                vertsBuffer.put(dy);
                vertsBuffer.put(dz);

                // Add normal
                normsBuffer.put(norm.x);
                normsBuffer.put(norm.y);
                normsBuffer.put(norm.z);
            }

            vertsBuffer.flip();
            normsBuffer.flip();

            vertsID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vertsID);
            glBufferData(GL_ARRAY_BUFFER, vertsBuffer, GL_STATIC_DRAW);

            normsID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, normsID);
            glBufferData(GL_ARRAY_BUFFER, normsBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
        
        refCount++;
//        System.out.println("Cylinder refcount = " + refCount);
    }

    public void setColor(float red, float green, float blue) {
        setColor(red, green, blue, 1f);
    }
    
    public void setColor(float red, float green, float blue, float alpha) {
        setIsDirty(true);
        color.set(red, green, blue, alpha);
        shader.setAmbientColor(color.x/20f, color.y/20f, color.z/20f, 1f);
        shader.setDiffusetColor(color.x, color.y, color.z, color.w);
        shader.setSpecularColor(0.3f, 0.3f, 0.3f, 1f);
        shader.setSpecularCoefficient(50f);
    }
    
    public void invertNormals(boolean invertNormals) {
        shader.setFlipNormals(invertNormals);
    }

    @Override
    public void render() {
        setIsDirty(false);
        if (!getVisible()) return;
        
        glPushAttrib(GL_ENABLE_BIT | GL_POLYGON_BIT);
                     
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        
        glTranslatef(0, 0, -zoffset);
        glScalef(radius, radius, length);
        
        glColor4f(color.x, color.y, color.z, color.w);
//	    glShadeModel(GL_FLAT);
//	    glPolygonMode( GL_FRONT, GL_LINE );
        glEnable(GL_NORMALIZE);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);

        glBindBuffer(GL_ARRAY_BUFFER, vertsID);
        glVertexPointer(3, GL_FLOAT, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, normsID);
        glNormalPointer(GL_FLOAT, 0, 0);
        
        // Flip faces if we are an "inside" surface
        if (normdir < 0) {
            shader.setFlipNormals(true);
            glFrontFace(GL_CW);
        }

        shader.start();
        glDrawArrays(GL_QUAD_STRIP, 0, (angleStepCount + 1) * 2);
        shader.stop();
        
        if (normdir < 0) {
            shader.setFlipNormals(false);
            glFrontFace(GL_CCW);
        }

        // turn off client state flags
//        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);

        // clean up
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        glPopMatrix();
        
        glPopAttrib();

    }
    
    @Override
    public void release() {
//        System.out.println("Cylinder refcount = " + refCount);
        refCount--;
        if (refCount == 0) {
//            System.out.println("Releasing cylinder data.");
            glDeleteBuffers(vertsID);
            glDeleteBuffers(normsID);
            vertsID = 0;
            normsID = 0;
        }
    }

}
