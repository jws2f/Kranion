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

import java.nio.FloatBuffer;
import static org.fusfoundation.kranion.Trackball.toMatrix4f;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Quaternion;


/**
 *
 * @author john
 */
public class TransformationAdapter extends Renderable implements Pickable {

    private Renderable child;
    
    private Vector3f translation = new Vector3f();
    private Quaternion rotation = new Quaternion().setIdentity();
    private FloatBuffer rotationBuffer = BufferUtils.createFloatBuffer(16);
    
    public TransformationAdapter(Renderable child) {
        this.child = child;
    }
    
    
    public void setRotation(Vector3f axis, float angle) {
        rotation = rotation.setIdentity();
        rotate(axis, angle);
    }
    
    public void rotate(Vector3f axis, float angle) {
        child.setIsDirty(true);
        Quaternion newRotation = new Quaternion();
        Vector4f axisAngle = new Vector4f(axis.x, axis.y, axis.z, angle/180f*(float)Math.PI);
        newRotation.setFromAxisAngle(axisAngle);
        Quaternion.mul(newRotation, rotation, rotation);
    }
    
    public void setTranslation(float x, float y, float z) {
        if (translation.x != x || translation.y != y || translation.z != z) {
            child.setIsDirty(true);
        }
        translation.set(x, y, z);
    }
    
    public Vector3f getTranslation() {
        return translation;
    }
    
    public Quaternion getRotation() {
        return rotation;
    }
    
    public void translate(float x, float y, float z) {
        translate(new Vector3f(x, y, z));
    }
    
    public void translate(Vector3f offset) {
        child.setIsDirty(true);
        Vector3f.add(translation, offset, translation);
    }
    
    public void setIdentity() {
        child.setIsDirty(true);
        translation.set(0f, 0f, 0f);
        rotation = rotation.setIdentity();
    }
    
    @Override
    public void render() {
        setIsDirty(false);
        if (!this.getVisible()) return;
        
        
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
        
        // Translation
            glTranslatef(translation.x, translation.y, translation.z);
        
        // Rotation
            toMatrix4f(rotation).store(rotationBuffer);
            rotationBuffer.flip();
            glMultMatrix(rotationBuffer);
        
        
            child.render();
            
        Main.glPopMatrix();
    }

    @Override
    public void release() {
        child.release();
    }

    @Override
    public boolean getVisible() {
        return child.getVisible();
    }

    @Override
    public Renderable setVisible(boolean visible) {
        child.setVisible(visible);
        return this;
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
    public boolean getIsDirty() {
        return child.getIsDirty();
    }

    @Override
    public Renderable setIsDirty(boolean dirty) {
        child.setIsDirty(dirty);
        return this;
    }

    @Override
    public void renderPickable() {
        if (!this.getVisible()) return;
        
        if (child instanceof Pickable) {
            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();

            // Translation
                glTranslatef(translation.x, translation.y, translation.z);

            // Rotation
                toMatrix4f(rotation).store(rotationBuffer);
                rotationBuffer.flip();
                glMultMatrix(rotationBuffer);


                ((Pickable)child).renderPickable();

            Main.glPopMatrix();
        }
    }
}
