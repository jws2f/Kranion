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

public class Sphere extends GUIControl {
	

	private static int vertsID, normsID;
        private static int refCount = 0;
	
	
	// Hemispheric  variables
	private float radius;
	private float sphereTesselation = 180;
	private float incrAngle = 360.0f/sphereTesselation;
	private int longAngleStepCount = (int)(360.0f/incrAngle);
	private int latAngleStepCount = (int)(180.0f/incrAngle);
	
        private Vector3f location = new Vector3f();
	private Vector4f color = new Vector4f(0.3f, 0.3f, 0.85f, 1f);

        private StandardShader shader = new StandardShader();
	  
	public Sphere(float r)  {
				
		// housing data
		radius = Math.abs(r);
                
                setColor(color.x, color.y, color.z, color.w);
                
            if (refCount == 0) {
                float radSign = Math.signum(r);
                FloatBuffer vertsBuffer, normsBuffer;
                vertsBuffer = BufferUtils.createFloatBuffer((int) (longAngleStepCount + 1) * 3 * 2 * (int) (latAngleStepCount + 1));
                normsBuffer = BufferUtils.createFloatBuffer((int) (longAngleStepCount + 1) * 3 * 2 * (int) (latAngleStepCount + 1));

                // probably unnecessary
                vertsBuffer.rewind();
                normsBuffer.rewind();

                for (int lat = -latAngleStepCount/2; lat <= latAngleStepCount/2; lat++) {
                    for (int i = 0; i <= longAngleStepCount; i++) {
                        float dx = (float) Math.cos((double) i / longAngleStepCount * 2.0 * Math.PI);
                        float dy = (float) Math.sin((double) i / longAngleStepCount * 2.0 * Math.PI);
                        float dx2 = (float) Math.cos((double) (i + 1) / longAngleStepCount * 2.0 * Math.PI);
                        float dy2 = (float) Math.sin((double) (i + 1) / longAngleStepCount * 2.0 * Math.PI);

                        double phi = incrAngle / 360.0f * 2.0 * Math.PI;

                        float rdx = dx * (float) Math.cos(lat * phi);
                        float rdy = dy * (float) Math.cos(lat * phi);
                        float dz1 = (float) Math.sin(lat * phi);
                        float dz2 = (float) Math.sin((lat + 1) * phi);

                        Vector3f v1 = new Vector3f(rdx, rdy, dz1);
                        Vector3f v2 = new Vector3f(dx * (float) Math.cos((lat + 1) * phi), dy * (float) Math.cos((lat + 1) * phi), dz2);

                        Vector3f n1 = new Vector3f(rdx, rdy, (dz1));
                        n1.normalise();
                        Vector3f n2 = new Vector3f(v2.x, v2.y, (dz2));
                        n2.normalise();

                        // Add vertex
                        vertsBuffer.put(v1.x);
                        vertsBuffer.put(v1.y);
                        vertsBuffer.put(v1.z);

//				// Add normal
//				Vector3f norm = new Vector3f(dir * rdx, dir * rdy, -(150f-dz1) * dir);
//				norm.normalise();
                        normsBuffer.put(n1.x);
                        normsBuffer.put(n1.y);
                        normsBuffer.put(n1.z);

                        // Add vertex
                        vertsBuffer.put(v2.x);
                        vertsBuffer.put(v2.y);
                        vertsBuffer.put(v2.z);
//				txdrHousingVertsBuffer.put(dx * (float)Math.cos((lat+1)*phi));
//				txdrHousingVertsBuffer.put(dy * (float)Math.cos((lat+1)*phi);
//				txdrHousingVertsBuffer.put(dz2);

                        // Add normal
//				norm.set(dir * rdx, dir * rdy, -(150f-dz2) * dir);
//				norm.normalise();
                        normsBuffer.put(n2.x);
                        normsBuffer.put(n2.y);
                        normsBuffer.put(n2.z);
                    }
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
        }
        
        public void setRadius(float r) {
            radius = r;
        }
        
        public float getRadius() {
            return radius;
        }
        
        public Sphere setColor(float red, float green, float blue) {
            setColor(red, green, blue, 1f);
            
            return this;
        }

        public Sphere setColor(float red, float green, float blue, float alpha) {
            setIsDirty(true);
            color.set(red, green, blue, alpha);
            shader.setAmbientColor(color.x/20f, color.y/20f, color.z/20f, 1f);
            shader.setDiffusetColor(color.x, color.y, color.z, color.w);
            shader.setSpecularColor(0.3f, 0.3f, 0.3f, 1f);
            shader.setSpecularCoefficient(50f);
            
            return this;
        }
        
        public Sphere setLocation(float x, float y, float z) {
            if (x != location.x || y != location.y || z != location.z) {
                setIsDirty(true);
            }
            location.set(x, y, z);
            return this;
        }
        
        public Sphere setLocation(Vector3f loc) {
            location.set(loc.x, loc.y, loc.z);
            return this;
        }
	
        public void invertNormals(boolean invert) {
            shader.setFlipNormals(invert);
        }
        
	public void render() {
	  
            setIsDirty(false);
            if (!getVisible()) return;
            
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
//            glLoadIdentity();

            glTranslatef(location.x, location.y, location.z);
            
            glScalef(radius, radius, radius);

	    glEnable(GL_NORMALIZE);
            
	    glColor4f(color.x, color.y, color.z, color.w);
//	    glShadeModel(GL_FLAT);
//	    glPolygonMode( GL_FRONT, GL_LINE );
	    glEnableClientState(GL_VERTEX_ARRAY);
	    glEnableClientState(GL_NORMAL_ARRAY);
	    
	    glBindBuffer(GL_ARRAY_BUFFER, vertsID);
	    glVertexPointer(3, GL_FLOAT, 0, 0);
	    glBindBuffer(GL_ARRAY_BUFFER, normsID);
	    glNormalPointer(GL_FLOAT, 0, 0);
	    
//            glEnable(GL_CULL_FACE);
//            glCullFace(GL_FRONT);
        // Flip faces if we are an "inside" surface
            
            shader.start();
	    for (int r=0; r<latAngleStepCount; r++) {
	    	glDrawArrays(GL_QUAD_STRIP, r*(int)(longAngleStepCount+1)*2, (int)(longAngleStepCount+1)*2);
	    }
            shader.stop();
//            glDisable(GL_CULL_FACE);
        // Flip faces if we are an "inside" surface    
	    
	    // turn off client state flags
//	    glDisableClientState(GL_COLOR_ARRAY);
	    glDisableClientState(GL_NORMAL_ARRAY);
	    glDisableClientState(GL_VERTEX_ARRAY);
	    
	    // clean up
	    glBindBuffer(GL_ARRAY_BUFFER, 0);
            
        Main.glPopMatrix();
        //Main.popMatrixWithCheck();
}
        
@Override
    public void update(Object newValue) {
        try {
            if (newValue instanceof Vector3f) {
                setLocation((Vector3f)newValue);
            }
            else {
                throw new Exception("Wrong type");
            }
        }
        catch(Exception e) {
            System.out.println(this + " Wrong or NULL new value.");
        }
    }
    
    public void release() {
        refCount--;
//        System.out.println("Hemisphere refCount = " + refCount);
        if (refCount == 0) {
//            System.out.println("Hemisphere deleted buffers.");
            glDeleteBuffers(vertsID);
            glDeleteBuffers(normsID);
            vertsID=0;
            normsID=0;
        }
    }
	

}
