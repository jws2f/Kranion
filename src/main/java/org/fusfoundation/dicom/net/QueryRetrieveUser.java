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

import org.fusfoundation.dicom.DicomString;
import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.VrReader;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomDate;
import org.fusfoundation.dicom.PersonName;
import org.fusfoundation.dicom.DicomNumber;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

/**
 *
 * @author  jsnell
 */
public class QueryRetrieveUser implements ServiceClassUser, ServiceClassProvider {
   Association association = null;
   int   findContextId = -1;
   int   moveContextId = -1;
   int   getContextId = -1;
   int   ctStoreContextId = -1;
   
   static private final UID uids[] = {UID.PatientRootQueryRetrieveFind, UID.PatientRootQueryRetrieveMove, UID.PatientRootQueryRetrieveGet};
   static private final UID provideruids[] = {UID.CTImageStorage};
   static private final UID tsyn[] = {UID.ExplicitVRBigEndian, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian};
   static final private Logger logger = Logger.getGlobal();;

   
   static private final int RESPONSE_REFUSED = 0xA700;
   static private final int RESPONSE_REFUSED_OUT_OF_RESOURCES_MATCHING = 0xA701;
   static private final int RESPONSE_REFUSED_OUT_OF_RESOURCES_SUBOPERATIONS = 0xA702;
   static private final int RESPONSE_REFUSED_UNKNOWN_DESTINATION = 0xA801;
   static private final int RESPONSE_FAILED = 0xA900;
   static private final int RESPONSE_CANCEL = 0xFE00;
   static private final int RESPONSE_PENDING = 0xFF00;
   static private final int RESPONSE_PENDING_WITH_WARNING = 0xFF01;
   static private final int RESPONSE_SUCCESS = 0x0000;
   static private final int RESPONSE_SUBOPERATIONS_COMPLETE_WITH_FAILURES = 0xB000;
   
   /** Creates a new instance of QueryRetrieveUser */
   public QueryRetrieveUser() {
   }
   
   public void setPresentationContextId(UID sopclass, int cntxid) {
      if (sopclass.equals(UID.PatientRootQueryRetrieveFind)) {
         findContextId = cntxid;
         logger.info("Find Presentation Context ID = " + cntxid);
      }
      else if (sopclass.equals(UID.PatientRootQueryRetrieveMove)) {
         moveContextId = cntxid;
         logger.info("Move Presentation Context ID = " + cntxid);
      }
      else if (sopclass.equals(UID.PatientRootQueryRetrieveGet)) {
         getContextId = cntxid;
         logger.info("Get Presentation Context ID = " + cntxid);
      }
      else if (sopclass.equals(UID.CTImageStorage)) {
         ctStoreContextId = cntxid;
         logger.info("CTImageStorage Presentation Context ID = " + cntxid);
      }
   }
   
   public UID[] getSupportedTransferSyntaxes() {
      return tsyn;
   }
   
   public boolean isActive() {
      if (findContextId > -1 && moveContextId > -1 && getContextId > -1 && association != null) {
         return true;
      }
      else {
         return false;
      }
   }
   
   public void setNegotiatedStatus(UID sopclass, int role) {
   }
   
   public UID[] getSOPClassUserUIDs() {
      return uids;
   }
   
   public UID[] getSOPClassProviderUIDs() {
      return provideruids;
   }
   
   public void setAssociation(Association assoc) {
      logger.info("Got an Association");
      association = assoc;
   }
   
   public Thread ProcessCommand(Association assoc, int presentationContextID, DicomObject command, VrReader vrreader) {
      return null;
   }
   
   public Collection doQuery(String queryRetrieveLevel, DicomObject queryKeys) throws IOException {
      LinkedList qresult = new LinkedList();
      
      if (isActive()) {
         logger.info("Sending query");
         DicomObject command = new DicomObject();
         command.addVR(new VR("AffectedSOPClassUID", new DicomString(UID.PatientRootQueryRetrieveFind.toString())));
         command.addVR(new VR("CommandField", new DicomNumber(0x0020)));
         command.addVR(new VR("DataSetType", new DicomNumber(0xFEFE)));
         command.addVR(new VR("Priority", new DicomNumber(0x0000)));
         
         logger.info(command.toString());
         
         int msgId = association.WriteCommand(command, findContextId);
         
         DicomObject message = new DicomObject();
         byte[] empty = new byte[0];
         message.addVR(new VR("QueryRetrieveLevel", new DicomString(queryRetrieveLevel)));
         Iterator vrIterator = queryKeys.iterator();
         while (vrIterator.hasNext()) {
            message.addVR((VR)vrIterator.next());
         }
         
         logger.info(message.toString());
         
         association.WriteMessage(message, findContextId);
         
         logger.info("Reading response");
         
         while (true) {
            DicomObject response = association.ReadCommand(findContextId);
            
            logger.info(response.toString());
            
            if (response != null) {
               int status = response.getVR("Status").getIntValue();
               int recvMsgId = response.getVR("MessageIDBeingRespondedTo").getIntValue();
               int cmdField = response.getVR("CommandField").getIntValue();
               int dataSetType = response.getVR("DataSetType").getIntValue();
               
               if (status == QueryRetrieveUser.RESPONSE_PENDING || status == QueryRetrieveUser.RESPONSE_PENDING_WITH_WARNING) {
                  logger.info("Command Response: PENDING");
                  if (dataSetType != 0x0101) {
                     
                     DicomObject results = association.ReadMessage(findContextId);
                     logger.info(results.toString());
                     
                     qresult.add(results);
                  }
               }
               else if (status == QueryRetrieveUser.RESPONSE_SUCCESS) {
                  logger.info("Command Response: SUCCESS");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_FAILED) {
                  logger.info("Command Response: FAILED");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_CANCEL) {
                  logger.info("Command Response: CANCEL");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_REFUSED) {
                  logger.info("Command Response: REFUSED");
                  break;
               }
               else if ((status & 0xf000) == 0xC000) {
                  logger.info("Command Response: UNABLE_TO_PROCESS");
                  break;
               }
               else {
                  logger.severe("Command Response: Unknown response code.");
               }
               
               
            }
         }
         
      }
      else {
         logger.severe("SCU not active on this Association");
      }
      
      return qresult;
   }
   
   public Collection movePatient(String patientID, String destinationAE) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("PatientID", new DicomString(patientID)));
      
      return doMove(destinationAE, "PATIENT", keys);
   }
   
   public Collection moveStudy(String studyUID, String destinationAE) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("StudyInstanceUID", new DicomString(studyUID)));
      
      return doMove(destinationAE, "STUDY", keys);
   }
   
   public Collection moveSeries(String seriesUID, String destinationAE) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("StudyInstanceUID", new DicomString(seriesUID)));
      
      return doMove(destinationAE, "SERIES", keys);
   }
   
   public Collection moveInstance(String instanceUID, String destinationAE) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("SOPInstanceUID", new DicomString(instanceUID)));
      
      return doMove(destinationAE, "IMAGE", keys);
   }
   
   public Collection doMove(String destinationAE, String queryRetrieveLevel, DicomObject queryKeys) throws IOException {
      LinkedList qresult = new LinkedList();
      
      if (isActive()) {
         logger.info("Sending move command");
         DicomObject command = new DicomObject();
         command.addVR(new VR("AffectedSOPClassUID", new DicomString(UID.PatientRootQueryRetrieveMove.toString())));
         command.addVR(new VR("CommandField", new DicomNumber(0x0021)));
         command.addVR(new VR("DataSetType", new DicomNumber(0xFEFE))); // Identifier message follows
         command.addVR(new VR("Priority", new DicomNumber(0x0000)));
         command.addVR(new VR("MoveDestination", new DicomString(destinationAE)));
         
         logger.info(command.toString());
         
         int msgId = association.WriteCommand(command, moveContextId);
         
         DicomObject message = new DicomObject();
         byte[] empty = new byte[0];
         message.addVR(new VR("QueryRetrieveLevel", new DicomString(queryRetrieveLevel)));
         Iterator vrIterator = queryKeys.iterator();
         while (vrIterator.hasNext()) {
            message.addVR((VR)vrIterator.next());
         }
         
         logger.info(message.toString());
         
         association.WriteMessage(message, moveContextId);
         
         logger.info("Reading response");
         
         while (true) {
            DicomObject response = association.ReadCommand(moveContextId);
            
            logger.info(response.toString());
            
            if (response != null) {
               int status = response.getVR("Status").getIntValue();
               int recvMsgId = response.getVR("MessageIDBeingRespondedTo").getIntValue();
               int cmdField = response.getVR("CommandField").getIntValue();
               int dataSetType = response.getVR("DataSetType").getIntValue();
               
               if (status == QueryRetrieveUser.RESPONSE_PENDING ||
               status == QueryRetrieveUser.RESPONSE_PENDING_WITH_WARNING) {
                  logger.info("Command Response: PENDING");
                  if (dataSetType != 0x0101) {
                     
                     DicomObject results = association.ReadMessage(moveContextId);
                     logger.info(results.toString());
                     
                     logger.info("Suboperations: " + results.getVR("NumberOfRemainingSuboperations").getValue());
                     
                  }
               }
               else if (status == QueryRetrieveUser.RESPONSE_SUCCESS) {
                  logger.info("Command Response: SUCCESS");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_SUBOPERATIONS_COMPLETE_WITH_FAILURES) {
                  logger.info("Command Response: SUBOPERATIONS COMPLETE WITH FAILURES");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_FAILED) {
                  logger.info("Command Response: FAILED");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_REFUSED_UNKNOWN_DESTINATION) {
                  logger.info("Command Response: REFUSED: UNKNOWN_DESTINATION");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_CANCEL) {
                  logger.info("Command Response: CANCEL");
                  break;
               }
               else if (status == QueryRetrieveUser.RESPONSE_REFUSED ||
               status == QueryRetrieveUser.RESPONSE_REFUSED_OUT_OF_RESOURCES_MATCHING ||
               status == QueryRetrieveUser.RESPONSE_REFUSED_OUT_OF_RESOURCES_SUBOPERATIONS) {
                  logger.info("Command Response: REFUSED");
                  break;
               }
               else if ((status & 0xf000) == 0xC000) {
                  logger.info("Command Response: UNABLE_TO_PROCESS");
                  break;
               }
               else {
                  logger.severe("Command Response: Unknown response code: " + status);
               }
               
            }
         }
         
      }
      else {
         logger.severe("SCU not active on this Association");
      }
      
      return qresult;
   }
   
   public Collection getPatient(String patientID) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("PatientID", new DicomString(patientID)));
      
      return doGet("PATIENT", keys);
   }
   
   public Collection getStudy(String studyUID) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("StudyInstanceUID", new DicomString(studyUID)));
      
      return doGet("STUDY", keys);
   }
   
   public Collection getSeries(String seriesUID) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("StudyInstanceUID", new DicomString(seriesUID)));
      
      return doGet("SERIES", keys);
   }
   
   public DicomObject getInstance(String instanceUID) throws IOException {
      DicomObject keys = new DicomObject();
      keys.addVR(new VR("SOPInstanceUID", new DicomString(instanceUID)));
      
      Collection images =  doGet("IMAGE", keys);
      return (DicomObject)(images.toArray()[0]);
   }
   
   public Collection doGet(String queryRetrieveLevel, DicomObject queryKeys) throws IOException {
      LinkedList qresult = new LinkedList();
      
      if (isActive()) {
         logger.info("Sending C-GET command");
         DicomObject command = new DicomObject();
         command.addVR(new VR("AffectedSOPClassUID", new DicomString(UID.PatientRootQueryRetrieveGet.toString())));
         command.addVR(new VR("CommandField", new DicomNumber(0x0010)));
         command.addVR(new VR("DataSetType", new DicomNumber(0xFEFE))); // Identifier message follows
         command.addVR(new VR("Priority", new DicomNumber(0x0000)));
         
         logger.info(command.toString());
         
         int msgId = association.WriteCommand(command, getContextId);
         
         DicomObject message = new DicomObject();
         byte[] empty = new byte[0];
         message.addVR(new VR("QueryRetrieveLevel", new DicomString(queryRetrieveLevel)));
         Iterator vrIterator = queryKeys.iterator();
         while (vrIterator.hasNext()) {
            message.addVR((VR)vrIterator.next());
         }
         
         logger.info(message.toString());
         
         association.WriteMessage(message, getContextId);
         
         logger.info("Reading response");
         
         while (true) {
            DicomObject response = association.ReadCommand(-1);
            
            if (response != null) {
               logger.info("Received comand: " + response);
               
               int cmdField = response.getVR("CommandField").getIntValue();
               int dataSetType = response.getVR("DataSetType").getIntValue();
               String sopClass = response.getVR("AffectedSOPClassUID").getStringValue();
               
               if (cmdField == 0x8010) { // C-GET Resonse
                  logger.info("C-GET Response: " + response);
                  int status = response.getVR("Status").getIntValue();
                  int recvMsgId = response.getVR("MessageIDBeingRespondedTo").getIntValue();
                         
                  if (status == QueryRetrieveUser.RESPONSE_PENDING ||
                  status == QueryRetrieveUser.RESPONSE_PENDING_WITH_WARNING) {
                     logger.info("Command Response: PENDING");
                     if (dataSetType != 0x0101) {
                        
                        DicomObject results = association.ReadMessage(moveContextId);
                        logger.info(results.toString());
                        
                        logger.info("Suboperations: " + results.getVR("NumberOfRemainingSuboperations").getValue());
                        
                     }
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_SUCCESS) {
                     logger.info("Command Response: SUCCESS");
                     break;
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_SUBOPERATIONS_COMPLETE_WITH_FAILURES) {
                     logger.info("Command Response: SUBOPERATIONS COMPLETE WITH FAILURES");
                     break;
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_FAILED) {
                     logger.info("Command Response: FAILED");
                     break;
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_REFUSED_UNKNOWN_DESTINATION) {
                     logger.info("Command Response: REFUSED: UNKNOWN_DESTINATION");
                     break;
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_CANCEL) {
                     logger.info("Command Response: CANCEL");
                     break;
                  }
                  else if (status == QueryRetrieveUser.RESPONSE_REFUSED ||
                  status == QueryRetrieveUser.RESPONSE_REFUSED_OUT_OF_RESOURCES_MATCHING ||
                  status == QueryRetrieveUser.RESPONSE_REFUSED_OUT_OF_RESOURCES_SUBOPERATIONS) {
                     logger.info("Command Response: REFUSED");
                     break;
                  }
                  else if ((status & 0xf000) == 0xC000) {
                     logger.info("Command Response: UNABLE_TO_PROCESS");
                     break;
                  }
                  else {
                     logger.severe("Command Response: Unknown response code: " + status);
                  }
               }
               else if (cmdField == 0x0001) { // C-STORE Req
                  logger.info("C-STORE Req: ");
                  logger.info(response.toString());
                  
                  DicomObject image = association.ReadMessage(-1);
                  qresult.add(image);
                  logger.info("Received image: " + image);
                  
                  int cstoremsgId = response.getVR("MessageID").getIntValue();
                  String instanceUID = response.getVR("AffectedSOPInstanceUID").getStringValue();
                  
                  DicomObject cstorersp = new DicomObject();
                  cstorersp.addVR(new VR("AffectedSOPClassUID", new DicomString(sopClass.toString())));
                  cstorersp.addVR(new VR("AffectedSOPInstanceUID", new DicomString(instanceUID)));
                  cstorersp.addVR(new VR("CommandField", new DicomNumber(0x8001))); // C-STORE RSP
                  cstorersp.addVR(new VR("MessageIDBeingRespondedTo", new DicomNumber(cstoremsgId)));
                  cstorersp.addVR(new VR("DataSetType", new DicomNumber(0x0101))); // No dataset
                  cstorersp.addVR(new VR("Status", new DicomNumber(0x0))); // Success
                  
                  logger.info("Sending C-STORE rsp: SUCCCESS");
                  association.WriteCommand(cstorersp, this.ctStoreContextId);

               }
               
            }
         }
         
      }
      else {
         logger.severe("SCU not active on this Association");
      }
      
      return qresult;
   }
   
   public Collection findPatients(PersonName pn) throws IOException {
      return findPatients(pn, "", null, "");
   }
   
   public Collection findPatients(PersonName pn, String patientID) throws IOException {
      return findPatients(pn, patientID, null, "");
   }
   
   public Collection findPatients(PersonName pn, String patientID, DicomDate patientBirthDate, String patientSex) throws IOException {
      
      DicomObject keys = new DicomObject();
      byte[] empty = new byte[0];
      keys.addVR(new VR("PatientsName", pn));
      keys.addVR(new VR("PatientID", new DicomString(patientID)));
      if (patientBirthDate != null) {
         keys.addVR(new VR("PatientsBirthDate", patientBirthDate ));
      }
      else {
         keys.addVR(new VR("PatientsBirthDate", empty ));
      }
      keys.addVR(new VR("PatientsSex", new DicomString(patientSex)));
      keys.addVR(new VR("NumberOfPatientRelatedStudies", empty));//new DicomString("*")));
      keys.addVR(new VR("NumberOfPatientRelatedSeries", empty));//new DicomString("*")));
      keys.addVR(new VR("NumberOfPatientRelatedInstances", empty));//new DicomString("*")));
      
      return doQuery("PATIENT", keys);
   }
   
   public Collection findStudies(String patientID) throws IOException {
      
      DicomObject keys = new DicomObject();
      byte[] empty = new byte[0];
      keys.addVR(new VR("PatientID", new DicomString(patientID)));
      keys.addVR(new VR("StudyInstanceUID", empty));
      keys.addVR(new VR("StudyID", empty));
      keys.addVR(new VR("StudyDate", empty));
      keys.addVR(new VR("StudyTime", empty));
      keys.addVR(new VR("AccessionNumber", empty));
      keys.addVR(new VR("NumberOfStudyRelatedSeries", empty));
      keys.addVR(new VR("NumberOfStudyRelatedInstances", empty));
      
      return doQuery("STUDY", keys);
   }
   
   public Collection findSeries(String studyUID) throws IOException {
      
      DicomObject keys = new DicomObject();
      byte[] empty = new byte[0];
      keys.addVR(new VR("StudyInstanceUID", new DicomString(studyUID)));
      keys.addVR(new VR("Modality", empty));
      keys.addVR(new VR("SeriesNumber", empty));
      keys.addVR(new VR("SeriesInstanceUID", empty));
      keys.addVR(new VR("NumberOfSeriesRelatedInstances", empty));
      
      return doQuery("SERIES", keys);
   }
   
   public Collection findInstances(String seriesUID) throws IOException {
      
      DicomObject keys = new DicomObject();
      byte[] empty = new byte[0];
      keys.addVR(new VR("SeriesInstanceUID", new DicomString(seriesUID)));
      keys.addVR(new VR("SOPInstanceUID", empty));
      keys.addVR(new VR("InstanceNumber", empty));
      
      return doQuery("IMAGE", keys);
   }
}
