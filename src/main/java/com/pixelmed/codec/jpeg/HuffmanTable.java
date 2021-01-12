/* Copyright (c) 2014, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

/**
 * <p>A JPEG Huffman Table.</p>
 *
 * @author	dclunie
 */
public class HuffmanTable {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/HuffmanTable.java,v 1.4 2014/03/23 11:41:54 dclunie Exp $";

	private int TableClass;
	private int HuffmanTableIdentifier;
	private int[] nHuffmanCodesOfLengthI;
	private int[][] ValueOfHuffmanCodeIJ;
	
	public HuffmanTable(int TableClass,int HuffmanTableIdentifier,int[] nHuffmanCodesOfLengthI,int[][] ValueOfHuffmanCodeIJ) {
		this.TableClass = TableClass;
		this.HuffmanTableIdentifier = HuffmanTableIdentifier;
		this.nHuffmanCodesOfLengthI = nHuffmanCodesOfLengthI;
		this.ValueOfHuffmanCodeIJ = ValueOfHuffmanCodeIJ;
		expand();
	}
	
	private int countNumberOfCodes() {
		int count=0;
		for (int i=0; i<nHuffmanCodesOfLengthI.length; ++i) {
			count += nHuffmanCodesOfLengthI[i];
		}
		return count;
	}
	
	// a literal implementation of 10918-1 F.2.2.3 and figure F.15
	
	private int[] BITS = new int[17];		// number of codes of each size for code size I 1-16 (index 0 unused)
	
	private int[] HUFFVAL;		// array of values in same order as encoded in ValueOfHuffmanCodeIJ with dimension nCodes + 1 to account for unused 0 index
	private int[] HUFFSIZE;
	private int[] HUFFCODE;
	
	private int[] MINCODE = new int[17];	// the smallest code value for code size I 1-16 (index 0 unused)
	private int[] MAXCODE = new int[17];	// the largest  code value for code size I 1-16 (index 0 unused)
	private int[] VALPTR  = new int[17];	// index to the start of the list of values in HUFFVAL (indexed from 1 not 0)

	public int[] getMINCODE() { return MINCODE; };
	public int[] getMAXCODE() { return MAXCODE; };
	public int[] getVALPTR()  { return VALPTR; };
	public int[] getHUFFVAL() { return HUFFVAL; };
	
	// for our redaction purposes, we need to replace AC coefficients with all zeroes (EOB), so take note of this code whilst expanding tables
	private int EOBCode;
	private int EOBCodeLength;
	
	public int getEOBCode()			{ return EOBCode; }
	public int getEOBCodeLength()	{ return EOBCodeLength; }

		
	private void expand() {
//System.err.println("HuffmanTable.expand(): class="+TableClass+" identifier="+HuffmanTableIdentifier);

		// list BITS(1..16) number of codes of each size ... is nHuffmanCodesOfLengthI(0..15)
		for (int I=1; I<=16; ++I) {
//System.err.println("HuffmanTable.expand(): I="+I);
			BITS[I] = nHuffmanCodesOfLengthI[I-1];
		}
		
		int nCodes = countNumberOfCodes();
		// HUFFVAL is a flat list of codes in the order read they are encoded in the DHT segment, which is already sorted into ascending orded
		{
			HUFFVAL = new int[nCodes+1];
			int J = 0;	// N.B. This is one of the few tables in ISO 10918-1 that starts with an index of zero, not one; must match VALPTR values used as indices into HUFFVAL
			for (int i=0; i<nHuffmanCodesOfLengthI.length; ++i) {
				int nCodesThisLength = nHuffmanCodesOfLengthI[i];
				if (nCodesThisLength > 0) {
					for (int j=0; j<nCodesThisLength; ++j) {
						HUFFVAL[J] = ValueOfHuffmanCodeIJ[i][j];
						++J;
					}
				}
			}
		}
		
		// 10918-1 C.2 Figure C.1 Generate_size_table
		// HUFFSIZE contains a list of code lengths
		//int LASTK = 0;
		HUFFSIZE = new int[nCodes+1];
		{
			int K=0;
			int I=1;
			int J=1;
			while (true) {
				if (J > BITS[I]) {
					++I;
					J=1;
					if (I > 16) {
						HUFFSIZE[K] = 0;
						//LASTK = K;
						break;
					}
				}
				else {
					HUFFSIZE[K] = I;
					++K;
					++J;
				}
			}
		}

		// 10918-1 C.2 Figure C.2 Generate_code_table
		// HUFFCODE contains a code for each size in HUFFSIZE
		HUFFCODE = new int[nCodes+1];
		{
			int K=0;
			int CODE=0;
			int SI = HUFFSIZE[0];
			while (true) {
				HUFFCODE[K] = CODE;
				++CODE;
				++K;
				if (SI != HUFFSIZE[K]) {
					if (HUFFSIZE[K] == 0) break;
					do {
						CODE = CODE << 1;
						++SI;
					} while (SI != HUFFSIZE[K]);
				}
			
			}
		}
		
		// 10918-1 C.2 Figure F.15 Decoder_tables generation
		{
			int I=0;
			int J=0;
			while (true) {
				++I;
				if (I > 16) break;
				if (BITS[I] == 0) {
					MAXCODE[I] = -1;
				}
				else {
					VALPTR[I] = J;
					MINCODE[I] = HUFFCODE[J];
					J = J + BITS[I] - 1;
					MAXCODE[I] = HUFFCODE[J];
					++J;
				}
			}
		}
		
		// walk the arrays to find the EOB code and its length
		{
			for (int I=1; I<=16; ++I) {
				for (int J = VALPTR[I]; J < VALPTR[I] + BITS[I]; ++J) {
					if (HUFFVAL[J] == 0) {	// 0x00 is the EOB code (rrrrssss == 0)
						EOBCode = HUFFCODE[J];
						EOBCodeLength = I;
					}
				}
			}
		}
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Huffman Table:\n");
		buf.append("\t TableClass = "            +TableClass+"\n");
		buf.append("\t HuffmanTableIdentifier = "+HuffmanTableIdentifier+"\n");
		for (int i=0; i<16; ++i) {
			buf.append("\t\t nHuffmanCodesOfLength "+i+" = "+nHuffmanCodesOfLengthI[i]+"\n");
			for (int j=0; j<nHuffmanCodesOfLengthI[i];++j) {
				buf.append("\t\t\t ValueOfHuffmanCode "+j+" = "+ValueOfHuffmanCodeIJ[i][j]+"\n");
			}
		}
		buf.append("\t Expanded:\n");
		for (int I=1; I<=16; ++I) {
			buf.append("\t\t["+I+"] MINCODE="+Integer.toBinaryString(MINCODE[I])+" MAXCODE="+Integer.toBinaryString(MAXCODE[I])+""+" VALPTR="+VALPTR[I]+"\n");
		}
		for (int J=0; J<HUFFVAL.length; ++J) {
			buf.append("\t\t["+J+"] HUFFVAL=0x"+Integer.toHexString(HUFFVAL[J])+"\n");
		}
		buf.append("\t\tEOBCode="+Integer.toBinaryString(EOBCode)+" 0x"+Integer.toHexString(EOBCode)+" (length "+EOBCodeLength+" dec)\n");
		return buf.toString();
	}

}
