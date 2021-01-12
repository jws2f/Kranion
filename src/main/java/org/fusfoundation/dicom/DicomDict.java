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
 * Title: DicomDict
 * Description: Represents the DICOM data dictionary
 * Copyright:    Copyright (c) 2002
 * Company: University of Virginia
 * @author John Snell
 * @version 1.0
 */

import java.util.*;
import java.io.*;

public class DicomDict implements Serializable {

  // This class implements the Singleton pattern
  static private DicomDict theDict = null;

  // Factory method fot the single DicomDict instance
  synchronized static public DicomDict getDictionary() {
    if (theDict == null) {
      theDict = new DicomDict();
    }
    return theDict;
  }

  // private constructor for Singleton pattern
  private DicomDict() {
    dictEntries = new HashMap();
    alphaDictEntries = new HashMap();
    dictRangeEntries = new ArrayList();
    readDict("dicom.dic");
  }

  // Arrays to hold the dictionary elements
  private ArrayList dictRangeEntries;
  private HashMap dictEntries; // hash dictionary entries by group/element id
  private HashMap alphaDictEntries; // hash dictrionary entries by name
  
  public String getVrName(int ID)
  {
      int  group = (ID & 0xffff0000) >>> 16;
      if (group % 2 == 1) return "Private";
      
      DictEntry entry = (DictEntry)dictEntries.get(new Integer(ID));
      
      if (entry == null) {
        return "Unknown";
      }
      else {
        return entry.Name;
      }
  }
  
  public int getVrType(int ID)
  {
      DictEntry entry = (DictEntry)dictEntries.get(new Integer(ID));
      
      if (entry == null) {
          return VR.UN;
      }
      else {
          return entry.VR;
      }
  }
  
  public int getVrId(String name)
  {
      // Look up the the name in the dictionary and return the ID
     DictEntry entry = (DictEntry)alphaDictEntries.get(name);
     return (entry.getGroup() & 0xFFFF) << 16 | (entry.getElement() & 0xFFFF);
  }

  // Inner classes to represent dictionary elements and element ranges
  private class DictEntry {
    public String Name;
    public int ID;
    public int VMmin, VMmax;
    public int VR;

    public DictEntry(String name, int group, int elem, int vr) {
      Name = name;
      ID = ((group & 0xFFFF) << 16) | (elem & 0xFFFF);
      VR = vr;
      VMmin = VMmax = 1;
    }

    public DictEntry(String name, int group, int elem, int vr, int vmMin, int vmMax) {
      Name = name;
      ID = ((group & 0xFFFF) << 16) | (elem & 0xFFFF);
      VR = vr;
      VMmin = vmMin;
      VMmax = vmMax;
    }
    
    public int hashCode() { return ID; }
    
    public int getGroup() { return (ID >>> 16) & 0xFFFF; }
    public int getElement() { return ID & 0xFFFF; }

  }

  private class DictRangeEntry {
    public String Name;
    public int groupLow, groupHigh;
    public int groupRangeRestrict;
    public int elementLow, elementHigh;
    public int elementRangeRestrict;
    public int VMmin, VMmax;
    public int VR;

    public static final int rangeEven=0;
    public static final int rangeOdd=1;
    public static final int rangeAll=2;

    public DictRangeEntry(String name, int grpLow, int grpHigh, int grpRestrict, int elemLow, int elemHigh, int elemRestrict, int vr, int vmMin, int vmMax) {
      Name = name;
      VR = vr;
      groupLow = grpLow;
      groupHigh = grpHigh;
      groupRangeRestrict = grpRestrict;
      elementLow = elemLow;
      elementHigh = elemHigh;
      elementRangeRestrict = elemRestrict;
      VMmin = vmMin;
      VMmax = vmMax;
    }

    public int getGroupLow() { return groupLow; }
    public int getElementLow() { return groupLow; }
    public int getGroupHigh() { return groupHigh; }
    public int getElementHigh() { return groupHigh; }
    public int getGroupRangeRestriction() { return groupRangeRestrict; }
    public int getElementRangeRestriction() { return elementRangeRestrict; }

  }

  private int getVMmin(String entry) {
    if (entry.indexOf("-") > 0) {
      return Integer.parseInt(entry.substring(0, entry.indexOf("-")));
    }
    else {
      return Integer.parseInt(entry);
    }
  }

  private int getVMmax(String entry) {
    if (entry.indexOf("-") > 0) {
      String second = entry.substring(entry.indexOf("-")+1, entry.length());
      if (second.compareTo("n") == 0) {
        return -1; // means any number (n)
      }
      else if (second.indexOf("n") >= 0) {
        return -Integer.parseInt(second.substring(0, 1)); // means any number (n)
      }
      else
        return Integer.parseInt(second);
    }
    else {
      return Integer.parseInt(entry);
    }
  }

  private int getRangeMin(String entry) {
    if (entry.indexOf("-") > 0) {
      return Integer.parseInt(entry.substring(0, entry.indexOf("-")), 16);
    }
    else {
      return Integer.parseInt(entry, 16);
    }
  }

  private int getRangeMax(String entry) {
    if (entry.indexOf("-o-") > 0 || entry.indexOf("-u-") > 0) {
      return Integer.parseInt(entry.substring(7, 11), 16);
    }
    else if (entry.indexOf("-") > 0) {
      return Integer.parseInt(entry.substring(5, 9), 16);
    }
    else {
      return Integer.parseInt(entry, 16);
    }
  }

  private int getRangeRestriction(String entry) {
    if (entry.indexOf("-o-") > 0) {
      return DictRangeEntry.rangeOdd;
    }
    else if (entry.indexOf("-u-") > 0) {
      return DictRangeEntry.rangeAll;
    }
    else if (entry.indexOf("-") > 0) {
      return DictRangeEntry.rangeEven;
    }
    else {
      return DictRangeEntry.rangeAll;
    }
  }

  private void readDict(String resourceName) {
    try {

      InputStream rstm = this.getClass().getResourceAsStream(resourceName);

      BufferedReader fileStr = new BufferedReader(new InputStreamReader(rstm));
      String thisLine;
      while ((thisLine = fileStr.readLine()) != null) {
        if (thisLine.startsWith("#")) {
        }
        else {
          StringTokenizer tok = new StringTokenizer(thisLine);
          String id, vr, name, vm, version;

          id = tok.nextToken();

          String group = id.substring(1, id.indexOf(","));
          String element = id.substring(id.indexOf(",")+1, id.length()-1);

          vr = tok.nextToken();
          name = tok.nextToken();
          vm = tok.nextToken();
          version = tok.nextToken();

          if (group.indexOf("-") > 1 || element.indexOf("-") > 1) {
//            System.out.println(name + " - " + group + "-" + element);
            DictRangeEntry entry = new DictRangeEntry(
              name,
              this.getRangeMin(group),
              this.getRangeMax(group),
              this.getRangeRestriction(group),
              this.getRangeMin(element),
              this.getRangeMax(element),
              this.getRangeRestriction(element),
              VR.idFromString(vr),
              this.getVMmin(vm),
              this.getVMmax(vm)
            );
            this.dictRangeEntries.add(entry);

          }
          else {
            DictEntry entry = new DictEntry(
                name,
                Integer.parseInt(group, 16), Integer.parseInt(element, 16),
                VR.idFromString(vr),
                getVMmin(vm), getVMmax(vm)
              );
            this.dictEntries.put(new Integer(entry.ID), entry);
            this.alphaDictEntries.put(new String(name), entry);

          }
        }
      }
/*
      FileWriter fw = new FileWriter("/home/jsnell/dict.java");
      fw.write("class DicomDictData {\r\n");
      fw.write("  public DicomDictData() {}\r\n");
      fw.write("  private static String elementName[] = {\r\n");

      Iterator i = dictEntries.values().iterator();
      while (i.hasNext()) {
        DictEntry k = (DictEntry)i.next();
        int tg = (k.ID >>> 16) & 0xffff;
        int te = k.ID & 0xffff;

        fw.write("    \""+k.Name+"\"");
        if (i.hasNext()) {
          fw.write(",");
        }
        fw.write("\r\n");
       }
       fw.write("  };\r\n");

       fw.write("  private static int elementID[] = {\r\n");
        i = dictEntries.values().iterator();
        while (i.hasNext()) {
          DictEntry k = (DictEntry)i.next();

          fw.write("    "+k.ID);
          if (i.hasNext()) {
            fw.write(",");
          }
          fw.write("\r\n");
         }
         fw.write("  };\r\n");

       fw.write("  private static int elementVR[] = {\r\n");
        i = dictEntries.values().iterator();
        while (i.hasNext()) {
          DictEntry k = (DictEntry)i.next();
          int tg = (k.ID >>> 16) & 0xffff;
          int te = k.ID & 0xffff;

          fw.write("    "+VR.stringFromId(k.VR)+ " VM: " + k.VMmin + " - " + k.VMmax);
          if (i.hasNext()) {
            fw.write(",");
          }
          fw.write("\r\n");
         }
         fw.write("  };\r\n");

         fw.write("//Ranges\r\n");
         i = this.dictRangeEntries.iterator();
         while(i.hasNext()) {
          DictRangeEntry e = (DictRangeEntry)i.next();
          fw.write("// " + e.Name + " " + Integer.toHexString(e.groupLow) + "-" + Integer.toHexString(e.groupHigh) + ", " + Integer.toHexString(e.elementLow) + "-" + Integer.toHexString(e.elementHigh) + " VR:" + VR.stringFromId(e.VR) + " VM: " + e.VMmin + " - " + e.VMmax + "\r\n");
         }

       // Closing brace for the class
       fw.write("}");
       fw.close();
*/

    }
    catch(Exception e) {
      System.out.println(e.toString());
    }
  }

  public static void main(String args[]) {
      DicomDict dict = DicomDict.getDictionary();
  }
}
