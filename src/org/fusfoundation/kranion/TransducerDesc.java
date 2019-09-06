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

import com.sun.media.jfxmedia.logging.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class TransducerDesc {
    private Matrix4f xfrm;
    private String elementDescFileName;
    
    public TransducerDesc() {
        xfrm = new Matrix4f();
        xfrm.setIdentity();
    }
    
    public static class Mesh {
        private String filename;
        private final Vector4f color;
        private final Vector4f clipColor;
        
        Mesh() {
            color = new Vector4f(0.5f, 0.5f, 0.5f, 1f);
            clipColor = new Vector4f(0.25f, 0.25f, 0.25f, 1f);
        }
        
        String getFilename() { return filename; }
        void setFileName(String newFileName) {
            filename = newFileName;
        }
        Vector4f getColor() { return color; }
        void setColor(Vector4f c) {
            color.set(c);
        }
        
        Vector4f getClipColor() { return clipColor; }
        void setClipColor(Vector4f c) {
            clipColor.set(c);
        }
    }
    
    private List<Mesh> meshList = new ArrayList<>();
    
    public String getElementDescFilename() { return elementDescFileName; }
    public void setElementDescFilename(String filename) {
        elementDescFileName = filename;
    }
    
    public void setTransform(Matrix4f m) {
        xfrm = m;
    }
    public Matrix4f getTransform() { return xfrm; }
    
    public void addMesh(Mesh mesh) {
        meshList.add(mesh);
    }
    
    public Iterator<Mesh> getMeshIterator() {
        return meshList.iterator();
    }
    
    public static TransducerDesc loadDescriptionXML(File file) {
        TransducerDesc result = null;
        XStream xstream = new XStream();
        
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypesByWildcard(new String[] {
            "org.fusfoundation.kranion.**"
        });
        
        xstream.alias("TransducerDesc", TransducerDesc.class);
        xstream.registerConverter(new TransducerDescXStreamConverter());
        try {
            InputStream modelxmlstream = new FileInputStream(file);
            result = (TransducerDesc) xstream.fromXML(modelxmlstream);
        }
        catch (FileNotFoundException e) {
            Logger.logMsg(Logger.ERROR, "TransducerDesc: File not found.");
            System.out.println("TransducerDesc: File not found.");
        }
        catch (ConversionException ce) {
            Logger.logMsg(Logger.ERROR, "TransducerDesc: Malformed transducer XML descriptor file.");
            System.out.println("TransducerDesc: Malformed transducer XML descriptor file.");
        }
        return result;
    }
}
