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
package org.fusfoundation.kranion.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.Observer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.view.View;

/**
 *
 * @author john
 */
public abstract class Controller  implements PropertyChangeListener, ActionListener, ProgressListener{
    protected Model model;
    protected View view;
    protected List<ActionListener> actionListeners;
        
    public Controller() {
        actionListeners = new ArrayList<ActionListener>();
    }
       
    public void setModel(Model m) {
        if (model != null) {
            model.removePropertyChangeListener(this);
        }
        model = m;
        if (model != null) {
            model.addPropertyChangeListener(this);
        }
    }
    
    public Model getModel() {
        return model;
    }
    
    public void setView(View view) {
        this.view = view;
    }
    
    public View getView() {
        return this.view;
    }
    
    public void addActionListener(ActionListener al) {
        actionListeners.add(al);
    }
    
    public void removeActionListener(ActionListener al) {
        actionListeners.remove(al);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Iterator<ActionListener> i = actionListeners.iterator();
        while(i.hasNext()) {
            i.next().actionPerformed(e);
        }
    }
            
            

}
