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

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:	University of Virginia
 * @author John W. Snell
 * @version 1.0
 */

import java.io.Serializable;
import java.util.*;

public class DicomObject implements Serializable {

  private  TreeSet tagSet = new TreeSet();
  private DicomDict dict = DicomDict.getDictionary();

  public DicomObject() {
  }
  
  public boolean addVR(VR vr)
  {
      if (vr == null) {
          return false;
      }
      
      // replace existing element if it exists
      if (tagSet.contains(vr)) {
          tagSet.remove(vr);
      }
      return tagSet.add(vr);
  }
  
  public String toString()
  {
      int i;
      StringBuffer buf = new StringBuffer();
      buf.append("DicomObject:\n");
      Iterator si = tagSet.iterator();
      for (i=0; i<tagSet.size(); i++) {
          VR vr = (VR)si.next();
          buf.append("  ");
          buf.append(vr);
          buf.append("\n");
      }
      return buf.toString();
  }
  
  public Iterator iterator()
  {
      return tagSet.iterator();
  }
  
  public int size()
  {
      return tagSet.size();
  }
  
  public VR getVR(String name)
  {
      VR key = new VR(name, new byte[0]);
      if (!tagSet.contains(key)) return null;
      Iterator i = tagSet.iterator();
      while (i.hasNext()) {
          VR vr = (VR)i.next();
          if (key.compareTo(vr) == 0)
              return vr;
      }
      return null;
  }
  
  public VR getVR(int group, int element)
  {
      Iterator i = tagSet.iterator();
      while (i.hasNext()) {
          VR vr = (VR)i.next();
          if (vr.getGroup() == group && vr.getElement() == element)
              return vr;
      }
      return null;
  }
}
