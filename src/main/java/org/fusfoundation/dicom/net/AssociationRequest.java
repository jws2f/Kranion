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

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.dicom.UID;

/**
 * Title: AssociationRequest
 * Description: Class to represent a DICOM Association Request
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

public class AssociationRequest {
   
   private String callingAETitle = "";
   private String calledAETitle = "";
   private String appContextName = UID.DicomAppContext.toString();
   private String callingImplementationUID = "";
   private String callingImplementationVersionName = "";
   private ArrayList presentationContexts;
   private long maxPDULength = 32768;
   
   static final private Logger logger = Logger.getGlobal();;

   
   public AssociationRequest() {
      maxPDULength = 32768;
      presentationContexts = new ArrayList();
   }
   
   public void addPresentationContext(PresentationContext pc) { presentationContexts.add(pc); }
   public Collection getPresentationContexts() { return presentationContexts; }
   public String getCallingAETitle() { return callingAETitle; }
   public void setCallingAETitle(String val) { callingAETitle = val; }
   public String getCalledAETitle() { return calledAETitle; }
   public void setCalledAETitle(String val) { calledAETitle = val; }
   public String getAppContextName() { return appContextName; }
   public String callingImplementationVersionName() { return callingImplementationVersionName; }
   public long getMaxPDULength() { return maxPDULength; }
   
   private static class ScpScuRoleSelectionItem {
      protected static final int typeID = 0x0054;
      protected String sopClassUid;
      protected int scuRole;
      protected int scpRole;
      
      protected ScpScuRoleSelectionItem() {
         sopClassUid = "";
         scuRole = -1;
         scpRole = -1;
      }
      
      protected ScpScuRoleSelectionItem(String sopclassuid, int scurole, int scprole) {
         sopClassUid = sopclassuid;
         scuRole = scurole;
         scpRole = scprole;
      }
      
      protected ScpScuRoleSelectionItem(byte[] data) {
         int length = (data[0] & 0xFF) << 8;
         length |= (data[1] & 0xff);
         try {
            sopClassUid = new String(data, 2, length, "8859_1");
         } catch (UnsupportedEncodingException e) {}
         scuRole = data[2+length];
         scpRole = data[3+length];
      }
      
      protected byte[] getData() {
         byte[] data = new byte[sopClassUid.length() + 4];
         int length = sopClassUid.length();
         data[0] = (byte)((length >>> 8)  & 0xFF);
         data[1] = (byte)(length & 0xFF);
         byte[] uidBytes = null;
         try {
            uidBytes = sopClassUid.getBytes("8859_1");
         } catch (UnsupportedEncodingException e) {}
         System.arraycopy(uidBytes, 0, data, 2, length);
         data[2+length] = (byte)scuRole;
         data[3+length] = (byte)scpRole;
         return data;
      }
   }
   
   public void writeRequest(OutputStream ostream) throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(bos);
      
      // A-ASSOC PDU
      ////////////////////////////////////
      
      // write the protocol version
      dos.writeByte(0);
      dos.writeByte(0x1);
      
      // reserved bytes
      dos.writeByte(0);
      dos.writeByte(0);
      
      // AETitles
      byte buf[] = new byte[16];
      System.arraycopy(calledAETitle.getBytes("8859_1"), 0, buf, 0, Math.min(16, calledAETitle.length()));
      dos.write(buf);
      buf = new byte[16];
      System.arraycopy(callingAETitle.getBytes("8859_1"), 0, buf, 0, Math.min(16, callingAETitle.length()));
      dos.write(buf);
      
      // reserved bytes
      buf = new byte[32];
      dos.write(buf);
      
      // Application Context Item
      /////////////////////////////////////
      PDU appContextPDUItem = new PDU();
      appContextPDUItem.setType(0x10);
      appContextPDUItem.setBuffer(UID.DicomAppContext.toString().getBytes("8859_1"));
      appContextPDUItem.writeItem(dos);
      
      // Presentation Context Items
      /////////////////////////////////////
      Iterator iter = presentationContexts.iterator();
      while (iter.hasNext()) {
         PDU pcItem = new PDU();
         pcItem.setType(0x20);
         PresentationContext pc = (PresentationContext)iter.next();
         ByteArrayOutputStream pcbuf = new ByteArrayOutputStream();
         DataOutputStream pcdos = new DataOutputStream(pcbuf);
         
         pcdos.write(pc.getID());
         pcdos.write(0);
         pcdos.write(0); // reserved for Reason code in A-ASSOC-AC
         pcdos.write(0);
         
         // Abstract Syntax Item
         /////////////////////////////
         PDU asItem = new PDU();
         asItem.setType(0x30);
         asItem.setBuffer(pc.getAbstractSyntax().getUID().toString().getBytes("8859_1"));
         asItem.writeItem(pcdos);
         
         // Tranfer Syntax Item(s)
         ////////////////////////////
         Iterator txs = pc.getAbstractSyntax().getTransferSyntaxes().iterator();
         while(txs.hasNext()) {
            UID txUID = (UID)txs.next();
            PDU txItem = new PDU();
            txItem.setType(0x40);
            txItem.setBuffer(txUID.toString().getBytes("8859_1"));
            txItem.writeItem(pcdos);
         }
         
         pcItem.setBuffer(pcbuf.toByteArray());
         pcItem.writeItem(dos);
      }
      
      // User Info
      /////////////////////////////////////
      
      PDU userInfoItem = new PDU();
      userInfoItem.setType(0x50);
      ByteArrayOutputStream uibos = new ByteArrayOutputStream();
      DataOutputStream uidos = new DataOutputStream(uibos);
      
      PDU maxLengthItem = new PDU();
      maxLengthItem.setType(0x51);
      byte[] maxbuf = new byte[4];
      maxbuf[0] = (byte)((int)(maxPDULength >>> 24) & 0xff);
      maxbuf[1] = (byte)((int)(maxPDULength >>> 16) & 0xff);
      maxbuf[2] = (byte)((int)(maxPDULength >>> 8) & 0xff);
      maxbuf[3] = (byte)((int)(maxPDULength) & 0xff);
      maxLengthItem.setBuffer(maxbuf);
      maxLengthItem.writeItem(uidos);
      
      PDU implUIDItem = new PDU();
      implUIDItem.setType(0x52);
      implUIDItem.setBuffer(UID.ImplementationClass.toString().getBytes("8859_1"));
      implUIDItem.writeItem(uidos);
      
      // Scp/Scu Role Negotiation Items
      Iterator pciter = presentationContexts.iterator();
      while (pciter.hasNext()) {
         PresentationContext pc = (PresentationContext)pciter.next();
         PDU pcRoleItem = new PDU();
         pcRoleItem.setType(0x54);
         ScpScuRoleSelectionItem roleSel = new AssociationRequest.ScpScuRoleSelectionItem(pc.getAbstractSyntax().getUID().toString(), 1, 1);
         pcRoleItem.setBuffer(roleSel.getData());
         pcRoleItem.writeItem(uidos);
      }

      userInfoItem.setBuffer(uibos.toByteArray());
      userInfoItem.writeItem(dos);
      
      
      // Now write the whole A-ASSOC PDU
      PDU aassocpdu = new PDU();
      aassocpdu.setType(0x1);
      aassocpdu.setBuffer(bos.toByteArray());
      aassocpdu.writePDU(ostream);
      
      
      
   }
   
   public void readRequest(InputStream istream) throws IOException {
      // read and process an ASSOC-RQ
      byte buf[] = PDU.readPDU(istream, 0x1);
      DataInputStream bis = new DataInputStream(new ByteArrayInputStream(buf));
      
      // check protocol version
      int protVer = bis.readByte();
      protVer = protVer << 8 | bis.readByte();
      if (protVer != 0x1)
         throw new IOException("Wrong protocol version: " + protVer);
      
      bis.readByte();
      bis.readByte();
      
      byte[] tmp1 = new byte[16];
      bis.read(tmp1);
      calledAETitle = PDU.byteArrayToString(tmp1);
      bis.read(tmp1);
      callingAETitle = PDU.byteArrayToString(tmp1);
      
      for (int i=0; i<32; i++)
         bis.readByte();
      
      logger.info("Calling AE: " + callingAETitle);
      logger.info("Called AE: " + calledAETitle);
      
      appContextName = PDU.byteArrayToString(PDU.readItem(bis, 0x10));
      logger.info("Application Context: " + appContextName);
      
      PDU pdu;
      while (bis.available() > 0) {
         pdu = PDU.readItem(bis);
         // Read Presentation Contexts
         if (pdu.getType() == 0x20) {
            DataInputStream pctxt = pdu.getInputStream();
            
            //Read Presentation Context ID (should be odd number between 0 - 255
            int presentationContext = pctxt.readByte();
            pctxt.skipBytes(3);
            
            //Read Abstract Syntax UID
            String abstractSyntax = PDU.byteArrayToString(PDU.readItem(pctxt, 0x30));
            AbstractSyntax as = new AbstractSyntax(abstractSyntax);
            
            // Read 1 or more Transfer Syntax
            while (pctxt.available() > 0) {
               String tranSyntax = PDU.byteArrayToString(PDU.readItem(pctxt, 0x40));
               as.AddTransferSyntax(tranSyntax);
            }
            
            // Save the list of Presentation Contexts
            presentationContexts.add(new PresentationContext(presentationContext, as));
         }
         // Read user data items
         else if (pdu.getType() == 0x50) {
            PDU userDataSubItem = PDU.readItem(pdu.getInputStream());
            logger.info("" + userDataSubItem.getType());
            
            if (userDataSubItem.getType() == 0x51) { // maximum length item
               byte tmp[] = userDataSubItem.getBuffer();
               
               long maxLen = tmp[0] & 0xff;
               maxLen = maxLen << 8 | tmp[1] & 0xff;
               maxLen = maxLen << 8 | tmp[2] & 0xff;
               maxLen = maxLen << 8 | tmp[3] & 0xff;
               
               maxPDULength = Math.min(maxLen, maxPDULength);
               
               logger.info("Maximum length = " + maxPDULength);
            }
            else if (userDataSubItem.getType() == 0x52) { // Calling Implementation UID
               byte tmp[] = userDataSubItem.getBuffer();
               
               callingImplementationUID = PDU.byteArrayToString(tmp);
               logger.info("Implementation class UID = " + callingImplementationUID);
            }
            else if (userDataSubItem.getType() == 0x55) { // Calling Implmentation Version Name
               byte tmp[] = userDataSubItem.getBuffer();
               
               callingImplementationVersionName = PDU.byteArrayToString(tmp);
               logger.info("Implementation version name = " + callingImplementationVersionName);
            }
         }
         else {
            throw new IOException("Association protocol error");
         }
      }
   }
   
   void readResponse(InputStream is) throws IOException {
      PDU responsePdu = PDU.readPDU(is);
      
      DataInputStream bis = null;
      if (responsePdu.getType() == 0x2) {
         bis = new DataInputStream(new ByteArrayInputStream(responsePdu.getBuffer()));
      }
      else if (responsePdu.getType() == 0x3){
         logger.info("Association rejected.");
         throw new IOException("Association rejected.");
      }
      else {
         logger.log(Level.SEVERE, "Unexpected response from remote AE");
         throw new IOException("Unexpected response from remote AE");
      }
      
      // check protocol version
      int protVer = bis.readByte();
      protVer = protVer << 8 | bis.readByte();
      if (protVer != 0x1)
         throw new IOException("Wrong protocol version: " + protVer);
      
      bis.readByte();
      bis.readByte();
      
      byte[] tmp1 = new byte[16];
      bis.read(tmp1);
      calledAETitle = PDU.byteArrayToString(tmp1);
      bis.read(tmp1);
      callingAETitle = PDU.byteArrayToString(tmp1);
      
      for (int i=0; i<32; i++)
         bis.readByte();
      
      logger.info("Calling AE: " + callingAETitle);
      logger.info("Called AE: " + calledAETitle);
      
      appContextName = PDU.byteArrayToString(PDU.readItem(bis, 0x10));
      logger.info("Application Context: " + appContextName);
      
      PDU pdu;
      while (bis.available() > 0) {
         pdu = PDU.readItem(bis);
         // Read Presentation Contexts
         logger.log(Level.INFO, "PDU type = " + pdu.getType());
         if (pdu.getType() == 0x21) {
            DataInputStream pctxt = pdu.getInputStream();
            
            //Read Presentation Context ID (should be odd number between 0 - 255
            int presentationContext = pctxt.readByte();
            PresentationContext pc = (PresentationContext)presentationContexts.get((presentationContext-1)/2);
            logger.info("PC = " + presentationContext + " - " + pc.getAbstractSyntax().getUID());
            pctxt.skipBytes(1);
            int result = pctxt.read();
            pc.setResult(result);
            logger.info("  result = " + result);
            pctxt.skipBytes(1);
            
            //Read Abstract Syntax UID
            //        String abstractSyntax = PDU.byteArrayToString(PDU.readItem(pctxt, 0x30));
            //        AbstractSyntax as = new AbstractSyntax(abstractSyntax);
            
            // Read 1 or more Transfer Syntax
            while (pctxt.available() > 0) {
               String tranSyntax = PDU.byteArrayToString(PDU.readItem(pctxt, 0x40));
               logger.info("  Transfer Syntax UID = " + tranSyntax);
               //          as.AddTransferSyntax(tranSyntax);
               UID tsyn = new UID(tranSyntax);
               ArrayList transferSyntaxes = pc.getAbstractSyntax().getTransferSyntaxes();
               for (int i=0; i<transferSyntaxes.size(); i++) {
                  UID candidateSyntax = (UID)transferSyntaxes.get(i);
                  if (candidateSyntax.equals(tsyn)) {
                     pc.setSelectedTransferSyntaxIndex(i);
                  }
               }
///???????????????????
               
            }
            
            // Save the list of Presentation Contexts
            //        presentationContexts.add(new PresentationContext(presentationContext, as));
         }
         // Read user data items
         else if (pdu.getType() == 0x50) {
            PDU userDataSubItem = PDU.readItem(pdu.getInputStream());
            logger.info("User Sub-item type = " + userDataSubItem.getType());
            
            if (userDataSubItem.getType() == 0x51) { // maximum length item
               byte tmp[] = userDataSubItem.getBuffer();
               
               long maxLen = tmp[0] & 0xff;
               maxLen = maxLen << 8 | tmp[1] & 0xff;
               maxLen = maxLen << 8 | tmp[2] & 0xff;
               maxLen = maxLen << 8 | tmp[3] & 0xff;
               
               maxPDULength = Math.min(maxLen, maxPDULength);
               
               logger.info("Maximum length = " + maxPDULength);
            }
            else if (userDataSubItem.getType() == 0x52) { // Calling Implementation UID
               byte tmp[] = userDataSubItem.getBuffer();
               
               callingImplementationUID = PDU.byteArrayToString(tmp);
               logger.info("Implementation class UID = " + callingImplementationUID);
            }
            else if (userDataSubItem.getType() == 0x54) { // SCP/SCU role extended negotiation
               byte tmp[] = userDataSubItem.getBuffer();
               
               logger.info("SCP/SCU role negotiation item");
            }
            else if (userDataSubItem.getType() == 0x55) { // Calling Implmentation Version Name
               byte tmp[] = userDataSubItem.getBuffer();
               
               callingImplementationVersionName = PDU.byteArrayToString(tmp);
               logger.info("Implementation version name = " + callingImplementationVersionName);
            }
         }
         else {
            throw new IOException("Association protocol error");
         }
      }
   }
   
   void writeResponse(OutputStream os) throws IOException {
      DataOutputStream dos = new DataOutputStream(os);
      
      //Write the output stream to a variable length buffer first
      //so that we can know the PDU length before we send it to
      //the client
      ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
      DataOutputStream bos = new DataOutputStream(buffer);
      
      long pduLength = 0;
      
      //PDU type
      dos.writeByte(0x2);
      dos.writeByte(0);
      
      //
      // Buffer the following so we can send the buffer length
      //////////////////////////////////////////////////////////
      
      //protocol version = 0x01
      bos.writeByte(0x0);
      bos.writeByte(0x1);
      
      // reserved bytes
      bos.writeByte(0x0);
      bos.writeByte(0x0);
      
      // AETitles
      byte buf[] = new byte[16];
      System.arraycopy(calledAETitle.getBytes("8859_1"), 0, buf, 0, Math.min(16, calledAETitle.length()));
      bos.write(buf);
      buf = new byte[16];
      System.arraycopy(callingAETitle.getBytes("8859_1"), 0, buf, 0, Math.min(16, callingAETitle.length()));
      bos.write(buf);
      
      //reserved bytes
      byte tmp[] = new byte[32];
      bos.write(tmp);
      
      //Application Context Item
      bos.writeByte(0x10);
      bos.writeByte(0x0);
      int len = this.appContextName.length();
      bos.writeByte((len >>> 8) & 0xFF);
      bos.writeByte((len >>> 0) & 0xFF);
      bos.writeBytes(this.appContextName);
      
      //Presentation Context Items
      Iterator p = presentationContexts.iterator();
      while (p.hasNext()) {
         PresentationContext pc = (PresentationContext)p.next();
         
         //PDU Item type
         bos.writeByte(0x21);
         bos.writeByte(0x0);
         
         ByteArrayOutputStream pcbuffer = new ByteArrayOutputStream(256);
         DataOutputStream pcos = new DataOutputStream(pcbuffer);
         
         // Presentation context ID
         pcos.writeByte(pc.getID());
         pcos.writeByte(0x0);
         
         // Result/Reason
         pcos.writeByte(pc.getResult());
         pcos.writeByte(0x0);
         
         //Transfer Syntax Sub-Item
         
         // PDU item type
         pcos.writeByte(0x40);
         pcos.writeByte(0x0);
         
         int tindex = pc.getSelectedTransferSyntaxIndex();
         String ts;
         
         if (tindex >= 0) {
            ts = pc.getAbstractSyntax().getTransferSyntaxes().get(tindex).toString();
         }
         else {
            ts = "";
         }
         len = ts.length();
         pcos.writeByte((len >>> 8) & 0xFF);
         pcos.writeByte((len >>> 0) & 0xFF);
         pcos.writeBytes(ts);
         
         // Presentation item length
         bos.writeByte((pcbuffer.size() >>> 8) & 0xFF);
         bos.writeByte((pcbuffer.size() >>> 0) & 0xFF);
         
         // Presentation item bytes
         bos.write(pcbuffer.toByteArray());
      }
      
      // User Information Item
      
      // PDU item type
      bos.writeByte(0x50);
      bos.writeByte(0x0);
      
      ByteArrayOutputStream uibuffer = new ByteArrayOutputStream(256);
      DataOutputStream uos = new DataOutputStream(uibuffer);
      
      // Maximum PDU Length Item type
      uos.writeByte(0x51);
      uos.writeByte(0x0);
      
      len = 4;
      uos.writeByte((len >>> 8) & 0xFF);
      uos.writeByte((len >>> 0) & 0xFF);
      uos.writeByte((int)(maxPDULength >>> 24) & 0xff);
      uos.writeByte((int)(maxPDULength >>> 16) & 0xff);
      uos.writeByte((int)(maxPDULength >>>  8) & 0xff);
      uos.writeByte((int)(maxPDULength >>>  0) & 0xff);
      
      // Implementation Class UID Item type
      uos.writeByte(0x52);
      uos.writeByte(0x0);
      
      len = callingImplementationUID.length();
      uos.writeByte((len >>> 8) & 0xFF);
      uos.writeByte((len >>> 0) & 0xFF);
      uos.writeBytes(callingImplementationUID);
      
      // User Info item length
      bos.writeByte((uibuffer.size() >>> 8) & 0xFF);
      bos.writeByte((uibuffer.size() >>> 0) & 0xFF);
      
      // User Info item bytes
      bos.write(uibuffer.toByteArray());
      
      ///////////////////////////////////////////////////////////
      // Ok, now send the Associate AC PDU length
      //
      
      // Association AC item length
      long bufferSize = buffer.size();
      dos.writeByte((int)((bufferSize >>> 24) & 0xFF));
      dos.writeByte((int)((bufferSize >>> 16) & 0xFF));
      dos.writeByte((int)((bufferSize >>> 8) & 0xFF));
      dos.writeByte((int)((bufferSize >>> 0) & 0xFF));
      
      // Association AC item bytes
      dos.write(buffer.toByteArray());
      
      logger.info("Association AC len = " + buffer.size());
      
   }
   
}
