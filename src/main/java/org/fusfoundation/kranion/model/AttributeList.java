/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author john
 */
public class AttributeList implements Serializable {
    
    private static final long  serialVersionUID =  -3117599764725849715L;
    
    private HashMap<String, Object> attributes = new HashMap<>();
    private transient HashMap<String, Object> transient_attributes = new HashMap<>();
    
    public AttributeList() {}
    
    public int size() {
        return attributes.size() + transient_attributes.size();
    }
    
    public int nontransientSize() {
        return attributes.size();
    }
    
    public Object get(String name) {
        Object retVal = null;
                              
        retVal = attributes.get(name);
        
        if (retVal == null) {
            retVal = transient_attributes.get(name);
        }
        
        return retVal;
    }
    
    public Object get(String name, Object defaultValue) {
        Object result = get(name);
        if (result == null) {
            put(name, defaultValue);
            return defaultValue;
        }
        else {
            return result;
        }
    }
    
    public Object remove(String name) {
        Object retVal = null;
                
        Object a = attributes.remove(name);
        Object b = transient_attributes.remove(name);
        
        if (a != null) retVal = a;
        if (retVal == null && b != null) retVal = b;
        
        return retVal;
    }
    
    public Set<String> keySet() {
        Set<String> mergedKeys = new HashSet<>();
        
        mergedKeys.addAll(attributes.keySet());
        mergedKeys.addAll(transient_attributes.keySet());
        
        return mergedKeys;
    }
    
    public Set<String> nontransientKeySet() {        
        return attributes.keySet();
    }
    
    public void put(String name, Object value) {
        put(name, value, false);
    }
    
    public void put(String name, Object value, boolean isTransient) {
        if (name != null && name.length()>0 && value != null) {
            if (isTransient) {
                transient_attributes.put(name, value);
                attributes.remove(name);
            }
            else {
                attributes.put(name, value);
                transient_attributes.remove(name);
            }
        }
    }
    
    public boolean getIsAttributeTransient(String name) {
        return transient_attributes.get(name) != null;
    }
    
    public void clear() {
        attributes.clear();
        transient_attributes.clear();
    }
    
    // Need to do some custom filtering
    private void writeObject(ObjectOutputStream out)throws IOException {

        // Write filtered attribute list
        out.writeInt(attributes.size());
        Iterator<String> iter = attributes.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            Object obj = attributes.get(key);
//            System.out.println("Out -> Key: " + key);
            
            out.writeUTF(key);
            out.writeObject(translateOut(obj));
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        attributes = new HashMap<String, Object>();
        transient_attributes = new HashMap<String, Object>();
        
        int size = in.readInt();
        for (int i=0; i<size; i++) {
            String key = (String)in.readUTF();
//            System.out.println("In <- Key: " + key);
            attributes.put(key, translateIn(in.readObject()));
        }
    }

    // translateIn and translateOut are an effort to future proof the representation of
    // vector,matrix,quaternion objects in serialized storage. So these entities' values are stored
    // in a simple proxy class that we know will be supported, even if functionally we use
    // a different vector/matrix class library later. This will definitely happen on the transition
    // form lwjgl 2.9 to lwjgl 3.x in the near future. Don't want to orphan the stored files people
    // have saved. Downside of using native Java serialization, but simple and fast.
    public static Object translateOut(Object obj) {
        
        if (obj == null) return null;
        
        // So defensively only serializing object classes that we know will
        // be supportted either natively or by Kranion classes in the future.
        // Otherwise somebody could add custom attributes with class types
        // that might not be on the classpath in the future and make loading
        // the model impossible in the future. Unknown objects get stored as
        // their .toString() values, for lack of anything better to do.
        switch (obj.getClass().getName()) {
            // Built in things we support, pass through
            case "java.lang.Float":
            case "java.lang.Double":
            case "java.lang.Integer":
            case "java.lang.Short":
            case "java.lang.Byte":
            case "java.lang.Character":
            case "java.lang.Boolean":
            case "java.lang.String":
            case "[F": // one dimension arrays of built in types
            case "[D":
            case "[I":
            case "[S":
            case "[J":
            case "[Z":
            case "[B":
            case "[C":
            case "org.fusfoundation.dicom.DicomDate":
            case "org.fusfoundation.dicom.DicomString":
            case "org.fusfoundation.dicom.PersonName":
            case "org.fusfoundation.dicom.DicomNumber":
            case "org.fusfoundation.kranion.model.image.ImageVolume4D":
                return obj;
            // Vector types that we need to translate: LWJGL->our archival format
            case "org.lwjgl.util.vector.Vector2f": {
                org.lwjgl.util.vector.Vector2f vec = (org.lwjgl.util.vector.Vector2f) obj;
                Vector2f tmp = new Vector2f(vec.x, vec.y);
                return tmp;
            }
            case "org.lwjgl.util.vector.Vector3f": {
                org.lwjgl.util.vector.Vector3f vec = (org.lwjgl.util.vector.Vector3f) obj;
                Vector3f tmp = new org.fusfoundation.kranion.model.Vector3f(vec.x, vec.y, vec.z);
                return tmp;
            }
            case "org.lwjgl.util.vector.Quaternion": {
                org.lwjgl.util.vector.Quaternion q = (org.lwjgl.util.vector.Quaternion) obj;
                Quaternionf tmp = new org.fusfoundation.kranion.model.Quaternionf(q.x, q.y, q.z, q.w);
                return tmp;
            }
            case "org.lwjgl.util.vector.Matrix3f": {
                org.lwjgl.util.vector.Matrix3f m = (org.lwjgl.util.vector.Matrix3f) obj;
                Matrix3f tmp = new org.fusfoundation.kranion.model.Matrix3f(m.m00, m.m01, m.m02,
                                            m.m10, m.m11, m.m12,
                                            m.m20, m.m21, m.m22);
                return tmp;
            }
            case "org.lwjgl.util.vector.Matrix4f": {
                org.lwjgl.util.vector.Matrix4f m = (org.lwjgl.util.vector.Matrix4f) obj;
                Matrix4f tmp = new org.fusfoundation.kranion.model.Matrix4f(m.m00, m.m01, m.m02, m.m03,
                                            m.m10, m.m11, m.m12, m.m13,
                                            m.m20, m.m21, m.m22, m.m23,
                                            m.m30, m.m31, m.m32, m.m33);
                return tmp;
            }
            // otherwise just save string representation
            // might loose info, but want to protect against saving
            // objects that might not be on classpath in future.
            default:
//                System.out.println("AttributeList: saving " + obj.getClass().getName());
                return obj.toString();

        }
    }
    
    public static Object translateIn(Object obj) {
        
        if (obj == null) return null;
        
        // If we encounter any built in vector/matrix/quaternion classes,
        // then translate them to whatever vector/matrix/quaternion class library
        // we are really using. Again, just trying to make the transition from
        // lwjgl2 -> lwjgl3 forward compatible.
        switch (obj.getClass().getName()) {
            // Vector types that we need to translate: our archival format->LWJGL (JOML in future version)
            case "org.fusfoundation.kranion.model.Vector2f": {
                org.fusfoundation.kranion.model.Vector2f vec = (org.fusfoundation.kranion.model.Vector2f) obj;
                org.lwjgl.util.vector.Vector2f tmp = new org.lwjgl.util.vector.Vector2f(vec.x, vec.y);
                return tmp;
            }
            case "org.fusfoundation.kranion.model.Vector3f": {
                org.fusfoundation.kranion.model.Vector3f vec = (org.fusfoundation.kranion.model.Vector3f) obj;
                org.lwjgl.util.vector.Vector3f tmp = new org.lwjgl.util.vector.Vector3f(vec.x, vec.y, vec.z);
                return tmp;
            }
            case "org.fusfoundation.kranion.model.Quaternionf": {
                org.fusfoundation.kranion.model.Quaternionf q = (org.fusfoundation.kranion.model.Quaternionf) obj;
                org.lwjgl.util.vector.Quaternion tmp = new org.lwjgl.util.vector.Quaternion(q.x, q.y, q.z, q.w);
                return tmp;
            }
            case "org.fusfoundation.kranion.model.Matrix3f": {
                org.fusfoundation.kranion.model.Matrix3f m = (org.fusfoundation.kranion.model.Matrix3f) obj;
                org.lwjgl.util.vector.Matrix3f tmp = new org.lwjgl.util.vector.Matrix3f();
                
                tmp.m00 = m.m00;
                tmp.m01 = m.m01;
                tmp.m02 = m.m02;
                
                tmp.m10 = m.m10;
                tmp.m11 = m.m11;
                tmp.m12 = m.m12;
                
                tmp.m20 = m.m20;
                tmp.m21 = m.m21;
                tmp.m22 = m.m22;
                
                return tmp;
            }
            case "org.fusfoundation.kranion.model.Matrix4f": {
                org.fusfoundation.kranion.model.Matrix4f m = (org.fusfoundation.kranion.model.Matrix4f) obj;
                org.lwjgl.util.vector.Matrix4f tmp = new org.lwjgl.util.vector.Matrix4f();

                tmp.m00 = m.m00;
                tmp.m01 = m.m01;
                tmp.m02 = m.m02;
                tmp.m03 = m.m03;
                
                tmp.m10 = m.m10;
                tmp.m11 = m.m11;
                tmp.m12 = m.m12;
                tmp.m13 = m.m13;
                
                tmp.m20 = m.m20;
                tmp.m21 = m.m21;
                tmp.m22 = m.m22;
                tmp.m23 = m.m23;
                
                tmp.m30 = m.m30;
                tmp.m31 = m.m31;
                tmp.m32 = m.m32;
                tmp.m33 = m.m33;
                                
                return tmp;
            }
            default:
                // No filtering
                return obj;

        }
    }
}
    
 
        
