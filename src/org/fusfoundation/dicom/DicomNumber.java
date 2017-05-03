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
package org.fusfoundation.dicom;

import org.fusfoundation.util.StringConvert;
import java.io.*;
import java.util.*;

/**
 *
 * @author  jsnell
 */
public class DicomNumber implements VrValue {
    
     private double value;
     private int    vrType;
    
    /** Creates a new instance of DicomNumber */
    public DicomNumber() {
        vrType = VR.FD;
        value = 0.0;
    }

    /** Creates a new instance of DicomFloat */    
    public DicomNumber(double d) {
        vrType = VR.FD;
        value = d;
    }
    
    public DicomNumber(float f) {
        vrType = VR.FL;
        value = f;
    }
    
     public DicomNumber(int i) {
        vrType = VR.SL;
        value = i;
    }
    
    public DicomNumber(byte[] val) {
        vrType = VR.FD;
        this.setVrValue(VR.FD, val);
    }

    public void setValue(short n) { value = (double)n; }
    public void setValue(int n) { value = (double)n; }
    public void setValue(long n) { value = (double)n; }
    public void setValue(float n) { value = (double)n; }
    public void setValue(double n) { value = n; }
    public double getValue() { return value; }
    public double getDoubleValue() { return value; }
    public float getFloatValue() { return (float)value; }
    public int getIntValue() { return (int)value; }
    
    public byte[] getVrValue(int vrType) {
        ByteArrayOutputStream os = new ByteArrayOutputStream(8);
        DataOutputStream dos = new DataOutputStream(os);
        
        try {
            if (vrType == VR.FD) {
                dos.writeDouble(value);
            }
            else if (vrType == VR.FL) {
                dos.writeFloat((float)value);
            }
            else if (vrType == VR.SL) {
                dos.writeInt((int)value);
            }
            else if (vrType == VR.SS) {
                dos.writeShort((short)value);
            }
            else if (vrType == VR.US) {
                byte[] v = new byte[2];
                int i = (int)value;
                // Big-Endian
                v[0] = (byte)((i & 0xff00) >>> 8);
                v[1] = (byte)(i & 0xff);
                dos.write(v);
            }
            else if (vrType == VR.UL) {
                byte[] v = new byte[4];
                long i = (long)value;
                // Big-Endian
                v[0] = (byte)((i & 0xff000000) >>> 24);
                v[1] = (byte)((i & 0xff0000) >>> 16);
                v[2] = (byte)((i & 0xff00) >>> 8);
                v[3] = (byte)(i & 0xff);
                dos.write(v);
            }
            else if (vrType == VR.IS) {
                String s = Integer.toString((int)value);
                dos.writeBytes(s);
            }
            else if (vrType == VR.DS) {
                String s = Float.toString((float)value);
                dos.writeBytes(s);
                if (s.length()%2 != 0) {
                    dos.write(0x20); // pad with a space if necessary to make even length
                }
            }
                
        }
        catch (IOException e) {
        }
                
         return os.toByteArray();
    }
    
    public void setVrValue(int vrType, byte[] val) {
        
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(val);
            DataInputStream dis = new DataInputStream(is);
            
            if (vrType == VR.FD) {
                value = dis.readDouble();
            }
            else if (vrType == VR.FL) {
                value = (double)dis.readFloat();
            }
            else if (vrType == VR.SL) {
                value = (double)dis.readInt();
            }
            else if (vrType == VR.SS) {
                value = (double)dis.readShort();
            }
            else if (vrType == VR.US) {
                value = (double)dis.readUnsignedShort();
            }
            else if (vrType == VR.UL) {
                byte[] v = new byte[4];
                dis.read(v);               
                long i = ((v[0] & 0xff) << 24) | ((v[1] & 0xff) << 16) | ((v[2] & 0xff) << 8) | (v[3] & 0xff);
                value = (double)i;
             }
            else if (vrType == VR.IS || vrType == VR.DS) {
                String v = StringConvert.bytesToString(val).trim();
                value = Double.parseDouble(v);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            value = 0.0;
        }
        catch (NumberFormatException e) {
            //e.printStackTrace();
            value = 0.0;
        }
        
        this.vrType = vrType;
   }
    
    public String toString() {
        if (vrType == VR.FL || vrType == VR.DS)
            return Float.toString((float)value);
        else if (vrType == VR.FD)
            return Double.toString(value);
        
        return Integer.toString((int)value);
    }
    
}
