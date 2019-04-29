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

import java.util.Iterator;
import org.lwjgl.opengl.Display;
import org.lwjgl.LWJGLException;

/**
 *
 * @author john
 */
public class Scene extends GUIControl implements Resizeable, Pickable {
    
    @Override
    public void render() {
        if (getIsDirty()) {
            renderChildren();
            setIsDirty(false);
        }
    }

    @Override
    public void release() {
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            child.release();
        }                
    }

    @Override
    public void doLayout() {
        
        this.setBounds(0, 0, Display.getWidth(), Display.getHeight());
        
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child instanceof Resizeable) {
                ((Resizeable)child).doLayout();//false);
            }
        }        
    }

    @Override
    public void renderPickable() {
        this.renderPickableChildren();
    }
    
}
