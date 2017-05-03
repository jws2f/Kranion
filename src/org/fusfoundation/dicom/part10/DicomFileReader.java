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
package org.fusfoundation.dicom.part10;

import org.fusfoundation.dicom.DicomString;
import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.DicomObjectReader;
import org.fusfoundation.dicom.DicomException;
import org.fusfoundation.dicom.VrReader;
import org.fusfoundation.dicom.VrInputStream;
import org.fusfoundation.dicom.DicomObjectWriter;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomNumber;
import java.io.*;
import java.util.*;

/**
 *
 * @author  jsnell
 */
public class DicomFileReader implements VrReader {
    File inputFile;
    InputStream fis;
    VrInputStream vis;
    DicomObject metaInfo;
    
    /** Creates a new instance of DicomFileReader */
    public DicomFileReader(File file) throws DicomException, FileNotFoundException, IOException {
        inputFile = file;
        fis = new BufferedInputStream(new FileInputStream(file));
        
        fis.skip(128L);
        byte[] cookie = new byte[4];
        fis.read(cookie);
        if (cookie[0] != 'D' || cookie[1] != 'I' || cookie[2] != 'C' || cookie[3] != 'M') {
            fis.close();
            fis = new BufferedInputStream(new FileInputStream(file));
            vis = new VrInputStream(fis, true, true);
            metaInfo = new DicomObject();
            metaInfo.addVR(new VR("TransferSyntaxUID", new DicomString(UID.ImplicitVRLittleEndian.toString())));
            // throw new DicomException("Inavlid DICOM Part-10 file format: 'DICM' not found");
        } else {
            vis = new VrInputStream(fis, true, false); // meta information is Little Endian, Explicit VR
            VR metaGroupTag = vis.readVR();
            if(metaGroupTag.getGroup() != 2 || metaGroupTag.getElement() != 0) {
                throw new DicomException("Invalid Dicom Part-10 file format: must include (0002,0000)");
            }
            
            // Read and parse the rest of the meta information
            int grplen = ((DicomNumber)(metaGroupTag.getValue(0))).getIntValue();
            
            byte[] metabuf = new byte[grplen];
            fis.read(metabuf);
            ByteArrayInputStream bis = new ByteArrayInputStream(metabuf);
            VrInputStream mvis = new VrInputStream(bis, true, false); // little-endian, explicit
            metaInfo = new DicomObject();
            metaInfo.addVR(metaGroupTag);
            while (mvis.available() > 0) {
                VR v = mvis.readVR();
                metaInfo.addVR(v);
            }
            
            String v = metaInfo.getVR("TransferSyntaxUID").getStringValue();
            UID tsyn = new UID(v);
            if (tsyn.equals(UID.ImplicitVRLittleEndian)) {
                vis.setIsImplicitVR(true);
                vis.setIsLittleEndian(true);
            } else if (tsyn.equals(UID.ExplicitVRLittleEndian)) {
                vis.setIsImplicitVR(false);
                vis.setIsLittleEndian(true);
            } else if (tsyn.equals(UID.ExplicitVRBigEndian)) {
                vis.setIsImplicitVR(false);
                vis.setIsLittleEndian(false);
            } else { // Encapsulated transfer syntax, JPEG, RLE, etc.
                vis.setIsImplicitVR(false);
                vis.setIsLittleEndian(true);
            }
        }
    }
    
    public int available() throws IOException { return vis.available(); }
    public VrInputStream getVrInputStream() { return vis; }
    public VR readVR() throws IOException { return vis.readVR(); }
    public DicomObject getMetaInfo() { return metaInfo; }
    public void setReadPixelData(boolean value) {vis.setReadPixelData(value);}
    
    public static void main(String[] args) {
        // File file = new File("/home/jsnell/tmp/testfile");
        javax.swing.JFileChooser fileDlg = new javax.swing.JFileChooser();
        fileDlg.setMultiSelectionEnabled(true);
        fileDlg.showOpenDialog(null);
        File[] files = fileDlg.getSelectedFiles();
        
        for (int i=0; i<files.length; i++) {
            File file = files[i];
            try {
                DicomFileReader dfr = new DicomFileReader(file);
                
                if (args != null && args.length >= 1 && args[0].equals("--nopixeldata")) {
                    dfr.setReadPixelData(false);
                }
                
                if (args != null && args.length > 1 && args[0].equals("--filter")) {
                    DicomObjectReader objFileStream = new DicomObjectReader(dfr);
                    DicomObject imageInfo = objFileStream.read();
                    
                    for (int f=0; f<args.length-1; f++) {
                        System.out.print(/*args[f+1] + ": " + */ imageInfo.getVR(args[f+1]).getValue() + "\t");
                    }   System.out.println();
                    
                } else {
                    DicomObjectReader objFileStream = new DicomObjectReader(dfr);
                    DicomObject imageInfo = objFileStream.read();
                    
                    System.out.println("Meta Information:\n" + dfr.getMetaInfo());
                    System.out.println("Image Information:\n" + imageInfo);
                    
                    ByteArrayOutputStream os = new ByteArrayOutputStream(640000);
                    DicomObjectWriter oObjStm = new DicomObjectWriter(os, true, false);
                    oObjStm.write(imageInfo);
                    
                    ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
                    DicomObjectReader iObjStm = new DicomObjectReader(bis, true, false);
                    DicomObject memobj = iObjStm.read();
                    
                    System.out.println(memobj);
                }
                
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        
        System.exit(0);
        
    }
    
    public void close() throws IOException {
        if (vis != null) {
            vis.close();
        }
        if (fis != null) {
            fis.close();
        }
    }
    
}