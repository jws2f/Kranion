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
import org.fusfoundation.dicom.DicomException;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomNumber;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author  jsnell
 */
public class StorageUser implements ServiceClassUser {
   
   Association association = null;
   int   mrStoreContextId = -1;
   int   ctStoreContextId = -1;
   
   static private final UID uids[] = {UID.MRImageStorage, UID.CTImageStorage};
   static private final UID tsyn[] = {UID.ExplicitVRBigEndian, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian};
   static final private Logger logger = Logger.getGlobal();

   
   static public int STATUS_SUCCESS = 0x0000;
   static public int STATUS_WARNING = 0xB000;
   
   /** Creates a new instance of StorageUser */
   public StorageUser() {
   }
   
   public boolean storeObject(DicomObject obj) throws IOException, DicomException {
      if (isActive()) {
         int cntxid = -1;
         String affectedSopClassUID, affectedSopInstanceUID;
         
         try {
            affectedSopClassUID = obj.getVR("SOPClassUID").getStringValue();
            if (affectedSopClassUID.compareTo(UID.MRImageStorage.toString()) == 0) {
               cntxid = mrStoreContextId;
            }
            else if (affectedSopClassUID.compareTo(UID.CTImageStorage.toString()) == 0) {
               cntxid = ctStoreContextId;
            }
            else {
               throw new DicomException("Current association does not support the objects SOPClass: " + affectedSopClassUID);
            }
            
            affectedSopInstanceUID = obj.getVR("SOPInstanceUID").getStringValue();
         }
         catch(Exception e) {
            e.printStackTrace();
            return false;
         }
         
         // Build the Command object
         DicomObject command = new DicomObject();
         command.addVR(new VR("CommandField", new DicomNumber(0x0001))); // C-Store command
         command.addVR(new VR("AffectedSOPClassUID", new DicomString(affectedSopClassUID)));
         command.addVR(new VR("Priority", new DicomNumber(0))); // Priority = MEDIUM
         command.addVR(new VR("DataSetType", new DicomNumber(0xFEFE))); // dataset follows
         command.addVR(new VR("AffectedSOPInstanceUID", new DicomString(affectedSopInstanceUID)));
         
         logger.info("Send C-Store command...");
         logger.info(command.toString());
         // Send the command followed by the image dataset
         int msgId = association.WriteCommand(command, cntxid);
         logger.info("Send C-Store message " + msgId + "...");
         association.WriteMessage(obj, cntxid);
         
         logger.info("Read respone...");
         // Get the response
         DicomObject response = association.ReadCommand(cntxid);
         if (response != null) {
            int status = response.getVR("Status").getIntValue();
            int recvMsgId = response.getVR("MessageIDBeingRespondedTo").getIntValue();
            int cmdField = response.getVR("CommandField").getIntValue();
            int dataSetType = response.getVR("DataSetType").getIntValue();
            
            if (cmdField == 0x8001  &&
            msgId == recvMsgId &&
            dataSetType == 0x101 &&
            (status == STATUS_SUCCESS || (status & 0xF000) == STATUS_WARNING) ) {
               return true;
            }
            else {
               logger.severe("C-Store error:");
               logger.severe(response.toString());
            }
         }
         else {
            logger.severe("Got a null response");
            return false;
         }
         
      }
      
      logger.severe("Service User is inactive");
      return false;
   }
   
   public void setPresentationContextId(UID sopclass, int contextId) {
      if (sopclass.equals(UID.MRImageStorage)) {
         mrStoreContextId = contextId;
      }
      else if (sopclass.equals(UID.CTImageStorage)) {
         ctStoreContextId = contextId;
      }
   }
   
   public UID[] getSupportedTransferSyntaxes() {
      return tsyn;
   }
   
   public boolean isActive() {
      if ((mrStoreContextId > -1 || ctStoreContextId > -1) && association != null) {
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
   
   public void setAssociation(Association assoc) {
      association = assoc;
   }
   
}
