/*
 * StringConversion.java
 *
 * Created on February 4, 2002, 3:55 PM
 */

package org.fusfoundation.util;

import org.fusfoundation.dicom.DicomRuntimeException;
import java.io.*;

/**
 *
 * @author  jsnell
 */
public class StringConvert {

    /** Creates a new instance of StringConversion */
    private StringConvert() {}
    
    static public byte[] stringToBytes(String s) {
        try {
            return s.getBytes("8859_1");
        }
        catch(UnsupportedEncodingException e) {
            throw new DicomRuntimeException("Unknown string Encoding");
        }
    }
    
    static public String bytesToString(byte[] val)
    {
        try {
            return new String(val, "8859_1");
        }
        catch(UnsupportedEncodingException e) {
            throw new DicomRuntimeException("Unknown string Encoding");
        }
    }

}
