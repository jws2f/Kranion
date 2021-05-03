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
package org.fusfoundation.kranion.view;

import org.fusfoundation.kranion.controller.Controller;
import java.beans.PropertyChangeEvent;
import java.util.Observer;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import org.fusfoundation.kranion.FileDialog;
import org.fusfoundation.kranion.GUIControl;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.Resizeable;
import org.fusfoundation.kranion.model.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author John Snell
 */
public abstract class View extends Renderable implements PropertyChangeListener, ActionListener, ProgressListener, Resizeable{
    protected Model model;
    protected Controller controller;    
    private String propertyPrefix;
    
    protected Thread myThread;
    
    public View() {
        myThread = Thread.currentThread();
    }
    
    public void setPropertyPrefix(String prefix) {
        propertyPrefix = prefix;
    }
    
    public String getPropertyPrefix() { return propertyPrefix; }
    
    public void setModel(Model model) {
        
        if (this.model != null) {
            this.model.removePropertyChangeListener(this);
        }
        this.model = model;
        if (this.model != null) {
            this.model.addPropertyChangeListener(this);
        }
    }
    
    public Model getModel() { return model; }
    
    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    public Controller getController() { return controller; }
    
    public abstract void create() throws LWJGLException;
    
    protected String getFilteredPropertyName(PropertyChangeEvent arg) {
        String propName = "";
        String nameString = arg.getPropertyName();
        
        if (nameString.startsWith(propertyPrefix + "[")) {
            int last = nameString.indexOf("]", propertyPrefix.length()+1);
            propName = nameString.substring(propertyPrefix.length()+1, last);            
        }
        
        return propName;
    }
    
    public abstract int doPick(int mouseX, int mouseY);
    
    public abstract org.lwjgl.util.vector.Vector3f doRayPick(int mouseX, int mouseY, /* out */org.lwjgl.util.vector.Vector3f pickRay);

    public abstract boolean okToExit();
    
    public abstract File chooseFile(String title, FileDialog.fileChooseMode mode, String[] fileFilters);

    public abstract boolean doOkCancelMessageBox(String title, String message);
    
    public abstract void processInput();
    
    public abstract void doTransition(int milliseconds);

}
