/*
 * Anonymizer.java
 *
 * Created on December 20, 2004, 2:48 PM
 */

package org.fusfoundation.util;

import org.fusfoundation.dicom.part10.DicomFileReader;
import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.VrOutputStream;
import org.fusfoundation.dicom.DicomObjectReader;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomDate;
import org.fusfoundation.dicom.DicomNumber;
import java.io.*;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  jws2f
 */
public class Anonymizer {
    
    /** Creates a new instance of Anonymizer */
    public Anonymizer() {
    }
    
    public static void main(String[] argv) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Source DICOM Image");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//JFileChooser.DIRECTORIES_ONLY);
            fileChooser.showOpenDialog(null);
            
            
            parseDirectory(fileChooser.getSelectedFile());
            
            
            
        }
        catch(Exception e) {
            e.printStackTrace();
            return;
        }
        
        System.exit(1);
        
    }
    
    private static void parseDirectory(File dir) {
        File[] files = dir.listFiles();
        
        for (int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                parseDirectory(files[i]);
            }
            else if (!files[i].getPath().endsWith(".vol") && !files[i].getPath().endsWith(".DS_Store")/*files[i].getPath().endsWith(".dcm")*/) {
                System.out.println(files[i].getPath());
                
                try {
                    DicomObject image, metainfo;
                    DicomFileReader dfr = new DicomFileReader(files[i]);
                    DicomObjectReader dor = new DicomObjectReader(dfr);
                    
                    image = dor.read();
                    metainfo = dfr.getMetaInfo();
                    
                    dfr.close();
                    
                    File newFile = new File(files[i].getPath());
                    System.out.println(newFile.getPath());
                    
                    VrOutputStream os = new VrOutputStream(new FileOutputStream(newFile), true, false);
                    
                    // write the preamble
                    for (int j=0; j<128; j++) {
                        os.write(0);
                    }
                    os.write('D');
                    os.write('I');
                    os.write('C');
                    os.write('M');
                    
                    writeObject(os, metainfo);
                    writeObject(os, image);
                }
                catch(Exception e) {
                    System.out.println(e);
                    System.exit(1);
                }
                
            }
        }
        
        
    }
    
    private static void writeObject(VrOutputStream os, DicomObject obj) throws IOException {
        Iterator vrs = obj.iterator();
        while(vrs.hasNext()) {
            VR vr = (VR)vrs.next();
            
            if (vr.getGroup() == 0x10) {
                System.out.println(vr);
                
                if (vr.getType() == VR.DA) {
                    vr.setValueBytes(new DicomDate(new Date()).getVrValue(VR.DA));
                }
                else if (vr.getType() == VR.DS) {
                    vr.setValueBytes(new DicomNumber(0).getVrValue(VR.DS));
                }
                else {
                    byte[] val = vr.getValueBytes();
                    for (int i=0; i<val.length; i++) {
                        val[i] = 0;
                    }
                    vr.setValueBytes(val);
                }
            }
            
            os.writeVR(vr);
        }
    }
    
}
