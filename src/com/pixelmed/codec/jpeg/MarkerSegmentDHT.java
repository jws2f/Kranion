/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.util.Map;

/**
 * <p>A JPEG DHT Marker Segment.</p>
 *
 * @author	dclunie
 */
public class MarkerSegmentDHT {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/MarkerSegmentDHT.java,v 1.2 2014/03/21 21:46:20 dclunie Exp $";
	
	private int nTables;

	private int[] TableClass;
	private int[] HuffmanTableIdentifier;
	private int[][] nHuffmanCodesOfLengthI;
	private int[][][] ValueOfHuffmanCodeIJ;

	public MarkerSegmentDHT(byte[] b,int length) throws Exception {
		TableClass             = new int [4];
		HuffmanTableIdentifier = new int [4];
		nHuffmanCodesOfLengthI = new int [4][];
		ValueOfHuffmanCodeIJ   = new int [4][][];
	
		nTables=0;
		int offset=0;
		while (length > 0) {
			if (nTables >= 4) {
				throw new Exception("Only 4 tables are permitted");
			}
			TableClass[nTables] = Utilities.extract8(b,offset) >> 4;
			HuffmanTableIdentifier[nTables] = Utilities.extract8(b,offset) & 0x0f;
			++offset; --length;
			
			nHuffmanCodesOfLengthI[nTables] = new int[16];
			for (int i=0; i<16; ++i) {
				nHuffmanCodesOfLengthI[nTables][i] = Utilities.extract8(b,offset);
				++offset; --length;
			}
			
			ValueOfHuffmanCodeIJ[nTables] = new int[16][];
			for (int i=0; i<16; ++i) {
				ValueOfHuffmanCodeIJ[nTables][i] = new int[nHuffmanCodesOfLengthI[nTables][i]];
				for (int j=0; j<nHuffmanCodesOfLengthI[nTables][i]; ++j) {
					ValueOfHuffmanCodeIJ[nTables][i][j] = Utilities.extract8(b,offset);
					++offset; --length;
				}
			}
			++nTables;
		}
	}
	
	public void addToMapByClassAndIdentifier(Map<String,HuffmanTable> htByClassAndIdentifer) {
		for (int t=0; t<nTables; ++t) {
			int cl = TableClass[t];
			int id = HuffmanTableIdentifier[t];
			String key = Integer.toString(cl) + "+" + Integer.toString(id);
			htByClassAndIdentifer.put(key,new HuffmanTable(cl,id,nHuffmanCodesOfLengthI[t],ValueOfHuffmanCodeIJ[t]));
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\n\tDHT:\n");
		for (int t=0; t<nTables; ++t) {
			buf.append("\t\t TableClass = "            +TableClass[t]+"\n");
			buf.append("\t\t HuffmanTableIdentifier = "+HuffmanTableIdentifier[t]+"\n");
			for (int i=0; i<16; ++i) {
				buf.append("\t\t\t nHuffmanCodesOfLength "+i+" = "+nHuffmanCodesOfLengthI[t][i]+"\n");
				for (int j=0; j<nHuffmanCodesOfLengthI[t][i];++j) {
					buf.append("\t\t\t\t ValueOfHuffmanCode "+j+" = "+ValueOfHuffmanCodeIJ[t][i][j]+"\n");
				}
			}
		}
		return buf.toString();
	}

}

