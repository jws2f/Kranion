/* Copyright (c) 2014-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

/**
 * <p>A JPEG SOF Marker Segment.</p>
 *
 * @author	dclunie
 */
public class MarkerSegmentSOF {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/MarkerSegmentSOF.java,v 1.4 2015/10/17 21:20:52 dclunie Exp $";
	
	private int  marker;
	private int  SamplePrecision;
	private int  nLines;
	private int  nSamplesPerLine;
	private int  nComponentsInFrame;
	private int[] ComponentIdentifier;
	private int[] HorizontalSamplingFactor;
	private int[] VerticalSamplingFactor;
	private int[] QuantizationTableDestinationSelector;

	public int  getMarker() { return marker; }

	public int  getSamplePrecision() { return SamplePrecision; }
	public int  getNLines() { return nLines; }
	public int  getNSamplesPerLine() { return nSamplesPerLine; }
	public int  getNComponentsInFrame() { return nComponentsInFrame; }

	public int[] getHorizontalSamplingFactor() { return HorizontalSamplingFactor; }
	public int[] getVerticalSamplingFactor()   { return VerticalSamplingFactor; }

	public MarkerSegmentSOF(int marker,byte[] b,int length) throws Exception {
		this.marker = marker;
		
		SamplePrecision    = Utilities.extract8(b,0);
		nLines             = Utilities.extract16be(b,1);
		nSamplesPerLine    = Utilities.extract16be(b,3);
		nComponentsInFrame = Utilities.extract8(b,5);
		
		int lengthExpected = 6+nComponentsInFrame*3;
		 if (length != lengthExpected) {
			throw new Exception("Incorrect length of SOF Parameters Marker Segment, expected "+lengthExpected+" (based on nComponentsInFrame "+nComponentsInFrame+") but was "+length);
		}
		
		ComponentIdentifier                 = new int[nComponentsInFrame];
		HorizontalSamplingFactor            = new int[nComponentsInFrame];
		VerticalSamplingFactor              = new int[nComponentsInFrame];
		QuantizationTableDestinationSelector= new int[nComponentsInFrame];

		for (int i=0; i<nComponentsInFrame; ++i) {
			ComponentIdentifier[i]                  = Utilities.extract8(b,6+i*3);
			HorizontalSamplingFactor[i]             = Utilities.extract8(b,6+i*3+1) >> 4;
			VerticalSamplingFactor[i]               = Utilities.extract8(b,6+i*3+1) & 0x0f;
			QuantizationTableDestinationSelector[i] = Utilities.extract8(b,6+i*3+2);
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\n\t"+Markers.getAbbreviation(marker)+":\n");
		buf.append("\t\t SamplePrecision = "	+SamplePrecision+"\n");
		buf.append("\t\t nLines = "             +nLines+"\n");
		buf.append("\t\t nSamplesPerLine = "    +nSamplesPerLine+"\n");
		buf.append("\t\t nComponentsInFrame = " +nComponentsInFrame+"\n");
		for (int i=0; i<nComponentsInFrame; ++i) {
			buf.append("\t\t component " +i+"\n");
			buf.append("\t\t\t ComponentIdentifier = "                  +ComponentIdentifier[i]+"\n");
			buf.append("\t\t\t HorizontalSamplingFactor = "             +HorizontalSamplingFactor[i]+"\n");
			buf.append("\t\t\t VerticalSamplingFactor = "               +VerticalSamplingFactor[i]+"\n");
			buf.append("\t\t\t QuantizationTableDestinationSelector = " +QuantizationTableDestinationSelector[i]+"\n");
		}
		return buf.toString();
	}

}

