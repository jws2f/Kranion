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
package org.fusfoundation.kranion.model.image.io;

import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.dicom.part10.DicomFileReader;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomException;
import org.fusfoundation.dicom.DicomObjectReader;
import org.fusfoundation.dicom.VR;
import com.pixelmed.codec.jpeg.OutputArrayOrStream;
import com.pixelmed.codec.jpeg.Parse;
import java.io.*;
import java.util.*;
import org.fusfoundation.kranion.model.image.*;
import org.fusfoundation.dicom.util.ljpeg.LJPEGDecoder;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author  jsnell
 */
public class DicomImageLoader implements ImageLoader {
    
    /** Creates a new instance of DicomImageLoader */
    public DicomImageLoader() {
    }
    
    private DicomObject openDicomFile(File file)
    {
        return openDicomFile(file, true);
    }
    
    private DicomObject openDicomFile(File file, boolean bReadPixelData) {
        DicomObjectReader dor = null;
        DicomObject obj = null;
        try {
            DicomFileReader dicomFileReader = new DicomFileReader(file);
            dicomFileReader.setReadPixelData(bReadPixelData);
            dor = new DicomObjectReader(dicomFileReader);
            obj = dor.read();
            
            // Add the transfer syntax
            VR vr = dicomFileReader.getMetaInfo().getVR("TransferSyntaxUID");
            if (vr != null) {
                obj.addVR(vr);
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (DicomException e) {
            return null;
        }

        return obj;
    }
    
    float[] getImageOrientationPatient(DicomObject obj) {
        float[] ImageOrientationPatient = new float[6];

        try {
            for (int index = 0; index < 6; index++) {
                ImageOrientationPatient[index] = obj.getVR("ImageOrientationPatient").getFloatValue(index);
                System.out.println("ImageOrientationPatinet[" + index + "] = " + ImageOrientationPatient[index]);
            }
        } catch (NullPointerException e) {
            ImageOrientationPatient[0] = 1f;
            ImageOrientationPatient[1] = 0f;
            ImageOrientationPatient[2] = 0f;

            ImageOrientationPatient[3] = 0f;
            ImageOrientationPatient[4] = 1f;
            ImageOrientationPatient[5] = 0f;
        }

        return ImageOrientationPatient;
    }
    
    float[] getImagePositionPatient(DicomObject obj) {
        float[] ImagePositionPatient = new float[3];
        try {
            for (int index = 0; index < 3; index++) {
                ImagePositionPatient[index] = obj.getVR("ImagePositionPatient").getFloatValue(index);
            }
        } catch (Exception e) {
            ImagePositionPatient[0] = 0f;
            ImagePositionPatient[1] = 0f;
            ImagePositionPatient[2] = 0f;
        }
        
        return ImagePositionPatient;
    }
    
    public ImageVolume load(File file, ProgressListener listener) {
        ImageVolume image = null;
       
                
        try {
            
            DicomObject selectedDicomObj = openDicomFile(file);
            String selectedSeriesUID = null;
            float[] ImageNormal; ImageNormal = new float[3];
            float[] ImageOrientationPatient; ImageOrientationPatient = new float[6];
            float[] ImagePositionPatient = new float[3];
            
            float sliceZ=0f;
            
            if (selectedDicomObj != null) {
System.out.println(selectedDicomObj);
                selectedSeriesUID = selectedDicomObj.getVR("SeriesInstanceUID").getStringValue();
                System.out.println("Selected series = " + selectedSeriesUID);
                
                
                ImageOrientationPatient = getImageOrientationPatient(selectedDicomObj);

                ImageNormal[0] = ImageOrientationPatient[1] * ImageOrientationPatient[5] - ImageOrientationPatient[2] * ImageOrientationPatient[4];
                ImageNormal[1] = ImageOrientationPatient[2] * ImageOrientationPatient[3] - ImageOrientationPatient[0] * ImageOrientationPatient[5];
                ImageNormal[2] = ImageOrientationPatient[0] * ImageOrientationPatient[4] - ImageOrientationPatient[1] * ImageOrientationPatient[3];
                
                ImagePositionPatient = getImagePositionPatient(selectedDicomObj);
                try {
                for (int index =0; index < 3; index++) {
                    System.out.println("ImagePositionPatient[" + index + "] = " + ImagePositionPatient[index]);
                    
                    sliceZ += ImagePositionPatient[index] * ImageNormal[index];
                }
                }
                catch(NullPointerException e) {
                    sliceZ = 0;
                }
                
                System.out.println("slice Z = " + sliceZ);
            }
            
            if (selectedDicomObj != null && selectedSeriesUID != null) {
            
 
                List slicePositions = new ArrayList();
                List sliceFiles = new ArrayList();
                TreeMap positionsFiles = new TreeMap();
                                
                File parentDir = new File(file.getParent());
                File[] listOfFiles = parentDir.listFiles();

                for (int i = 0; i < listOfFiles.length; i++) {

                    if (listOfFiles[i].isFile()) {
                        System.out.println(listOfFiles[i].getName());
                    }
                    else {
                        continue;
                    }
                
                    File sliceFile = listOfFiles[i];
                    
                    // Is it a DICOM file? Try to parse it.
                    DicomObjectReader dor = null;
                    DicomObject obj = openDicomFile(sliceFile, false); // Don't need to read pixel data in this first loop
                    if (obj == null) {
                        continue;
                    }
                    
//                    System.out.println(obj);
                                        
                    float slicePosition = sliceZ;
//                    try {
//                        slicePosition = obj.getVR("SliceLocation").getFloatValue();
//                        System.out.println("Slice position = " + slicePosition);
//                    }
//                    catch(Exception e) {
//                        continue;
//                    }
                    
                    // Make sure we include files from the same dicom series
                    if (obj.getVR("SeriesInstanceUID") == null || selectedSeriesUID.compareTo(obj.getVR("SeriesInstanceUID").getStringValue()) != 0) {
                        continue;
                    }
                    
                    
                    // Slice
                    // Get/Check image orientation for each slice in the series
                    try {
                        ImageOrientationPatient = getImageOrientationPatient(obj);
//                        for (int index=0; index<6; index++) {
//                            if (ImageOrientationPatient[index] != obj.getVR("ImageOrientationPatient").getFloatValue(index)) {
//                                continue;
//                            }
//                        }
                    }
                    catch(Exception e) {
                        continue;
                    }
                    
                    // Query slice position in patient coordinate system
                    //float[] ImagePositionPatient;
                    ImagePositionPatient = new float[3];
                    float sliceNormalOffset = 0.0f;
                    try {
                        if (obj.getVR("ImagePositionPatient") != null) {
                            ImagePositionPatient = getImagePositionPatient(obj);
                            for (int index = 0; index < 3; index++) {
                                //                            ImagePositionPatient[index] = obj.getVR("ImagePositionPatient").getFloatValue(index);
                                //                            System.out.println("ImagePositionPatinet[" + index + "] = " + ImagePositionPatient[index]);
                                //                            
                                sliceNormalOffset += ImagePositionPatient[index] * ImageNormal[index];
                            }
                        }
                        else if (obj.getVR("SliceThickness") != null) {
                            sliceNormalOffset = (float)obj.getVR("SliceThickness").getFloatValue() * i;
                        }
                        else {
                            throw new DicomException("No slice position information found.");
                        }
                    }
                    catch(Exception e) {
                        continue;
                    }
                     
                    sliceFiles.add(sliceFile.getPath());
                                     
                    System.out.println("Slice position = " + sliceNormalOffset);
                    slicePositions.add(new Float(sliceNormalOffset));
                    
                    positionsFiles.put(sliceNormalOffset, sliceFile);
                    
                    if (listener != null) {
                        listener.percentDone("Scanning dicom headers", (int)Math.round((double)(i+1)/(listOfFiles.length)*100.0));
                    }
                }
                
                Object[] positions = slicePositions.toArray();
                
                Set s = positionsFiles.entrySet();
                Iterator iter = s.iterator();
                         
                float ImagePositionPatientRoot[] = new float[3]; // coord of top left corner of first image plane
                
                for (int i=0; iter.hasNext()==true; i++) {                  
                    
                    Map.Entry entry = (Map.Entry)iter.next();
                    
                    System.out.println("slice position stored = " + (float)entry.getKey());
                    
                    File sliceFile = (File)entry.getValue();

                //// Hack to save series files somewhere in order, GE randomizes them on CD
//                    System.out.println(sliceFile);
//                    String destPath = "H:\\CT Data\\image" + i + ".dcm";
//                    Path src = Paths.get(sliceFile.getPath());
//                    Path dst = Paths.get(destPath);
//                    Files.copy(src, dst);
                    
                    
                    DicomObject obj = openDicomFile(sliceFile);
                    
                    if (i==0) {
                        System.out.println(obj);
                    }
                    
                    if (obj == null) {
                        return null;
                    }
                    
                    if (i==s.size()/2) {
//                        try {
//                            image.setAttribute("WindowWidth", obj.getVR("WindowWidth").getFloatValue());
//                            image.setAttribute("WindowCenter", obj.getVR("WindowCenter").getFloatValue());
//                        }
//                        catch(Exception e) {} // TODO: fix exception handling here for missing tags
//
                    }                    
                    else if (i==0) {
                        System.out.println("ImageVolume init");
                        
                        int cols;
                        int rows;
                        
                        float xres;
                        float yres;
                        
                        float sliceThickness;
                        
                        try {
                            xres = obj.getVR("PixelSpacing").getFloatValue(0);
                            yres = obj.getVR("PixelSpacing").getFloatValue(1);
                        }
                        catch(Exception e) {
                            xres = 1f;
                            yres = 1f;
                        }
                        
                            cols = obj.getVR("Columns").getIntValue();
                            rows = obj.getVR("Rows").getIntValue();
                            
//                        try {
//                            sliceThickness = obj.getVR("SliceThickness").getFloatValue();
//                        }
//                        catch (Exception e) {
//                            sliceThickness = 1.0f;
//                        }
                        
                        Object[] pos = positionsFiles.entrySet().toArray();
                        if (pos.length > 1) {
                            float loc1 = (Float)(((Map.Entry)pos[1]).getKey());
                            float loc0 = (Float)(((Map.Entry)pos[0]).getKey());
                            sliceThickness = Math.abs(loc1 - loc0);
                        }
                        else {
                            sliceThickness = obj.getVR("SliceThickness").getFloatValue();
                        }
                        
                        ImagePositionPatientRoot = getImagePositionPatient(obj);
//                        for (int index=0; index<3; index++) {
//                            ImagePositionPatientRoot[index] = obj.getVR("ImagePositionPatient").getFloatValue(index);
//                       }
                        
                        float zres = sliceThickness;
                        System.out.println(cols + " x " + rows + " x " + positions.length );
                        System.out.println(xres + " x " + yres + " x " + zres);
                        
                        // Put the image volume center at the origin for now
                        ImagePositionPatientRoot[0] = 0;
                        ImagePositionPatientRoot[1] = 0;
                        ImagePositionPatientRoot[2] = 0;

                        image = new ImageVolume4D(ImageVolume.USHORT_VOXEL, cols, rows, positions.length, 1);
                        image.getDimension(0).setSampleSpacing(xres);
                        image.getDimension(1).setSampleSpacing(yres);
                        image.getDimension(2).setSampleSpacing(zres);
                        
                        image.getDimension(0).setSampleWidth(xres);
                        image.getDimension(1).setSampleWidth(yres);
                        image.getDimension(2).setSampleWidth(zres);
                        
                        try {
                            image.setAttribute("PatientName", obj.getVR("PatientName").getValue());
                            image.setAttribute("PatientID", obj.getVR("PatientID").getValue());
                            image.setAttribute("PatientBirthDate", obj.getVR("PatientBirthDate").getValue());
                            image.setAttribute("PatientSex", obj.getVR("PatientSex").getValue());
                            image.setAttribute("AcquisitionDate", obj.getVR("AcquisitionDate").getValue());
                            image.setAttribute("AcquisitionTime", obj.getVR("AcquisitionTime").getValue());
                            image.setAttribute("InstitutionName", obj.getVR("InstitutionName").getValue());
                        }
                        catch(Exception e) {} // TODO: fix exception handling here for missing tags
                        
                        try {
                            image.setAttribute("ProtocolName", obj.getVR("ProtocolName").getValue());
                        }
                        catch(Exception e) {
                        }

                        try {
                            image.setAttribute("ImageOrientation", ImageOrientationPatient);
                            image.setAttribute("ImagePosition", ImagePositionPatientRoot);
                            image.setAttribute("ImageTranslation", new Vector3f(ImagePositionPatientRoot[0], ImagePositionPatientRoot[1], ImagePositionPatientRoot[2]));
                        }
                        catch(Exception e) {} // TODO: fix exception handling here for missing tags
                        
                        try {
                            VR vr = obj.getVR("WindowWidth");
                            int multiplicity = vr.getValueMultiplicity();
                            if (multiplicity > 0) {
                                image.setAttribute("WindowWidth", vr.getFloatValue(multiplicity-1));                                
                            }
                            else {
                                image.setAttribute("WindowWidth", vr.getFloatValue());
                            }

                            vr = obj.getVR("WindowCenter");
                            multiplicity = vr.getValueMultiplicity();
                            if (multiplicity > 0) {
                                image.setAttribute("WindowCenter", vr.getFloatValue(multiplicity-1));
                            }
                            else {
                                image.setAttribute("WindowCenter", vr.getFloatValue());
                            }
                        }
                        catch(Exception e) {} // TODO: fix exception handling here for missing tags
                        
                        try {
                            image.setAttribute("RescaleIntercept", obj.getVR("RescaleIntercept").getFloatValue());
                            image.setAttribute("RescaleSlope", obj.getVR("RescaleSlope").getFloatValue());
                        }
                        catch (Exception e) {
                            image.setAttribute("RescaleIntercept", 0f);
                            image.setAttribute("RescaleSlope", 1f);
                        }
                        
                        System.out.println("ImageVolume init done");
                    }
                    
                    System.out.println("Set DICOM attributes on ImageVolume");
                    
//                    float thick = obj.getVR("SliceThickness").getFloatValue();
                    
//                    System.out.println("SliceThickness = " + thick);
                    
                    float sliceTime = 0f;
                    try {
                        //sliceTime = obj.getVR("TriggerTime").getFloatValue();
                        sliceTime = obj.getVR("InstanceNumber").getFloatValue() % 40;
                    }
                    catch (Exception e) {}
                    
                    System.out.println("InstanceNumber = " + sliceTime);
                    
                    float slicePosition = 0f;
                    try {
                        slicePosition = obj.getVR("SliceLocation").getFloatValue();
                    }
                    catch(Exception e) {}
                    
                    System.out.println("SliceLocation = " + slicePosition);
                    
                    float sliceTE = 0f;
                    try {
                        VR vr = obj.getVR("EchoTime");
                        System.out.println("Got EchoTime VR");
                        if (vr != null) {
                            sliceTE = obj.getVR("EchoTime").getFloatValue();
                        }
                    }
                    catch(Exception e) {
                        System.out.println("No EchoTime attr");
                    }
                    
                    System.out.println("EchoTime = " + sliceTE);                                                                                                  
                                        
                    image.getDimension(2).setSamplePosition(i, (Float)entry.getKey());
                    
                    float sliceThickness = 0.0f;
                    Object[] pos = positionsFiles.entrySet().toArray();
                    if (pos.length > 1) {
                        float loc1 = (Float)(((Map.Entry)pos[1]).getKey());
                        float loc0 = (Float)(((Map.Entry)pos[0]).getKey());
                        sliceThickness = Math.abs(loc1 - loc0);
                    }
                    else {
                        sliceThickness = obj.getVR("SliceThickness").getFloatValue();
                    }
                    image.getDimension(2).setSampleWidth(i, sliceThickness);
                    
                    short[] voxelData = (short[])image.getData();
                    byte[] sliceData = obj.getVR("PixelData").getValueBytes();
                    if (sliceData != null && sliceData.length == 0) {
                        List frames = obj.getVR("PixelData").getImageFrames();
                        if (frames != null && frames.size() > 0) {
                            sliceData = (byte[])frames.get(0);
                        }
                    }
                    
                    boolean bLosslessJPEG = false;
                    
                    VR tsVR = obj.getVR("TransferSyntaxUID");
                    if (tsVR != null) {
                        String transferSyntax = tsVR.getStringValue();
                        if (transferSyntax.endsWith(".70")) {
                            bLosslessJPEG = true;
                        }
                    }
                    
                    short[] outData = null;
                    if (bLosslessJPEG) {
                        Parse.DecompressedOutput decomOutput = new Parse.DecompressedOutput(null ,ByteOrder.BIG_ENDIAN);
                        Parse.parse(new ByteArrayInputStream(sliceData), null, null, decomOutput);

                        int ncomponents = decomOutput.getDecompressedOutputPerComponent().length;

                        OutputArrayOrStream[] compStreams = decomOutput.getDecompressedOutputPerComponent();

                        outData = compStreams[0].getShortArray();
                    }
                    
                    
//                    LJPEGDecoder decoder = new LJPEGDecoder();
//                    decoder.setInputStream(new ByteArrayInputStream(sliceData));
//                    int cols = obj.getVR("Columns").getIntValue();
//                    int rows = obj.getVR("Rows").getIntValue();
//                    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(rows * cols * 2);
//                    decoder.setOutputStream(bytesOut);
//                    decoder.decodeImageHeader();
//                    decoder.decodeImageData();                  
//                    sliceData = bytesOut.toByteArray();
                    
                    int frameSize = image.getDimension(0).getSize() * image.getDimension(1).getSize();
                    int offset = (s.size() - 1 - i)*frameSize;
                    
                    int min=9999999;
                    int max=0;
                    for (int v=0; v<frameSize; v++) {
                        if (!bLosslessJPEG) {
                //            voxelData[offset + v] = (short)((((int)sliceData[v*2] & 0xff) << 8 | ((int)sliceData[v*2 + 1] & 0xff)) & 0xfffL);
                            voxelData[offset + v] = (short)(((int)sliceData[v*2] & 0xff) << 8 | ((int)sliceData[v*2 + 1] & 0xff));
                            //int val = v%4096;
                            //voxelData[offset + v] = (short)(((int)val & 0xff) << 8 | ((int)val & 0xff));
                            int val = voxelData[offset + v] & 0x0fff;
                            if (val > max)
                                max = val;
                            if (val < min)
                                min = val;
                        }
                        else {
                            voxelData[offset + v] = (short)(outData[v]);
                        }
                    }
                    
                    System.out.println("Image min = " + min + " max = " + max);
                    
                    System.out.println("Loaded " + (int)Math.round((double)(i+1)/(positions.length)*100.0));
                    
                    if (listener != null) {
                        listener.percentDone("Loading dicom image data", (int)Math.round((double)(i+1)/(positions.length)*100.0));
                    }

                }
                                                 
             }
            
            if (listener != null) {
                listener.percentDone("Ready.", -1);
            }            

            return image;
        }
        catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.percentDone("Error loading dicom image series", -1);
            }
            image = null;
            return image;
        }
        
//        if (listener != null) {
//            listener.percentDone("Loading dicom image data...", 0);
//        }
        
//        DicomObjectReader dor = null;
//        DicomObject obj = null;
//        try {
//            dor = new DicomObjectReader(new DicomFileReader(file));
//            obj = dor.read();
//        }
//        catch (FileNotFoundException e) {
//            System.out.println("File not found.");
//            listener.percentDone("DICOM Error", -1);
//            return null;
//        }
//        catch (IOException e) {
//            System.out.println(e);
//            listener.percentDone("IO Error.", -1);
//            return null;
//        }
//        catch (DicomException e) {
//            System.out.println("Error loading DICOM file");
//            listener.percentDone("File not found.", -1);
//            return null;
//        }
//        
//        int cols = obj.getVR("Columns").getIntValue();
//        int rows = obj.getVR("Rows").getIntValue();
//        
//        float xres = obj.getVR("PixelSpacing").getFloatValue(0);
//        float yres = obj.getVR("PixelSpacing").getFloatValue(1);
//        float zres = 0.0f;
//        System.out.println(cols + " x " + rows + " x ");
//        System.out.println(xres + " x " + yres + " x " + zres);
//        
//        image = new ImageVolume4D(ImageVolume.USHORT_VOXEL, cols, rows, 1, 0);
//        image.getDimension(0).setSampleSpacing(xres);
//        image.getDimension(1).setSampleSpacing(yres);
//        image.getDimension(2).setSampleSpacing(zres);
//        
//        short[] voxelData = (short[])image.getData();
//        byte[] sliceData = obj.getVR("PixelData").getValueBytes();
//        int offset = 0;
//        for (int v=0; v<rows*cols; v++) {
//            voxelData[offset + v] = (short)((sliceData[v*2] & 0xff) << 8 | (sliceData[v*2 + 1] & 0xff));
//        }
//        
//        
//        if (listener != null)
//            listener.percentDone("Ready.", -1);
//        
//        return image;
    }
    
    public int probe(File file) {

        if (file.isDirectory()) { return 0; }
        
        int result1 = probeForDicomFile(file);
        int result2 = probeForDicomSeriesIndexFile(file);
        
        return Math.max(result1, result2);
    }
    
    private int probeForDicomSeriesIndexFile(File file)
    {
        String cookieStr="";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));       
            cookieStr = br.readLine();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (cookieStr.equals("dicomserieshack")) {
            System.out.println("Looks like a hacked dicom directory index");
            return 100;
        }
        else {
            System.out.println("Not a hacked dicom directory index");
        }
        
        return 0;
    }
    
    private int probeForDicomFile(File file)
    {
        System.out.println("Probing for a DICOM file");
        
        DataInputStream is;
        try {
            is = new DataInputStream(new FileInputStream(file));
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found.");
            return 0;
        }
        byte[] hdr = new byte[128];
        byte[] cookie = new byte[4];
        
        try {
            is.readFully(hdr);
            is.readFully(cookie);
        }
        catch (IOException io) {
            System.out.println("IOException");
            return 0;
        }
        
        for (int i=0; i<4; i++) {
            try {
            System.out.print(new String(cookie, "8859_1"));
            }
            catch (UnsupportedEncodingException e) {
            }
            System.out.println("");
        }
        
        if (cookie[0] == 'D' && cookie[1] == 'I' && cookie[2] == 'C' && cookie[3] == 'M') {
            System.out.println("Looks like a DICOM header.");
            return 100;
        }
        else {
            System.out.println("Not a DICOM header.");
        }
        
        if (is != null) {
            try {
                is.close();
            }
            catch (IOException e) {
            }
        }
        
        return 0;
    }
    
    private String buildFileName(int i, String prefix, String suffix) {
        StringBuffer buf = new StringBuffer(32);
        
        buf.append(prefix);
        
        if (i<10)
            buf.append("0");
        if (i<100)
            buf.append("0");
        if (i<1000)
            buf.append("0");
        
        buf.append(Integer.toString(i));
        buf.append(suffix);
        
        return buf.toString();
    }
    
    private String siemensSubString(String source, String key, int length) {
        int start = source.indexOf(key);
        if (start >= 0) {
            start += key.length();
            return source.substring(start, start+length);
        }
        return "";
    }
    
}
