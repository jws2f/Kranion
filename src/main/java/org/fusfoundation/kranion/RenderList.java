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
public class RenderList extends Clippable implements Resizeable, Pickable {
    protected final List<Renderable> renderlist;
    
    private boolean isVisible = true;
    
    public RenderList() {
        renderlist = new ArrayList<>();
    }
    public void add(Renderable obj) {
        renderlist.add(obj);
    }
    
    public void remove(Renderable obj) {
        renderlist.remove(obj);
    }
    
    public int getSize() {
        return renderlist.size();
    }
    
    public Iterator<Renderable> getChildIterator() {
        return renderlist.iterator();
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
        
        if (isClipped) {
            isClipped = false;
            renderClipped();
            isClipped = true;
        }
        else {
            ListIterator<Renderable> it = renderlist.listIterator();
            while (it.hasNext()) {
                Renderable r = it.next(); 
                r.render();
            }
        }
    }
    
    @Override
    public void release() {
        ListIterator<Renderable> it = renderlist.listIterator();
        while (it.hasNext()) {
            it.next().release();
        }
        renderlist.clear();
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
    public Renderable setIsDirty(boolean dirty) {
            Iterator<Renderable> i = renderlist.iterator();
            while (i.hasNext()) {
                Renderable child = i.next();
                child.setIsDirty(dirty);//false);
            }
            
            return this;
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

    @Override
    public void renderPickable() {
        if (!getVisible()) return;
        
        for (ListIterator<Renderable> it = renderlist.listIterator(); it.hasNext();) {
            Renderable r = it.next();
            if (r instanceof Pickable) {
                ((Pickable)r).renderPickable();
            }
        }
    }
    
}
