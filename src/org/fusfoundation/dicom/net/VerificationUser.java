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
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomNumber;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author  jsnell
 */
public class VerificationUser implements ServiceClassUser {
   Association association = null;
   int   contextId = -1;
   
   static private final UID uids[] = {UID.Verification};
   static private final UID tsyn[] = {UID.ExplicitVRBigEndian, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian};
   static final Logger logger = org.apache.logging.log4j.LogManager.getLogger();

   /** Creates a new instance of VerificationUser */
    public VerificationUser() {
    }
    
    public boolean verify() throws IOException {
       if (isActive()) {
          DicomObject command = new DicomObject();
            command.addVR(new VR("AffectedSOPClassUID", new DicomString(UID.Verification.toString())));
            command.addVR(new VR("CommandField", new DicomNumber(0x0030)));
            command.addVR(new VR("DataSetType", new DicomNumber(0x0101)));
          
          logger.debug(command);
          
          int msgId = association.WriteCommand(command, contextId);
          DicomObject response = association.ReadCommand(contextId);
          
          logger.debug(response);
         
          if (response != null) {
             int status = response.getVR("Status").getIntValue();
             int recvMsgId = response.getVR("MessageIDBeingRespondedTo").getIntValue();
             int cmdField = response.getVR("CommandField").getIntValue();
             int dataSetType = response.getVR("DataSetType").getIntValue();
             
             if ( /* C-Echo response */ cmdField == 0x8030 &&
                  /* no dataset */ dataSetType == 0x0101 &&
                  /* SUCCESS */ status == 0 &&
                  /* msg ID matches */ recvMsgId == msgId) {
                logger.info("C-Echo successful");
                return true;
             }
          }
          
          return false;
          
       }
       return false;
    }

    public UID[] getSupportedTransferSyntaxes() {
       return tsyn;
    }
    
    public boolean isActive() {
       if (contextId > -1 && association != null) {
          return true;
       }
       else {
          logger.error("SCU not active on this Association");
          return false;
       }
    }
    
    public UID[] getSOPClassUserUIDs() {
       return uids;
    }
    
    public void setAssociation(Association assoc) {
       association = assoc;
    }
    
    public void setNegotiatedStatus(UID SOPClass, int role) {
    }
    
    public void setPresentationContextId(UID sopclass, int presentationContextId) {
       if (sopclass.equals(UID.Verification)) {
          contextId = presentationContextId;
       }
    }
    
}
