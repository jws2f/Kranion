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

import java.io.*;
import java.util.*;

/**
 *
 * @author  jsnell
 */
public class VrInputStream implements VrReader {
    
    private boolean isLE; // little-endian?
    private boolean isImplicit; // implicit VR syntax?
    private boolean readPixelData = true; // should we read the pixeldata?
    private InputStream is; // source input stream
    private long position; // current position in the stream
    
    // Inner class to keep track of closing
    // SQ and SQ Item tags
    private class TagMarker {
        static final int SQ_END = 1;
        static final int SQ_ITEM_END = 2;
        
        TagMarker(long pos, int type) {
            position = pos;
            markerType = type;
        }
        
        long    position;
        int     markerType;
    }
    
    private Stack markerStack;
    
    private final DicomDict dict = DicomDict.getDictionary();
    
    private int readUInt16() throws IOException {
        int lo, hi;
        
        if (isLE) {
            lo = is.read();
            hi = is.read();
        }
        else {
            hi = is.read();
            lo = is.read();
        }
        
        if (lo == -1 || hi == -1) {
            throw new EOFException();
        }
        
        position += 2;
        
        return ((hi & 0xff)  << 8) | (lo & 0xff);
    }
    
    private long readUInt32() throws IOException {
        int b1, b2, b3, b4;
        
        if (isLE) {
            b1 = is.read();
            b2 = is.read();
            b3 = is.read();
            b4 = is.read();
        }
        else {
            b4 = is.read();
            b3 = is.read();
            b2 = is.read();
            b1 = is.read();
        }
        
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == 01) {
            throw new EOFException();
        }
        
        position += 4;
        
        return ((b4 & 0xff) << 24) | ((b3 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b1 & 0xff);
    }
    
    /** Creates a new instance of VrInputStream */
    public VrInputStream() {
        isLE = true;
        isImplicit = true;
        position = 0;
        markerStack = new Stack();
    }
    
    public VrInputStream(InputStream source) {
        isLE = true;
        isImplicit = true;
        is = source;
        position = 0;
        markerStack = new Stack();
    }
    
    public VrInputStream(InputStream source, boolean littleEndian, boolean implicit) {
        isLE = littleEndian;
        isImplicit = implicit;
        is = source;
        position =  0;
        markerStack = new Stack();
    }
    
    public boolean getIsLittleEndian() { return isLE; }
    public void setIsLittleEndian(boolean val) { isLE = val; }
    
    public boolean getIsImplicitVR() { return isImplicit; }
    public void setIsImplicitVR(boolean val) { isImplicit = val; }
    
    public boolean getReadPixelData() { return readPixelData; }
    public void setReadPixelData(boolean val) { readPixelData = val; }
    
    public long getPosition() { return position; }
    
    public int available() throws IOException { return is.available(); }
    
    public InputStream getSource() { return is; }
    public void setSource(InputStream val) { is = val; markerStack = new Stack(); position = 0;}
    
    private void readValue(byte[] buf, InputStream is, int vrType) throws IOException {
        // Read in the bytes first
        DataInputStream dis = new DataInputStream(is);
        /*
        int toread = buf.length;
        int offset = 0;
        int result=0;
        while (toread > 0) {
            result = dis.read(buf, offset, toread);
            if (result < 0) throw new IOException("EOF");
            offset += result;
            toread -= result;
            try {
             if (toread > 0) {
               Thread.sleep(0,1);
             }
            }
            catch(InterruptedException e) {}
        }
         */
        dis.readFully(buf);
        position += buf.length;
        
        // If the stream is Little-Endian, swab appropriately by type
        // (Java is always Big-Endian)
        if (isLE) {
            if (vrType == VR.UL || vrType == VR.SL || vrType == VR.FL) {
                swab(buf, 4);
            }
            else if (vrType == VR.US || vrType == VR.SS || vrType == VR.OW || vrType == VR.AT) {
                swab(buf, 2);
            }
            else if (vrType == VR.FD) {
                swab(buf, 8);
            }
        }
    }
    
    // Probably not the fastest method, but
    // works simply for now.
    private byte[] swabbuf = new byte[8];
    private void swab(byte[] buf, int radix) {
        if (buf.length < radix) return;
        
        byte[] tmp;
        if (radix <= 8) {
            tmp = swabbuf;
        }
        else {
            tmp = new byte[radix];
        }
        
        for (int i=0; i<buf.length; i+=radix) {
            for (int j=0; j<radix; j++) {
                tmp[j] = buf[i+radix-1-j]; // reverse bytes into tmp
            }
            //System.arraycopy(tmp, 0, buf, i, radix); // replace bytes
            for (int j=0; j<radix; j++) {
                buf[i+j] = tmp[j];
            }
        }
    }
    
    private void setMarker(long pos, int type) {
        markerStack.push(new TagMarker(pos, type));
    }
    
    private int checkMarker(long pos) {
        try {
            TagMarker tm = (TagMarker)markerStack.peek();
            if (tm.position == pos) {
                markerStack.pop();
                return tm.markerType;
            }
            else {
                return 0;
            }
        }
        catch (EmptyStackException e) {
            return 0;
        }
    }
    
    private void readSequenceItems(VR parent) throws IOException, DicomException {
        DicomObject item;
        while((item = readSequenceItem()) != null) {
            parent.addSequenceItem(item);
        }
    }
    
    private DicomObject readSequenceItem() throws IOException, DicomException {
        // pass true so that reading pixel data in seq items works out correctly.
        // Callers can skip reading the main pixel data tag at the end of the object,
        // but we can't skip reading pixel data in sequence items which appear if
        // the object has icon sets, etc.
        VR vr = readVR(true); 
        if (isSequenceEnd(vr)) {
            return null;
        }
        else if (!isItemStart(vr)) {
            throw new DicomException("Malformed DICOM sequence.");
        }
        else {
            DicomObject item = new DicomObject();
            VR itemTag = readVR(true);
            while (itemTag != null && !isItemEnd(itemTag)) {
                item.addVR(itemTag);
                itemTag = readVR(true);
            }
            return item;
        }
        
    }
    
    private boolean isItemStart(VR vr) {
        if (vr != null && vr.getGroup() == 0xFFFE && vr.getElement() == 0xE000) {
            return true;
        }
        else {
            return false;
        }
    }
    
    private boolean isItemEnd(VR vr) {
        if (vr == null) {
            return true;
        }
        else if (vr.getGroup() == 0xFFFE && vr.getElement() == 0xE00D) {
            return true;
        }
        else {
            return false;
        }
    }
    
    private boolean isSequenceEnd(VR vr) {
        if (vr != null && vr.getGroup() == 0xFFFE && vr.getElement() == 0xE0DD) {
            return true;
        }
        else {
            return false;
        }
    }
        
    public VR readVR() throws IOException {
        return readVR(false);
    }
    
    public VR readVR(boolean isSequenceItem) throws IOException {
        int grp, elem, vrType;
        long len;
        int t;
        byte[] valBuf;
        long tagStartPosition = position;
        
        // First check to see if we need to emit a delimitation
        // itme for an open sequence or sequence item. We do this
        // because we have chosen to flatten all sequences and items
        // to the undefined length form with delimitation items.
        // This allows downstream VR parsing to use one method in
        // order to process sequence VR's.
        int delimType = checkMarker(position);
        if (delimType == TagMarker.SQ_END) {
            return new VR(0xFFFE, 0xE0DD, new byte[0]);
        }
        else if (delimType == TagMarker.SQ_ITEM_END) {
            return new VR(0xFFFE, 0xE00D, new byte[0]);
        }
        
        try {
            grp = readUInt16();
            elem = readUInt16();
                                                
            // Sometimes we just want to scan the image tags without
            // reading the pixel data. Except if this pixel data tag is part
            // of a sequence like icon sets that aren't the actual image data
            if (readPixelData==false && grp == 0x7fe0 && !isSequenceItem) {
                is.close();
                throw new EOFException();
            }

            // Sequence Item tags are always Implicit VR
            if (grp == 0xFFFE) {
                len = readUInt32();
                
                tagStartPosition = position;
                
                // SQ Item of defined length
                if (elem == 0xE000 && len != 0xFFFFFFFF) {
                    setMarker(tagStartPosition + len, TagMarker.SQ_ITEM_END);
                    len = 0;
                }
                // SQ Item of undefined length
                else if (elem == 0xE000 && len == 0xFFFFFFFF) {
                    len = 0;
                }
                // SQ Item Delimitation
                else if (elem == 0xE00D) {
                    len = 0;
                }
                // SQ Delimitation
                else if (elem == 0xE0DD) {
                    len = 0;
                }
                
                return new VR(grp, elem, new byte[0]);
            }
            
            if (isImplicit) { // Implicit VR
                vrType = dict.getVrType((grp & 0xFFFF) << 16 | (elem & 0xFFFF));

                len = readUInt32();
                
                if (len == 0xFFFFFFFF) {
                    vrType = VR.SQ;
                }
                
                tagStartPosition = position;
                
                // NEED SPECIAL HANDLING FOR SQ
                // SQ of defined length
                if (vrType == VR.SQ && len != 0xFFFFFFFF) {
                    setMarker(tagStartPosition + len, TagMarker.SQ_END);
                    len = 0;
                }
                // SQ of undefined length
                else if (vrType == VR.SQ && len == 0xFFFFFFFF) {
                    len = 0;
                }
                
                valBuf = new byte[(int)len];
                readValue(valBuf, is, vrType);
                
                VR result = new VR(grp, elem, valBuf);
                
                if (vrType == VR.SQ) {
                    readSequenceItems(result);
                }
                // Handle encapsulated image data specifically
                else if (grp == 0x7fe0 && elem == 0x0010 && len == 0xffffffff) {
                    int tagGrp, tagElem;
                    long valLen;
                    byte[] value;
                    int count=0;
                    
                    do {
                        tagGrp = readUInt16();
                        tagElem = readUInt16();
                        valLen = readUInt32();
                        value = new byte[(int)valLen];
                        read(value);
                        //System.out.println("Frame " + count++ + ", len = " + value.length);
                        if (count>0 && tagElem!=0xE0DD) {
                            result.addImageFrame(value);
                        }
                        count++;
                        
                    } while (!(tagGrp == 0xFFFE && tagElem == 0xE0DD));
                }
                
                return result;
            }
            else { // Explicit VR
                // Read VR type
                byte[] vrtype = new byte[2];
                
                DataInputStream dis = new DataInputStream(is);
                dis.readFully(vrtype);
                position += 2;
                vrType = VR.idFromString(new String(vrtype, "8859_1"));
    if (vrType == VR.UT) {
        System.out.println("text field");
    }
                
                // 'length' field is conditional on vrtype:
                if (vrType == VR.OB || vrType == VR.OW || vrType == VR.SQ || vrType == VR.UN || vrType == VR.UT) {
                    readUInt16(); // Reserved
                    len = readUInt32();
                }
                else {
                    len = readUInt16();
                }
                
                tagStartPosition = position;
                
                // NEED SPECIAL HANDLING FOR SQ
                // SQ of defined length
                if (vrType == VR.SQ && len != 0xFFFFFFFF) {
                    setMarker(tagStartPosition + len, TagMarker.SQ_END);
                    len = 0;
                }
                // SQ of undefined length
                else if (vrType == VR.SQ && len == 0xFFFFFFFF) {
                    len = 0;
                }
                
                valBuf = new byte[(int)Math.max(0, len)];
                readValue(valBuf, is, vrType);
                
                VR result = new VR(grp, elem, vrType, valBuf);
                
                // Handle sequence items
                if (vrType == VR.SQ) {
                    readSequenceItems(result);
                }
                // Handle encapsulated image data specifically
                else if (grp == 0x7fe0 && elem == 0x0010 && len == 0xffffffff) {
                    int tagGrp, tagElem;
                    long valLen;
                    byte[] value;
                    int count=0;
                    
                    do {
                        tagGrp = readUInt16();
                        tagElem = readUInt16();
                        valLen = readUInt32();
                        value = new byte[(int)valLen];
                        read(value);
                        //System.out.println("Frame " + count++ + ", len = " + value.length);
                        if (count>0 && tagElem!=0xE0DD) {
                            result.addImageFrame(value);
                        }
                        count++;
                        
                    } while (!(tagGrp == 0xFFFE && tagElem == 0xE0DD));
                }
                
                return result;
            }
        }
        catch (DicomException e) {
            return null;
        }
        catch (EOFException e) {
            return null;
        }
    }
    
    public void close() throws IOException {
        is.close();
    }
    
    public int read(byte[] buf) throws IOException {
        int bytesRead = is.read(buf);
        position += bytesRead;
        return bytesRead;
    }
    
}
