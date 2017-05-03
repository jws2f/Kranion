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
package org.fusfoundation.dicom;

import java.util.*;
import java.io.*;
import java.text.*;

/**
 *
 * @author  jsnell
 */
public class DicomDate implements VrValue {
   
   /** Creates a new instance of DicomDate */
   private Date date;
   private int vt = -1;
   
   public DicomDate() {
   }
   
   public DicomDate(Date d) {
      date = (Date)d.clone();
   }
   public DicomDate(String dateString) {
      DateFormat dateformat = new SimpleDateFormat("MM/dd/yyyy");
      try {
         date = dateformat.parse(dateString);
      }
      catch (ParseException e) {
         throw new DicomRuntimeException("DicomDate format error.");
      }
   }
   
   public DicomDate(String dateString, String format) {
      DateFormat dateformat = new SimpleDateFormat(format);
      try {
         date = dateformat.parse(dateString);
      }
      catch (ParseException e) {
         throw new DicomRuntimeException("DicomDate format error.");
      }
   }
   
   public DicomDate(byte[] valBytes) {
      this.setVrValue(VR.DA, valBytes);
   }
   
   public DicomDate(int vrType, byte[] valBytes) {
      this.setVrValue(vrType, valBytes);
   }
   
   public byte[] getVrValue(int vrType) {
      if (vrType == VR.DA) {
         DateFormat df = new SimpleDateFormat("yyyyMMdd");
         String dateStr = df.format(date);
         byte[] value;
         
         try {
            value = dateStr.getBytes("8859_1");
         }
         catch (UnsupportedEncodingException e) {
            throw new DicomRuntimeException("Unknown encoding.");
         }
         
         return value;
      }
      else if (vrType == VR.TM) {
         DateFormat df = new SimpleDateFormat("hhmmss.SSSS");
         String dateStr = df.format(date);
         byte[] value;
         
         try {
            value = dateStr.getBytes("8859_1");
         }
         catch (UnsupportedEncodingException e) {
            throw new DicomRuntimeException("Unknown encoding.");
         }
         
         return value;
      }
      else if (vrType == VR.DT) {
         DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss.SSSSZZZZ");
         String dateStr = df.format(date);
         byte[] value;
         
         try {
            value = dateStr.getBytes("8859_1");
         }
         catch (UnsupportedEncodingException e) {
            throw new DicomRuntimeException("Unknown encoding.");
         }
         
         return value;
      }
      else {
         throw new DicomRuntimeException("DicomDate bad VR type: " + vrType);
      }
   }
   
   public void setVrValue(int vrType, byte[] value) {
      String dateStr;
      
      try {
         dateStr = new String(value, "8859_1");
      }
      catch(UnsupportedEncodingException e) {
         throw new DicomRuntimeException("Unknown encoding");
      }
      
      if (dateStr.length() == 0) {
         date = null;
      }
      else if (vrType == VR.DA) {
         try {
            DateFormat df;
            
            if (dateStr.length() == 8) {
               df = new SimpleDateFormat("yyyyMMdd");
            }
            else {
               df = new SimpleDateFormat("yyyy.MM.dd");
            }
            
            date = df.parse(dateStr);
            
            vt = vrType;
         }
         catch (ParseException e) {
            throw new DicomRuntimeException("Parse error. [" + dateStr + "]");
         }
      }
      else if (vrType == VR.TM) {
         try {
            DateFormat df;
            try {
               df = new SimpleDateFormat("hhmmss.SSSS");
               date = df.parse(dateStr);
            }
            catch(ParseException e) {
               try {
                  df = new SimpleDateFormat("hhmmss");
                  date = df.parse(dateStr);
               }
               catch(ParseException e1) {
                  try {
                     df = new SimpleDateFormat("hh:mm:ss.SSSS");
                     date = df.parse(dateStr);
                  }
                  catch(ParseException e2) {
                     try {
                        df = new SimpleDateFormat("hh:mm:ss");
                        date = df.parse(dateStr);
                     }
                     catch(ParseException e3) {
                        throw e3;
                     }
                  }
               }
            }
            
            vt = vrType;
         }
         catch (ParseException e) {
            throw new DicomRuntimeException("DicomDate parse error: " + dateStr);
         }
      }
      else if (vrType == VR.DT) {
         try {
            DateFormat df;
            
            df = new SimpleDateFormat("yyyyMMddhhmmss.SSSSZZZZ");
            
            date = df.parse(dateStr);
            
            vt = vrType;
         }
         catch (ParseException e) {
            throw new DicomRuntimeException("DicomDate parse error: " + dateStr);
         }
      }
      else {
         throw new DicomRuntimeException("DicomDate bad VR type: " + vrType);
      }
      
   }
   
   public Date getDate() { return date; }
   
   public String toString() {
      if (date == null) {
         return null;
      }
      else if (vt == VR.DA) {
         DateFormat df = DateFormat.getDateInstance();
         return df.format(date);
      }
      else if (vt == VR.TM) {
         DateFormat df = DateFormat.getTimeInstance();
         return df.format(date);
      }
      else {
         return date.toString();
      }
   }
   
}
