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
 * Title: Association
 * Description: Class representing a DICOM network association
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

import org.fusfoundation.dicom.VR;
import org.fusfoundation.dicom.VrOutputStream;
import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.DicomObjectReader;
import org.fusfoundation.dicom.VrInputStream;
import org.fusfoundation.dicom.DicomRuntimeException;
import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.DicomNumber;
import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Association {
   
   private HashMap presentationContexts = new HashMap();
   private HashMap serviceClassProviders = new HashMap();
   private HashMap serviceClassUsers = new HashMap();
   private HashMap presentationContextSyntax = new HashMap();
   
   protected String callingAETitle = "";
   protected String calledAETitle = "";
   
   private InputStream is;
   private OutputStream os;
   
   private int messageId=1;
   private long maxPDULength = 0;
   private static final Logger logger = Logger.getGlobal();

      
   public Association(InputStream istream, OutputStream ostream) throws IOException {
      is = istream;
      os = ostream;
   }
   
   public String getCalledAETitle() { return calledAETitle; }
   public String getCallingAETitle() { return callingAETitle; }
   
   public InputStream getInputStream() { return is; }
   public OutputStream getOutputStream() { return os; }
   
   public void setMaxPDULength(long len) { maxPDULength = len; }
   public long getMaxPDULength() { return maxPDULength; }
   
   public UID getTransferSyntax(int ctxID) {
      Integer key = new Integer(ctxID);
      if (presentationContextSyntax.containsKey(key)) {
         return (UID)presentationContextSyntax.get(new Integer(ctxID));
      }
      else {
         return null;
      }
   }
   
   public PresentationContext getPresentationContext(int ctxID) {
      Integer key = new Integer(ctxID);
      if (presentationContexts.containsKey(key)) {
         return (PresentationContext)presentationContexts.get(key);
      }
      else {
         return null;
      }
   }
   
   public Set getPresentationContextIDs() {
      return presentationContexts.keySet();
   }
   
   public AbstractSyntax getAbstractSyntax(int ctxID) {
      PresentationContext pc = getPresentationContext(ctxID);
      if (pc != null) {
         return pc.getAbstractSyntax();
      }
      else {
         return null;
      }
   }
   
   public boolean isLittleEndian(int ctxID) {
      UID tsyn = (UID)presentationContextSyntax.get(new Integer(ctxID));
      
      if (tsyn.equals(UID.ImplicitVRLittleEndian)) {
         return true;
      }
      else if (tsyn.equals(UID.ExplicitVRLittleEndian)) {
         return true;
      }
      else if (tsyn.equals(UID.ExplicitVRBigEndian)) {
         return false;
      }
      else if (tsyn.equals(UID.RLELossless)) {
         return true;
      }
      else if (tsyn.equals(UID.JPEGBaseline)) {
         return true;
      }
      else if (tsyn.equals(UID.JPEGLossless)) {
         return true;
      }
      else {
         throw new DicomRuntimeException("Unknown transfer syntax.");
      }
   }
   
   public boolean isImplicitVR(int ctxID) {
      UID tsyn = (UID)presentationContextSyntax.get(new Integer(ctxID));
      
      if (tsyn.equals(UID.ImplicitVRLittleEndian)) {
         return true;
      }
      else if (tsyn.equals(UID.ExplicitVRLittleEndian)) {
         return false;
      }
      else if (tsyn.equals(UID.ExplicitVRBigEndian)) {
         return false;
      }
      else if (tsyn.equals(UID.RLELossless)) {
         return false;
      }
      else if (tsyn.equals(UID.JPEGBaseline)) {
         return false;
      }
      else if (tsyn.equals(UID.JPEGLossless)) {
         return false;
      }
      else {
         throw new DicomRuntimeException("Unknown transfer syntax.");
      }
   }
   
   public void setContextProvider(PresentationContext pc, UID transferSyntax, ServiceClassProvider provider) {
      Integer key = new Integer(pc.getID());
      presentationContexts.put(key, pc);
      serviceClassProviders.put(key, provider);
      presentationContextSyntax.put(key, transferSyntax);
   }
   
   public void setContextUser(PresentationContext pc, UID transferSyntax, ServiceClassUser user) {
      Integer key = new Integer(pc.getID());
      presentationContexts.put(key, pc);
      serviceClassUsers.put(key, user);
      presentationContextSyntax.put(key, transferSyntax);
   }
   
   public ServiceClassUser getContextUser(int ctxID) {
      // Look up the registered ServiceClassUser for this presentation context id on this Association
      Integer key = new Integer(ctxID);
      if (serviceClassUsers.containsKey(key)) {
         return (ServiceClassUser)serviceClassUsers.get(key);
      }
      else {
         return null;
      }
   }
   
    public ServiceClassProvider getContextProvider(int ctxID) {
      // Look up the registered ServiceClassProvider for this presentation context id on this Association
      Integer key = new Integer(ctxID);
      if (serviceClassProviders.containsKey(key)) {
         return (ServiceClassProvider)serviceClassProviders.get(key);
      }
      else {
         return null;
      }
      
      // returning a new instance to prepare for multithreaded server later
      // probably could use an object pool here at some point
      /*
      try
      {
         return (ServiceClassProvider)scp.getClass().newInstance();
      }
      catch (IllegalAccessException e)
      {
          throw new DicomRuntimeException(e.toString());
      }
      catch (InstantiationException e)
      {
          throw new DicomRuntimeException(e.toString());
      }
       */
   }
   
   public void sendReleaseRq() throws IOException {
      PDU pdu = new PDU();
      pdu.setType(PDU.A_RELEASE_RQ);
      pdu.setBuffer(new byte[4]);
      pdu.writePDU(os);
   }
   
   public void sendReleaseRp() throws IOException {
      PDU pdu = new PDU();
      pdu.setType(PDU.A_RELEASE_RP);
      pdu.setBuffer(new byte[4]);
      pdu.writePDU(os);
      os.flush();
   }
   
   public void sendAbort(boolean amServiceProvider, int reason) throws IOException {
      byte payload[] = new byte[4];
      PDU pdu = new PDU();
      pdu.setType(PDU.A_ABORT);
      
      if (amServiceProvider)
         payload[2] = 2;
      else
         payload[2] = 0;
      
      payload[3] = (byte)(reason & 0xFF);
      
      pdu.setBuffer(payload);
      pdu.writePDU(os);
   }
   
   public void WriteMessage(DicomObject obj, int contextID) throws IOException {
      int maxPayload = (int)(this.maxPDULength - 6); //subract 6 bytes for length(4), context id (1) and msg header (1)
      int pdvlen = 0;
      byte[] itemlen = new byte[4];
      Iterator iter = obj.iterator();
      ByteArrayOutputStream bos = new ByteArrayOutputStream((int)this.maxPDULength);
      bos.write(itemlen);
      bos.write(contextID);
      bos.write(0x0); // message control header == Not last message fragment
      // create a VrOutputStream with the negotiated Transfer Syntax for this Association
      VrOutputStream vros = new VrOutputStream(bos, this.isLittleEndian(contextID), this.isImplicitVR(contextID));
      byte[] pdubuf = new byte[(int)this.maxPDULength]; // pdu buffer to reuse while writing out the object
      
      while (iter.hasNext() || bos.size() > maxPDULength) {
          
         if (iter.hasNext()) {
            VR vr = (VR)iter.next();
            vros.writeVR(vr);
         }
         
         // if the maximum PDU size has been exceeded, then write a PDU
         // and reset the buffer and VR streams, initializing with the overage
         if (bos.size() > maxPDULength) { // fix this later with real max PDU size
            logger.log(Level.INFO, "pdu buffer size = " + bos.size());
            
            // probably a better way to avoid copying the array here
            byte[] buf = bos.toByteArray();
            System.arraycopy(buf, 0, pdubuf, 0, (int)maxPDULength);
            PDU pdu = new PDU();
            pdu.setType(PDU.P_DATA_TF);
            pdvlen = pdubuf.length - 4;
            pdubuf[0] = (byte)((pdvlen & 0xff000000) >>> 24);
            pdubuf[1] = (byte)((pdvlen & 0xff0000) >>> 16);
            pdubuf[2] = (byte)((pdvlen & 0xff00) >>> 8);
            pdubuf[3] = (byte)(pdvlen & 0xff);
            pdubuf[4] = (byte)(contextID & 0xff);
            pdu.setBuffer(pdubuf);
            logger.log(Level.INFO, "message fragment length = " + pdvlen);
            logger.log(Level.INFO, "writing PDU - message fragment");
            pdu.writePDU(os);
            
            // reinitialize the output streams
            bos.reset();
            bos.write(itemlen);
            bos.write(contextID);
            bos.write(0x0); // message control header == Not last message fragment
            bos.write(buf, (int)maxPDULength, buf.length - (int)maxPDULength);
            vros = new VrOutputStream(bos, this.isLittleEndian(contextID), this.isImplicitVR(contextID));
         }
      }
      // all VR's have been processed, now write the final (or only) data PDU
      if (bos.size() > 0) {
         PDU pdu = new PDU();
         pdu.setType(PDU.P_DATA_TF);
         byte[] bosbuf = bos.toByteArray();
         pdvlen = bosbuf.length - 4;
         logger.log(Level.INFO, "final message fragment length = " + pdvlen);
         bosbuf[0] = (byte)((pdvlen & 0xff000000) >>> 24);
         bosbuf[1] = (byte)((pdvlen & 0xff0000) >>> 16);
         bosbuf[2] = (byte)((pdvlen & 0xff00) >>> 8);
         bosbuf[3] = (byte)(pdvlen & 0xff);
         bosbuf[4] = (byte)(contextID & 0xff);
         bosbuf[5] = (byte)(0x2);
         pdu.setBuffer(bosbuf);
         logger.log(Level.INFO, "writing PDU - final message fragment");
         pdu.writePDU(os);
      }
   }
   
   public int WriteCommand(DicomObject obj, int contextId) throws IOException {
      VR msgid = new VR("MessageID", new DicomNumber(this.messageId));
      obj.addVR(msgid);
      
      logger.log(Level.INFO, "Association: writing command:");
      logger.log(Level.INFO, obj.toString());
      
      Iterator iter = obj.iterator();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
      VrOutputStream vros = new VrOutputStream(bos);
      
      while (iter.hasNext()) {
         VR vr = (VR)iter.next();
         vros.writeVR(vr);
      }
      byte[] cmdbuf = bos.toByteArray();
      VR groupTag = new VR("CommandGroupLength", new DicomNumber(cmdbuf.length));
      
      // prepend the CommandGroupLength tag to the command elements in a new buffer
      bos.reset();
      vros.setDest(bos);
      vros.writeVR(groupTag);
      bos.write(cmdbuf);
      cmdbuf = bos.toByteArray();
      
      int cmdlen = cmdbuf.length;
      int bytesleft = cmdlen;
      int offset = 0;
      
      int maxPayload = (int)maxPDULength - 6; // (maxPDULength - length(4 bytes) -  presentationContext(1 byte) - header(1 byte)
      
      while (bytesleft > 0) {
         ByteArrayOutputStream fragmentBuf = new ByteArrayOutputStream(256);
         
         // write the message header
         ////////////////////////////////////
         
         // calc the PDU length
         int pduLength = Math.min(maxPayload, bytesleft);
         
         // write the length
         int pdvlen = pduLength+2;
         fragmentBuf.write((pdvlen & 0xff000000) >>> 24);
         fragmentBuf.write((pdvlen & 0xff0000) >>> 16);
         fragmentBuf.write((pdvlen & 0xff00) >>> 8);
         fragmentBuf.write( pdvlen & 0xff);
         
         // write the presentation context id
         fragmentBuf.write(contextId);
         
         // write the message control header (1 byte)
         if (bytesleft > maxPayload) {
            fragmentBuf.write(0x01); // Command, not last fragment
         }
         else {
            fragmentBuf.write(0x03); // Command, last fragment
         }
         
         // write the payload
         byte[] pdubuf = new byte[pduLength];
         System.arraycopy(cmdbuf, offset, pdubuf, 0, pduLength);
         fragmentBuf.write(pdubuf);
         
         // write the PDU
         PDU pdu = new PDU();
         pdu.setType(PDU.P_DATA_TF);
         pdu.setBuffer(fragmentBuf.toByteArray());
         pdu.writePDU(this.os);
         
         bytesleft -= pduLength;
         offset += pduLength;
      }
      
      return this.messageId++;
   }
     
   private InputStream currentPduStream = null;
   private PDU getNextPduItem() throws IOException {
      if (currentPduStream == null || currentPduStream.available() <= 0) {
         byte[] tmp = PDU.readPDU(this.is, PDU.P_DATA_TF);
         currentPduStream = new ByteArrayInputStream(tmp);
      }
      
      return PDU.readItem(currentPduStream);
   }
   
   public DicomObject ReadCommand(int contextId) throws IOException {
      ByteArrayOutputStream cmdBuffer = new ByteArrayOutputStream();
      
      while(true) {
         PDU p = getNextPduItem();
         
         byte itemBuf[] = p.getBuffer();
         
         int ctxID = itemBuf[0] & 0xff; // Context ID
         logger.log(Level.INFO, "Context ID: " + itemBuf[0]);
         
         // if contextId is <= 0 then treat as "don't care"
         // otherwise enforce the expectation of a particular
         // presentation context id
         if (contextId > 0 && ctxID != contextId) {
            logger.log(Level.INFO, "Unexpected presentation context");
            break;
         }
         
         if ((itemBuf[1] & 0x1) == 0x1) { // Command Fragment
            logger.log(Level.INFO, "Command");
            cmdBuffer.write(itemBuf, 2, itemBuf.length - 2);
            if ((itemBuf[1] & 0x2) == 0x2) {
               logger.log(Level.INFO, "Last command fragment");
               ByteArrayInputStream cmdbytes = new ByteArrayInputStream(cmdBuffer.toByteArray());
               DicomObjectReader ord = new DicomObjectReader(cmdbytes);
               DicomObject cmd = ord.read();
               return cmd;
            }
            else {
               //                logger.info("Not Last");
            }
         } // end if command fragment
         else {
            logger.log(Level.INFO, "Not a command, or erroneous message control header");
            break;
         }
         
         
      } // end while
      
      return null;
   }
   
   public DicomObject ReadMessage(int contextId) throws IOException {
      ByteArrayOutputStream msgBuffer = new ByteArrayOutputStream();
      
      while(true) {
         PDU p = getNextPduItem();
         
         byte itemBuf[] = p.getBuffer();
         
         int ctxID = itemBuf[0] & 0xff; // Context ID
         logger.log(Level.INFO, "Context ID: " + itemBuf[0]);
         
         // if contextId is <= 0 then treat as "don't care"
         // otherwise enforce the expectation of a particular
         // presentation context id
         if (contextId > 0 && ctxID != contextId) {
            logger.log(Level.SEVERE, "Unexpected presentation context");
            break;
         }
         
         logger.log(Level.INFO, "Message Control Header = " + (itemBuf[1] & 0xff));
         
         if ((itemBuf[1] & 0x1) == 0x0) { // Message Fragment
            logger.log(Level.INFO, "Command");
            msgBuffer.write(itemBuf, 2, itemBuf.length - 2);
            if ((itemBuf[1] & 0x2) == 0x2) {
               logger.log(Level.INFO, "Last message fragment");
               ByteArrayInputStream msgbytes = new ByteArrayInputStream(msgBuffer.toByteArray());
               DicomObjectReader ord = new DicomObjectReader(msgbytes);
               DicomObject msg = ord.read();
               return msg;
            }
            else {
               //                logger.info("Not Last");
            }
         } // end if command fragment
         else {
            logger.log(Level.SEVERE, "Not a message dataset, or erroneous message control header");
            break;
         }
         
      } // end while
      
      return null;
   }
   
   private class PDUBufferedPipedInputStream extends PipedInputStream {
      //subclass PipedInputStream with a buffer big enough to hold at least one PDU
      PDUBufferedPipedInputStream(PipedOutputStream p) throws IOException { super(p); buffer = new byte[32768];}
   }
   
   public void Receive() throws IOException {
     
      ByteArrayOutputStream cmdBuffer = new ByteArrayOutputStream();
      //PipedOutputStream tpos = new PipedOutputStream();
      
      CircularByteBuffer circBuffer = new CircularByteBuffer(32768 * 32); // TODO: Reasonable sized buffer? Needs to be auto-resizing?
      circBuffer.clear();
      
      OutputStream pos = circBuffer.getOutputStream(); //new BufferedOutputStream(tpos);
      InputStream pis  = circBuffer.getInputStream(); //new BufferedInputStream(new PDUBufferedPipedInputStream(tpos));
      
      
      //Pipe threadPipe = Pipe.open();
      
     Thread providerThread = null;

      while(true) {
         
         logger.log(Level.INFO, "reading pdu...");
         PDU pdu = PDU.readPDU(is);
         logger.log(Level.INFO, "PDU size: " + pdu.size());
         byte tmp[] = pdu.getBuffer();
         
         if (pdu.getType() == PDU.A_ABORT) {
            //sendReleaseRp();
            logger.log(Level.WARNING, "Got A-ABORT");
            return;
         }
         else if (pdu.getType() == PDU.A_RELEASE_RQ) {
            sendReleaseRp();
            logger.info("Got association release req");
            return;
         }
         else if (pdu.getType() == PDU.A_RELEASE_RP) {
            logger.info("Got association release rep");
            return;
         }
         else if (pdu.getType() == PDU.P_DATA_TF) {
            
            DataInputStream pdvBuf = new DataInputStream(new ByteArrayInputStream(tmp));
            
            while (pdvBuf.available() > 0) {
               PDU p = PDU.readItem(pdvBuf);
               
               byte itemBuf[] = p.getBuffer();
               
               int ctxID = itemBuf[0] & 0xff; // Context ID
               logger.log(Level.INFO, "Context ID: " + itemBuf[0]);
               
               if ((itemBuf[1] & 0x1) == 0x1) { // Command Fragment
                  logger.log(Level.INFO, "Command");
                  cmdBuffer.write(itemBuf, 2, itemBuf.length - 2);
                  if ((itemBuf[1] & 0x2) == 0x2) {
                     logger.log(Level.INFO, "Last");
                     logger.log(Level.INFO, "Sending stream to the service.");
                     ByteArrayInputStream cmdbytes = new ByteArrayInputStream(cmdBuffer.toByteArray());
                     DicomObjectReader ord = new DicomObjectReader(cmdbytes);
                     DicomObject cmd = ord.read();
                     ServiceClassProvider provider = this.getContextProvider((int)itemBuf[0]);
                     if (provider != null)
                        providerThread = provider.ProcessCommand(this, ctxID, cmd, new VrInputStream(pis, this.isLittleEndian(ctxID), this.isImplicitVR(ctxID)));
                     
                     // Reset the command accumulation buffer
                     cmdBuffer.reset();
                  }
                  else {
                     //                logger.info("Not Last");
                  }
               }
               else { // Message Fragment
                  logger.log(Level.INFO, "Message fragment - " + itemBuf.length);
                  try {
                     logger.log(Level.INFO, "writing fragment to service");
                     pos.write(itemBuf, 2, itemBuf.length - 2);
                     logger.log(Level.INFO, "done writing");
                  }
                  catch (Exception e) {
                     System.out.println(e);
                  }
                  
                  if ((itemBuf[1] & 0x2) == 0x2) {
                     logger.log(Level.INFO, "Last");
                     logger.log(Level.INFO, "Closing message stream.");
                     pos.close();
               //      tpos = new PipedOutputStream();
               //      pos = new BufferedOutputStream(tpos);
               //      pis = new BufferedInputStream(new PDUBufferedPipedInputStream(tpos));
                     try {
                        if (providerThread != null) {
                           providerThread.join();
                        }
                     }
                     catch (InterruptedException e) {
                     }
                     providerThread = null;
                     circBuffer.clear();
                    
                  }
                  else {
                     //                logger.info("Not Last");
                  }
               }
            }
         }
         else { // Some unknown or unexpected PDU type
            sendAbort(true, 0);
            logger.log(Level.WARNING, "Some unknown or unexpected PDU type: [" + pdu.getType() + "] Dropping Association.");
            return;
         }
      } // end outer while
   }
   
}
