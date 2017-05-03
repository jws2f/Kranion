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
package org.fusfoundation.kranion.model;

import java.io.*;
import java.util.*;
import java.beans.PropertyChangeEvent;
import org.fusfoundation.kranion.model.image.*;

/**
 *
 * @author John Snell
 */
public class Model extends Observable implements Serializable, Observer {
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    
    // CT image (only one)
    private ImageVolume4D ct_image;
    
    // MR images (one or more planning series)
    private ArrayList<ImageVolume4D> mr_images;
    private int selectedMR = -1;
    
    // Image viewing parameters
    
    // Transducer Info
    // Sonications
    //      - location
    //      - power/duration
    //      - Thermometry
    private ArrayList<Sonication> sonications;
    private int selectedSonication = -1;
    
    // Save/Load
    // - will use Serializable support for now
    
    // Notifications
    
    public Model() {
        selectedMR = -1;
        selectedSonication = -1;
        mr_images = new ArrayList<ImageVolume4D>(3); // initial array size
        sonications = new ArrayList<Sonication>(25);
    }
    
    public void clear() {
//        this.deleteObservers();
        
//        attributes.clear(); // TODO: we might want to sometimes do this?
        
        ImageVolumeUtil.releaseTextures(ct_image);
        this.setCtImage(null);
        
        int count = 0;
        Iterator<ImageVolume4D> i = mr_images.iterator();
        while(i.hasNext()) {
            ImageVolume4D img = i.next();
            ImageVolumeUtil.releaseTextures(img);
            this.setMrImage(count++, null);
        }
        
        selectedMR = -1;
        
        Iterator<Sonication> s = sonications.iterator();
        while(s.hasNext()) {
            s.next().clear();
        }
        
        sonications.clear();
        
        selectedSonication = -1;
    }
    
    
    public int getSelectedMR() { return selectedMR; }
    public void setSelectedMR(int index) {
        if (index >= mr_images.size() || index < -1) {
            throw new IndexOutOfBoundsException();
        }
        selectedMR = index;
        
        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.SelectedMR", null, this));        
    }
    
    public int getSelectedSonication() {
        return selectedSonication;
    }

    public void setSelectedSonication(int index) {
        if (index >= sonications.size() || index < -1) {
            throw new IndexOutOfBoundsException();
        }
        selectedSonication = index;

        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.SelectedSonication", null, this));
    }
        
    public ImageVolume getCtImage() {
        return ct_image;
    }
    
    public void setCtImage(ImageVolume4D image) {
        
        if (ct_image != null) {
            ct_image.deleteObserver(this);
        }
        
        ct_image = image;
        if (image != null) {
            ct_image.addObserver(this);
        }
        
        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.CtImage", null, image));
    }
    
    public ImageVolume getMrImage(int index) {
        try {
            return mr_images.get(index);
        }
        catch(Exception e) {
            return null;
        }
    }
    
    public int getMrImageCount() {
        return mr_images.size();
    }
    
    public void setMrImage(int index, ImageVolume4D image) {
        if (index < mr_images.size() && mr_images.get(index) != null) {
            mr_images.get(index).deleteObserver(this);
        }
        
        if (index >= mr_images.size()) {
            mr_images.add(index, image);
        }
        else {
            mr_images.set(index, image);
        }
        
        if (image != null) {
            image.addObserver(this);
        }
        
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.MrImage["+index+"]", null, image));
    }
    
    public void clearMrImages() {
        mr_images.clear();
    }
    
    public void addMrImage(ImageVolume4D image) {
        mr_images.add(image);
        
        image.addObserver(this);
        
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.MrImage["+mr_images.lastIndexOf(image)+"]", null, image));
    }
    
    public Sonication getSonication(int index) {
        return sonications.get(index);
    }
    
    public void setSonication(int index, Sonication sonication) {
        if (index < sonications.size() && sonications.get(index) != null) {
            sonications.get(index).deleteObserver(this);
        }
        
        if (index >= sonications.size()) {
            sonications.add(index, sonication);
        }
        else {
            sonications.set(index, sonication);
        }
        
        sonication.addObserver(this);

        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.Sonication["+index+"]", null, sonication));
    }
    
    public void addSonication(Sonication sonication) {
        sonications.add(sonication);
        
        sonication.addObserver(this);

        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.Sonication["+sonications.lastIndexOf(sonication)+"]", null, sonication));
    }
    
    public int getSonicationCount() { return sonications.size(); }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
            //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Model.Attribute["+name+"]", null, value));
    }
        
    public Iterator getAttributeKeys() {
        return attributes.keySet().iterator();
    }    

    // Recieve updates from child observables and forward to model observers
    @Override
    public void update(Observable o, Object arg) {
        System.out.println("----Model update: " + o.toString());
        if (arg != null && arg instanceof PropertyChangeEvent) {
            PropertyChangeEvent propEvt = (PropertyChangeEvent)arg;
            
            String childName = "";
            if (o == this.ct_image) {
                childName = "CtImage.";
            }
            else if (o instanceof ImageVolume && mr_images.contains((ImageVolume) o)) {
                int index = mr_images.indexOf(o);
                childName = "MrImage[" + index + "].";
            }
            else if (o instanceof Sonication && sonications.contains((Sonication)o)) {
                int index = sonications.indexOf(o);
                childName = "Sonication[" + index + "].";
            }
            
            setChanged();
            notifyObservers(new PropertyChangeEvent(this, "Model." + childName + propEvt.getPropertyName(), null, propEvt.getNewValue()));
       }
        
    }
}
