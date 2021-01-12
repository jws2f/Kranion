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

import java.io.*;

public class PDU {
  private int type;
  private byte buf[];

  public PDU() {
    type = 0;
    buf = null;
  }

  public int size() { return buf.length; }
  public int getType() { return type; }
  public void setType(int t) { type = t; }
  public byte[] getBuffer() { return buf; }
  public void setBuffer(byte buffer[]) { buf = buffer; }
  public DataInputStream getInputStream() { return new DataInputStream(new ByteArrayInputStream(buf)); }

  public static int A_ASSOCIATE_RQ = 0x01;
  public static int A_ASSOCIATE_AC = 0x02;
  public static int A_ASSOCIATE_RJ = 0x03;
  public static int A_RELEASE_RQ = 0x05;
  public static int A_RELEASE_RP = 0x06;
  public static int P_DATA_TF = 0x04;
  public static int A_ABORT = 0x07;


  public static PDU readPDU(InputStream istream) throws IOException {
    long pduLength;
    PDU pdu = new PDU();

    DataInputStream is = new DataInputStream(istream);

    pdu.type = is.readByte();

    is.readByte(); //Ignore this reserved byte

    // Now read the remaining PDU length
    pduLength = is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    //System.out.print(pduLength);
    //System.out.print("\r\n");

    pdu.buf = new byte[(int)pduLength];
    is.readFully(pdu.buf);

    return pdu;
  }

  public void writePDU(OutputStream ostream) throws IOException {

    DataOutputStream os = new DataOutputStream(ostream);

    // write the PDU type
    os.writeByte(type);
    os.writeByte(0);

    // Now write the remaining PDU length
    long pduLength = buf.length;
    os.writeByte((int)((pduLength >>> 24) & 0xFF));
    os.writeByte((int)((pduLength >>> 16) & 0xFF));
    os.writeByte((int)((pduLength >>> 8) & 0xFF));
    os.writeByte((int)((pduLength) & 0xFF));

    os.write(this.buf);
  }

  public static PDU readItem(InputStream istream) throws IOException {
    long pduLength;
    PDU pdu = new PDU();

    DataInputStream is = new DataInputStream(istream);

    pdu.type = is.readByte();

    is.readByte(); //Ignore this reserved byte

    // Now read the remaining PDU length
    pduLength = is.readUnsignedShort();

    pdu.buf = new byte[(int)pduLength];
    is.readFully(pdu.buf);

    return pdu;
  }

  public static byte[] readPDU(InputStream istream, int PDUType) throws IOException {
    byte pduType;
    long pduLength=0;
    byte buf[];
    DataInputStream is = new DataInputStream(istream);

    pduType = is.readByte();
    if (pduType != PDUType)
      throw new IOException("Wrong PDUType: " + PDUType + " expected, " + pduType + " received.");

    is.readByte(); //Ignore this reserved byte

    // Now read the remaining PDU length
    pduLength = is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    pduLength = pduLength << 8 | is.readUnsignedByte();
    //System.out.print(pduLength);
    //System.out.print("\r\n");

    buf = new byte[(int)pduLength];
    is.readFully(buf);

    return buf;

  }

  public void writeItem(OutputStream ostream) throws IOException
  {
     DataOutputStream dos = new DataOutputStream(ostream);
     dos.write(type);
     dos.write(0);
     int length = buf.length;
     dos.write((length & 0xFF00) >> 8);
     dos.write(length & 0xFF);
     dos.write(buf);
  }
  
  public static byte[] readItem(InputStream istream, int PDUType) throws IOException {
    byte pduType;
    long pduLength;
    byte buf[];
    DataInputStream is = new DataInputStream(istream);

    pduType = is.readByte();
    if (pduType != PDUType)
      throw new IOException("Wrong PDU Item Type: " + pduType);

    is.readByte(); //Ignore this reserved byte

    // Now read the remaining PDU length
    pduLength = is.readUnsignedShort();
    //System.out.println("[" + pduLength + ", avail=" + is.available() + "]");

    buf = new byte[(int)pduLength];
    is.readFully(buf);

    return buf;

  }

  public static String byteArrayToString(byte s[])
  {
    char stringBuf[] = new char[s.length];
    for (int i=0; i<s.length; i++) {
      stringBuf[i] = (char)s[i];
    }
    return String.valueOf(stringBuf).trim();
  }


}
