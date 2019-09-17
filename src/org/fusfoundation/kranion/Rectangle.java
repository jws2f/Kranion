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

/**
 *
 * @author john
 */
public class Rectangle {
    public float x, y, width, height;
    
    public Rectangle() {
        x = y = 0f;
        width = height = 0f;
    }
    
    public Rectangle(Rectangle r) {
        this.x = r.x;
        this.y = r.y;
        this.width = r.width;
        this.height = r.height;
    }
    
    public Rectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public Rectangle shrinkHorz(float deltax) {
        return new Rectangle(x+deltax, y, width-deltax*2, height);
    }
    
    public Rectangle shrinkVert(float deltay) {
        return new Rectangle(x, y+deltay, width, height-deltay*2);
    }
    
    public int getIntWidth() {
        return (int)Math.ceil(width);
    }
    
    public int getIntHeight() {
        return (int)Math.ceil(height);
    }
    
    public int getIntX() {
        return (int)Math.floor(x);
    }
    
    public int getIntY() {
        return (int)Math.floor(y);
    }
    
    public void setBounds(Rectangle r) {
        this.x = r.x;
        this.y = r.y;
        this.width = r.width;
        this.height = r.height;
    }
    
    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public boolean contains(float px, float py) {
        float dx = (px - x) / width;
        float dy = (py - y) / height;
        
        if ( dx < 1f && dx >= 0f &&
             dy < 1f && dy >= 0f) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public Rectangle union(Rectangle r) {
        Rectangle result = new Rectangle();
        
        result.x = Math.min(this.x, r.x);
        result.y = Math.min(this.y, r.y);
        
        float xmax = Math.max(this.x + this.width, r.x + r.width);
        float ymax = Math.max(this.y + this.height, r.y + r.height);
        
        result.width = xmax -result.x;
        result.height = ymax - result.y;
        
        return result;
    }
    
    public Rectangle intersection(Rectangle r) {
        Rectangle result = new Rectangle();
        
        result.x = Math.max(this.x, r.x);
        result.y = Math.max(this.y, r.y);
        
        float xmin = Math.min(this.x + this.width, r.x + r.width);
        float ymin = Math.min(this.y + this.height, r.y + r.height);
        
        result.width = Math.max(0, xmin - result.x);
        result.height = Math.max(0, ymin - result.y);
        
        return result;
    }
    
    public void translate(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }
}
