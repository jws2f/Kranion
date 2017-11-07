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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
/**
 *
 * @author john
 */
public class RenderableAdapter extends Renderable {
    Renderable renderable;
    String methodName;
    Method method;
    
    public RenderableAdapter(Renderable renderable, String methodName) {
        this.renderable = renderable;
        this.methodName = methodName;
        try {
          method = renderable.getClass().getMethod(methodName);
        }
        catch (SecurityException e) {
            method = null;
            System.out.println(renderable);
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            method = null;
            System.out.println(renderable);
            e.printStackTrace();
        }
    }

    @Override
    public void render() {
        if (!getVisible()) return;
        
        if (renderable != null && method != null) {
            try {
            method.invoke(renderable);
            }
            catch (IllegalArgumentException e) {
                System.out.println(renderable);
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                System.out.println(renderable);
                e.printStackTrace();
            }
            catch (InvocationTargetException e) {
                System.out.println(renderable);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void release() {
        renderable = null;
    }

    @Override
    public boolean getIsDirty() {
        return this.renderable.getIsDirty();
    }

    @Override
    public Renderable setIsDirty(boolean dirty) {
        this.renderable.setIsDirty(dirty);
        
        return this;
    }
    
}
