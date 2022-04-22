/*
 * The MIT License
 *
 * Copyright 2020 Focused Ultrasound Foundation.
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

import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author jsnell
 */
public class ImageLandmarkConstraint {
    private Vector3f point;
    private Vector3f direction;
    private float offset = 0;
    
    public ImageLandmarkConstraint() {
        point = null;
        direction = null;
    }
    
    public ImageLandmarkConstraint(Vector3f p, Vector3f d) {
        point = new Vector3f(p);
        direction = new Vector3f(d);
    }
    
    public void setPoint(Vector3f p) {
        point = new Vector3f(p);
    }
    
    public Vector3f getPoint() {
        return point;
    }
    
    public void setDirection(Vector3f d) {
        direction = new Vector3f(d);
    }
    
    public Vector3f getDirection() {
        return direction;
    }
    
    public float getOffset() {
        return offset;
    }
    
    public void setOffset(float newOffset) {
        offset = newOffset;
    }
    
    // This contraint snaps landmark position updates
    // to the projection onto the ray specified by point and direction
    public Vector3f filterImagePoint(Vector3f wp) {
        if (point==null || direction==null) {
            return wp;
        }
        
        Vector3f result = new Vector3f();
                
        offset = Vector3f.dot(direction, Vector3f.sub(wp, point, null));
        
        offset = Math.max(offset, 0);
        
        result.x = direction.x * offset + point.x;
        result.y = direction.y * offset + point.y;
        result.z = direction.z * offset + point.z;
        
        return result;
    }
}
