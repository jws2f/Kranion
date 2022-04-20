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

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.util.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.fusfoundation.kranion.CrossThreadCallable;
import org.fusfoundation.kranion.GUIControl;
import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.Transducer;
import org.fusfoundation.kranion.TransducerRayTracer;
import org.fusfoundation.kranion.TransducerXStreamConverter;
import org.fusfoundation.kranion.model.image.*;


/**
 *
 * @author John Snell
 */
public class Model implements Serializable, PropertyChangeListener {
    //private HashMap<String, Object> attributes = new HashMap<String, Object>();
    
    private AttributeList attributes = new AttributeList();
    
    private static final long serialVersionUID = -729845232606031972L;
    
    private PropertyChangeSupport propertyChangeSupport;
    
    // CT image (only one)
    private ImageVolume4D ct_image;
    
    // MR images (one or more planning series)
    private ArrayList<ImageVolume4D> mr_images;
    private int selectedMR = -1;
    
    // Image viewing parameters
    
    // Transducer Info
    private Transducer transducer;
    
    // Sonications
    //      - location
    //      - power/duration
    //      - Thermometry
    private ArrayList<Sonication> sonications;
    private int selectedSonication = -1;
    
    // Save/Load
    // - will use Serializable support for now
    
    // Notifications
    
    protected Thread myThread;    
    
    public Model() {
        selectedMR = -1;
        selectedSonication = -1;
        mr_images = new ArrayList<>(10); // initial array size
        sonications = new ArrayList<>(25);
        myThread = Thread.currentThread();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }
    
    public void addBoundControlListener(GUIControl listener) {
        String command = listener.getCommand();
        if (command != null && !command.isEmpty()) {
            listener.setPropertyPrefix("Model.Attribute");
            propertyChangeSupport.addPropertyChangeListener("Model.Attribute[" + listener.getCommand() + "]", listener);
        } else {
            Logger.getGlobal().log(Level.WARNING, "GUIControl has no command to bind to model");
        }
    }
    
    public void addBoundControlListener(GUIControl listener, String altProperty) {
        if (altProperty != null && !altProperty.isEmpty()) {
            listener.setPropertyPrefix("Model.Attribute");
            propertyChangeSupport.addPropertyChangeListener("Model.Attribute[" + altProperty + "]", listener);
        } else {
            Logger.getGlobal().log(Level.WARNING, "GUIControl has no command to bind to model");
        }
    }
    
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListeners() {
        PropertyChangeListener listeners[] = this.propertyChangeSupport.getPropertyChangeListeners();
        for (int i=0; i<listeners.length; i++) {
            removePropertyChangeListener(listeners[i]);
        }
    }
    
    // When loading a new model we need to copy the PropertyChangeListeners from the old model to the new one11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111
    public void copyPropertyChangeListeners(Model sourceModel) {
        PropertyChangeListener listeners[] = sourceModel.propertyChangeSupport.getPropertyChangeListeners();
        for (int i=0; i<listeners.length; i++) {
            this.propertyChangeSupport.addPropertyChangeListener(listeners[i]);
        }        
    }
     
    public void clear() {
        
        attributes.clear(); // TODO: we might want to sometimes do this?
        
        ImageVolumeUtil.releaseTextures(ct_image);
        this.setCtImage(null);
                
        clearCtImage();
        clearMrImages();
        
        selectedMR = -1;
        
        for (int i=0; i<sonications.size(); i++) {
            this.deleteSonication(i);
        }        
        sonications.clear();        
        selectedSonication = -1;
                
        updateAllAttributes();
        
        this.removePropertyChangeListeners();
    }
    
    // When loading a model from disk we need to alert all observers of all new attribute values
    // This should update GUI observers
    public void updateAllAttributes() {
        propertyChangeSupport.firePropertyChange("Model.CtImage", null, getCtImage());
        
        for (int n=0; n<this.getMrImageCount(); n++) {
            propertyChangeSupport.firePropertyChange("Model.MrImage["+n+"]", null, getMrImage(n));            
        }

        Iterator<String> i = getAttributeKeys();
        while(i.hasNext()) {
            String key = i.next();
            Object value = getAttribute(key);
            propertyChangeSupport.firePropertyChange("Model.Attribute["+key+"]", null, value);
        }
    }
    
    public AttributeList getAttributeList() { return attributes; }
    
    
    public int getSelectedMR() { return selectedMR; }
    public void setSelectedMR(int index) {
        if (index >= mr_images.size() || index < -1) {
            throw new IndexOutOfBoundsException();
        }
        
        int oldVal = selectedMR;
        selectedMR = index;
        
        // notify
        propertyChangeSupport.firePropertyChange("Model.SelectedMR", oldVal, selectedMR);
    }
    
    public int getSelectedSonication() {
        return selectedSonication;
    }

    public void setSelectedSonication(int index) {
        if (index >= sonications.size() || index < -1) {
            //throw new IndexOutOfBoundsException();
            index = 0; // TODO: fix this properly
        }
        
        int oldVal = selectedSonication;
        selectedSonication = index;

        // notify
        propertyChangeSupport.firePropertyChange("Model.SelectedSonication", oldVal, selectedSonication);
    }
        
    public ImageVolume getCtImage() {
        return ct_image;
    }
    
    public void setCtImage(ImageVolume4D image) {
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Void call() {
                    Model.this.setCtImage(image);
                    return null;
                }
            };

            Main.callCrossThreadCallable(c);

            return;
        }
        
        if (ct_image != null) {
            ct_image.removePropertyChangeListener(this);
        }
        
        ImageVolume4D oldVal = ct_image;
        
        ct_image = image;
        if (image != null) {
            image.setThread(myThread);
            ct_image.addPropertyChangeListener(this);
        }
        
        // notify
        propertyChangeSupport.firePropertyChange("Model.CtImage", oldVal, ct_image);
    }
    
    public void setTransducer(Transducer t) {
        transducer = t;
        propertyChangeSupport.firePropertyChange("Model.Transducer", null, transducer);        
    }
    
    public Transducer getTransducer() {
        return transducer;
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
        if (index < 0) {
            return;
        }
        
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Void call() {
                    Model.this.setMrImage(index, image);
                    return null;
                }
            };

            Main.callCrossThreadCallable(c);

            return;
        }
        
        ImageVolume4D oldVal = null;
                
        if (index >= mr_images.size()) {
            mr_images.add(index, image);
        }
        else {
            oldVal = mr_images.get(index);
            if (oldVal != null) {
                oldVal.removePropertyChangeListener(this);
            }
            mr_images.set(index, image);
        }
        
        
        if (image != null) {
            image.addPropertyChangeListener(this);
        }
        
        //notify
        propertyChangeSupport.firePropertyChange("Model.MrImage["+index+"]", oldVal, image);
    }
    
    public void clearImages() {
        clearCtImage();
        clearMrImages();
    }
    
    public void clearCtImage() {
        if (ct_image != null) {
            this.ct_image.removePropertyChangeListener(this);
            ImageVolumeUtil.releaseTextures(ct_image);
            this.setCtImage(null);
        }
    }
    
    public void clearMrImages() {
        int count = 0;
        Iterator<ImageVolume4D> i = mr_images.iterator();
        while(i.hasNext()) {
            ImageVolume4D img = i.next();
            img.removePropertyChangeListener(this);
            ImageVolumeUtil.releaseTextures(img);
            this.setMrImage(count++, null);
        }
        mr_images.clear();
    }
    
    public void addMrImage(ImageVolume4D image) {
        if (image != null) {
            mr_images.add(image);

            image.addPropertyChangeListener(this);

            //notify
            propertyChangeSupport.firePropertyChange("Model.MrImage["+mr_images.lastIndexOf(image)+"]", null, image);
        }
    }
    
    public Sonication getSonication(int index) {
        try {
            return sonications.get(index);
        }
        catch(IndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public void setSonication(int index, Sonication sonication) {
        if (index < 0) {
            return;
        }
        
        Sonication oldVal = null;
        
        if (index >= sonications.size()) {
            sonications.add(index, sonication);
        }
        else {
            oldVal = sonications.get(index);
            if (oldVal != null) {
                oldVal.removePropertyChangeListener(this);
            }
            sonications.set(index, sonication);
        }
        
        if (sonication != null) {
            sonication.addPropertyChangeListener(this);
        }

        //notify
        propertyChangeSupport.firePropertyChange("Model.Sonication["+index+"]", oldVal, sonication);
    }
    
    public void addSonication(Sonication sonication) {
        sonications.add(sonication);
        
        sonication.addPropertyChangeListener(this);

        //notify
        propertyChangeSupport.firePropertyChange("Model.Sonication["+sonications.lastIndexOf(sonication)+"]", null, sonication);
    }
    
    public void deleteSonication(int index) {
        if (index < 0) return;
        
        Sonication s = sonications.get(index);
        
        if (s != null) {
            s.removePropertyChangeListener(this);

            // notify observers
            // meant to signal sonication deletion
            propertyChangeSupport.firePropertyChange("Model.Sonication["+index+"]", s, null);
            
            s.clear();
        }
               
        sonications.remove(index);
        
    }
    
    public int getSonicationCount() { return sonications.size(); }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public Object getAttribute(String name, Object defaultValue) {
        return attributes.get(name, defaultValue);
    }
    
    public void removeAttribute(String name) {
        Object oldVal = attributes.get(name);
        if (attributes.remove(name) != null) {
            propertyChangeSupport.firePropertyChange("Model.Attribute["+name+"]", oldVal, null);
        }
    }
    
    public void setAttribute(String name, Object value) {

        setAttribute(name, value, false); // attributes are not transient by default        
    }
    
    public void setAttribute(String name, Object value, boolean isTransient) {
        if (Thread.currentThread() != myThread) {

            CrossThreadCallable c = new CrossThreadCallable() {
                @Override
                public Void call() {
                    Model.this.setAttribute(name, value, false);
                    return null;
                }
            };

            Main.callCrossThreadCallable(c);

            return;
        }

        Object oldValue = attributes.get(name);
        
//        if (oldValue != value) {
            attributes.put(name, value, isTransient);

            //notify
            propertyChangeSupport.firePropertyChange("Model.Attribute["+name+"]", null, value);
//        }
    }
    
    public boolean getIsAttributeTransient(String name) {
        return attributes.getIsAttributeTransient(name);
    }
        
    public Iterator<String> getAttributeKeys() {        
        return attributes.keySet().iterator();
    }    

    // Recieve updates from child observables and forward to model observers
    @Override
    public void propertyChange(PropertyChangeEvent arg) {
        
//        System.out.println("----Model update: " + o.toString());
        if (arg != null) {
            Object o = arg.getSource();
            
            String childName = null;
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
            
            if (childName != null) {
                this.propertyChangeSupport.firePropertyChange("Model." + childName + arg.getPropertyName(), arg.getOldValue(), arg.getNewValue());
            }
       }
        
    }
    
    public void saveModel(File file, ProgressListener listener) throws IOException {
        
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            
            ImageVolume4DXStreamConverter imageConverter = new ImageVolume4DXStreamConverter();
            TransducerXStreamConverter transducerConverter = new TransducerXStreamConverter();
            
            // Set xstream security. Only allow classes from this package to be instantiated.
            XStream xstream = new XStream();
            XStream.setupDefaultSecurity(xstream);
            xstream.allowTypesByWildcard(new String[] {
                "org.fusfoundation.kranion.**"
            });
            
            xstream.alias("Model", Model.class);
            xstream.alias("Sonication", Sonication.class);
            xstream.alias("Transducer", Transducer.class);
            xstream.alias("AttributeList", AttributeList.class);
            xstream.alias("Vector2f", org.fusfoundation.kranion.model.Vector2f.class);
            xstream.alias("Vector3f", org.fusfoundation.kranion.model.Vector3f.class);
            xstream.alias("Matrix3f", org.fusfoundation.kranion.model.Matrix3f.class);
            xstream.alias("Matrix4f", org.fusfoundation.kranion.model.Matrix4f.class);
            xstream.alias("Quaternionf", org.fusfoundation.kranion.model.Quaternionf.class);
            xstream.alias("ImageVolume4D", ImageVolume4D.class);
            xstream.registerConverter(new ModelXStreamConverter());
            xstream.registerConverter(new SonicationXStreamConverter());
            xstream.registerConverter(imageConverter);
            xstream.registerConverter(new AttributeListXStreamConverter());
            xstream.registerConverter(transducerConverter);
                        
            if (listener != null) {
                listener.percentDone("Saving scene", 0);
            }
            
            zos.putNextEntry(new ZipEntry("model.xml"));                       
            xstream.toXML(this, zos);            
            zos.closeEntry();
            
            imageConverter.marshalVoxelData(zos, listener);
            transducerConverter.marshalMeshData(zos, listener);
            
            zos.close();
    }
    
    public static Model loadModel(File file, ProgressListener listener) throws IOException, ClassNotFoundException {
        
            // attempt to put up the progress bar immediately since
            // some storage can be slow to respond
            if (listener != null) {
                listener.percentDone("Loading scene", 0);
                Main.processNextFrame();
            }
                
            Model newModel = null;
            ObjectInputStream is = null;
                    
            // Try first to load the model using old Serialization mechanism.
            // This is simple and fast, but opaque and brittle
            try {
                is = new ObjectInputStream(new FileInputStream(file));
                newModel = (Model)is.readObject();
            }
            catch(Exception e) {
                newModel = null;
            }
            finally {
                if (is != null) {
                    is.close();
                }
            }

            // If Java Serialization loading failed then try the new style
            // Zip archive with XML model serialization and raw voxel data streams.
            // This is slower but more archival in that the XML is human readable
            // and raw voxel data should be readable by other applications.
            if (newModel == null) {
                ImageVolume4DXStreamConverter imageConverter = new ImageVolume4DXStreamConverter();
                TransducerXStreamConverter transducerConverter = new TransducerXStreamConverter();

            // Set xstream security. Only allow classes from this package to be instantiated.
                XStream xstream = new XStream();
                XStream.setupDefaultSecurity(xstream);
                xstream.allowTypesByWildcard(new String[] {
                    "org.fusfoundation.kranion.**"
                });
                
                xstream.alias("Model", Model.class);
                xstream.alias("Sonication", Sonication.class);
                xstream.alias("AttributeList", AttributeList.class);
                xstream.alias("Vector2f", org.fusfoundation.kranion.model.Vector2f.class);
                xstream.alias("Vector3f", org.fusfoundation.kranion.model.Vector3f.class);
                xstream.alias("Matrix3f", org.fusfoundation.kranion.model.Matrix3f.class);
                xstream.alias("Matrix4f", org.fusfoundation.kranion.model.Matrix4f.class);
                xstream.alias("Quaternionf", org.fusfoundation.kranion.model.Quaternionf.class);
                xstream.alias("ImageVolume4D", ImageVolume4D.class);
                xstream.registerConverter(new ModelXStreamConverter());
                xstream.registerConverter(new SonicationXStreamConverter());
                xstream.registerConverter(imageConverter);
                xstream.registerConverter(transducerConverter);
                xstream.registerConverter(new AttributeListXStreamConverter());

                if (listener != null) {
                    listener.percentDone("Loading scene", 0);
                }
                
                // read the model XML entry
                ZipFile modelFile = new ZipFile(file);
                ZipEntry modelxml = modelFile.getEntry("model.xml");
                InputStream modelxmlstream = modelFile.getInputStream(modelxml);
                newModel = (Model)xstream.fromXML(modelxmlstream);
                modelxmlstream.close();

                // read entries for each image data channel
                imageConverter.unmarshalVoxelData(modelFile, listener);
                // read entries for transducer model meshes
                transducerConverter.unmarshalMeshData(modelFile, listener);
                
                modelFile.close();
                
                if (listener != null) {
                    listener.percentDone("Ready.", -1);
                }
            }
            
            
            return newModel;
    }
}
