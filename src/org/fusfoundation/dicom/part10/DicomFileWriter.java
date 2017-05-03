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
import org.fusfoundation.dicom.VrOutputStream;
import org.fusfoundation.dicom.VrWriter;
import org.fusfoundation.dicom.DicomObjectWriter;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomNumber;
import org.fusfoundation.dicom.DicomUnknown;
import java.io.*;
import java.util.*;

/**
 *
 * @author  jsnell
 */
public class DicomFileWriter implements VrWriter {
    
    private FileOutputStream fos;
    private VrOutputStream fileVRStream;

    /** Creates a new instance of DicomFileWriter */
    public DicomFileWriter(File file, String SopClass, String Instance) throws IOException, FileNotFoundException {
        fos = new FileOutputStream(file);
        
        // write the preamble
        for (int i=0; i<128; i++) {
            fos.write(0);
        }
        fos.write('D');
        fos.write('I');
        fos.write('C');
        fos.write('M');
        
        // write the meta information
        DicomObject metainf = new DicomObject();
        byte[] verBuf = new byte[2];
        verBuf[0] = 0x00;
        verBuf[1] = 0x01;
        metainf.addVR(new VR("FileMetaInformationVersion", new DicomUnknown(verBuf)));
        metainf.addVR(new VR("MediaStorageSOPClassUID", new DicomString(SopClass)));
        metainf.addVR(new VR("MediaStorageSOPInstanceUID", new DicomString(Instance)));
        metainf.addVR(new VR("TransferSyntaxUID", new DicomString(UID.ExplicitVRLittleEndian.toString())));
        metainf.addVR(new VR("ImplementationClassUID", new DicomString("1.2.4.0.1.2.3.4.5.6")));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        VrOutputStream vros = new VrOutputStream(bos, true, false); // Little Endian, Explicit VR
        Iterator iter = metainf.iterator();
        while (iter.hasNext()) {
            vros.writeVR((VR)iter.next());
        }
        VR groupTag = new VR("MetaElementGroupLength", new DicomNumber(bos.size()));
        
        fileVRStream = new VrOutputStream(fos, true, false);
        fileVRStream.writeVR(groupTag);
        fos.write(bos.toByteArray());       
    }
    
    public void close() throws IOException { fos.close(); }

    public void writeVR(VR vr) throws IOException {
        fileVRStream.writeVR(vr);
    }
    
    public void write(DicomObject obj) throws IOException {
        DicomObjectWriter dow = new DicomObjectWriter(fileVRStream);
        dow.write(obj);
        close();
    }
    
}
