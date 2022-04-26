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
import org.fusfoundation.util.PrintfFormat;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

public class VR implements Serializable, Comparable {
    
    private static final Logger logger = Logger.getGlobal();
    
    private int group, element;
    private int vrType;
    private long length;
    private byte[] value;
    private int multiplicity;
    private Collection seqItems = null;
    private List imageFrames = null;
    
    private static HashMap valueTypeMap;
    
    static {
        try {
            valueTypeMap = new HashMap();
            valueTypeMap.put(new Integer(VR.DA), Class.forName("org.fusfoundation.dicom.DicomDate"));
            valueTypeMap.put(new Integer(VR.TM), Class.forName("org.fusfoundation.dicom.DicomDate"));
            valueTypeMap.put(new Integer(VR.DT), Class.forName("org.fusfoundation.dicom.DicomDate"));
            valueTypeMap.put(new Integer(VR.PN), Class.forName("org.fusfoundation.dicom.PersonName"));
            
            valueTypeMap.put(new Integer(VR.FD), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.FL), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.SS), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.US), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.SL), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.UL), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.DS), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            valueTypeMap.put(new Integer(VR.IS), Class.forName("org.fusfoundation.dicom.DicomNumber"));
            
            valueTypeMap.put(new Integer(VR.UI), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.UT), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.SH), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.ST), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.LO), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.LT), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.AE), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.AS), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.CS), Class.forName("org.fusfoundation.dicom.DicomString"));
            valueTypeMap.put(new Integer(VR.AE), Class.forName("org.fusfoundation.dicom.DicomString"));
            
            valueTypeMap.put(new Integer(VR.UN), Class.forName("org.fusfoundation.dicom.DicomUnknown"));
            valueTypeMap.put(new Integer(VR.OB), Class.forName("org.fusfoundation.dicom.DicomUnknown"));
            valueTypeMap.put(new Integer(VR.OW), Class.forName("org.fusfoundation.dicom.DicomUnknown"));
            valueTypeMap.put(new Integer(VR.SQ), Class.forName("org.fusfoundation.dicom.DicomUnknown"));
            
            valueTypeMap.put(new Integer(VR.AT), Class.forName("org.fusfoundation.dicom.DicomAttrTag"));
        } catch (ClassNotFoundException e) {
            logger.severe("VrValue type mapping error in VR.java");
            //e.printStackTrace();
        }
    }
    
    public VR() {}
    
    public VR(int valueGroup, int valueElement, int valueType, byte[] valueBytes) {
        group = valueGroup;
        element = valueElement;
        vrType = valueType;
        length = valueBytes.length;
        value = valueBytes;
        multiplicity = analyzeValueMultiplicity(group, element, vrType, value);
    }
    
    public VR(int valueGroup, int valueElement, byte[] valueBytes) {
        group = valueGroup;
        element = valueElement;
        length = valueBytes.length;
        value = valueBytes;
        
        // Look up the implicit VR type
        DicomDict dict = DicomDict.getDictionary();
        vrType = dict.getVrType(group << 16 | element);
        multiplicity = analyzeValueMultiplicity(group, element, vrType, value);
    }
    
    public VR(String elementName, byte[] valueBytes) {
        // Look up the implicit VR type
        DicomDict dict = DicomDict.getDictionary();
        int id = dict.getVrId(elementName);
        group = (id >>> 16) & 0xFFFF;
        element = id & 0xFFFF;
        vrType = dict.getVrType(id);
        length = valueBytes.length;
        value = valueBytes;
        multiplicity = analyzeValueMultiplicity(group, element, vrType, value);
    }
    
    public VR(int valueGroup, int valueElement, int valueType, Object[] values) {
        group = valueGroup;
        element = valueElement;
        vrType = valueType;
        multiplicity = values.length;
        int vrRep = representationTypeFromId(vrType);
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(64);
            for (int i=0; i<values.length; i++) {
                VrValue vrv = (VrValue)values[i];
                os.write(vrv.getVrValue(this.vrType));
                if (vrRep < 0 && i < (values.length - 1)) { // string rep and not last value
                    os.write('\\'); // add the separator character
                }
            }
            if (os.size() % 2 == 1) os.write(0);
            
            value = os.toByteArray();
            length = value.length;
        } catch(IOException e) {
            throw new RuntimeException("Failed to build VR");
        }
    }
    
    public VR(int valueGroup, int valueElement, Object[] values) {
        group = valueGroup;
        element = valueElement;
        
        // Look up the implicit VR type
        DicomDict dict = DicomDict.getDictionary();
        vrType = dict.getVrType((group & 0xFFFF) << 16 | (element & 0xFFFF));
        
        multiplicity = values.length;
        int vrRep = representationTypeFromId(vrType);
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(64);
            for (int i=0; i<values.length; i++) {
                VrValue vrv = (VrValue)values[i];
                os.write(vrv.getVrValue(this.vrType));
                if (vrRep < 0 && i < (values.length - 1)) { // string rep and not last value
                    os.write('\\'); // add the separator character
                }
            }
            if (os.size() % 2 == 1) os.write(0);
            
            value = os.toByteArray();
            length = value.length;
        } catch(IOException e) {
            throw new RuntimeException("Failed to build VR");
        }
    }
    
    public VR(String elementName, Object[] values) {
        // Look up the implicit VR type
        DicomDict dict = DicomDict.getDictionary();
        int id = dict.getVrId(elementName);
        group = (id >>> 16) & 0xFFFF;
        element = id & 0xFFFF;
        vrType = dict.getVrType(id);
        multiplicity = values.length;
        int vrRep = representationTypeFromId(vrType);
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(64);
            for (int i=0; i<values.length; i++) {
                VrValue vrv = (VrValue)values[i];
                os.write(vrv.getVrValue(this.vrType));
                if (vrRep < 0 && i < (values.length - 1)) { // string rep and not last value
                    os.write('\\'); // add the separator character
                }
            }
            if (os.size() % 2 == 1) os.write(0);
            
            value = os.toByteArray();
            length = value.length;
        } catch(IOException e) {
            throw new RuntimeException("Failed to build VR");
        }
    }
    
    public VR(String elementName, Object val) {
        // Look up the implicit VR type
        DicomDict dict = DicomDict.getDictionary();
        int id = dict.getVrId(elementName);
        group = (id >>> 16) & 0xFFFF;
        element = id & 0xFFFF;
        vrType = dict.getVrType(id);
        multiplicity = 1;
        int vrRep = representationTypeFromId(vrType);
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(64);
            VrValue vrv = (VrValue)val;
            os.write(vrv.getVrValue(this.vrType));
            if (os.size() % 2 == 1) os.write(0);
            
            value = os.toByteArray();
            length = value.length;
        } catch(IOException e) {
            throw new RuntimeException("Failed to build VR");
        }
    }
    
    public String getName() {
        DicomDict dict = DicomDict.getDictionary();
        return dict.getVrName(group << 16 | element);
    }
    
    public int getGroup() { return group; }
    public void setGroup(int g) { group = g; }
    
    public int getElement() { return element; }
    public void setElement(int e) { element = e; }
    
    public int getId() { return (group & 0xFFFF) << 16 | (element & 0xFFFF); }
    
    public int getType() { return vrType; }
    public void setType(int t) { vrType = t; }
    
    public String getTypeString() { return stringFromId(vrType); }
    
    public int getValueMultiplicity() { return multiplicity; }
    
    public long getLength() { return length; }
    
    public byte[] getValueBytes() { return value; }
    public void setValueBytes(byte[] v) { value = v; length = v.length;}
    
    public int getIntValue() {
        return ((DicomNumber)getValue()).getIntValue();
    }
    
    public int getIntValue(int i) {
        return ((DicomNumber)getValue(i)).getIntValue();
    }
    
    public float getFloatValue() {
        return ((DicomNumber)getValue()).getFloatValue();
    }
    
    public float getFloatValue(int i) {
        return ((DicomNumber)getValue(i)).getFloatValue();
    }
    
    public double getDoubleValue() {
        return ((DicomNumber)getValue()).getDoubleValue();
    }
    
    public double getDoubleValue(int i) {
        return ((DicomNumber)getValue(i)).getDoubleValue();
    }
    
    public String getStringValue() {
        return ((DicomString)getValue()).getValue();
    }
    
    public String getStringValue(int i) {
        return ((DicomString)getValue(i)).getValue();
    }
    
    public Date getDateValue() {
        return ((DicomDate)getValue()).getDate();
    }
    
    public Date getDateValue(int i) {
        return ((DicomDate)getValue(i)).getDate();
    }
    
    public Object getValue() {
        return getValue(0);
    }
    
    public Object getValue(int i)
    
    {
        VrValue vrval = null;
        
        try {
            Class vrclass = (Class)valueTypeMap.get(new Integer(this.vrType));
            vrval = (VrValue)vrclass.newInstance();
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Unimplemented type ID: " + this.vrType);
        }
        
        vrval.setVrValue(this.vrType, getIndexedBytes(i));
        
        return vrval;
    }
    
    // Hack for sequence items
    public void addSequenceItem(DicomObject item) {
        if (seqItems == null) {
            seqItems = new Vector();
        }
        seqItems.add(item);
    }
    
    public Iterator getSequenceItems() {
        if (seqItems == null) {
            return null;
        } else {
            return seqItems.iterator();
        }
    }
    
    public int getSequenceLength() {
        if (seqItems == null) {
            return 0;
        } else {
            return seqItems.size();
        }
    }
    
    // Hack for multi-frame, encapsulated image data
    public void addImageFrame(byte[] frameData) {
        if (imageFrames == null) {
            imageFrames = new ArrayList();
        }
        imageFrames.add(frameData);
    }
    
    public List getImageFrames() {
        return imageFrames;
    }
    
    private Float bytesToFloat(byte[] val) {
        int i = val[3] << 24 | val[2] << 16 | val[1] | val[0];
        Float result = new Float(Float.intBitsToFloat(i));
        return result;
    }
    
    
    public final static int AE=0;
    public final static int AS=1;
    public final static int AT=2;
    public final static int CS=3;
    public final static int DA=4;
    public final static int DS=5;
    public final static int DT=6;
    public final static int FL=7;
    public final static int FD=8;
    public final static int IS=9;
    public final static int LO=10;
    public final static int LT=11;
    public final static int OB=12;
    public final static int OW=13;
    public final static int PN=14;
    public final static int SH=15;
    public final static int SL=16;
    public final static int SQ=17;
    public final static int SS=18;
    public final static int ST=19;
    public final static int TM=20;
    public final static int UI=21;
    public final static int UL=22;
    public final static int UN=23;
    public final static int US=24;
    public final static int UT=25;
    
    private final static String vrCodes[] = {
        "AE", "AS", "AT", "CS", "DA",
                "DS", "DT", "FL", "FD", "IS",
                "LO", "LT", "OB", "OW", "PN",
                "SH", "SL", "SQ", "SS", "ST",
                "TM", "UI", "UL", "UN", "US", "UT"
    };
    
    static int idFromString(String twoLetterName) {
        for (int i=0; i<vrCodes.length; i++) {
            if (twoLetterName.compareTo(vrCodes[i]) == 0) {
                return i;
            }
        }
        
        if (twoLetterName.compareTo("up") == 0) {
            return VR.UL;
        } else if (twoLetterName.compareTo("ox") == 0) {
            return VR.OW;
        } else if (twoLetterName.compareTo("xs") == 0) {
            return VR.US;
        } else if (twoLetterName.compareTo("na") == 0) {
            return VR.UN;
        }
        
        logger.info("Unknown VR code: " + twoLetterName);
        
        return VR.UN;
    }
    
    public byte[] getTypeBytes() {
        return StringConvert.stringToBytes(VR.stringFromId(this.vrType));
    }
    
    static String stringFromId(int id) {
        switch(id) {
            case AE:
                return "AE";
            case AS:
                return "AS";
            case AT:
                return "AT";
            case CS:
                return "CS";
            case DA:
                return "DA";
            case DS:
                return "DS";
            case DT:
                return "DT";
            case FL:
                return "FL";
            case FD:
                return "FD";
            case IS:
                return "IS";
            case LO:
                return "LO";
            case LT:
                return "LT";
            case OB:
                return "OB";
            case OW:
                return "OW";
            case PN:
                return "PN";
            case SH:
                return "SH";
            case SL:
                return "SL";
            case SQ:
                return "SQ";
            case SS:
                return "SS";
            case ST:
                return "ST";
            case TM:
                return "TM";
            case UI:
                return "UI";
            case UL:
                return "UL";
            case US:
                return "US";
            case UT:
                return "UT";
            case UN:
                return "UN";
            default:
                return null;
        }
    }
    
    // A positive return value indicates that the
    // type is represented by that fixed number of bytes.
    // A negative value indicates that the value is
    // represented by a string with max length = ABS(returned value).
    // A return of -1 indicates that VRs of this type never have
    // multiple values.
    // A return of 0 indicates that Vrs of this type never have
    // multiple values.
    private int representationTypeFromId(int id) {
        switch(id) {
            case AE:
                return -16;
            case AS:
                return -4;
            case AT:
                return 4;
            case CS:
                return -16;
            case DA:
                return -10;
            case DS:
                return -16;
            case DT:
                return -26;
            case FL:
                return 4;
            case FD:
                return 8;
            case IS:
                return -12;
            case LO:
                return -64;
            case LT:
                return -1;
            case OB:
                return 0;
            case OW:
                return 0;
            case PN:
                return -64;
            case SH:
                return -16;
            case SL:
                return 4;
            case SQ:
                return 0;
            case SS:
                return 2;
            case ST:
                return -1;
            case TM:
                return -16;
            case UI:
                return -64;
            case UL:
                return 4;
            case US:
                return 2;
            case UT:
                return -1;
            case UN:
                return 0;
            default:
                return 0;
        }
    }
    
    private int analyzeValueMultiplicity(int group, int element, int vrType, byte[] value) {
        int repType = representationTypeFromId(vrType);
        
        if (repType == 0 || repType == -1) {
            return 1;
        } else if (repType < 0) { // is a string representation
            String valStr;
            try {
                valStr = new String(value, "8859_1");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unknown encoding.");
            }
            
            StringTokenizer tok = new StringTokenizer(valStr);
            int m = 0;
            try {
                while (tok.nextToken("\\").length() > 0) {
                    m++;
                }
            } catch(NoSuchElementException e2) {
            }
            
            return m;
        } else { //if (repType > 0) : is a fixed binary representation
            return value.length / repType;
        }
        
    }
    
    public int compareTo(Object obj) {
        if (obj instanceof VR) {
            VR that = (VR)obj;
            int  groupdiff = this.getGroup() - that.getGroup();
            int elemdiff = this.getElement() - that.getElement();
            if (groupdiff != 0) {
                return groupdiff;
            } else {
                return elemdiff;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    private byte[] getIndexedBytes(int i) {
        int typeRep = representationTypeFromId(this.vrType);
        if (typeRep >=0) {
            int size = typeRep; // represents fixed number bytes, 0 == no mult values
            return getIndexedBytesAsBinary(i, size);
        } else {
            int size = -typeRep; // represents a maximum size for strings, 1 == no mult values
            return getIndexedBytesAsString(i, size);
        }
    }
    
    private byte[] getIndexedBytesAsString(int i, int size) {
        try {
            
            String source = new String(this.value, "8859_1");
            if (source.length() > 2 && size > 1) {
                StringTokenizer tok = new StringTokenizer(source);
                String result = "";
                
                for (int index=0; index<=i; index++) {
                    result = tok.nextToken("\\");
                }
                
                return result.getBytes("8859_1");
            } else {
                return this.value;
            }
        } catch(UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown Encoding.");
        }
    }
    
    private byte[] getIndexedBytesAsBinary(int i, int size) {
        if (size > 0) {
            byte[] result = new byte[size];
            System.arraycopy(this.value, i*size, result, 0, size);
            return result;
        } else { // no multiple values, return the whole value
            return this.value;
        }
    }
    
    public String toString() {
        int i;
        StringBuffer buf = new StringBuffer();
        PrintfFormat hex = new PrintfFormat("%04x");
        PrintfFormat cwidth = new PrintfFormat("%4d");
        
        buf.append("VR: ");
        buf.append("Tag:[");
        buf.append(hex.sprintf(this.getGroup()));
        buf.append(", ");
        buf.append(hex.sprintf(this.getElement()));
        buf.append("] Type: ");
        buf.append(this.getTypeString());
        buf.append(" Len: ");
        buf.append(cwidth.sprintf(this.getLength()));
        buf.append(" VM: ");
        buf.append(cwidth.sprintf(this.getValueMultiplicity()));
        buf.append(" Name: ");
        buf.append(this.getName());
        buf.append(" = ");
        for (int j=0; j<this.getValueMultiplicity(); j++) {
            buf.append("[");
            try {
                buf.append(this.getValue(j));
            } catch (DicomRuntimeException e) {
                buf.append("!!! Parsing error: " + e);
            }
            buf.append("]");
        }
        
        if (getTypeString().compareTo("SQ") == 0) {
            Iterator items = getSequenceItems();
            int itemCount=0;
            while (items != null && items.hasNext()) {
                DicomObject itemval = (DicomObject)items.next();
                buf.append("\n   Item[" + itemCount++ + "]:");
                Iterator itemvaltags = itemval.iterator();
                while(itemvaltags.hasNext()) {
                    VR tag = (VR)itemvaltags.next();
                    buf.append("\n   ");
                    buf.append(tag);
                }
            }
        } else if (imageFrames != null) {
            buf.append("\n");
            for (int frame=0; frame<imageFrames.size(); frame++) {
                buf.append("   Frame[" + frame + "] length=" + ((byte[])imageFrames.get(frame)).length + "\n");
            }
        }
        
        return buf.toString();
    }
    
}
