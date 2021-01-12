/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.io.InputStream;
import java.io.IOException;

/**
 * <p>A class with various utitilies for handling byte and bit extraction.</p>
 *
 * @author	dclunie
 */
public class Utilities {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/Utilities.java,v 1.1 2014/03/21 15:28:07 dclunie Exp $";

	// modified from com.pixelmed.utils.HexDump.toPaddedHexString()
	public static String toPaddedHexString(int i,int length) {
		StringBuffer sb = new StringBuffer();
		sb.append("0x");
		String s=Integer.toHexString(i);
		int ls=s.length();
		while(ls++ < length) {
			sb.append("0");
		}
		sb.append(s);	// even if it is longer than length wanted
		return sb.toString();
	}

	public static String toPaddedHexString(long i,int length) {
		StringBuffer sb = new StringBuffer();
		sb.append("0x");
		String s=Long.toHexString(i);
		int ls=s.length();
		while(ls++ < length) {
			sb.append("0");
		}
		sb.append(s);	// even if it is longer than length wanted
		return sb.toString();
	}
	
	public static final int extract8(byte[] b,int offset) {
		return b[offset]&0xff;
	}
	
	public static final int extract16be(byte[] b,int offset) {
		return ((b[offset]&0xff)<<8) + (b[offset+1]&0xff);
	}
	
	public static final long extract24be(byte[] b,int offset) {
		return ((b[offset]&0xff)<<16) + ((b[offset+1]&0xff)<<8) + (b[offset+2]&0xff);
	}
	
	public static final long extract32be(byte[] b,int offset) {
		return ((b[offset]&0xff)<<24) + ((b[offset+1]&0xff)<<16) + ((b[offset+2]&0xff)<<8) + (b[offset+3]&0xff);
	}
	
	public static final int read16be(InputStream in) throws IOException {
		// big-endian
		int u;
		byte b[] = new byte[2];
		int count = in.read(b,0,2);
		if (count == 2) {
			u = extract16be(b,0);
		}
		else {
			u = -1;		// OK as EOF/failure value since int is 32 bits and valid can only be 16
		}
		return u;
	}
	
}

