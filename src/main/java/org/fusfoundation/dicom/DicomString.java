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

import java.io.Serializable;
import org.fusfoundation.util.StringConvert;

/**
 *
 * @author  jsnell
 */
public class DicomString implements Serializable, VrValue {

    private String value;
    
    /** Creates a new instance of DicomString */
    public DicomString() {
        value = "";
    }
    
    public DicomString(String s) {
        value = s;
    }
    
    public static DicomString New() {
        return new DicomString();
    }
    
    public String getValue() { return value; }
    public void setValue(String s) { value = s; }

    public void setVrValue(int vrType, byte[] val) {
        switch (vrType) {
            case VR.UI:
                 value = StringConvert.bytesToString(val).trim();
                break;
            case VR.UT:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.SH:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.ST:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.LO:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.LT:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.AE:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.AS:
                value = StringConvert.bytesToString(val).trim();
                break;
            case VR.CS:
                value = StringConvert.bytesToString(val).trim();
                break;
            default:
                throw new DicomRuntimeException("Bad VR type");
        }
    }
    
    public byte[] getVrValue(int vrType) {
        switch (vrType) {
            case VR.UI:
                 return StringConvert.stringToBytes(value);
            case VR.UT:
                 return StringConvert.stringToBytes(value);
             case VR.SH:
                 return StringConvert.stringToBytes(value);
            case VR.ST:
                 return StringConvert.stringToBytes(value);
            case VR.LO:
                 return StringConvert.stringToBytes(value);
            case VR.LT:
                 return StringConvert.stringToBytes(value);
            case VR.AE:
                 return StringConvert.stringToBytes(value);
            case VR.AS:
                 return StringConvert.stringToBytes(value);
            case VR.CS:
                 return StringConvert.stringToBytes(value);
            default:
                throw new DicomRuntimeException("Bad VR type");
        }
    }
    
    public String toString() {
        return value.toString();
    }
    
}
