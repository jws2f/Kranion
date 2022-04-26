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
package org.fusfoundation.dicom.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Iterator;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.VrReader;
import org.fusfoundation.dicom.VrOutputStream;
import org.fusfoundation.dicom.DicomString;
import org.fusfoundation.dicom.DicomNumber;
import org.fusfoundation.dicom.DicomRuntimeException;

import org.fusfoundation.dicom.part10.DicomFileWriter;

import org.fusfoundation.dicom.net.Association;

/**
 *
 * @author  jsnell
 */
public class StorageService extends ServiceClassProviderBaseImpl {
   
   private static final UID sopuids[] = {UID.CTImageStorage, UID.MRImageStorage, UID.CRImageStorage,
   UID.DigitalXrayForPresentationImageStorage, UID.SecondaryCaptureImageStorage,
   UID.UltrasoundImageStorage};
   private static final UID tsyn[] = {UID.ExplicitVRBigEndian, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian};
   static final private Logger logger = Logger.getGlobal();

   private Connection c = null;
   private PreparedStatement ps1, ps2, ps3, ps4;
   
   /** Creates a new instance of StorageService */
   public StorageService() {
      // Setup DB connection
         try {
            Class.forName("org.postgresql.Driver");
         }
         catch (ClassNotFoundException e) {
            logger.severe("Failed to load DB driver");
            throw new DicomRuntimeException(e.toString());
         }
                  
         try {
            // The second and third arguments are the username and password,
            // respectively. They should be whatever is necessary to connect
            // to the database.
            c = DriverManager.getConnection("jdbc:postgresql://localhost/dicomstorage",
            "jsnell", "illuminator");
         } catch (SQLException se) {
            logger.severe("Couldn't connect to DB");
            throw new DicomRuntimeException(se.toString());
         }
                  
         try {
            ps1 = c.prepareStatement("INSERT INTO patients VALUES (?, ?, ?, ?, ?, ?)");
            ps2 = c.prepareStatement("INSERT INTO studies VALUES (?, ?, ?, ?, ?)");
            ps3 = c.prepareStatement("INSERT INTO series VALUES (?, ?, ?, ?)");
            ps4 = c.prepareStatement("INSERT INTO objects VALUES (?, ?, ?, ?)");
         }
         catch (SQLException se) {
            logger.severe("We got an exception while preparing a statement:" +
            "Probably bad SQL.");
            throw new DicomRuntimeException(se.toString());
         }
         
         logger.info("DB connection setup complete.");
         
   }
   
   public UID[] getSupportedTransferSyntaxes() {
      return tsyn;
   }
   
   public UID[] getSOPClassProviderUIDs() {
      return sopuids;
   }
   
   public void handleCommand(Association assoc, int PresentationContextID, DicomObject command, VrReader messageStream) {
      UID sopClass = new UID(command.getVR("AffectedSOPClassUID").getStringValue());
      
      if (sopClass.equals(UID.CTImageStorage)) {
         logger.info("Received C-STORE: CT Storage");
      }
      else if (sopClass.equals(UID.MRImageStorage)) {
         logger.info("Received C-STORE: MR Storage");
      }
      else if (sopClass.equals(UID.SecondaryCaptureImageStorage)) {
         logger.info("Received C-STORE: Secondary Capture Storage");
      }
      else if (sopClass.equals(UID.CRImageStorage)) {
         logger.info("Received C-STORE: CR Storage");
      }
      else if (sopClass.equals(UID.DigitalXrayForPresentationImageStorage)) {
         logger.info("Received C-STORE: Digital Xray Storage");
      }
      else if (sopClass.equals(UID.UltrasoundImageStorage)) {
         logger.info("Received C-STORE: Ultrasound Storage");
      }
      else {
         throw new DicomRuntimeException("Unrecognized SOP Class");
      }
      
      try {
         DicomObject imageObject = new DicomObject();
         VR v;
         while ((v = messageStream.readVR()) != null) {
            logger.info(v.toString());
            imageObject.addVR(v);
         }
         logger.info("Dataset received.");
         
         DicomObject response = new DicomObject();
         ByteArrayOutputStream bos1 = new ByteArrayOutputStream(256);
         VrOutputStream vos1 = new VrOutputStream(bos1);
         
         int msgID = command.getVR("MessageID").getIntValue();
         String instanceUID = command.getVR("AffectedSOPInstanceUID").getStringValue();
         
         String patientName = new String(imageObject.getVR("PatientsName").getValueBytes()).trim();
         String studyuid = imageObject.getVR("StudyID").getStringValue();
         String seriesuid = "" + imageObject.getVR("SeriesNumber").getIntValue();
         
         // Write the dataset to a file
         logger.info("Storing object");
         File file = new File("/home/jsnell/tmp/" + patientName + "/" + studyuid + "/" + seriesuid);
         file.mkdirs();
         file = new File("/home/jsnell/tmp/" + patientName + "/" + studyuid + "/" + seriesuid + "/" + instanceUID);
         DicomFileWriter fw = new DicomFileWriter(file, UID.CTImageStorage.toString(), instanceUID);
         fw.write(imageObject);
         
         // Index in the database
         logger.info("Indexing object");
                   
         
         try {
            ps1.setString(1, imageObject.getVR("PatientID").getStringValue());
            ps1.setString(2, patientName);
            ps1.setString(3, null);
            ps1.setString(4, null);
            ps1.setString(5, null);
            ps1.setString(6, null);
            try {
               ps1.setString(3, imageObject.getVR("PatientsAge").getStringValue());
            }
            catch (Exception e) {}
            try {
               ps1.setString(4, imageObject.getVR("PatientsSex").getStringValue());
            }
            catch (Exception e) {}
            try {
               ps1.setString(5, imageObject.getVR("PatientsBirthDate").getValue().toString());
            }
            catch (Exception e) {}
            try {
               ps1.setString(6, imageObject.getVR("PatientComments").getStringValue());
            }
            catch (Exception e) {}
         } catch (SQLException se) {
            logger.severe("We got an exception while preparing a statement:" +
            "Probably bad SQL.");
            throw new DicomRuntimeException(se.toString());
         }
         
         try {
            ps1.executeUpdate();
         } catch (SQLException se) {
            logger.severe(se.toString());
            //throw new DicomRuntimeException(se.toString());
         }
         
         try {
            //ps = c.prepareStatement("INSERT INTO studies VALUES (?, ?, ?, ?, ?)");
            ps2.setString(1, imageObject.getVR("StudyInstanceUID").getStringValue());
            ps2.setNull(2, java.sql.Types.DATE);
            ps2.setNull(3, java.sql.Types.TIME);
            ps2.setNull(4, java.sql.Types.VARCHAR);
            try {
               ps2.setString(2, imageObject.getVR("StudyDate").getValue().toString());
            }
            catch (Exception e) {}
            try {
               ps2.setString(3, imageObject.getVR("StudyTime").getValue().toString());
            }
            catch (Exception e) {}
            try {
               ps2.setString(4, imageObject.getVR("ProtocolName").getStringValue());
            }
            catch (Exception e) {}
            ps2.setString(5, imageObject.getVR("PatientID").getStringValue());
         } catch (SQLException se) {
            logger.severe(se.toString());
            throw new DicomRuntimeException(se.toString());
         }
         
         try {
            ps2.executeUpdate();
         } catch (SQLException se) {
            logger.severe(se.toString());
            //throw new DicomRuntimeException(se.toString());
         }
         
         try {
            //ps = c.prepareStatement("INSERT INTO series VALUES (?, ?, ?, ?)");
            ps3.setString(1, imageObject.getVR("SeriesInstanceUID").getStringValue());
            ps3.setString(2, null);
            ps3.setString(3, null);
            try {
               ps3.setString(2, imageObject.getVR("SeriesDate").getValue().toString());
            }
            catch (Exception e) {}
            try {
               ps3.setString(3, imageObject.getVR("Modality").getStringValue());
            }
            catch (Exception e) {}
            ps3.setString(4, imageObject.getVR("StudyInstanceUID").getStringValue());
         } catch (SQLException se) {
            logger.severe(se.toString());
            throw new DicomRuntimeException(se.toString());
         }
         
         try {
            ps3.executeUpdate();
         } catch (SQLException se) {
            //logger.severe(se.toString());
            //throw new DicomRuntimeException(se.toString());
         }
         
         try {
            //ps = c.prepareStatement("INSERT INTO objects VALUES (?, ?, ?, ?)");
            ps4.setString(1, imageObject.getVR("SOPInstanceUID").getStringValue());
            ps4.setInt(2, imageObject.getVR("InstanceNumber").getIntValue());
            ps4.setString(3, file.getPath());
            ps4.setString(4, imageObject.getVR("SeriesInstanceUID").getStringValue());
         } catch (SQLException se) {
            logger.severe(se.toString());
            throw new DicomRuntimeException(se.toString());
         }
         
         try {
            ps4.executeUpdate();
         } catch (SQLException se) {
            logger.severe("Duplicate Object Found: " + se.toString());
            //throw new DicomRuntimeException("Duplicate Object Found: " + se.toString());
         }
         
         logger.info("Sending response.");
         response.addVR(new VR("AffectedSOPClassUID", new DicomString(sopClass.toString())));
         response.addVR(new VR("AffectedSOPInstanceUID", new DicomString(instanceUID)));
         response.addVR(new VR("CommandField", new DicomNumber(0x8001))); // C-STORE RSP
         response.addVR(new VR("MessageIDBeingRespondedTo", new DicomNumber(msgID)));
         response.addVR(new VR("DataSetType", new DicomNumber(0x0101))); // No dataset
         response.addVR(new VR("Status", new DicomNumber(0x0))); // Success
         
         logger.info("Response:\n" + response);
         
         Iterator i = response.iterator();
         while (i.hasNext()) {
            vos1.writeVR((VR)i.next());
         }
         byte[] msg = bos1.toByteArray();
         VR groupTag = new VR("CommandGroupLength", new DicomNumber(msg.length));
         
         bos1 = new ByteArrayOutputStream(256);
         vos1.setDest(bos1);
         
         bos1.write(PresentationContextID); // COMMAND HEADER 1 - PC
         bos1.write(0x3); // COMMAND HEADER 2 (COMMAND MSG | LAST)
         vos1.writeVR(groupTag);
         bos1.write(msg);
         msg = bos1.toByteArray();
         int biglen = msg.length;
         
         vos1 = null;
         bos1 = new ByteArrayOutputStream(256);
         bos1.write((biglen & 0xff000000) >>> 24);
         bos1.write((biglen & 0xff0000) >>> 16);
         bos1.write((biglen & 0xff00) >>> 8);
         bos1.write( biglen & 0xff);
         bos1.write(msg);
         msg = bos1.toByteArray();
         
         PDU pdu = new PDU();
         pdu.setType(PDU.P_DATA_TF);
         pdu.setBuffer(msg);
         pdu.writePDU(assoc.getOutputStream());
      }
      catch (IOException e) {
         throw new DicomRuntimeException(e.toString());
      }
   }
   
}
