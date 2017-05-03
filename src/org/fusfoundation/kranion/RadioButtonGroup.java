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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

/**
 *
 * @author john
 */
public class RadioButtonGroup extends GUIControl implements ActionListener {

    public RadioButtonGroup() {
        
    }
    
    @Override
    public void render() {
        if (this.getVisible()) {
            renderChildren();
        }
    }

    @Override
    public void addChild(Renderable child) {
        super.addChild(child);
        if (child instanceof Button) {
            ((Button)child).addActionListener(this);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src instanceof Button && e.getID() == ActionEvent.ACTION_PERFORMED) {
            Iterator<Renderable> i = this.children.iterator();
            while (i.hasNext()) {
                Renderable r = i.next();
                if (r instanceof Button && r != src) {
                    ((Button)r).setIndicator(false);
                }
                else if (r instanceof Button && r == src) {
                    ((Button)r).setIndicator(true);
                }
            }
        }
    }
    
}
