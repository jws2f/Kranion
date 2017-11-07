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

import java.io.Serializable;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

public class UID implements Serializable {
  public static final UID ImplementationClass = new UID("9.9.9.9.9");
  public static final UID DicomAppContext = new UID("1.2.840.10008.3.1.1.1");
  
  // SOP Classes
  public static final UID Verification = new UID("1.2.840.10008.1.1");
  public static final UID CRImageStorage = new UID("1.2.840.10008.5.1.4.1.1.1");
  public static final UID DigitalXrayForPresentationImageStorage = new UID("1.2.840.10008.5.1.4.1.1.1.1");
  public static final UID CTImageStorage = new UID("1.2.840.10008.5.1.4.1.1.2");
  public static final UID MRImageStorage = new UID("1.2.840.10008.5.1.4.1.1.4");
  public static final UID UltrasoundImageStorage = new UID("1.2.840.10008.5.1.4.1.1.6.1");
  public static final UID SecondaryCaptureImageStorage = new UID("1.2.840.10008.5.1.4.1.1.7");
  
  public static final UID PatientRootQueryRetrieveFind = new UID("1.2.840.10008.5.1.4.1.2.1.1");
  public static final UID PatientRootQueryRetrieveMove = new UID("1.2.840.10008.5.1.4.1.2.1.2");
  public static final UID PatientRootQueryRetrieveGet  = new UID("1.2.840.10008.5.1.4.1.2.1.3");
  
  public static final UID StudyRootQueryRetrieveFind = new UID("1.2.840.10008.5.1.4.1.2.2.1");
  public static final UID StudyRootQueryRetrieveMove = new UID("1.2.840.10008.5.1.4.1.2.2.2");
  public static final UID StudyRootQueryRetrieveGet  = new UID("1.2.840.10008.5.1.4.1.2.2.3");
  
  // Transfer Syntaxes
  public static final UID ImplicitVRLittleEndian = new UID("1.2.840.10008.1.2");
  public static final UID ExplicitVRLittleEndian = new UID("1.2.840.10008.1.2.1");
  public static final UID ExplicitVRBigEndian = new UID("1.2.840.10008.1.2.2");
  public static final UID RLELossless = new UID("1.2.840.10008.1.2.5");
  public static final UID JPEGBaseline = new UID("1.2.840.10008.1.2.4.50");
  public static final UID JPEGLossless = new UID("1.2.840.10008.1.2.4.70");

  private String value;
  
  public UID(String uid)
  {
      value = uid;
  }
  
  public String toString() { return value; }
  
  public int hashCode() {
      return value.hashCode();
  }
  
  public boolean equals(Object o)
  {
      if (o instanceof UID) {
          UID uid = (UID)o;
          return value.equals(uid.value);
      }
      
      return false;
  }
  
}
