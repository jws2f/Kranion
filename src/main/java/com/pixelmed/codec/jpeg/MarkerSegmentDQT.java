/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.util.Map;

/**
 * <p>A JPEG DQT Marker Segment.</p>
 *
 * @author	dclunie
 */
public class MarkerSegmentDQT {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/MarkerSegmentDQT.java,v 1.2 2014/03/21 21:46:20 dclunie Exp $";
	
	private int nTables;

	private int[] QuantizationTableElementPrecision;
	private int[] QuantizationTableIdentifier;
	private int[][] QuantizationTableElement;

	public MarkerSegmentDQT(byte[] b,int length) throws Exception {
		QuantizationTableElementPrecision = new int [4];
		QuantizationTableIdentifier       = new int [4];
		QuantizationTableElement          = new int [4][];
	
		nTables=0;
		int offset=0;
		while (length > 0) {
			if (nTables >= 4) {
				throw new Exception("Only 4 tables are permitted");
			}
			
			QuantizationTableElementPrecision[nTables] = Utilities.extract8(b,offset) >> 4;
			QuantizationTableIdentifier[nTables]       = Utilities.extract8(b,offset) & 0x0f;
			QuantizationTableElement[nTables]          = new int[64];
			++offset; --length;
			
			for (int i=0; i<64; ++i) {
				if (QuantizationTableElementPrecision[nTables] > 0) {
					QuantizationTableElement[nTables][i] = Utilities.extract16be(b,offset);
					offset+=2; length-=2;
				}
				else {
					QuantizationTableElement[nTables][i] = Utilities.extract8(b,offset);
					++offset; --length;
				}
			}
			++nTables;
		}
	}

	public void addToMapByIdentifier(Map<String,QuantizationTable> qtByIdentifer) {
		for (int t=0; t<nTables; ++t) {
			int id = QuantizationTableIdentifier[t];
			String key = Integer.toString(id);
			qtByIdentifer.put(key,new QuantizationTable(id,QuantizationTableElementPrecision[t],QuantizationTableElement[t]));
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\n\tDQT:\n");
		for (int t=0; t<nTables; ++t) {
			buf.append("\t\t QuantizationTableElementPrecision = "+QuantizationTableElementPrecision[t]+"\n");
			buf.append("\t\t QuantizationTableIdentifier = "      +QuantizationTableIdentifier[t]+"\n");
			for (int i=0; i<64; ++i) {
				buf.append("\t\t\t QuantizationTableElement "+i+" = "+QuantizationTableElement[t][i]+"\n");
			}
		}
		return buf.toString();
	}

}

