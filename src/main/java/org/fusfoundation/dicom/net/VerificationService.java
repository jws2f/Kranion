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
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import org.apache.logging.log4j.Logger;

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



/** This class implements the C-ECHO service provider.
 */

public class VerificationService
  extends ServiceClassProviderBaseImpl {

  static private final UID uids[] = {UID.Verification};
  static private final UID tsyn[] = {UID.ExplicitVRBigEndian, UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian};
  static final Logger logger = org.apache.logging.log4j.LogManager.getLogger();

  /** Default constuctor
   */  
  public VerificationService() {
  }
    
  // ServiceClassProvider interface methods
  /////////////////////////////////////////////////
  /** Get the list of SOP classes this service implements.
   * @return Returns an array of strings containing the implemented SOP class UID's.
   */  
  public UID[] getSOPClassProviderUIDs()
  {
    return uids;
  }
  
  public UID[] getSupportedTransferSyntaxes() {
      return tsyn;
  }  

  /** Background thread procedure.
   */  
  public void handleCommand(Association assoc, int PresentationContextID, DicomObject cmd, VrReader messageStream) {
      UID sopClass = new UID(cmd.getVR("AffectedSOPClassUID").getStringValue());
      if (sopClass.equals(UID.Verification)) {
          logger.info("Received C-ECHO Command");
          try {
              DicomObject response = new DicomObject();
              ByteArrayOutputStream bos1 = new ByteArrayOutputStream(256);
              VrOutputStream vos1 = new VrOutputStream(bos1);

              int msgID = cmd.getVR("MessageID").getIntValue();

              response.addVR(new VR("AffectedSOPClassUID", new DicomString("1.2.840.10008.1.1")));
              response.addVR(new VR("CommandField", new DicomNumber(0x8030)));
              response.addVR(new VR("MessageIDBeingRespondedTo", new DicomNumber(msgID)));
              response.addVR(new VR("DataSetType", new DicomNumber(0x0101)));
              response.addVR(new VR("Status", new DicomNumber(0x0)));

              if (logger.isDebugEnabled()) logger.debug("Response:\n" + response);

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
          catch(IOException e)
          {
              throw new DicomRuntimeException(e.toString());
          }
      
      }
      else
      {
          throw new DicomRuntimeException("Unrecognized SOP Class");
      }
   }
  
}

