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

import java.util.*;
import java.io.*;

/**
 *
 * @author  jsnell
 */
public class PersonName implements Serializable, VrValue {

    private String _firstName, _lastName, _middleName, _prefix, _suffix;
    
    /** Creates a new instance of PersonName */
    
    public PersonName() {
      _firstName = "";
      _lastName = "";
      _middleName = "";
      _prefix = "";
      _suffix = "";
    }
    
    public PersonName(String familyName, String givenName, String middleName, String prefix, String suffix) {
        _firstName = givenName;
        _lastName = familyName;
        _middleName = middleName;
        _prefix = prefix;
        _suffix = suffix;
    }
    
    public PersonName(byte[] value)
    {
        this.setVrValue(VR.PN, value);
    }
    
    public PersonName(int vrType, byte[] value)
    {
        this.setVrValue(vrType, value);
    }
    
    public String getFamilyName() { return _firstName; }
    public String getGivenName() { return _lastName; }
    public String getMiddelName() { return _middleName; }
    public String getPrefix() { return _prefix; }
    public String getSuffix() { return _suffix; }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( _prefix);
        if (_prefix.length() > 0) buf.append(" ");
        buf.append(_firstName);
        if (_firstName.length() > 0) buf.append(" ");
        buf.append(_middleName);
        if (_middleName.length() > 0) buf.append(" ");
        buf.append(_lastName);
        if (_suffix.length() > 0) buf.append(", ");
        buf.append(_suffix);
        
        return buf.toString();
    }
    
    public byte[] getVrValue(int vrType) {
        if (vrType != VR.PN) {
            throw new RuntimeException("Bad VR Type");
        }
        
        int len2 = _firstName.length();
        int len3 = _middleName.length();
        int len4 = _prefix.length();
        int len5 = _suffix.length();
        
         StringBuffer buf = new StringBuffer();
        buf.append(_lastName);
        if (len2+len3+len4+len5 > 0) {
            buf.append("^");
        }
        buf.append(_firstName);
        if (len3+len4+len5 > 0) {
           buf.append("^");
        }
        buf.append(_middleName);
        if (len4+len5 > 0) {
           buf.append("^");
        }
        buf.append(_prefix);
        if (len5 > 0) {
           buf.append("^");
        }
        buf.append(_suffix);
        
        String result = buf.toString();
        
        // pad to even length
        int len = result.length();
        len += len % 2;
        
        // allocate new result buffer
        byte[] resultbuf = new byte[len];
        
        byte[] src = null;
        try {
            src = result.getBytes("8859_1");
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string Encoding");
        }
        
        // copy the characters into the padded result buffer
        System.arraycopy(src, 0, resultbuf, 0, result.length());
        
        return resultbuf;
   }    
    
    public void setVrValue(int vrType, byte[] value) {
        String valStr;
        
         if (vrType != VR.PN) {
            throw new RuntimeException("Bad VR Type");
        }
       
        try {
            valStr = new String(value, "8859_1").trim();
        }
        catch (UnsupportedEncodingException e2) {
            throw new RuntimeException("Unknow string Encoding");
        }
        
        StringTokenizer strtok = new java.util.StringTokenizer(valStr);
        
        _lastName = "";
        _firstName = "";
        _middleName = "";
        _prefix = "";
        _suffix = "";
        
        try {
            _lastName = strtok.nextToken("^");
            _firstName = strtok.nextToken("^");
            _middleName = strtok.nextToken("^");
            _prefix = strtok.nextToken("^");
            _suffix = strtok.nextToken("^");
        }
        catch(NoSuchElementException e) {
        }
    }
    
}
