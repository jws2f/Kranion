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
public class RenderList extends Clippable implements Resizeable {
    protected final List<Renderable> renderlist;
    
    private boolean isVisible = true;
    
    @Override
    public boolean getVisible() { return isVisible; }
    public void setVisible(boolean visible)
    {
        isVisible = visible;
    }
    
    public RenderList() {
        renderlist = new ArrayList<>();
    }
    public void add(Renderable obj) {
        renderlist.add(obj);
    }
    
    public void remove(Renderable obj) {
        renderlist.remove(obj);
    }
    
    @Override
    public void setClipped(boolean clipped) {
        super.setClipped(clipped);
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            Renderable r = it.next();
            if (r instanceof Clippable ) {
                ((Clippable) r).setClipped(clipped);
            }
        }        
    }
    
    @Override
    public void setTrackball(Trackball tb) {
        super.setTrackball(tb);
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            Renderable r = it.next();
            if (r instanceof Clippable ) {
                ((Clippable) r).setTrackball(tb);
            }
        }        
    }
    
    @Override
    public void setDolly(float c, float d) {
        super.setDolly(c, d);
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            Renderable r = it.next();
            if (r instanceof Clippable ) {
                ((Clippable) r).setDolly(c, d);
            }
        }        
    }
    
    @Override
    public void render() {
        if (!getVisible()) return;
        
        if (getClipped()) {
            setClipped(false);
            renderClipped();
            setClipped(true);
            return;
        }
        
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            Renderable r = it.next();
            r.render();
        }
    }
    
    @Override
    public void release() {
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            it.next().release();
        }
    }
    
    @Override
    public boolean getIsDirty() {        
        Iterator<Renderable> i = renderlist.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child.getIsDirty()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void setIsDirty(boolean dirty) {
            Iterator<Renderable> i = renderlist.iterator();
            while (i.hasNext()) {
                Renderable child = i.next();
                child.setIsDirty(dirty);//false);
            }
    }

    @Override
    public void doLayout() {
        Iterator<Renderable> i = renderlist.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child instanceof Resizeable) {
                ((Resizeable)child).doLayout();//false);
            }
        }        
    }
    
}
