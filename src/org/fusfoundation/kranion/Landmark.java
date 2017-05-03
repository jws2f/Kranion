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

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author John Snell
 */
public class Landmark extends GUIControl {
    private Vector3f location = new Vector3f();
    
    public Landmark() {}
    
    public Vector3f getLocation() {
        return location;
    }
    
    public float getXpos() { return location.x; }
    public float getYpos() { return location.y; }
    public float getZpos() { return location.z; }
    
    public void setLocation(Vector3f loc) {
        if (!location.equals(loc)) {
            setIsDirty(true);
            location.set(loc);
        }
    }
    
    @Override
    public void render() {
        setIsDirty(false);
    }

    @Override
    public void release() {
    }
    
    @Override
    public void update(Object newValue) {
        try {
            Vector3f target = (Vector3f)newValue;
            setLocation(target);
            System.out.println("Landmark.update");
        }
        catch(Exception e) {
            System.out.println(this + ": Wrong or NULL new value.");
        }
    }
    
}
