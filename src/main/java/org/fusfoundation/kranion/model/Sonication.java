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

// import the lwjgl Vector classes specifically
import org.lwjgl.util.vector.Vector3f;

//import com.sun.scenario.effect.impl.BufferUtil;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import java.nio.FloatBuffer;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;


/**
 *
 * @author John Snell
 */
public class Sonication extends Observable implements Serializable{

    private static final long serialVersionUID = 7114004181140529979L;
    
    public class SubSonication {
        private Vector3f focus_steering = new Vector3f(0f, 0f, 0f);
        private float power_w;
        private float duration_s;
        private List<Float> phases = new ArrayList<>(1024);
        private List<Float> amplitudes = new ArrayList<>(1024);
        
        private SubSonication(Vector3f steering, float power, float duration) {
            focus_steering.set(steering);
            power_w = power;
            duration_s = duration;            
        }
        
        public int getChannelCount() { return phases.size(); }
        
        public void setFocusSteering(Vector3f steer) {
            focus_steering.set(steer);
        }

        public void setPower(float power) {
            power_w = power;
        }

        public void setDuration(float duration) {
            duration_s = duration;
        }

        public float getPhase(int channel) {
            if (channel >= 0 && channel < phases.size()) {
                return phases.get(channel);
            } else {
                return 0f;
            }
        }

        public float getAmplitude(int channel) {
            if (channel >= 0 && channel < amplitudes.size()) {
                return amplitudes.get(channel);
            } else {
                return 0f;
            }
        }

        public void setPhase(int channel, float value) {
            if (channel >= 0) {// && channel < phases.size()) {
                try {
                    phases.set(channel, value);
                }
                catch (IndexOutOfBoundsException e) {
                    phases.add(channel, value);
                }
            }
        }

        public void setAmplitude(int channel, float value) {
            if (channel >= 0) {// && channel < amplitudes.size()) {
                try {
                    amplitudes.set(channel, value);
                }
                catch(IndexOutOfBoundsException e) {
                    amplitudes.add(channel, value);
                }
            }
        }
    }
    
    private ArrayList<SubSonication> subsonications;
    
    private Vector3f natural_focus_location = new Vector3f(0f, 0f, 0f); // UVW/LPS coordinate system
//    private Vector3f focus_steering = new Vector3f(0f, 0f, 0f);
//    private float power_w;
//    private float duration_s;
    private float frequency;
    private ImageVolume4D thermometryMagnitude;
    private ImageVolume4D thermometryPhase;
    private AttributeList attributes = new AttributeList();
//    private List<Float> phases = new ArrayList<>(1024);
//    private List<Float> amplitudes = new ArrayList<>(1024);

    public Sonication() {
        this.subsonications = new ArrayList<>();
        addSubSonication(new Vector3f(0,0,0), 0, 0);
    }
    
    public Sonication(Vector3f location, float power, float duration) {
        this.subsonications = new ArrayList<>();
//        power_w = power;
//        duration_s = duration;
        addSubSonication(new Vector3f(0,0,0), power, duration);
        natural_focus_location.set(location);
    }
    
    public SubSonication addSubSonication(Vector3f steering, float power, float duration) {
        SubSonication sub = new SubSonication(steering, power, duration);
                
        subsonications.add(sub);
        
        return sub;
    }
    
    public int getSubSonicationCount() {
        return subsonications.size();
    }
    
    public SubSonication getSubSonication(int subSonicationIndex) {
        return subsonications.get(subSonicationIndex);
    }
    
    public void clear() {
        subsonications.clear();
        attributes.clear();
        ImageVolumeUtil.releaseTextures(thermometryMagnitude);
        thermometryMagnitude = null;
        ImageVolumeUtil.releaseTextures(thermometryPhase);
        thermometryPhase = null;
    }
    
    public int getChannelCount() { return getSubSonication(0).getChannelCount(); }
    
    public void setNaturalFocusLocation(Vector3f pos) {
        natural_focus_location = pos;
        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "naturalFocusLocation", null, natural_focus_location));
    }
    
    public Vector3f getNaturalFocusLocation() {
        return natural_focus_location;
    }
    
    public void setFocusSteering(Vector3f pos) {
        setFocusSteering(0, pos);
    }
    
    public void setFocusSteering(int subSonicationIndex, Vector3f pos) {
        subsonications.get(subSonicationIndex).focus_steering.set(pos);
        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "focusSteering", null, subsonications.get(subSonicationIndex).focus_steering));
    }
    
    public Vector3f getFocusSteering() {
        return getFocusSteering(0);
    }
    
    public Vector3f getFocusSteering(int subSonicationIndex) {
        return subsonications.get(subSonicationIndex).focus_steering;
    }
    
    public void setFrequency(float freq) {
        frequency = freq;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "frequency", null, frequency));
    }
    
    public float getFrequency() { return frequency; }
    
    public float getPower() { return getPower(0); }
    
    public float getPower(int subSonicationIndex) {
        return subsonications.get(subSonicationIndex).power_w;
    }
    
    public void setPower(float power) {
        setPower(0, power);
    }
    
    public void setPower(int subSonicationIndex, float power) {
        subsonications.get(subSonicationIndex).power_w = power;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "power", null, power));
    }
    
    public float getDuration() {
        return getDuration(0);
    }
    
    public float getDuration(int subSonicationIndex) {
        return subsonications.get(subSonicationIndex).duration_s;
    }
    
    public void setDuration(float duration) {
        setDuration(0, duration);
    }
    
    public void setDuration(int subSonicationIndex, float duration) {
        subsonications.get(subSonicationIndex).duration_s = duration;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "duration", null, duration));
    }
    
    public ImageVolume getThermometryMagnitude() {
        return thermometryMagnitude;
    }
    
    public void setThermometryMagnitude(ImageVolume4D image) {
        thermometryMagnitude = image;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "thermometryMagnitude", null, image));
    }
    
    public ImageVolume getThermometryPhase() {
        return thermometryPhase;
    }
    
    public void setThermometryPhase(ImageVolume4D image) {
        thermometryPhase = image;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "thermometryPhase", null, image));
    }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public AttributeList getAttributeList() { return attributes; }
    
    public Iterator<String> getAttributeKeys() {
        return attributes.keySet().iterator();
    }
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
            //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Attribute["+name+"]", null, value));
    }
        
    public float getPhase(int channel) {
        if (channel >= 0 && channel < getChannelCount()) {
            return getSubSonication(0).getPhase(channel);
        } else {
            return 0f;
        }
    }

    public float getAmplitude(int channel) {
        if (channel >= 0 && channel < getChannelCount()) {
            return getSubSonication(0).getAmplitude(channel);
        } else {
            return 0f;
        }
    }

    public void setPhase(int channel, float value) {
        if (channel >= 0) {// && channel < phases.size()) {
            getSubSonication(0).setPhase(channel, value);
        }
    }

    public void setAmplitude(int channel, float value) {
        if (channel >= 0) {// && channel < amplitudes.size()) {
            getSubSonication(0).setAmplitude(channel, value);
        }
    }

}
