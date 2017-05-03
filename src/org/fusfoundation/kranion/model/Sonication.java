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

import com.sun.scenario.effect.impl.BufferUtil;
import java.beans.PropertyChangeEvent;
import java.io.*;
import org.lwjgl.util.vector.*;
import java.util.*;
import java.nio.FloatBuffer;
import org.fusfoundation.kranion.model.image.*;

/**
 *
 * @author John Snell
 */
public class Sonication extends Observable implements Serializable{
    private Vector3f natural_focus_location = new Vector3f(0f, 0f, 0f); // UVW/LPS coordinate system
    private Vector3f focus_steering = new Vector3f(0f, 0f, 0f);
    private float power_w;
    private float duration_s;
    private float frequency;
    private ImageVolume4D thermometryMagnitude;
    private ImageVolume4D thermometryPhase;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private FloatBuffer phases = BufferUtil.newFloatBuffer(1024);
    private FloatBuffer amplitudes = BufferUtil.newFloatBuffer(1024);
    
    public Sonication() {
    }
    
    public Sonication(Vector3f location, float power, float duration) {
        power_w = power;
        duration_s = duration;
        natural_focus_location.set(location);
    }
    
    public void clear() {
        attributes.clear();
        ImageVolumeUtil.releaseTextures(thermometryMagnitude);
        ImageVolumeUtil.releaseTextures(thermometryPhase);
    }
    
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
        focus_steering = pos;
        // notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "focusSteering", null, focus_steering));
    }
    
    public Vector3f getFocusSteering() {
        return focus_steering;
    }
    
    public void setFrequency(float freq) {
        frequency = freq;
    }
    
    public float getFrequency() { return frequency; }
    
    public float getPower() { return power_w; }
    public void setPower(float power) {
        power_w = power;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "power", null, power));
    }
    public float getDuration() { return duration_s; }
    public void setDuration(float duration) {
        duration_s = duration;
        //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "duration", null, duration_s));
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
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
            //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Attribute["+name+"]", null, value));
    }
    
    public FloatBuffer getPhases() { return phases; }
    public FloatBuffer getAmplitudes() { return amplitudes; }
}
