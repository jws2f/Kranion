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
package org.fusfoundation.kranion.model;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Iterator;
import org.fusfoundation.kranion.model.image.ImageVolume4D;

/**
 *
 * @author jsnell
 */
public class SonicationXStreamConverter implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        Sonication sonication = (Sonication)o;
        
        writer.addAttribute("power_w", Float.toString(sonication.getPower()));
        writer.addAttribute("duration_s", Float.toString(sonication.getDuration()));
        writer.addAttribute("frequency", Float.toString(sonication.getFrequency()));
        
        writer.startNode("NaturalFocusLocation");
            mc.convertAnother(sonication.getNaturalFocusLocation());
        writer.endNode();
        writer.startNode("FocusSteering");
            mc.convertAnother(sonication.getFocusSteering());
        writer.endNode();
        writer.startNode("Attributes");
            mc.convertAnother(sonication.getAttributeList());
        writer.endNode();
        writer.startNode("Channels");
            writer.addAttribute("count", Integer.toString(sonication.getChannelCount()));
            for (int i=0; i<sonication.getChannelCount(); i++) {
                writer.startNode("Channel");
                    writer.addAttribute("num", Integer.toString(i));
                    writer.addAttribute("phase", Float.toString(sonication.getPhase(i)));
                    writer.addAttribute("amplitude", Float.toString(sonication.getAmplitude(i)));
                writer.endNode();
            }
        writer.endNode();
        if (sonication.getThermometryPhase() != null) {
            writer.startNode("ThermometryPhase");
                mc.convertAnother(sonication.getThermometryPhase());
            writer.endNode();
        }
        if (sonication.getThermometryMagnitude() != null) {
            writer.startNode("ThermometryMagnitude");
                mc.convertAnother(sonication.getThermometryMagnitude());
            writer.endNode();
        }
       
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        Sonication sonication = new Sonication();
        
        float power = Float.parseFloat(reader.getAttribute("power_w"));
        float duration = Float.parseFloat(reader.getAttribute("duration_s"));
        float frequency = Float.parseFloat(reader.getAttribute("frequency"));
        
        sonication.setPower(power);
        sonication.setDuration(duration);
        sonication.setFrequency(frequency);
        
        while(reader.hasMoreChildren()) {
            reader.moveDown();
            String nodeName = reader.getNodeName();
            
            switch(nodeName) {
                case "NaturalFocusLocation":
                    sonication.setNaturalFocusLocation((org.lwjgl.util.vector.Vector3f)AttributeList.translateIn(uc.convertAnother(null, Vector3f.class)));
                    break;
                case "FocusSteering":
                    sonication.setFocusSteering((org.lwjgl.util.vector.Vector3f)AttributeList.translateIn(uc.convertAnother(null, Vector3f.class)));
                    break;
                case "Attributes":
                    AttributeList attrList = (AttributeList)uc.convertAnother(sonication, AttributeList.class);
                    Iterator<String> keys = attrList.keySet().iterator();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        sonication.setAttribute(key, AttributeList.translateIn(attrList.get(key)));
                    }
                    break;
                case "Channels":
                    int channelCount = Integer.parseInt(reader.getAttribute("count"));
                    while (reader.hasMoreChildren()) {
                        reader.moveDown();
                        if (reader.getNodeName().equalsIgnoreCase("Channel")) {
                            int num = Integer.parseInt(reader.getAttribute("num"));
                            float phase = Float.parseFloat(reader.getAttribute("phase"));
                            float amplitude = Float.parseFloat(reader.getAttribute("amplitude"));
                            
                            sonication.setPhase(num, phase);
                            sonication.setAmplitude(num, amplitude);
                        }
                        reader.moveUp();
                    }
                    break;
                case "ThermometryPhase":
                    sonication.setThermometryPhase((ImageVolume4D)uc.convertAnother(sonication, ImageVolume4D.class));
                    break;
                case "ThermometryMagnitude":
                    sonication.setThermometryMagnitude((ImageVolume4D)uc.convertAnother(sonication, ImageVolume4D.class));
                    break;
            }
            reader.moveUp();
        }
        
        return sonication;
    }

    @Override
    public boolean canConvert(Class type) {
        return type == org.fusfoundation.kranion.model.Sonication.class;
    }
    
}
