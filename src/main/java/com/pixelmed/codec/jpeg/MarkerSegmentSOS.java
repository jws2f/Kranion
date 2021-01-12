/* Copyright (c) 2014-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

/**
 * <p>A JPEG SOS Marker Segment.</p>
 *
 * @author	dclunie
 */
public class MarkerSegmentSOS {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/MarkerSegmentSOS.java,v 1.3 2015/10/17 21:20:52 dclunie Exp $";
	
	private int nComponentsPerScan;
	private int[] ScanComponentSelector;
	private int[] DCEntropyCodingTableSelector;
	private int[] ACEntropyCodingTableSelector;
	private int[] MappingTableSelector;			// LS
	private int StartOfSpectralOrPredictorSelection;
	private int EndOfSpectralSelection;
	private int SuccessiveApproximationBitPositionHigh;
	private int SuccessiveApproximationBitPositionLowOrPointTransform;
	
	public int   getNComponentsPerScan() { return nComponentsPerScan; }
	public int[] getDCEntropyCodingTableSelector() { return DCEntropyCodingTableSelector; }
	public int[] getACEntropyCodingTableSelector() { return ACEntropyCodingTableSelector; }
	public int   getStartOfSpectralOrPredictorSelection() { return StartOfSpectralOrPredictorSelection; }
	public int   getSuccessiveApproximationBitPositionLowOrPointTransform() { return SuccessiveApproximationBitPositionLowOrPointTransform; }

	public MarkerSegmentSOS(byte[] b,int length) throws Exception {
		nComponentsPerScan=Utilities.extract8(b,0);
		int lengthExpected = 1+nComponentsPerScan*2+3;
		 if (length != lengthExpected) {
			throw new Exception("Incorrect length of SOS Parameters Marker Segment, expected "+lengthExpected+" (based on nComponentsPerScan "+nComponentsPerScan+") but was "+length);
		}
		ScanComponentSelector       =new int[nComponentsPerScan];
		DCEntropyCodingTableSelector=new int[nComponentsPerScan];
		ACEntropyCodingTableSelector=new int[nComponentsPerScan];
		MappingTableSelector        =new int[nComponentsPerScan];	// LS
		for (int i=0; i<nComponentsPerScan; ++i) {
			ScanComponentSelector[i]       =Utilities.extract8(b,1+i*2);
			DCEntropyCodingTableSelector[i]=Utilities.extract8(b,1+i*2+1) >> 4;
			ACEntropyCodingTableSelector[i]=Utilities.extract8(b,1+i*2+1) & 0x0f;
			MappingTableSelector[i]        =Utilities.extract8(b,1+i*2+1);	// LS
		}
		StartOfSpectralOrPredictorSelection                  =Utilities.extract8(b,1+nComponentsPerScan*2);
		EndOfSpectralSelection                               =Utilities.extract8(b,1+nComponentsPerScan*2+1);
		SuccessiveApproximationBitPositionHigh               =Utilities.extract8(b,1+nComponentsPerScan*2+2) >> 4;
		SuccessiveApproximationBitPositionLowOrPointTransform=Utilities.extract8(b,1+nComponentsPerScan*2+2) & 0x0f;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\n\tSOS:\n");
		buf.append("\t\t nComponentsPerScan = "+nComponentsPerScan+"\n");
		for (int i=0; i<nComponentsPerScan; ++i) {
			buf.append("\t\t component "+i+"\n");
			buf.append("\t\t\t ScanComponentSelector = "+ScanComponentSelector[i]+"\n");
			buf.append("\t\t\t DCEntropyCodingTableSelector = "+DCEntropyCodingTableSelector[i]+"\n");
			buf.append("\t\t\t ACEntropyCodingTableSelector = "+ACEntropyCodingTableSelector[i]+"\n");
			buf.append("\t\t\t MappingTableSelector(LS) = "+MappingTableSelector[i]+"\n");	// LS
		}
		buf.append("\t\t StartOfSpectralOrPredictorSelection/NearLosslessDifferenceBound(LS) = "+StartOfSpectralOrPredictorSelection+"\n");
		buf.append("\t\t EndOfSpectralSelection/InterleaveMode(LS) = "+EndOfSpectralSelection+"\n");
		buf.append("\t\t SuccessiveApproximationBitPositionHigh = "+SuccessiveApproximationBitPositionHigh+"\n");
		buf.append("\t\t SuccessiveApproximationBitPositionLowOrPointTransform = "+SuccessiveApproximationBitPositionLowOrPointTransform+"\n");
		return buf.toString();
	}

}

