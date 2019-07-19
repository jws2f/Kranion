/*
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.fusfoundation.dicom.DicomException;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomObjectReader;

/**
 *
 * @author jsnell
 */
public class DicomdirReaderTest {
    public static void main(String[] args) {
        // File file = new File("/home/jsnell/tmp/testfile");
        javax.swing.JFileChooser fileDlg = new javax.swing.JFileChooser();
        fileDlg.setMultiSelectionEnabled(false);
        fileDlg.showOpenDialog(null);
        File file = fileDlg.getSelectedFile();
        
        try {
            DicomFileReader dfr = new DicomFileReader(file);
            DicomObjectReader objFileStream = new DicomObjectReader(dfr);
            DicomObject dicomDir = objFileStream.read();
            
//            System.out.println(dicomDir);
            
            String fileSetID = dicomDir.getVR("FileSetID").getStringValue();
            System.out.println("FileSetID = " + fileSetID);
//            if (fileSetID.equalsIgnoreCase("STDSET")) {
                Iterator items = dicomDir.getVR("DirectoryRecordSequence").getSequenceItems();
                
//                DicomObject obj = (DicomObject)items.next();
//                System.out.println(obj.getVR("DirectoryRecordType").getStringValue());
                
                int patientCount = 0;
                int studyCount = 0;
                int seriesCount = 0;
                int imageCount = 0;
                while (items.hasNext()) {
                    DicomObject subobj = (DicomObject)items.next();
                    String recordType = subobj.getVR("DirectoryRecordType").getStringValue();
                    
                    switch(recordType) {
                        case "PATIENT":
                            studyCount = 0;
                            seriesCount = 0;
                            imageCount = 0;
                            System.out.println("PATIENT " + patientCount++ + ": " + subobj.getVR("PatientName").getValue());
                            break;
                        case "STUDY":
                            seriesCount = 0;
                            imageCount = 0;
                            System.out.println("   STUDY " + studyCount++ + ": " + subobj.getVR("StudyDate").getValue());
                            break;
                        case "SERIES":
                            imageCount = 0;
                            System.out.println("      SERIES " + seriesCount++ + ": " + subobj.getVR("SeriesDescription").getValue());
                            break;
                        case "IMAGE":
                        
                    }
                }
                
//            }
//            else {
//                System.out.println("Wrong fileSetID: " + fileSetID);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     
}
