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
public class VrOutputStream implements VrWriter {

    private boolean isLE; // little-endian?
    private boolean isImplicit; // implicit VR syntax?
    private OutputStream os; // source input stream
    private long position; // current position in the stream
        
    private final DicomDict dict = DicomDict.getDictionary();
    
    private void writeUInt16(int i) throws IOException
    {
        int lo, hi;
        
        lo = i & 0x00ff;
        hi = (i & 0xff00) >>> 8;
        
        try {
            if (isLE) {
                os.write(lo);
                os.write(hi);
            }
            else {
                os.write(hi);
                os.write(lo);
            }
            
            position += 2;
        }
        catch (IOException e)
        {
            throw e;
        }
    }
    
     private void writeUInt32(long i) throws IOException
    {
        int b1, b2, b3, b4;
        
        b1 = (int)(i & 0xff);
        b2 = (int)((i & 0xff00) >>> 8);
        b3 = (int)((i & 0xff0000) >>> 16);
        b4 = (int)((i & 0xff000000) >>> 24);
        
        try {
            if (isLE) {
                os.write(b1);
                os.write(b2);
                os.write(b3);
                os.write(b4);
            }
            else {
                os.write(b4);
                os.write(b3);
                os.write(b2);
                os.write(b1);
           }
            
           position += 4;
        }
        catch(IOException e)
        {
            throw e;
        }
     }
     
     public void write(char b) throws IOException {
         os.write((int)b);
     }
     
     public void write(byte b) throws IOException {
         os.write((int)b);
     }
     
     public void write(int b) throws IOException {
         os.write(b);
     }
     
     public void write(byte[] buf) throws IOException {
         os.write(buf);
     }
   
    /** Creates a new instance of VrInputStream */
    public VrOutputStream() {
        isLE = true;
        isImplicit = true;
        position = 0;
    }
    
    public VrOutputStream(OutputStream dest)
    {
        isLE = true;
        isImplicit = true;
        os = dest;
        position = 0;
     }
    
    public VrOutputStream(OutputStream dest, boolean littleEndian, boolean implicit)
    {
        isLE = littleEndian;
        isImplicit = implicit;
        os = dest;
        position =  0;
    }
    
    public boolean getIsLittleEndian() { return isLE; }
    public void setIsLittleEndian(boolean val) { isLE = val; }
    
    public boolean getIsImplicitVR() { return isImplicit; }
    public void setIsImplicitVR(boolean val) { isImplicit = val; }
    
    public long getPosition() { return position; }
        
    public OutputStream getDest() { return os; }
    public void setDest(OutputStream val) { os = val; position = 0;}
    
    private void writeValue(byte[] buf, OutputStream os, int vrType) throws IOException
    {
        // default is to just write the original buffer. however, if
        // the byte-ordering must be changed, then we need to make a
        // defensive copy so we don't corrupt the data.

        byte[] tmp = buf;
        
        if (tmp.length % 2 != 0) {
            throw new DicomRuntimeException("Value length must be an even number of bytes.");
        }
        
        // If the stream is Little-Endian, swab appropriately by type
        // (Java is always Big-Endian)
        if (isLE) {
            if (vrType == VR.UL || vrType == VR.SL || vrType == VR.FL) {
                // make a defensive copy
                tmp = (byte[])buf.clone();
                swab(tmp, 4);
            }
            else if (vrType == VR.US || vrType == VR.SS || vrType == VR.AT) {
                // make a defensive copy
                tmp = (byte[])buf.clone();
                swab(tmp, 2);
            }
            else if (vrType == VR.FD) {
                // make a defensive copy
                tmp = (byte[])buf.clone();
                swab(tmp, 8);
            }
            // special case here for OW, since these values
            // will potentially be large. swab and write in
            // chunks of manageable size, and without side-
            // effects for the original value buffer.
            else if (vrType == VR.OW) {
                // this does not make a copy of buf
                ByteArrayInputStream bis = new ByteArrayInputStream(buf);
                
                // do 4K chunks for now
                byte[] chunk = new byte[4096];
                while (bis.available() > 0) {
                    // read into chunk buffer. really does an
                    // ArrayCopy, so we won't harm original data
                    // when we swab()
                    int bytesRead = bis.read(chunk);
                    swab(chunk, 2);
                    os.write(chunk, 0, bytesRead);
                    position += bytesRead;
                }
                return;
            }
        }
        
        os.write(tmp);
        position += tmp.length;

    }
    
    // Probably not the fastest method, but
    // works simply for now.
    private byte[] swabbuf = new byte[8];
    private void swab(byte[] buf, int radix)
    {
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
            
    public void writeVR(VR vr) throws IOException
    {        
        int grp = vr.getGroup();
        int elem = vr.getElement();
        int vrType = vr.getType();
        long len = vr.getLength();
         
        writeUInt16(grp);
        writeUInt16(elem);
        
        // Sequence Item tags are always Implicit VR
        // and we always write the undefined length
        // style of SQ and SQ Item encoding, so make
        // sure you always write delimitation tags
        // for SQ and SQ Item tags
        if (grp == 0xFFFE) {
            // SQ Item of defined length
            if (elem == 0xE000 && len != 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
            // SQ Item of undefined length
            else if (elem == 0xE000 && len == 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
            // SQ Item Delimitation
            else if (elem == 0xE00D) {
                len = 0;
            }
            // SQ Delimitation
            else if (elem == 0xE0DD) {
                len = 0;
            }
            
            writeUInt32(len);
            
            return;
        }
         
        if (isImplicit) { // Implicit VR

            // NEED SPECIAL HANDLING FOR SQ
            // SQ of defined length
            if (vrType == VR.SQ && len != 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
             // SQ of undefined length
            else if (vrType == VR.SQ && len == 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
            
            // NEED SPECIAL HANDLING FOR ENCAPSULATED MULTI-FRAME IMAGES
            if (grp==0x7fe0 && elem==0x0010 && vr.getImageFrames() != null) {
                len = 0xFFFFFFFF;
            }
            writeUInt32(len);
            
            writeValue(vr.getValueBytes(), os, vrType);
            
            // if this is a SQ vr, write SQ items out if any
            if (vrType == VR.SQ) {
               Iterator i = vr.getSequenceItems();
               while(i.hasNext()) {
                  // write item start tag
                  writeVR(new VR(0xFFFE, 0xE000, new byte[0]));
                  
                  // write item
                  DicomObject item = (DicomObject)i.next();
                  Iterator j = item.iterator();
                  while(j.hasNext()) {
                     writeVR((VR)j.next());
                  }
                                  
                  //write item start tag
                  writeVR(new VR(0xFFFE, 0xE00D, new byte[0]));
                  
               }
               // write sequence end tag
               writeVR(new VR(0xFFFE, 0xE0DD, new byte[0]));
            }
            // Write out the image frames if this is a multiframe image
            else if (grp == 0x7fe0 && elem == 0x0010 && vr.getImageFrames() != null) {
                List frames = vr.getImageFrames();
                writeUInt16(0xFFFE);
                writeUInt16(0xE000);
                writeUInt32(frames.size() * 4);
                int offset = 0;
                for (int frame=0; frame < frames.size(); frame++) {
                    byte[] frameData = (byte[])frames.get(frame);
                    writeUInt32(offset);
                    offset += frameData.length + 8;
                }
                for (int frame=0; frame < frames.size(); frame++) {
                    byte[] frameData = (byte[])frames.get(frame);
                    writeUInt16(0xFFFE);
                    writeUInt16(0xE000);
                    writeUInt32(frameData.length);
                    write(frameData);
                }
                writeUInt16(0xFFFE);
                writeUInt16(0xE0DD);
                writeUInt32(0);               
            }

            return;
        }
        else { // Explicit VR
            // Write VR type
            os.write(vr.getTypeBytes());
            position += 2;
            
            // NEED SPECIAL HANDLING FOR SQ
            // SQ of defined length
            if (vrType == VR.SQ && len != 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
             // SQ of undefined length
            else if (vrType == VR.SQ && len == 0xFFFFFFFF) {
                len = 0xFFFFFFFF;
            }
            
            // NEED SPECIAL HANDLING FOR ENCAPSULATED MULTI-FRAME IMAGES
            if (grp==0x7fe0 && elem==0x0010 && vr.getImageFrames() != null) {
                len = 0xFFFFFFFF;
            }
           
            // 'length' field is conditional on vrtype:
            if (vrType == VR.OB || vrType == VR.OW || vrType == VR.SQ || vrType == VR.UN) {
                writeUInt16(0); // Reserved
                writeUInt32(len);
            }
            else {
                writeUInt16((int)len);
            }
            
            writeValue(vr.getValueBytes(), os, vrType);
            
            // if this is a SQ vr, write SQ items out if any
            if (vrType == VR.SQ) {
               Iterator i = vr.getSequenceItems();
               while(i.hasNext()) {
                  // write item start tag
                  writeVR(new VR(0xFFFE, 0xE000, new byte[0]));
                  
                  // write item
                  DicomObject item = (DicomObject)i.next();
                  Iterator j = item.iterator();
                  while(j.hasNext()) {
                     writeVR((VR)j.next());
                  }
                  
                  //write item start tag
                  writeVR(new VR(0xFFFE, 0xE00D, new byte[0]));
                  
               }
               // write sequence end tag
               writeVR(new VR(0xFFFE, 0xE0DD, new byte[0]));
            }
            // Write out the image frames if this is a multiframe image
            else if (grp == 0x7fe0 && elem == 0x0010 && vr.getImageFrames() != null) {
                List frames = vr.getImageFrames();
                writeUInt16(0xFFFE);
                writeUInt16(0xE000);
                writeUInt32(frames.size() * 4);
                int offset = 0;
                for (int frame=0; frame < frames.size(); frame++) {
                    byte[] frameData = (byte[])frames.get(frame);
                    writeUInt32(offset);
                    offset += frameData.length + 8;
                }
                for (int frame=0; frame < frames.size(); frame++) {
                    byte[] frameData = (byte[])frames.get(frame);
                    writeUInt16(0xFFFE);
                    writeUInt16(0xE000);
                    writeUInt32(frameData.length);
                    write(frameData);
                }
                writeUInt16(0xFFFE);
                writeUInt16(0xE0DD);
                writeUInt32(0);               
            }
            
            return;
        }
        
    }
}
