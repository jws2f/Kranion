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
import org.fusfoundation.dicom.DicomDate;
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
        } catch (Exception e) {
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
    
    public class seriesDescriptor {
        public String seriesUID;
        public String patientName;
        public String modality;
        public String acquisitionDate;
        public String protocolName;
        public String seriesDescription;
        public List<File> sliceFiles;
        public List<Float> sliceLocations;
        
        public seriesDescriptor() {
            seriesUID = "";
            patientName = "";
            modality = "";
            acquisitionDate = "";
            protocolName = "";
            sliceFiles = new LinkedList<File>();
            sliceLocations = new LinkedList<Float>();
        }
    }
    
    protected String safeGetVRStringValue(DicomObject obj, String key) {
        String result;
        try {
            result = obj.getVR(key).getValue().toString();
        }
        catch(NullPointerException e) {
            result = null;
        }
        
        return result;
    }
    
    private void parseDicomFile(File file, Map<String, seriesDescriptor> seriesMap) {
        if (file.isFile()) {
            DicomObject selectedDicomObj = openDicomFile(file, false);
            if (selectedDicomObj != null) {
                String seriesUID;
                seriesUID = safeGetVRStringValue(selectedDicomObj, "SeriesInstanceUID");

                if (seriesUID != null) {
                    seriesDescriptor descriptor = seriesMap.get(seriesUID);
                    if (descriptor == null) {
                        descriptor = new seriesDescriptor();
                        seriesMap.put(seriesUID, descriptor);
                    }

                    Float sliceLocation = null;
                    try {
                        sliceLocation = new Float(selectedDicomObj.getVR("SliceLocation").getFloatValue());
                    } catch (Exception e) {
                    }

                    descriptor.sliceFiles.add(file);
                    descriptor.sliceLocations.add(sliceLocation);
                    descriptor.seriesUID = seriesUID;
                    descriptor.modality = safeGetVRStringValue(selectedDicomObj, "Modality");
                    descriptor.protocolName = safeGetVRStringValue(selectedDicomObj, "ProtocolName");
                    descriptor.patientName = safeGetVRStringValue(selectedDicomObj, "PatientName");
                    descriptor.acquisitionDate = safeGetVRStringValue(selectedDicomObj, "AcquisitionDate");
                    descriptor.seriesDescription = safeGetVRStringValue(selectedDicomObj, "SeriesDescription");
                    String filterName = safeGetVRStringValue(selectedDicomObj, "ConvolutionKernel");
                    if (filterName != null) {
                        descriptor.seriesDescription = descriptor.seriesDescription + " - " + filterName;
                    }
                }
            }
        }
    }
    
    private void parseDicomDir(File file, Map<String, seriesDescriptor> seriesMap, ProgressListener listener) {
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
                
                int itemCount = dicomDir.getVR("DirectoryRecordSequence").getSequenceLength();
                int currentCount = 0;
                
                String modality = "";
                String protocolName = "";
                String patientName = "";
                String acquisitionDate = "";
                String seriesDescription = "";
                String seriesUID = "";
                Float sliceLocation = null;
                
                while (items.hasNext()) {
                    DicomObject subobj = (DicomObject)items.next();
                    String recordType = subobj.getVR("DirectoryRecordType").getStringValue();
                    
                    switch(recordType) {
                        case "PATIENT":
                            studyCount = 0;
                            seriesCount = 0;
                            imageCount = 0;
//                            System.out.println("PATIENT " + patientCount++ + ": " + subobj.getVR("PatientName").getValue());
//                            System.out.println(subobj);
                            patientName = safeGetVRStringValue(subobj, "PatientName"); 
                            break;
                        case "STUDY":
                            seriesCount = 0;
                            imageCount = 0;
//                            System.out.println("   STUDY " + studyCount++ + ": " + subobj.getVR("StudyDate").getValue());
//                            System.out.println(subobj);
                            protocolName = safeGetVRStringValue(subobj, "StudyDescription");
                            acquisitionDate = safeGetVRStringValue(subobj, "StudyDate");
                            break;
                        case "SERIES":
                            imageCount = 0;
//                            System.out.println("      SERIES " + seriesCount++ + ": " + subobj.getVR("SeriesDescription").getValue());
//                            System.out.println(subobj);
                            modality = safeGetVRStringValue(subobj, "Modality");
                            seriesUID = subobj.getVR("SeriesInstanceUID").getStringValue();
                            seriesDescription = safeGetVRStringValue(subobj, "SeriesDescription");
                            String filterName = safeGetVRStringValue(subobj, "ConvolutionKernel");
                            if (filterName != null) {
                                seriesDescription = seriesDescription + " - " + filterName;
                            }
                            break;
                        case "IMAGE":
                            VR fileSpec = subobj.getVR("ReferencedFileID");
                            int f = fileSpec.getValueMultiplicity();
                            String filename = file.getParentFile().getPath();
                            for (int i=0; i<f; i++) {
                                filename = new String(filename + File.separator + fileSpec.getValue(i));
                            }
//                            System.out.println(filename);
//                            System.out.println(subobj);
                            sliceLocation = null;
                            try {
                                sliceLocation = new Float(subobj.getVR("SliceLocation").getFloatValue());
                            } catch (Exception e) {
                            }
                            
                            
                            seriesDescriptor descriptor = seriesMap.get(seriesUID);
                            if (descriptor == null) {
                                descriptor = new seriesDescriptor();
                                seriesMap.put(seriesUID, descriptor);
                            }

                            descriptor.sliceFiles.add(new File(filename));
                            descriptor.sliceLocations.add(sliceLocation);
                            descriptor.seriesUID = seriesUID;
                            descriptor.modality = modality;
                            descriptor.protocolName = protocolName;
                            descriptor.patientName = patientName;
                            descriptor.acquisitionDate = acquisitionDate;
                            descriptor.seriesDescription = seriesDescription;
                            
                            if (listener != null && currentCount%10==0) {
                                listener.percentDone("Parsing DICOMDIR", (int)((float)currentCount/itemCount*100));
                            }
                        
                    }
                    
                    currentCount++;
                }
                
//            }
//            else {
//                System.out.println("Wrong fileSetID: " + fileSetID);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (listener != null) {
            listener.percentDone("Ready", -1);
        }
    }
    
    public Map<String, seriesDescriptor> scanDirectoryForSeries(File file, ProgressListener listener) {
                File parentDir = new File(file.getParent());
                File[] listOfFiles;
                Map<String, seriesDescriptor> seriesMap = new HashMap<>();
                
                if (file.isDirectory()) {
                    System.out.println("Scanning directory: " + file.getPath());
                    listOfFiles = file.listFiles();
                }
                else if (file.getName().equalsIgnoreCase("DICOMDIR")) {
                    parseDicomDir(file, seriesMap, listener);
                    return seriesMap;
                }
                else {
                    //listOfFiles = parentDir.listFiles();
                    return seriesMap;
                }

                for (int i = 0; i < listOfFiles.length; i++) {
                    File theFile = listOfFiles[i];
                    
                    if (theFile.isFile()) {
                      parseDicomFile(theFile, seriesMap);
                    }
//                    if (theFile.isFile()) {
//                        DicomObject selectedDicomObj = openDicomFile(listOfFiles[i], false);
//                        if (selectedDicomObj != null) {
//                            String seriesUID;
//                            seriesUID = safeGetVRStringValue(selectedDicomObj, "SeriesInstanceUID");
//
//                            if (seriesUID != null) {
//                                seriesDescriptor descriptor = seriesMap.get(seriesUID);
//                                if (descriptor == null) {
//                                    descriptor = new seriesDescriptor();
//                                    seriesMap.put(seriesUID, descriptor);
//                                }
//
//                                Float sliceLocation = null;
//                                try {
//                                    sliceLocation = new Float(selectedDicomObj.getVR("SliceLocation").getFloatValue());
//                                } catch (Exception e) {
//                                }
//
//                                descriptor.sliceFiles.add(theFile);
//                                descriptor.sliceLocations.add(sliceLocation);
//                                descriptor.seriesUID = seriesUID;
//                                descriptor.modality = safeGetVRStringValue(selectedDicomObj, "Modality");
//                                descriptor.protocolName = safeGetVRStringValue(selectedDicomObj, "ProtocolName");
//                                descriptor.patientName = safeGetVRStringValue(selectedDicomObj, "PatientName");
//                                descriptor.acquisitionDate = safeGetVRStringValue(selectedDicomObj, "AcquisitionDate");
//                                descriptor.seriesDescription = safeGetVRStringValue(selectedDicomObj, "SeriesDescription");
//                                String filterName = safeGetVRStringValue(selectedDicomObj, "ConvolutionKernel");
//                                if (filterName != null) {
//                                    descriptor.seriesDescription = descriptor.seriesDescription + " - " + filterName;
//                                }
//                            }
//
//                        }
//                    }
                    // recurse into subdirectories. not sure this is a good idea or not
                    else if (theFile.isDirectory()) {
                        System.out.println("   Scanning subdir: " + theFile.getPath());
                        Map<String, seriesDescriptor> subdirMap = this.scanDirectoryForSeries(theFile, listener);
                        seriesMap.putAll(subdirMap);
                    }
                    
                    if (listener != null) {
                        //System.out.println("scanning " + i + " of " + listOfFiles.length);
                        listener.percentDone("Scanning for dicom series", (int)Math.round((double)(i+1)/(listOfFiles.length)*100.0));
                    }
                    
                }
                
                if (listener != null) {
                    listener.percentDone("Ready", -1);
                }
                
                return seriesMap;
        
    }
    
    // load a dicom series from a list of files, sort them by slice position
    public ImageVolume load(List<File> listOfFiles, ProgressListener listener) {
        ImageVolume image = null;
       
                
        try {
            
            DicomObject selectedDicomObj = openDicomFile(listOfFiles.get(0));
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
            
 
                List<Float> slicePositions = new ArrayList<>();
                List<String> sliceFiles = new ArrayList<>();
                TreeMap<Float, File> positionsFiles = new TreeMap<>();
                                
                Iterator<File> fileIterator = listOfFiles.iterator();
                
                int sliceCount=0; // counter
                while(fileIterator.hasNext()) {
                    
                    File sliceFile = fileIterator.next();

                    if (sliceFile.isFile()) {
                        System.out.println(sliceFile.getName());
                    }
                    else {
                        continue;
                    }
                                    
                    // Is it a DICOM file? Try to parse it.
                    DicomObjectReader dor = null;
                    DicomObject obj = openDicomFile(sliceFile, false); // Don't need to read pixel data in this first loop
                    if (obj == null) {
                        continue;
                    }
                    
//                    System.out.println(obj);
                                        
//                    float slicePosition = sliceZ;
//                    try {
//                        slicePosition = obj.getVR("SliceLocation").getFloatValue();
//                        System.out.println("Slice position = " + slicePosition);
//                    }
//                    catch(Exception e) {
//                        continue;
//                    }

                    try {
                        System.out.print(obj.getVR("Modality").getStringValue() + " -- ");
                    } catch (Exception e) {}
                    try {
                        System.out.print(obj.getVR("ProtocolName").getStringValue() + " -- ");
                    } catch (Exception e) {}
                    try {
                        System.out.print(obj.getVR("AcquisitionDate").getDateValue());
                    } catch (Exception e) {}
                    System.out.println();
                    
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
//                        else if (obj.getVR("SliceThickness") != null) {
//                            sliceNormalOffset = (float)obj.getVR("SliceThickness").getFloatValue() * sliceCount;
//                        }
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
                        listener.percentDone("Scanning dicom headers", (int)Math.round((double)(sliceCount+1)/(listOfFiles.size())*100.0));
                    }
                    
                    sliceCount++;
                }
                
                
                Iterator<String> fileNamesIter = sliceFiles.iterator();
                System.out.println("Selected series file names:");
                while(fileNamesIter.hasNext()) {
                    System.out.println(fileNamesIter.next());
                }
                System.out.println("/n");
                
                Object[] positions = slicePositions.toArray();
                
                
                Set s = positionsFiles.entrySet();
                
                // save the pixel dimensions of the central slice.
                // Any slices in this series that don't match will be zeroed out
                // Sometimes there is an initial slice with resampling graphics and
                // we want to filter that out.
                float centerXres = 0;
                float centerYres = 0;
                Object entries[] = s.toArray();
                if (entries.length>0) {
                    Map.Entry centerSlice = (Map.Entry)entries[entries.length/2];
                    File centerFile = (File)centerSlice.getValue();
                    DicomObject obj = openDicomFile(centerFile, false);
                    if (obj != null) {
                        try {
                            centerXres = obj.getVR("PixelSpacing").getFloatValue(0);
                            centerYres = obj.getVR("PixelSpacing").getFloatValue(1);
                        }
                        catch(Exception e) {
                            centerXres = 0f;
                            centerYres = 0f;
                        }
                    }
                }
                
                
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
                    
               
                    if (i==0) {
                        System.out.println("ImageVolume init");
                        
                        int cols;
                        int rows;
                        
                            cols = obj.getVR("Columns").getIntValue();
                            rows = obj.getVR("Rows").getIntValue();
                            
                        System.out.println(cols + " x " + rows + " x " + positions.length );
                            
//                        try {
//                            sliceThickness = obj.getVR("SliceThickness").getFloatValue();
//                        }
//                        catch (Exception e) {
//                            sliceThickness = 1.0f;
//                        }
                        
                        
                        ImagePositionPatientRoot = getImagePositionPatient(obj);
//                        for (int index=0; index<3; index++) {
//                            ImagePositionPatientRoot[index] = obj.getVR("ImagePositionPatient").getFloatValue(index);
//                       }
                        
                        
                        // Put the image volume center at the origin for now
                        ImagePositionPatientRoot[0] = 0;
                        ImagePositionPatientRoot[1] = 0;
                        ImagePositionPatientRoot[2] = 0;

                        image = new ImageVolume4D(ImageVolume.USHORT_VOXEL, cols, rows, positions.length, 1);
                        
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
                    
                    if (i==s.size()/2) {
//                        try {
//                            image.setAttribute("WindowWidth", obj.getVR("WindowWidth").getFloatValue());
//                            image.setAttribute("WindowCenter", obj.getVR("WindowCenter").getFloatValue());
//                        }
//                        catch(Exception e) {} // TODO: fix exception handling here for missing tags
//
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
                        
                        Object[] pos = positionsFiles.entrySet().toArray();
                        if (pos.length > 1) {
                            int midIndex = pos.length/2;
                            float loc1 = (Float)(((Map.Entry)pos[midIndex+1]).getKey());
                            float loc0 = (Float)(((Map.Entry)pos[midIndex]).getKey());
                            sliceThickness = Math.abs(loc1 - loc0);
                            
//                            for (int t=0; t<pos.length; t++) {
//                                System.out.println("Slice pos " + t + " (" + ((File)((Map.Entry)pos[t]).getValue()).getName() + ") = " + (Float)(((Map.Entry)pos[t]).getKey()));
//                            }
                        }
                        else {
                            sliceThickness = obj.getVR("SliceThickness").getFloatValue();
                        }
                        
                        float zres = sliceThickness;
                        System.out.println(xres + " x " + yres + " x " + zres);
                        
                        image.getDimension(0).setSampleSpacing(xres);
                        image.getDimension(1).setSampleSpacing(yres);
                        image.getDimension(2).setSampleSpacing(zres);
                        
                        image.getDimension(0).setSampleWidth(xres);
                        image.getDimension(1).setSampleWidth(yres);
                        image.getDimension(2).setSampleWidth(zres);
                    }
                    
                    System.out.println("Set DICOM attributes on ImageVolume");
                    
//                    float thick = obj.getVR("SliceThickness").getFloatValue();
                    
//                    System.out.println("SliceThickness = " + thick);
                        float xres;
                        float yres;
                        
                        float sliceThickness;
                        
                        try {
                            xres = obj.getVR("PixelSpacing").getFloatValue(0);
                            yres = obj.getVR("PixelSpacing").getFloatValue(1);
                        }
                        catch(Exception e) {
                            xres = 0f;
                            yres = 0f;
                        }
                        
                        boolean sliceMisMatch = (xres != centerXres) || (yres != centerYres);

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
                                        
//                    image.getDimension(2).setSamplePosition(i, (Float)entry.getKey());
//                    
//                    float sliceThickness = 0.0f;
//                    Object[] pos = positionsFiles.entrySet().toArray();
//                    if (pos.length > 1) {
//                        int midIndex = pos.length/2;
//                        float loc1 = (Float)(((Map.Entry)pos[midIndex+1]).getKey());
//                        float loc0 = (Float)(((Map.Entry)pos[midIndex]).getKey());
//                        sliceThickness = Math.abs(loc1 - loc0);
//                    }
//                    else {
//                        sliceThickness = obj.getVR("SliceThickness").getFloatValue();
//                    }
//                    image.getDimension(2).setSampleWidth(sliceThickness);
                    
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

                    int signedPixelRep = 1;
                    try {
                        signedPixelRep = obj.getVR("PixelRepresentation").getIntValue();
                    }
                    catch(Exception e) {}
                    
                    if (signedPixelRep != 1) {
//                        throw new Exception("Unsupported PixelRepresentation. Only Signed pixel values supported currently;");                                
                    }
                    
                    int pixelPaddingValue = Integer.MIN_VALUE;
                    try {
                        pixelPaddingValue = obj.getVR("PixelPaddingValue").getIntValue();
                    }
                    catch(Exception e) {}
                    
                    int frameSize = image.getDimension(0).getSize() * image.getDimension(1).getSize();
                    int offset = (s.size() - 1 - i)*frameSize;
                    
                    int min=9999999;
                    int max=0;
                    for (int v = 0; v < frameSize; v++) {
                        if (!sliceMisMatch) {
                            if (!bLosslessJPEG) {
                                //            voxelData[offset + v] = (short)((((int)sliceData[v*2] & 0xff) << 8 | ((int)sliceData[v*2 + 1] & 0xff)) & 0xfffL);

                                // handle pixel padding value if given for CT
                                short rawValue;
                                if (signedPixelRep == 1) {
                                    rawValue = (short) ((sliceData[v * 2] & 0xff) << 8 | (sliceData[v * 2 + 1] & 0xff));
                                    if (rawValue < 0) {
                                        rawValue = 0; // in the signed case, we don't handle negative pixel values. Typically zero will get rescaled to -1024:Air
                                    }
                                } else {
                                    rawValue = (short) ((sliceData[v * 2] & 0xff) << 8 | (sliceData[v * 2 + 1] & 0xff));
                                }

                                if (rawValue == pixelPaddingValue) {
                                    rawValue = 0;
                                }
//                            if (rawValue < 0) rawValue = 0;
                                voxelData[offset + v] = rawValue;
                                //int val = v%4096;
                                //voxelData[offset + v] = (short)(((int)val & 0xff) << 8 | ((int)val & 0xff));
                                int val = voxelData[offset + v] & 0x0fff;
                                if (val > max) {
                                    max = val;
                                }
                                if (val < min) {
                                    min = val;
                                }
                            } else {
                                voxelData[offset + v] = (short) (outData[v]);
                            }
                        } else {
                            voxelData[offset + v] = 0; // sometimes this is one image prepended that has resampling grid graphics and doesn't match resolution
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
            
 
                List<Float> slicePositions = new ArrayList<>();
                List<String> sliceFiles = new ArrayList<>();
                TreeMap<Float, File> positionsFiles = new TreeMap<>();
                                
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

                    try {
                        System.out.print(obj.getVR("Modality").getStringValue() + " -- ");
                    } catch (Exception e) {}
                    try {
                        System.out.print(obj.getVR("ProtocolName").getStringValue() + " -- ");
                    } catch (Exception e) {}
                    try {
                        System.out.print(obj.getVR("AcquisitionDate").getDateValue());
                    } catch (Exception e) {}
                    System.out.println();
                    
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
                
                Iterator<String> fileNamesIter = sliceFiles.iterator();
                System.out.println("Selected series file names:");
                while(fileNamesIter.hasNext()) {
                    System.out.println(fileNamesIter.next());
                }
                System.out.println("/n");
                
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

                    int signedPixelRep = 1;
                    try {
                        signedPixelRep = obj.getVR("PixelRepresentation").getIntValue();
                    }
                    catch(Exception e) {}
                    
                    if (signedPixelRep != 1) {
//                        throw new Exception("Unsupported PixelRepresentation. Only Signed pixel values supported currently;");                                
                    }
                    
                    int pixelPaddingValue = Integer.MIN_VALUE;
                    try {
                        pixelPaddingValue = obj.getVR("PixelPaddingValue").getIntValue();
                    }
                    catch(Exception e) {}
                    
                    int frameSize = image.getDimension(0).getSize() * image.getDimension(1).getSize();
                    int offset = (s.size() - 1 - i)*frameSize;
                    
                    int min=9999999;
                    int max=0;
                    for (int v=0; v<frameSize; v++) {
                        if (!bLosslessJPEG) {
                //            voxelData[offset + v] = (short)((((int)sliceData[v*2] & 0xff) << 8 | ((int)sliceData[v*2 + 1] & 0xff)) & 0xfffL);
                
                            // handle pixel padding value if given for CT
                            short rawValue;
                            if (signedPixelRep == 1) {
                                rawValue = (short)((sliceData[v*2] & 0xff) << 8 | (sliceData[v*2 + 1] & 0xff));
                                if (rawValue<0) {
                                    rawValue = 0; // in the signed case, we don't handle negative pixel values. Typically zero will get rescaled to -1024:Air
                                }
                            }
                            else {
                                rawValue = (short)((sliceData[v*2] & 0xff) << 8 | (sliceData[v*2 + 1] & 0xff));
                            }
                            
                            if (rawValue == pixelPaddingValue) {
                                rawValue = 0;
                            }
//                            if (rawValue < 0) rawValue = 0;
                            voxelData[offset + v] = rawValue;
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
