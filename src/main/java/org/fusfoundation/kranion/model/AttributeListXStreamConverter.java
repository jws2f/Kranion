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
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jsnell
 */
public class AttributeListXStreamConverter implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        AttributeList list = (AttributeList)o;
        
        writer.startNode("AttributeList");
        writer.addAttribute("count", Integer.toString(list.nontransientSize()));
        Iterator<String> keys = list.nontransientKeySet().iterator();
        while(keys.hasNext()) {
            String key = keys.next();
            writer.startNode("Attribute");
            Object value = list.get(key);
            writer.addAttribute("key", key);
            Object translatedValue = list.translateOut(value);
            writer.addAttribute("type", translatedValue.getClass().getName());
                mc.convertAnother(translatedValue);
            writer.endNode();
        }
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        AttributeList list = new AttributeList();
        
        Object co = uc.currentObject();
        
        reader.moveDown();
                
        while(reader.hasMoreChildren()) {
            reader.moveDown();
            String key = reader.getAttribute("key");
            String type = reader.getAttribute("type");
            
            try {
                Object o = uc.convertAnother(null, Class.forName(type));

                list.put(key, o);
            }
//            catch(NoSuchMethodException nsm) {
//                System.out.println("Failed newInstance(): " + type);
//            }
//            catch(InvocationTargetException it) {
//                System.out.println("Failed newInstance(): " + type);
//            }
//            catch(IllegalAccessException ia) {
//                System.out.println("Failed newInstance(): " + type);
//            }
//            catch(InstantiationException ie) {
//                System.out.println("Failed newInstance(): " + type);
//            }
            catch(ClassNotFoundException e) {
                Logger.getGlobal().log(Level.SEVERE, "AttributeListXStreamConverter class not found: " + type);
            }
            
            reader.moveUp();
        }
        
        reader.moveUp();
                
        return list;
    }

    @Override
    public boolean canConvert(Class type) {
        return AttributeList.class == type;
    }
    
}
