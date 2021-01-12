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

import java.io.Serializable;
import static org.lwjgl.opengl.GL11.glMultMatrix;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

public class Trackball extends GUIControl implements Serializable {
	private Quaternion qCurrent, qDrag, qPrevious;
	private Vector2f trackball_center;
	private float trackball_radius;
	private int startMouseX, mouseX;
	private int startMouseY, mouseY;
	private transient FloatBuffer trackballBuffer = BufferUtils.createFloatBuffer(16);
	
	public Trackball(int centerX, int centerY, float radius) {
		set(centerX, centerY, radius);
		qCurrent = new Quaternion();
	}
        
        public Quaternion getCurrent() { return qCurrent; }
        public Quaternion getDragged() { return qDrag; }
        
        public void setCurrent(Quaternion q) {
            if (qCurrent.x != q.x || qCurrent.y != q.y || qCurrent.z != q.z || qCurrent.w != q.w) {
                qCurrent.set(q.x, q.y, q.z, q.w);
                setIsDirty(true);
            }
        }
        public Quaternion setIdentity() {
            qCurrent.setIdentity();
            setIsDirty(true);
            return qCurrent;
        }
	
	public void set(int centerX, int centerY, float radius) {
		trackball_radius = radius;
		trackball_center = new Vector2f(centerX, centerY);
	}
	
        @Override
	public void render() {
		toMatrix4f(qCurrent).store(trackballBuffer);
		trackballBuffer.flip();
		glMultMatrix(trackballBuffer);
                
                setIsDirty(false);
	}
        
	public void renderOpposite() {
		toMatrix4f(qCurrent.negate(null)).store(trackballBuffer);
		trackballBuffer.flip();
		glMultMatrix(trackballBuffer);
	}
        
	public void mousePressed(int x, int y) {
		startMouseX = x;
		startMouseY = y;
		qPrevious = new Quaternion(qCurrent);
		qDrag = null;
	}

	public void mouseReleased(int x, int y) {

	}  

	public void mouseDragged(int x, int y) {
		mouseX = x;
		mouseY = y;
		updateDrag();
		Quaternion.mul(qDrag, qPrevious, qCurrent);
                setIsDirty(true);
	}
	
	private Vector3f mouseOnSphere(Vector2f mouse) {
		Vector3f v = new Vector3f();
		v.x = -(mouse.x - trackball_center.x) / trackball_radius;
		v.y = (mouse.y - trackball_center.y) / trackball_radius;

		float mag = v.x * v.x + v.y * v.y;
		if (mag > 1.0f) {
			v.normalise();
		}
		else {
			v.z = (float)Math.sqrt(1.0 - mag);
		}
		return v;
	}
	
	private void updateDrag() {
		Vector2f pMouse = new Vector2f(startMouseX, startMouseY);
		Vector2f mouse = new Vector2f(mouseX, mouseY);
		Vector3f from = mouseOnSphere(pMouse);
		Vector3f to = mouseOnSphere(mouse);

		Vector3f xyz = Vector3f.cross(from, to, null);
		float w = Vector3f.dot(from, to);
		qDrag = new Quaternion(xyz.x, xyz.y, xyz.z, w);
	}

	public static Matrix4f toMatrix4f(Quaternion q) {
		Matrix4f matrix = new Matrix4f();
		matrix.m00 = 1.0f - 2.0f * (q.getY() * q.getY() + q.getZ() * q.getZ());
		matrix.m01 = 2.0f * (q.getX() * q.getY() + q.getZ() * q.getW());
		matrix.m02 = 2.0f * (q.getX() * q.getZ() - q.getY() * q.getW());
		matrix.m03 = 0.0f;

		// Second row
		matrix.m10 = 2.0f * (q.getX() * q.getY() - q.getZ() * q.getW());
		matrix.m11 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getZ() * q.getZ());
		matrix.m12 = 2.0f * (q.getZ() * q.getY() + q.getX() * q.getW());
		matrix.m13 = 0.0f;

		// Third row
		matrix.m20 = 2.0f * (q.getX() * q.getZ() + q.getY() * q.getW());
		matrix.m21 = 2.0f * (q.getY() * q.getZ() - q.getX() * q.getW());
		matrix.m22 = 1.0f - 2.0f * (q.getX() * q.getX() + q.getY() * q.getY());
		matrix.m23 = 0.0f;

		// Fourth row
		matrix.m30 = 0;
		matrix.m31 = 0;
		matrix.m32 = 0;
		matrix.m33 = 1.0f;

		return matrix;
	}

    @Override
    public void release() {
    }

}
