/* Copyright (c) 2014-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>A class containing static definitions of JPEG marker segments and related methods.</p>
 *
 * @author	dclunie
 */
public class Markers {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/Markers.java,v 1.4 2016/01/16 15:07:52 dclunie Exp $";
	
	// modified from dicom3tools appsrc/misc/jpegdump.cc ...

	public static final int APP0 = 0xffe0;
	public static final int APP1 = 0xffe1;
	public static final int APP2 = 0xffe2;
	public static final int APP3 = 0xffe3;
	public static final int APP4 = 0xffe4;
	public static final int APP5 = 0xffe5;
	public static final int APP6 = 0xffe6;
	public static final int APP7 = 0xffe7;
	public static final int APP8 = 0xffe8;
	public static final int APP9 = 0xffe9;
	public static final int APPA = 0xffea;
	public static final int APPB = 0xffeb;
	public static final int APPC = 0xffec;
	public static final int APPD = 0xffed;
	public static final int APPE = 0xffee;
	public static final int APPF = 0xffef;

	public static final int COM = 0xfffe;
	public static final int DAC = 0xffcc;
	public static final int DHP = 0xffde;
	public static final int DHT = 0xffc4;
	public static final int DNL = 0xffdc;
	public static final int DQT = 0xffdb;
	public static final int DRI = 0xffdd;
	public static final int EOI = 0xffd9;          // also JPEG 2000 "EOC"
	public static final int EXP = 0xffdf;

	public static final int JPG = 0xffc8;

	// left out reserved JPGn and RES
	// (especially those with first bit (not just byte) zero ... new LS 0 stuffing)

	public static final int RST0 = 0xffd0;
	public static final int RST1 = 0xffd1;
	public static final int RST2 = 0xffd2;
	public static final int RST3 = 0xffd3;
	public static final int RST4 = 0xffd4;
	public static final int RST5 = 0xffd5;
	public static final int RST6 = 0xffd6;
	public static final int RST7 = 0xffd7;

	public static final int SOF0 = 0xffc0;
	public static final int SOF1 = 0xffc1;
	public static final int SOF2 = 0xffc2;
	public static final int SOF3 = 0xffc3;

	public static final int SOF5 = 0xffc5;
	public static final int SOF6 = 0xffc6;
	public static final int SOF7 = 0xffc7;

	public static final int SOF9 = 0xffc9;
	public static final int SOFA = 0xffca;
	public static final int SOFB = 0xffcb;

	public static final int SOFD = 0xffcd;
	public static final int SOFE = 0xffce;
	public static final int SOFF = 0xffcf;

	public static final int SOI = 0xffd8;
	public static final int SOS = 0xffda;
	public static final int TEM = 0xff01;

	// New for JPEG-LS (14495-1:1997)

	public static final int SOF55 = 0xfff7;
	public static final int LSE   = 0xfff8;

	public static final int LSE_ID_L1 = 0x01;
	public static final int LSE_ID_L2 = 0x02;
	public static final int LSE_ID_L3 = 0x03;
	public static final int LSE_ID_L4 = 0x04;

	// New for JPEG 2000 (15444-1:2000)

	public static final int SOC = 0xff4f;
	public static final int SOT = 0xff90;
	public static final int SOD = 0xff93;
//	public static final int EOC = 0xffd9;        // same as JPEG EOI
	public static final int SIZ = 0xff51;
	public static final int COD = 0xff52;
	public static final int COC = 0xff53;
	public static final int RGN = 0xff5e;
	public static final int QCD = 0xff5c;
	public static final int QCC = 0xff5d;
	public static final int POC = 0xff5f;
	public static final int TLM = 0xff55;
	public static final int PLM = 0xff57;
	public static final int PLT = 0xff58;
	public static final int PPM = 0xff60;
	public static final int PPT = 0xff61;
	public static final int SOP = 0xff91;
	public static final int EPH = 0xff92;
	public static final int CRG = 0xff63;
	public static final int COM2K = 0xff64;

	public static final int FF30 = 0xff30;
	public static final int FF31 = 0xff31;
	public static final int FF32 = 0xff32;
	public static final int FF33 = 0xff33;
	public static final int FF34 = 0xff34;
	public static final int FF35 = 0xff35;
	public static final int FF36 = 0xff36;
	public static final int FF37 = 0xff37;
	public static final int FF38 = 0xff38;
	public static final int FF39 = 0xff39;
	public static final int FF3A = 0xff3a;
	public static final int FF3B = 0xff3b;
	public static final int FF3C = 0xff3c;
	public static final int FF3D = 0xff3d;
	public static final int FF3E = 0xff3e;
	public static final int FF3F = 0xff3f;

	public static final int isFixedLengthJPEGSegment(int marker) {
		int length;
		switch (marker) {
			case EXP:
				length=3; break;
			default:
				length=0; break;
		}
		return length;
	}
	
	public static final boolean isNoLengthJPEGSegment(int marker) {
		boolean nolength;
		switch (marker) {
			case SOI:
			case EOI:
			case TEM:
			case RST0:
			case RST1:
			case RST2:
			case RST3:
			case RST4:
			case RST5:
			case RST6:
			case RST7:
			case FF30:
			case FF31:
			case FF32:
			case FF33:
			case FF34:
			case FF35:
			case FF36:
			case FF37:
			case FF38:
			case FF39:
			case FF3A:
			case FF3B:
			case FF3C:
			case FF3D:
			case FF3E:
			case FF3F:
			case SOC:
			case SOD:
			//case EOC:         // same as JPEG EOI
			case EPH:
				nolength=true; break;
			default:
				nolength=false; break;
		}
		return nolength;
	}
	
	public static final boolean isVariableLengthJPEGSegment(int marker) {
		return !isNoLengthJPEGSegment(marker) && isFixedLengthJPEGSegment(marker) == 0;
	}

	public static final boolean isSOF(int marker) {
		boolean isSOF;
		switch (marker) {
			case SOF0:
			case SOF1:
			case SOF2:
			case SOF3:
			case SOF5:
			case SOF6:
			case SOF7:
			case SOF9:
			case SOFA:
			case SOFB:
			case SOFD:
			case SOFE:
			case SOFF:
			case SOF55:
				isSOF=true; break;
			default:
				isSOF=false; break;
		}
		return isSOF;
	}

	public static final boolean isHuffman(int marker) {
		boolean isHuffman;
		switch (marker) {
			case SOF0:
			case SOF1:
			case SOF2:
			case SOF3:
			case SOF5:
			case SOF6:
			case SOF7:
				isHuffman=true; break;
			default:
				isHuffman=false; break;
		}
		return isHuffman;
	}
	
	public static final boolean isDCT(int marker) {
		boolean isDCT;
		switch (marker) {
			case SOF0:
			case SOF1:
			case SOF2:
			case SOF5:
			case SOF6:
			case SOF9:
			case SOFA:
			case SOFD:
			case SOFE:
				isDCT=true; break;
			default:
				isDCT=false; break;
		}
		return isDCT;
	}
	
	public static final boolean isLossless(int marker) {
		boolean isLossless;
		switch (marker) {
			case SOF3:
			case SOF7:
			case SOFB:
			case SOFF:
				isLossless=true; break;
			default:
				isLossless=false; break;
		}
		return isLossless;
	}
	
	private static class MarkerDictionaryEntry {
		int markercode;
		String abbreviation;
		String description;
		
		MarkerDictionaryEntry(int markercode,String abbreviation,String description) {
			this.markercode = markercode;
			this.abbreviation = abbreviation;
			this.description = description;
		}
	};
	
	private static final MarkerDictionaryEntry[] markerDictionaryTable = {
		new MarkerDictionaryEntry(APP0, "APP0", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP1, "APP1", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP2, "APP2", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP3, "APP3", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP4, "APP4", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP5, "APP5", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP6, "APP6", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP7, "APP7", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP8, "APP8", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APP9, "APP9", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPA, "APPA", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPB, "APPB", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPC, "APPC", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPD, "APPD", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPE, "APPE", "Reserved for Application Use"),
		new MarkerDictionaryEntry(APPF, "APPF", "Reserved for Application Use"),

		new MarkerDictionaryEntry(COM, "COM", "Comment"),
		new MarkerDictionaryEntry(DAC, "DAC", "Define Arithmetic Conditioning Table(s)"),
		new MarkerDictionaryEntry(DHP, "DHP", "Define Hierarchical Progression"),
		new MarkerDictionaryEntry(DHT, "DHT", "Define Huffman Table(s)"),
		new MarkerDictionaryEntry(DNL, "DNL", "Define Number of Lines"),
		new MarkerDictionaryEntry(DQT, "DQT", "Define Quantization Table(s)"),
		new MarkerDictionaryEntry(DRI, "DRI", "Define Restart Interval"),
		new MarkerDictionaryEntry(EOI, "EOI", "End of Image (JPEG 2000 EOC End of codestream)"),
		new MarkerDictionaryEntry(EXP, "EXP", "Expand Reference Image(s)"),

		new MarkerDictionaryEntry(JPG, "JPG", "Reserved for JPEG extensions"),

		new MarkerDictionaryEntry(RST0, "RST0", "Restart with modulo 8 counter 0"),
		new MarkerDictionaryEntry(RST1, "RST1", "Restart with modulo 8 counter 1"),
		new MarkerDictionaryEntry(RST2, "RST2", "Restart with modulo 8 counter 2"),
		new MarkerDictionaryEntry(RST3, "RST3", "Restart with modulo 8 counter 3"),
		new MarkerDictionaryEntry(RST4, "RST4", "Restart with modulo 8 counter 4"),
		new MarkerDictionaryEntry(RST5, "RST5", "Restart with modulo 8 counter 5"),
		new MarkerDictionaryEntry(RST6, "RST6", "Restart with modulo 8 counter 6"),
		new MarkerDictionaryEntry(RST7, "RST7", "Restart with modulo 8 counter 7"),

		new MarkerDictionaryEntry(SOF0, "SOF0", "Huffman Baseline DCT"),
		new MarkerDictionaryEntry(SOF1, "SOF1", "Huffman Extended Sequential DCT"),
		new MarkerDictionaryEntry(SOF2, "SOF2", "Huffman Progressive DCT"),
		new MarkerDictionaryEntry(SOF3, "SOF3", "Huffman Lossless Sequential"),
		new MarkerDictionaryEntry(SOF5, "SOF5", "Huffman Differential Sequential DCT"),
		new MarkerDictionaryEntry(SOF6, "SOF6", "Huffman Differential Progressive DCT"),
		new MarkerDictionaryEntry(SOF7, "SOF7", "Huffman Differential Lossless"),
		new MarkerDictionaryEntry(SOF9, "SOF9", "Arithmetic Extended Sequential DCT"),
		new MarkerDictionaryEntry(SOFA, "SOFA", "Arithmetic Progressive DCT"),
		new MarkerDictionaryEntry(SOFB, "SOFB", "Arithmetic Lossless Sequential"),
		new MarkerDictionaryEntry(SOFD, "SOFD", "Arithmetic Differential Sequential DCT"),
		new MarkerDictionaryEntry(SOFE, "SOFE", "Arithmetic Differential Progressive DCT"),
		new MarkerDictionaryEntry(SOFF, "SOFF", "Arithmetic Differential Lossless"),

		new MarkerDictionaryEntry(SOF55, "SOF55", "LS"),

		new MarkerDictionaryEntry(SOI, "SOI", "Start of Image"),
		new MarkerDictionaryEntry(SOS, "SOS", "Start of Scan"),
		new MarkerDictionaryEntry(TEM, "TEM", "Temporary use with Arithmetic Encoding"),

		new MarkerDictionaryEntry(SOC, "SOC", "Start of codestream"),
		new MarkerDictionaryEntry(SOT, "SOT", "Start of tile-part"),
		new MarkerDictionaryEntry(SOD, "SOD", "Start of data"),
        //new MarkerDictionaryEntry(EOC, "EOC", "End of codestream"),          // same as JPEG EOI
		new MarkerDictionaryEntry(SIZ, "SIZ", "Image and tile size"),
		new MarkerDictionaryEntry(COD, "COD", "Coding style default"),
		new MarkerDictionaryEntry(COC, "COC", "Coding style component"),
		new MarkerDictionaryEntry(RGN, "RGN", "Rgeion-of-interest"),
		new MarkerDictionaryEntry(QCD, "QCD", "Quantization default"),
		new MarkerDictionaryEntry(QCC, "QCC", "Quantization component"),
		new MarkerDictionaryEntry(POC, "POC", "Progression order change"),
		new MarkerDictionaryEntry(TLM, "TLM", "Tile-part lengths"),
		new MarkerDictionaryEntry(PLM, "PLM", "Packet length, main header"),
		new MarkerDictionaryEntry(PLT, "PLT", "Packet length, tile-part header"),
		new MarkerDictionaryEntry(PPM, "PPM", "Packet packer headers, main header"),
		new MarkerDictionaryEntry(PPT, "PPT", "Packet packer headers, tile-part header"),
		new MarkerDictionaryEntry(SOP, "SOP", "Start of packet"),
		new MarkerDictionaryEntry(EPH, "EPH", "End of packet header"),
		new MarkerDictionaryEntry(CRG, "CRG", "Component registration"),
		new MarkerDictionaryEntry(COM2K, "COM", "Comment (JPEG 2000)"),

		new MarkerDictionaryEntry(FF30, "FF30", "Reserved"),
		new MarkerDictionaryEntry(FF31, "FF31", "Reserved"),
		new MarkerDictionaryEntry(FF32, "FF32", "Reserved"),
		new MarkerDictionaryEntry(FF33, "FF33", "Reserved"),
		new MarkerDictionaryEntry(FF34, "FF34", "Reserved"),
		new MarkerDictionaryEntry(FF35, "FF35", "Reserved"),
		new MarkerDictionaryEntry(FF36, "FF36", "Reserved"),
		new MarkerDictionaryEntry(FF37, "FF37", "Reserved"),
		new MarkerDictionaryEntry(FF38, "FF38", "Reserved"),
		new MarkerDictionaryEntry(FF39, "FF39", "Reserved"),
		new MarkerDictionaryEntry(FF3A, "FF3A", "Reserved"),
		new MarkerDictionaryEntry(FF3B, "FF3B", "Reserved"),
		new MarkerDictionaryEntry(FF3C, "FF3C", "Reserved"),
		new MarkerDictionaryEntry(FF3D, "FF3D", "Reserved"),
		new MarkerDictionaryEntry(FF3E, "FF3E", "Reserved"),
		new MarkerDictionaryEntry(FF3F, "FF3F", "Reserved")
	};

	private static final Map<Integer,MarkerDictionaryEntry> mapOfMarkerToDictionaryEntry = new HashMap<Integer,MarkerDictionaryEntry>();
	
	static {
		for (MarkerDictionaryEntry e : markerDictionaryTable) {
			mapOfMarkerToDictionaryEntry.put(new Integer(e.markercode),e);
		}
	}
	
	public static final String getAbbreviation(int marker) {
		MarkerDictionaryEntry e = mapOfMarkerToDictionaryEntry.get(new Integer(marker));
		return e == null ? "" : e.abbreviation;
	}

	public static final String getDescription(int marker) {
		MarkerDictionaryEntry e = mapOfMarkerToDictionaryEntry.get(new Integer(marker));
		return e == null ? "" : e.description;
	}

}

