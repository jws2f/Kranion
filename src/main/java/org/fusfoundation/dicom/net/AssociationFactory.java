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

/**
 * Title: AssociationFactory
 * Description: Class to create Associations
 * Copyright:    Copyright (c) 2002
 * Company: Univerity of Virginia
 * @author John W. Snell
 * @version 1.0
 */

import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.PersonName;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AssociationFactory {
   private HashMap serviceClassProviders;
   private HashMap serviceClassUsers;
   
   static final private Logger logger = Logger.getGlobal();;
   
   public AssociationFactory() {
      serviceClassProviders = new HashMap();
      serviceClassUsers = new HashMap();
   }
   
   public void AddProvider(ServiceClassProvider scp) {
      UID uids[] = scp.getSOPClassProviderUIDs();
      for (int i=0; i<uids.length; i++) {
         serviceClassProviders.put(uids[i], scp);
      }
   }
   
   public void AddUser(ServiceClassUser scu) {
      UID uids[] = scu.getSOPClassUserUIDs();
      for (int i=0; i<uids.length; i++) {
         serviceClassUsers.put(uids[i], scu);
      }
      if (scu instanceof ServiceClassProvider) {
         ServiceClassProvider scp = (ServiceClassProvider)scu;
         uids = scp.getSOPClassProviderUIDs();
         for (int i=0; i<uids.length; i++) {
            serviceClassUsers.put(uids[i], scu);
         }
      }
   }
   
   public Association CreateUserAssociation(String callingAETitle, String calledAETitle, InputStream istream, OutputStream ostream) throws IOException {
      Association assoc = null;
      AssociationRequest req = new AssociationRequest();
      
      try {
         assoc = new Association(istream, ostream);
         req.setCallingAETitle(callingAETitle);
         req.setCalledAETitle(calledAETitle);
         
         // fill in the association request info
         Set keySet = serviceClassUsers.keySet();
         Iterator iter = keySet.iterator();
         int pcid = 1;
         while (iter.hasNext()) {
            UID sopclass = (UID)iter.next();
            AbstractSyntax as = new AbstractSyntax(sopclass.toString());
            
            ServiceClassUser scu = (ServiceClassUser)serviceClassUsers.get(sopclass);
            scu.setPresentationContextId(sopclass, pcid);
            UID[] supportedTransferSyntaxes = scu.getSupportedTransferSyntaxes();
            for (int i=0; i< supportedTransferSyntaxes.length; i++) {
               as.AddTransferSyntax(supportedTransferSyntaxes[i].toString());
            }
            PresentationContext pc = new PresentationContext(pcid, as);
            req.addPresentationContext(pc);
            pcid += 2;
         }
         
         req.writeRequest(ostream);
         req.readResponse(istream);
         
         assoc.calledAETitle = req.getCalledAETitle();
         assoc.callingAETitle = req.getCallingAETitle();
         assoc.setMaxPDULength(req.getMaxPDULength());
         
         logger.log(Level.INFO, "Called AET: " + req.getCalledAETitle());
         logger.log(Level.INFO, "Calling AET: " + req.getCallingAETitle());
         logger.log(Level.INFO, "Max PDU length: " + req.getMaxPDULength());
         
         Collection pcs = req.getPresentationContexts();
         iter = pcs.iterator();
         while(iter.hasNext()) {
            PresentationContext pc = (PresentationContext)(iter.next());
            logger.log(Level.INFO, "Negotiatiated: " + pc.getID() + " result = " + pc.getResult());
            if (pc.getResult() == 0) {
               UID uid = pc.getAbstractSyntax().getUID();
               ServiceClassUser scu = (ServiceClassUser)serviceClassUsers.get(uid);
               UID supportedTxs[] = scu.getSupportedTransferSyntaxes();
               assoc.setContextUser(pc, supportedTxs[pc.getSelectedTransferSyntaxIndex()], scu);
               scu.setAssociation(assoc);
            }
         }
      }
      catch (IOException e) {
         logger.log(Level.SEVERE, e.toString());
         return null;
      }
            
      return assoc;
   }
   
   public Association CreateProviderAssociation(InputStream istream, OutputStream ostream) throws IOException {
      
      try {
         Association assoc = new Association(istream, ostream);
         AssociationRequest req = new AssociationRequest();
         req.readRequest(istream);
         
         assoc.calledAETitle = req.getCalledAETitle();
         assoc.callingAETitle = req.getCallingAETitle();
         
         Iterator p = req.getPresentationContexts().iterator();
         while (p.hasNext()) {
            PresentationContext pc = (PresentationContext)p.next();
            //System.out.println(pc.getID());
            
            UID uid = pc.getAbstractSyntax().getUID();
            
            if (serviceClassProviders.containsKey(uid)) { // Lookup provider
               ServiceClassProvider scp = (ServiceClassProvider)serviceClassProviders.get(uid);
               ArrayList txs = pc.getAbstractSyntax().getTransferSyntaxes();
               UID supportedTxs[] = scp.getSupportedTransferSyntaxes();
               for (int i=0; i<supportedTxs.length; i++) { // examine the PC for support of our transfer syntaxes
                  if (txs.contains(supportedTxs[i])) {
                     // accept the context
                     pc.setResult(0); // Accepted
                     // select the transfer syntax for the context
                     pc.setSelectedTransferSyntaxIndex(txs.indexOf(supportedTxs[i]));
                     // map the provder to this context in the new association
                     assoc.setContextProvider(pc, supportedTxs[i], (ServiceClassProvider)serviceClassProviders.get(uid));
                     break;
                  }
               }
            }
         }
         // send the results back to the association requestor
         req.writeResponse(ostream);
         
         assoc.setMaxPDULength(req.getMaxPDULength());
         
         // return the new association
         return assoc;
      }
      catch(IOException e) {
         logger.log(Level.SEVERE, e.toString());
         return null;
      }
   }
   
   public static void main(String[] args) {

      ServerSocket theServer;
      Socket theConnection;
      DataInputStream is;
      PrintStream ps;
      
      // Init log4j to the console
      // Really should configure logging with property file, but this is just a debug tool for now
//      BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%-5p [%t]:%c: %m%n")));
      //BasicConfigurator.disableDebug();
      
      VerificationUser verifyClient = new VerificationUser();
      QueryRetrieveUser qrClient = new QueryRetrieveUser();
      
      if (true || args.length >= 1 && args[0].compareTo("client") == 0) {
         AssociationFactory caf = new AssociationFactory();
         caf.AddUser(verifyClient);
         caf.AddUser(qrClient);
         
         try {
            theConnection = new Socket("localhost", 5104);
            Association cassoc = caf.CreateUserAssociation("StorageSCU", "AETitle", theConnection.getInputStream(), theConnection.getOutputStream());
            if (cassoc != null) {
               if (verifyClient.verify()) {
                  PersonName pn = new PersonName("*", "*", "", "", "");
                  Collection result = qrClient.findPatients(pn);
                  Iterator iter = result.iterator();
                  while (iter.hasNext()) {
                     DicomObject record = (DicomObject)iter.next();
                     System.out.println( record.getVR("PatientsName").getValue() + " - ID:" + record.getVR("PatientID").getValue()  + " DOB: " + record.getVR("PatientsBirthDate").getValue());
                     
                     String patientID = record.getVR("PatientID").getValue().toString();
                     
        //             qrClient.movePatient(patientID, "REMOTE");
                     qrClient.getPatient(patientID);
                  
                     Collection studies = qrClient.findStudies(patientID);
                     Iterator studyIter = studies.iterator();
                     while (studyIter.hasNext()) {
                        DicomObject studyrec = (DicomObject)studyIter.next();
                        Iterator studykeys = studyrec.iterator();
                        while (studykeys.hasNext()) {
                           VR key = (VR)studykeys.next();
                            System.out.println("   " + key.getName() + " = [" + key.getValue() + "]");
                        }
                        
                        String studyID = studyrec.getVR("StudyInstanceUID").getValue().toString();
                        
              //          qrClient.moveStudy(studyID, "REMOTE");
                        
                        Collection series = qrClient.findSeries(studyID);
                        Iterator seriesIter = series.iterator();
                        while (seriesIter.hasNext()) {
                           DicomObject seriesrec = (DicomObject)seriesIter.next();
                           Iterator serieskeys = seriesrec.iterator();
                           while (serieskeys.hasNext()) {
                              VR serkey = (VR)serieskeys.next();
                              System.out.println("      " + serkey.getName() + " = [" + serkey.getValue() + "]");
                           }
                           
                           String seriesID = seriesrec.getVR("SeriesInstanceUID").getValue().toString();
                           
                   //        qrClient.moveSeries(seriesID, "REMOTE");
                           
                           Collection imagesr = qrClient.findInstances(seriesID);
                           Iterator imageIter = imagesr.iterator();
                           while (imageIter.hasNext()) {
                              DicomObject imagerec = (DicomObject)imageIter.next();
                 //             Iterator items = imagerec.iterator();
                 //             while (items.hasNext()) {
                 //                VR item = (VR)items.next();
                 //                System.out.println("         " + item.getName() + " = [" + item.getValue() + "]");
                 //             }
                              
                              String instanceID = imagerec.getVR("SOPInstanceUID").getValue().toString();
                  //            qrClient.moveInstance(instanceID, "REMOTE");
                              
                 //               DicomObject image = qrClient.getInstance(instanceID);
                 //               logger.info("Image Received: " + image.getVR("PatientsName").getValue() + " - " + image.getVR("Modality").getStringValue() + " - " + image.getVR("StudyID").getValue() + " - " + image.getVR("SeriesNumber").getIntValue() + " - " + image.getVR("InstanceNumber").getIntValue());
                           }
                        }
                        
                        
                     }
                    
                  }
               }
               
               cassoc.sendReleaseRq();
               cassoc.Receive();
            }
         }
         catch (UnknownHostException e) {
            logger.log(Level.SEVERE, e.toString());
         }
         catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
         }
         
         return;
      }
      
      AssociationFactory af = new AssociationFactory();
      af.AddProvider(new VerificationService());
//      af.AddProvider(new StorageService());
      
      try {
         theServer = new ServerSocket(51050);
         while (true) {
            try {
               theConnection = theServer.accept();
               Association assoc = af.CreateProviderAssociation(theConnection.getInputStream(), theConnection.getOutputStream());
               if (assoc != null) {
                  assoc.Receive();
               }
               theConnection.close();
               // temporary for now
               //break;
            }
            catch (IOException e) {
               logger.log(Level.WARNING, "Unexpected exception thrown: " + e);
            }
         }
      }
      catch (IOException e) {
         logger.log(Level.SEVERE, "Failed to create ServerSocket: " + e);
      }
      finally {
         logger.log(Level.INFO, "Server terminating.");
      }
   }
}
