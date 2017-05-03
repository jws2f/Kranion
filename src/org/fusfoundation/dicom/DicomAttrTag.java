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

import org.fusfoundation.util.PrintfFormat;
/**
 *
 * @author  jsnell
 */
public class DicomAttrTag implements VrValue {
    int group, element;
    
    /** Creates a new instance of DicomAttrTag */
    public DicomAttrTag() {
        group = 0;
        element = 0;
    }
    
    public DicomAttrTag(int grp, int elem) {
        group = grp;
        element = elem;
    }
    
    public void setGroup(int grp) { group = grp; }
    public int getGroup() { return group; }
    public void setElement(int elem) { element = elem; }
    public int getElement() { return element; }
    
    public byte[] getVrValue(int vrType)
    {
        byte[] result = new byte[4];
        result[0] = (byte)(group & 0xff);
        result[1] = (byte)((group & 0xff00) >>> 8);
        result[2] = (byte)(element & 0xff);
        result[3] = (byte)((element & 0xff00) >>> 8);
        
        return result;
    }
    
    public void setVrValue(int vrType, byte[] value) {
        if (value.length != 4) {
            throw new java.lang.IllegalArgumentException();
        }
        group = value[0] | value[1] << 8;
        element = value[2] | value[3] << 8;
    }
    
    public String toString()
    {
        PrintfFormat hex = new PrintfFormat("%04x");
        StringBuffer buf = new StringBuffer(12);
        buf.append(hex.sprintf(group));
        buf.append(", ");
        buf.append(hex.sprintf(element));
        return buf.toString();
    }
}
