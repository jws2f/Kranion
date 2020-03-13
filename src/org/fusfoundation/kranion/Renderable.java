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

import java.util.*;

/**
 *
 * @author john
 */
public abstract class Renderable {
    private static int renderableCount = 0;
    private static HashSet<Renderable> renderableMap = new HashSet<>(256);
    
    private int id = renderableCount++;
    private String tag = new String("renderable"+id);
    
    protected boolean isVisible = true;
    protected boolean isDirty = false;
    
    private static Renderable keyboardFocus = null;
    private static Renderable defaultKeyboardFocus = null;
    private boolean acceptsKeyboardFocus = false;
    
    public Renderable() {
        renderableMap.add(this); // TODO: Slightly dangerous, not thread safe. The Renderable Set is private, so shouldn't publish "this" outside the constructor.
    }
    
    public void removeFromSet() {
        renderableMap.remove(this);
    }
    
    public static Renderable lookupByID(int id) {
        Iterator<Renderable> i = renderableMap.iterator();
        while(i.hasNext()) {
            Renderable r = i.next();
            if (r.getID() == id) {
                return r;
            }
        }
        return null;
    }
    
    public static Renderable lookupByTag(String tag) {
        Iterator<Renderable> i = renderableMap.iterator();
        while(i.hasNext()) {
            Renderable r = i.next();
            if (tag.equals(r.getTag())) {
                return r;
            }
        }
        return null;
    }
    
    public static Iterator<Renderable> iterator() {
        return renderableMap.iterator();
    }
    
    public long getID() { return id; }
    public String getTag() { return tag; }
    public Renderable setTag(String newTag) {
        tag = newTag;
        return this;
    }
    
    public abstract void render();
    public abstract void release();
    
    public boolean getVisible() {
        return isVisible;
    };
    public Renderable setVisible(boolean visible) {
        if (isVisible != visible) {
            isVisible = visible;
            setIsDirty(true);
        }
        return this;
    }
    
    public boolean getIsDirty() {
        return isDirty;
    }
            
    public Renderable setIsDirty(boolean dirty) {
        isDirty = dirty;
        return this;
    }
    
    public boolean getAcceptsKeyboardFocus() { return acceptsKeyboardFocus; }
    public void setAcceptsKeyboardFocus(Boolean acceptsKbFocus) {
        acceptsKeyboardFocus = acceptsKbFocus;
    }
    
    public static void setDefaultKeyboardFocus(Renderable defaultKbFocusHandler) {
        Renderable.defaultKeyboardFocus = defaultKbFocusHandler;
    }
    
    public static Renderable getDefaultKeyboardFocus() {
        return Renderable.defaultKeyboardFocus;
    }
    
    public static Renderable getCurrentKeyboardFocus() {
        return keyboardFocus;
    }
    
    public boolean hasKeyboardFocus() {
        return keyboardFocus == this;
    }
    
    public boolean acquireKeyboardFocus() {
        if (getAcceptsKeyboardFocus()) {
            if (keyboardFocus != null && keyboardFocus != this) {
                keyboardFocus.lostKeyboardFocus();
            }
            keyboardFocus = this;
//            System.out.println(keyboardFocus + " got keyboard focus.");
            return true;
        }
        else {
            return false;
        }
    }
    
    public void lostKeyboardFocus() {
        if (keyboardFocus == this) {
            keyboardFocus = null;
            if (Renderable.getDefaultKeyboardFocus() != null) {
                keyboardFocus = Renderable.getDefaultKeyboardFocus();
            }
        }
    }
    
    public static void ProcessKeyboard(int keyCode, char keyChar, boolean isKeyDown) {
        if (keyboardFocus != null) {
            keyboardFocus.OnKeyboard(keyCode, keyChar, isKeyDown);
        }
    }
    
    public void OnKeyboard(int keyCode, char keyChar, boolean isKeyDown) {
        
    }
}
