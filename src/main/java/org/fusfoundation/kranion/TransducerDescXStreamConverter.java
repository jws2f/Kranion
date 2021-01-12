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
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class TransducerDescXStreamConverter  implements Converter {

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
        TransducerDesc transDesc = new TransducerDesc();
        
        while(reader.hasMoreChildren()) {
            reader.moveDown();
            String nodeName = reader.getNodeName();
            
            switch(nodeName) {
                case "ElementDescFileName":
                    transDesc.setElementDescFilename(reader.getValue());
                    break;
                case "Transform":
                    transDesc.setTransform((org.lwjgl.util.vector.Matrix4f)uc.convertAnother(null, Matrix4f.class));
                    break;
                case "Meshes":
                    while (reader.hasMoreChildren()) { // Loop over meshes
                        reader.moveDown();
                        
                        if (reader.getNodeName().equalsIgnoreCase("Mesh")) {
                            TransducerDesc.Mesh mesh = new TransducerDesc.Mesh();
                            String meshFileName = reader.getAttribute("filename");
                            mesh.setFileName(meshFileName);

                            while(reader.hasMoreChildren()) { // Loop over mesh properties
                                reader.moveDown();
                                String meshNodeName = reader.getNodeName();
                                switch(meshNodeName) {
                                    case "color":
                                        mesh.setColor((org.lwjgl.util.vector.Vector4f)uc.convertAnother(null, Vector4f.class));
                                        break;
                                    case "clipColor":
                                        mesh.setClipColor((org.lwjgl.util.vector.Vector4f)uc.convertAnother(null, Vector4f.class));
                                        break;
                                }
                                reader.moveUp();
                            }
                            
                            transDesc.addMesh(mesh);
                        }
                        
                        reader.moveUp();
                    }
                    break;
            }
            
            reader.moveUp();
        }
        
        return transDesc;
    }

    @Override
    public boolean canConvert(Class type) {
        return TransducerDesc.class == type;
    }
    
}
