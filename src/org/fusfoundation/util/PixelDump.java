/*
 * PixelDump.java
 *
 * Created on November 18, 2002, 1:06 PM
 */

package org.fusfoundation.util;

import org.fusfoundation.dicom.part10.DicomFileReader;
import org.fusfoundation.dicom.DicomObjectReader;
import org.fusfoundation.dicom.DicomObject;
import java.io.*;
import javax.swing.JFileChooser;

/**
 *
 * @author  jsnell
 */
public class PixelDump {
    
    /** Creates a new instance of PixelDump */
    public PixelDump() {
    }
    
    public static void main(String[] argv) {
        DicomObject image;
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Source DICOM Image");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);//JFileChooser.DIRECTORIES_ONLY);
            fileChooser.showOpenDialog(null);
            
            
            DicomObjectReader dor = new DicomObjectReader(new DicomFileReader(fileChooser.getSelectedFile()));
            
            image = dor.read();
            
            System.out.println(image);
            
            fileChooser.setDialogTitle("Select Destination Image");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);//JFileChooser.DIRECTORIES_ONLY);
            fileChooser.showSaveDialog(null);
            System.out.println("Saving to: " + fileChooser.getSelectedFile());
            
            FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile());
            try {
                fos.write((byte[])image.getVR("PixelData").getImageFrames().get(0));
            }
            catch(NullPointerException e) {
                fos.write((byte[])image.getVR("PixelData").getValueBytes());
            }
            fos.close();
            
        }
        catch(Exception e) {
            e.printStackTrace();
            return;
        }
        
        System.out.println(image);
        
        System.exit(1);
        
    }
    
}
