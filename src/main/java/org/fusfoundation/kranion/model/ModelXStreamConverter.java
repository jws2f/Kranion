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
import org.fusfoundation.kranion.Transducer;
import org.fusfoundation.kranion.model.image.ImageVolume4D;

/**
 *
 * @author jsnell
 */
public class ModelXStreamConverter  implements Converter{

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        Model model = (Model)o;
        
        writer.startNode("Attributes");
        if (model.getAttributeList() != null) {
            mc.convertAnother(model.getAttributeList());
        }
        writer.endNode();
        
        writer.startNode("CT_Image");
        if (model.getCtImage() != null) {
            mc.convertAnother(model.getCtImage());
        }
        writer.endNode();
        
        writer.startNode("MR_Images");
            writer.addAttribute("count", Integer.toString(model.getMrImageCount()));
            writer.addAttribute("selectedMR", Integer.toString(model.getSelectedMR()));

            for (int i=0; i<model.getMrImageCount(); i++) {
                writer.startNode("MR_Image");
                    writer.addAttribute("num", Integer.toString(i));
                    mc.convertAnother(model.getMrImage(i));
                writer.endNode();
            }
        writer.endNode();
        
        writer.startNode("Sonications");
            writer.addAttribute("count", Integer.toString(model.getSonicationCount()));
            writer.addAttribute("selectedSonication", Integer.toString(model.getSelectedSonication()));
            for (int i=0; i<model.getSonicationCount(); i++) {
                writer.startNode("Sonication");
                    writer.addAttribute("num", Integer.toString(i));
                    mc.convertAnother(model.getSonication(i));
                writer.endNode();
            }
        writer.endNode();
        
        writer.startNode("Transducer");
            if (model.getTransducer() != null) {
                mc.convertAnother(model.getTransducer());
            }
        writer.endNode();
        
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        Model model = new Model();
        
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            
            String nodeName = reader.getNodeName();
            switch(nodeName) {
                case "Attributes":
                    AttributeList attrList = (AttributeList)uc.convertAnother(model, AttributeList.class);
                    Iterator<String> keys = attrList.keySet().iterator();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        model.setAttribute(key, AttributeList.translateIn(attrList.get(key)));
                    }
                    break;
                case "CT_Image":
                    model.setCtImage((ImageVolume4D)uc.convertAnother(model, ImageVolume4D.class));
                    break;
                case "MR_Images":
                    int mrCount = Integer.parseInt(reader.getAttribute("count"));
                    int selectedMR = Integer.parseInt(reader.getAttribute("selectedMR"));
                    while(reader.hasMoreChildren()) {
                        reader.moveDown();
                        if (reader.getNodeName().equalsIgnoreCase("MR_Image")) {
                            int mrIndex = Integer.parseInt(reader.getAttribute("num"));
                            model.setMrImage(mrIndex, (ImageVolume4D)uc.convertAnother(model, ImageVolume4D.class));
                        }
                        reader.moveUp();
                   }
                    model.setSelectedMR(selectedMR);
                    break;
                case "Sonications":
                    int sonicationrCount = Integer.parseInt(reader.getAttribute("count"));
                    int selectedSonication = Integer.parseInt(reader.getAttribute("selectedSonication"));
                    int sonicationindex = 0;
                    while(reader.hasMoreChildren()) {
                        reader.moveDown();
                        if (reader.getNodeName().equalsIgnoreCase("Sonication")) {
                            Sonication s = (Sonication)uc.convertAnother(model, Sonication.class);
                            model.addSonication(s);
                            System.out.println("Model unmarshalling sonication " + sonicationindex++);
                        }
                        reader.moveUp();
                    }
                    break;
                case "Transducer":
                    Transducer t = (Transducer)uc.convertAnother(model, Transducer.class);
                    model.setTransducer(t);
                    break;
            }
            
            reader.moveUp();
        }
        
        return model;
    }

    @Override
    public boolean canConvert(Class type) {
        return type == org.fusfoundation.kranion.model.Model.class;
    }
    
}
