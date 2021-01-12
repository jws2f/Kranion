/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

/**
 * <p>A JPEG APP0 JFIF Marker Segment.</p>
 *
 * @author	dclunie
 */
public class MarkerSegmentAPP0JFIF {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/MarkerSegmentAPP0JFIF.java,v 1.1 2014/03/21 15:28:07 dclunie Exp $";
	
	private int version;
	private int units;
	private int Xdensity;
	private int Ydensity;
	private int Xthumbnail;
	private int Ythumbnail;

	public MarkerSegmentAPP0JFIF(byte[] b,int length) {
		// identifier is 4 bytes plus a zero byte
		version=Utilities.extract16be(b,5);
		units=Utilities.extract8(b,7);
		Xdensity=Utilities.extract16be(b,8);
		Ydensity=Utilities.extract16be(b,10);
		Xthumbnail=Utilities.extract8(b,12);
		Ythumbnail=Utilities.extract8(b,13);
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\n\tAPP0 JFIF:\n");
		buf.append("\t\t Version = "+Utilities.toPaddedHexString(version,2)+"\n");
		buf.append("\t\t Units for the X and Y densities = "+units+"\n");
		buf.append("\t\t Horizontal pixel density = "+Xdensity+"\n");
		buf.append("\t\t Vertical pixel density = "+Ydensity+"\n");
		buf.append("\t\t Thumbnail horizontal pixel count = "+Xthumbnail+"\n");
		buf.append("\t\t Thumbnail vertical pixel count = "+Ythumbnail+"\n");
		return buf.toString();
	}

}

