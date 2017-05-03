/*
 * UUIDGenerator.java
 *
 * Created on August 21, 2002, 1:18 PM
 */

package org.fusfoundation.util;

import java.net.*;
import java.util.Enumeration;
import java.security.SecureRandom;
import org.fusfoundation.util.PrintfFormat;

/**
 *
 * @author  jsnell
 */
public class UUIDGenerator {
   
   // class variables used to cache some of the data
   private int timeLow; //32 bits
   private int node; // 32 bits
   private int inetAddressVal;
   private String hexInetAddress; //32 bits
   private String thisHashCode; // 32 bits
   private String midValue;
   private SecureRandom seeder;
   private PrintfFormat hexFormat8 = new PrintfFormat("%08X");
   
   private int getInt(byte[] bytes) {
      int val = bytes[0];
      val <<= 8;
      val |= bytes[1] & 0xff;
      val <<= 8;
      val |= bytes[2] & 0xff;
      val <<= 8;
      val |= bytes[3] & 0xff;
      
      return val;
   }
   
   /** Creates a new instance of UUIDGenerator */
   public UUIDGenerator() {
      try {
         StringBuffer tmpBuffer = new StringBuffer();
         
         // initalise the secure random instance
         seeder = new SecureRandom();
         
         // get the address bound to the first network interface
         Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
         NetworkInterface ni = (NetworkInterface)interfaces.nextElement();
         Enumeration addresses = ni.getInetAddresses();
         InetAddress addr = (InetAddress)addresses.nextElement();
         
         // get the inet address
         byte [] bytes = addr.getAddress();
         inetAddressVal = getInt(bytes);
         //System.out.println("inetAddressVal = " + addr.getHostAddress());
         hexInetAddress = hexFormat8.sprintf(inetAddressVal);
         
         // get the hashcode
         thisHashCode = hexFormat8.sprintf(this.hashCode());
         
           /* set up a cached midValue as this is the same per method
/ call as is object specific and is the
/ ...-xxxx-xxxx-xxxx-xxxx.. mid part of the sequence
            */
         tmpBuffer.append("-");
         tmpBuffer.append(hexInetAddress.substring(0,4));
         tmpBuffer.append("-");
         tmpBuffer.append(hexInetAddress.substring(4));
         tmpBuffer.append("-");
         tmpBuffer.append(thisHashCode.substring(0,4));
         tmpBuffer.append("-");
         tmpBuffer.append(thisHashCode.substring(4));
         midValue = tmpBuffer.toString();
         
         // load up the randomizer first value
         int node = seeder.nextInt();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   //In order to speed up the method calls we can cache some infomation in the create method  
   //The actual method call to get the unique numbers is then very simple
   public String getGUID() {
      
      long timeNow = System.currentTimeMillis();
      timeLow = (int) timeNow & 0xFFFFFFFF;
      
      int node = seeder.nextInt();
      
      return (hexFormat8.sprintf(timeLow) + midValue + hexFormat8.sprintf(node));
   }
   
   public String getUUID() {
      return getUUID("");
   }
   
   public String getUUID(String prefix) {
      long timeNow = System.currentTimeMillis();
      timeLow = (int) timeNow & 0x7FFFFF;
      
      int node = seeder.nextInt() & 0x7FFFFF;
      
      StringBuffer buf = new StringBuffer(64);
      if (prefix.length() > 0) {
         buf.append(prefix);
         buf.append(".");
      }
      buf.append(inetAddressVal & 0x7fffffff);
      buf.append(".");
      buf.append(this.hashCode() & 0x7FFF);
      buf.append(".");
      buf.append(timeLow);
      buf.append(".");
      buf.append(node);
      
      String result = buf.toString();
      if (result.length() > 64) {
         throw new RuntimeException("UUID is longer than 64 characters");
      }
      
      return result;
   }
   
   public static void main(String args[]) {
      UUIDGenerator gen = new UUIDGenerator();
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      gen = new UUIDGenerator();
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      System.out.println(gen.getGUID());
      
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      System.out.println(gen.getUUID("1.2.826.0.1.3680043.2.509"));
      gen = new UUIDGenerator();
      for (int i=0; i<50; i++) {
         String uid = gen.getUUID("1.2.826.0.1.3680043.2.509");
         System.out.println(uid + " - len=" + uid.length());
      }
   }
   
   
}
