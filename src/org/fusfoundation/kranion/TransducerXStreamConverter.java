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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author jsnell
 */
public class TransducerXStreamConverter implements Converter{

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        Transducer transducer = (Transducer)o;
        
        writer.addAttribute("name", transducer.getName());

        writer.startNode("Channels");
            writer.addAttribute("count", Integer.toString(transducer.getElementCount()));
            for (int i=0; i<transducer.getElementCount(); i++) {
                writer.startNode("Channel");
                    writer.addAttribute("num", Integer.toString(i));
                    writer.addAttribute("area", Float.toString(transducer.getElementArea(i)));
                    Vector3f position = transducer.getElementPos(i);
                    Vector3f normal = transducer.getElementNormal(i);
                    writer.addAttribute("x", Float.toString(position.x));
                    writer.addAttribute("y", Float.toString(position.y));
                    writer.addAttribute("z", Float.toString(position.z));
                    writer.addAttribute("nx", Float.toString(normal.x));
                    writer.addAttribute("ny", Float.toString(normal.y));
                    writer.addAttribute("nz", Float.toString(normal.z));
                writer.endNode();
            }
        writer.endNode();        
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean canConvert(Class type) {
        return Transducer.class == type;
    }
    
}
