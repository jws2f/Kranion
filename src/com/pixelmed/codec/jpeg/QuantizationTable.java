/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

/**
 * <p>A JPEG Quantization Table.</p>
 *
 * @author	dclunie
 */
public class QuantizationTable {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/QuantizationTable.java,v 1.1 2014/03/21 21:46:20 dclunie Exp $";

	private int QuantizationTableElementPrecision;
	private int QuantizationTableIdentifier;
	private int[] QuantizationTableElement;
	
	public QuantizationTable(int QuantizationTableIdentifier,int QuantizationTableElementPrecision,int[] QuantizationTableElement) {
		this.QuantizationTableElementPrecision = QuantizationTableElementPrecision;
		this.QuantizationTableIdentifier = QuantizationTableIdentifier;
		this.QuantizationTableElement = QuantizationTableElement;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Quantization Table:\n");
		buf.append("\t QuantizationTableElementPrecision = "+QuantizationTableElementPrecision+"\n");
		buf.append("\t QuantizationTableIdentifier = "      +QuantizationTableIdentifier+"\n");
		for (int i=0; i<64; ++i) {
			buf.append("\t\t QuantizationTableElement "+i+" = "+QuantizationTableElement[i]+"\n");
		}
		return buf.toString();
	}

}


	
