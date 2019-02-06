/*
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author jsnell
 */
public class ListControl extends GUIControl {

    private BufferedImage img;

    private class Pair implements Comparable<Pair>{
        public Pair(String key, Object val) {
            this.key = key;
            this.value = val;
        }
        public String key;
        public Object value;

        @Override
        public int compareTo(Pair o) {
            return key.compareTo(o.key); // Alphabetical by file name
        }
    }
    
    public ListControl(int x, int y, int width, int height, ActionListener listener) {
        setBounds(x, y, width, height);
        addActionListener(listener);
    }
    
    private final ArrayList<Pair> items = new ArrayList<>();
    
    public void addItem(String name, Object value) {
        items.add(new Pair(name, value));
        setIsDirty(true);
    }
    
    public void sort() {
        Collections.sort(items);
    }
    
    public void removeItem(int index) {
        items.remove(index);
        setIsDirty(true);
    }
    
    public int size() { return items.size(); }
    
    public void clear() {
        items.clear();
        setIsDirty(true);
}

    @Override
    public void render() {
        setIsDirty(false);
        Rectangle itemBounds = new Rectangle();
        itemBounds.height = 20;
        itemBounds.width = bounds.width;
        itemBounds.x = bounds.x;
        itemBounds.y = bounds.y;
        
        
        for (Pair item: items) {
            this.renderTextCore(img, item.key, itemBounds, stdfont, Color.white, null, isEnabled, VPosFormat.VPOSITION_CENTER, HPosFormat.HPOSITION_LEFT, false, 0);
            itemBounds.y += itemBounds.height;
        }
        
        renderBufferedImageViaTexture(img, bounds);

    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(r);
        img = new BufferedImage(bounds.getIntWidth(), bounds.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    }

    @Override
    public void setBounds(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        img = new BufferedImage(bounds.getIntWidth(), bounds.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    }
    
    
}
