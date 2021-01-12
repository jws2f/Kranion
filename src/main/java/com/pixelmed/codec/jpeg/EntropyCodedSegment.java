/* Copyright (c) 2014-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.awt.Rectangle;
import java.awt.Shape;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * <p>A JPEG Entropy Coded Segment.</p>
 *
 * <p>Development of this class was supported by funding from MDDX Research and Informatics.</p>
 *
 * @author	dclunie
 */
public class EntropyCodedSegment {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/EntropyCodedSegment.java,v 1.24 2016/01/16 13:30:09 dclunie Exp $";

	private boolean copying;
	private boolean decompressing;

	private OutputArrayOrStream[] decompressedOutputPerComponent;

	private boolean isHuffman;
	private boolean isDCT;
	private boolean isLossless;

	private ByteArrayOutputStream copiedBytes;
		
 	private final MarkerSegmentSOS sos;
 	private final MarkerSegmentSOF sof;
 	private final Map<String,HuffmanTable> htByClassAndIdentifer;
 	private final Map<String,QuantizationTable> qtByIdentifer;

 	private final int nComponents;
 	private final int[] DCEntropyCodingTableSelector;
 	private final int[] ACEntropyCodingTableSelector;
 	private final int[] HorizontalSamplingFactor;
 	private final int[] VerticalSamplingFactor;
		
 	private final int maxHorizontalSamplingFactor;
 	private final int maxVerticalSamplingFactor;
	
	private final int nMCUHorizontally;
	
	private final Vector<Shape> redactionShapes;

	// stuff for lossless decompression ...
	private final int predictorForFirstSample;
 	private final int[] predictorForComponent;
	private final int predictorSelectionValue;

	// these are class level and used by getOneLosslessValue() to maintain state (updates them) and initialized by constructor
 	private int[] rowNumberAtBeginningOfRestartInterval;	// indexed by component number, not final since set at beginning of each
 	private final int[] rowLength;							// indexed by component number
 	private final int[] currentRowNumber;					// indexed by component number
 	private final int[] positionWithinRow;					// indexed by component number
 	private final int[][] previousReconstructedRow;			// indexed by component number, positionWithinRow
 	private final int[][] currentReconstructedRow;			// indexed by component number, positionWithinRow

	// stuff for bit extraction ...
	// copied from com.pixelmed.scpecg.HuffmanDecoder ...
	private byte[] bytesToDecompress;
	private int availableBytes;
	private int byteIndex;
	private int bitIndex;
	private int currentByte;
	private int currentBits;
	private int haveBits;

	private static final int[] extractBitFromByteMask = { 0x80,0x40,0x20,0x10,0x08,0x04,0x02,0x01 };
	
	private final void getEnoughBits(int wantBits) throws Exception {
		while (haveBits < wantBits) {
			if (bitIndex > 7) {
				if (byteIndex < availableBytes) {
					currentByte=bytesToDecompress[byteIndex++];
//System.err.println("currentByte["+byteIndex+"] now = 0x"+Integer.toHexString(currentByte&0xff)+" "+Integer.toBinaryString(currentByte&0xff));
					bitIndex=0;
				}
				else {
					throw new Exception("No more bits (having decompressed "+byteIndex+" dec bytes)");
				}
			}
			int newBit = (currentByte & extractBitFromByteMask[bitIndex++]) == 0 ? 0 : 1;
			currentBits = (currentBits << 1) + newBit;
			++haveBits;
		}
//System.err.println("getEnoughBits(): returning "+haveBits+" bits "+Integer.toBinaryString(currentBits)+" (ending at byte "+byteIndex+" bit "+(bitIndex-1)+")");
	}
	
	private int writeByte;		// only contains meaningful content when writeBitIndex > 0
	private int writeBitIndex;	// 0 means ready to write 1st (high) bit to writeByte, 7 means ready to write last (low) bit to writeByte, will transiently (inside writeBits only) be 8 to signal new byte needed
	
	private final void initializeWriteBits() {
		copiedBytes = new ByteArrayOutputStream();
		writeByte = 0;
		writeBitIndex = 0;	// start writing into 1st (high) bit of writeByte
	}
	
	private final void flushWriteBits() {
		if (writeBitIndex > 0) {
			// bits have been written to writeByte so need to pad it with 1s and write it
			while (writeBitIndex < 8) {
				writeByte = writeByte | extractBitFromByteMask[writeBitIndex];
				++writeBitIndex;
			}
			copiedBytes.write(writeByte);
			if ((writeByte&0xff) == 0xff) {
				copiedBytes.write(0);	// stuffed zero byte after 0xff to prevent being considered marker
			}
			writeByte=0;
			writeBitIndex=0;
		}
		// else have not written any bits to writeByte, so do nothing
	}
	
	private final void writeBits(int bits,int nBits) {
//System.err.println("writeBits(): writing "+nBits+" bits "+Integer.toBinaryString(bits));
		if (nBits > 0) {
			for (int i=nBits-1; i>=0; --i) {
				final int whichBitMask = 1 << i;			// bits are "big endian"
				final int bitIsSet = bits & whichBitMask;	// zero or not zero
				// do not need to check writeBitIndex before "writing" ... will always be "ready"
				if (bitIsSet != 0) {
					writeByte = writeByte | extractBitFromByteMask[writeBitIndex];
				}
				++writeBitIndex;
				if (writeBitIndex > 7) {
//System.err.println("writeBits(): wrote = 0x"+Integer.toHexString(writeByte&0xff)+" "+Integer.toBinaryString(writeByte&0xff));
					copiedBytes.write(writeByte);
					if ((writeByte&0xff) == 0xff) {
						copiedBytes.write(0);	// stuffed zero byte after 0xff to prevent being considered marker
					}
					writeByte=0;
					writeBitIndex=0;
				}
			}
		}
	}


	
	private HuffmanTable usingTable = null;
	
//int counter = 0;
	
	// Use 10918-1 F.2 Figure F.16 decode procedure
	
	/**
	 * <p>Decode a single value.</p>
	 *
	 * @return	the decoded value
	 */
	private final int decode()  throws Exception {
		final int[] MINCODE = usingTable.getMINCODE();
		final int[] MAXCODE = usingTable.getMAXCODE();
		final int[] VALPTR  = usingTable.getVALPTR();
		final int[] HUFFVAL = usingTable.getHUFFVAL();
	
		int I=1;
		getEnoughBits(I);		// modifies currentBits
		int CODE = currentBits;
		while (I<MAXCODE.length && CODE > MAXCODE[I]) {
		//while (CODE > MAXCODE[I]) {
			++I;
//System.err.println("I = "+I);
			getEnoughBits(I);	// modifies currentBits
			CODE = currentBits;
//System.err.println("CODE "+Integer.toBinaryString(CODE));
//System.err.println("compare to MAXCODE[I] "+(I<MAXCODE.length ? Integer.toBinaryString(MAXCODE[I]) : "out of MAXCODE entries"));
		}
//System.err.println("Found CODE "+Integer.toBinaryString(CODE));
		int VALUE = 0;
		if (I<MAXCODE.length) {
			int J = VALPTR[I];
//System.err.println("Found VALPTR base "+J);
			J = J + CODE - MINCODE[I];
//System.err.println("Found VALPTR offset by code "+J);
			VALUE = HUFFVAL[J];
//System.err.println("Found VALUE "+VALUE+" dec (0x"+Integer.toHexString(VALUE)+")");
//System.err.println("HUFF_DECODE: "+VALUE+" COUNTER "+counter);
//++counter;
		}
		else {
			//we have exceeded the maximum coded value specified :(
			// copy IJG behavior in this situation from jdhuff.c "With garbage input we may reach the sentinel value l = 17" ... "fake a zero as the safest result"
//System.err.println("Bad Huffman code "+Integer.toBinaryString(CODE)+" so use VALUE "+VALUE+" dec (0x"+Integer.toHexString(VALUE)+")");
		}
		if (copying) { writeBits(currentBits,haveBits); }
		currentBits=0;
		haveBits=0;
		return VALUE;
	}
	
	private final int getValueOfRequestedLength(int wantBits) throws Exception {
		getEnoughBits(wantBits);	// modifies currentBits
		final int value = currentBits;
//System.err.println("getValueOfRequestedLength(): wantBits="+wantBits+" : Got value "+value+" dec (0x"+Integer.toHexString(value)+")");
		if (copying) { writeBits(currentBits,haveBits); }
		currentBits=0;
		haveBits=0;
		return value;
	}

	// values above index 11 only occur for 12 bit process ...
	private int[] dcSignBitMask = { 0x00/*na*/,0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80,0x100,0x200,0x400,0x800,0x1000,0x2000,0x4000 /*no entry for 16*/};
	private int[] maxAmplitude  = { 0/*na*/,0x02-1,0x04-1,0x08-1,0x10-1,0x20-1,0x40-1,0x80-1,0x100-1,0x200-1,0x400-1,0x800-1,0x1000-1,0x2000-1,0x4000-1,0x8000-1 /*no entry for 16*/};

	private final int convertSignAndAmplitudeBitsToValue(int value,int length) throws Exception {
		// see P&M Table 11-1 page 190 and Table 11-4 page 193 (same for DC and AC)
		if (length > 0) {
//System.err.println("dcSignBitMask = "+Integer.toHexString(dcSignBitMask[length]));
			if ((value & dcSignBitMask[length]) == 0) {
//System.err.println("Have sign bit");
				value = value - maxAmplitude[length];
			}
		}
		return value;
	}
	
	private final void writeEntropyCodedAllZeroACCoefficients() {
		// write a single EOB code, which is rrrrssss = 0x00;
		writeBits(usingTable.getEOBCode(),usingTable.getEOBCodeLength());
	}
	

	/**
	 * <p>Set up the environment to decode an EntropyCodedSeqment to dump, redact or copy as required.</p>
	 *
	 * @param	sos								SOS marker segment contents
	 * @param	sof								SOF marker segment contents
	 * @param	htByClassAndIdentifer			Huffman tables
	 * @param	qtByIdentifer					quantization tables
	 * @param	nMCUHorizontally				the number of MCUs in a single row
	 * @param	redactionShapes					a Vector of Shape that are Rectangle
	 * @param	copying							true if copying
	 * @param	dumping							true if dumping
	 * @param	decompressing					true if decompressing
	 * @param	decompressedOutput				the decompressed output (with specified or default endianness if precision &gt; 8)
	 * @throws Exception						if JPEG process not supported
	 */
	public EntropyCodedSegment(MarkerSegmentSOS sos,MarkerSegmentSOF sof,Map<String,HuffmanTable> htByClassAndIdentifer,Map<String,QuantizationTable> qtByIdentifer,int nMCUHorizontally,Vector<Shape> redactionShapes,boolean copying,boolean dumping,boolean decompressing,Parse.DecompressedOutput decompressedOutput) throws Exception {
 		this.sos = sos;
 		this.sof = sof;
 		this.htByClassAndIdentifer = htByClassAndIdentifer;
 		this.qtByIdentifer = qtByIdentifer;
		this.nMCUHorizontally = nMCUHorizontally;
		this.redactionShapes = redactionShapes;
		this.copying = copying;
		// dumping is not used other than in this constructor
		this.decompressing = decompressing;
		this.decompressedOutputPerComponent = decompressedOutput == null ? null : decompressedOutput.getDecompressedOutputPerComponent();
		
		this.isHuffman = Markers.isHuffman(sof.getMarker());
		if (!isHuffman) {
			throw new Exception("Only Huffman processes supported (not "+Markers.getAbbreviation(sof.getMarker())+" "+Markers.getDescription(sof.getMarker())+")");
		}
		this.isDCT = Markers.isDCT(sof.getMarker());
		this.isLossless = Markers.isLossless(sof.getMarker());

		nComponents = sos.getNComponentsPerScan();
		DCEntropyCodingTableSelector = sos.getDCEntropyCodingTableSelector();
		ACEntropyCodingTableSelector = sos.getACEntropyCodingTableSelector();
		HorizontalSamplingFactor = sof.getHorizontalSamplingFactor();
		VerticalSamplingFactor   = sof.getVerticalSamplingFactor();
		
		maxHorizontalSamplingFactor = max(HorizontalSamplingFactor);
//System.err.println("maxHorizontalSamplingFactor "+maxHorizontalSamplingFactor);
		maxVerticalSamplingFactor   = max(VerticalSamplingFactor);
//System.err.println("maxVerticalSamplingFactor "+maxVerticalSamplingFactor);

		if (isLossless && decompressing) {
//System.err.println("SamplePrecision "+sof.getSamplePrecision());
//System.err.println("SuccessiveApproximationBitPositionLowOrPointTransform "+sos.getSuccessiveApproximationBitPositionLowOrPointTransform());
			predictorForFirstSample = 1 << (sof.getSamplePrecision() - sos.getSuccessiveApproximationBitPositionLowOrPointTransform() - 1);
//System.err.println("predictorForFirstSample "+predictorForFirstSample+" dec");
			predictorForComponent = new int[nComponents];
			predictorSelectionValue = sos.getStartOfSpectralOrPredictorSelection();
//System.err.println("predictorSelectionValue "+predictorSelectionValue);

			rowLength = new int[nComponents];
			currentRowNumber = new int[nComponents];
			positionWithinRow = new int[nComponents];
			rowNumberAtBeginningOfRestartInterval = new int[nComponents];
			previousReconstructedRow = new int[nComponents][];
			currentReconstructedRow = new int[nComponents][];
			for (int c=0; c<nComponents; ++c) {
				//rowLength[c] = sof.getNSamplesPerLine()/sof.getHorizontalSamplingFactor()[c];
				rowLength[c] = (sof.getNSamplesPerLine()-1)/sof.getHorizontalSamplingFactor()[c]+1;		// account for sampling of row lengths not an exact multiple of sampling factor ... hmmm :(
//System.err.println("rowLength["+c+"] "+rowLength[c]);
				currentRowNumber[c] = 0;
				positionWithinRow[c] = 0;
				rowNumberAtBeginningOfRestartInterval[c] = 0;
				previousReconstructedRow[c] = new int[rowLength[c]];
				currentReconstructedRow[c] = new int[rowLength[c]];
			}
		}
		else {
			predictorForFirstSample = 0;	// silence uninitialized warnings
			predictorForComponent = null;
			predictorSelectionValue = 0;
			rowLength = null;
			currentRowNumber = null;
			positionWithinRow = null;
			rowNumberAtBeginningOfRestartInterval = null;
			previousReconstructedRow = null;
			currentReconstructedRow = null;
		}
		
		if (dumping) dumpHuffmanTables();
		//dumpQuantizationTables();
	}

	private final int getOneLosslessValue(int c,int dcEntropyCodingTableSelector,int colMCU,int rowMCU) throws Exception {
		// per P&M page 492 (DIS H-2)
		int prediction = 0;
		if (decompressing) {
			if (currentRowNumber[c] == rowNumberAtBeginningOfRestartInterval[c]) {		// will be true for first row since all rowNumberAtBeginningOfRestartInterval entries are initialized to zero
				if (positionWithinRow[c] == 0)	{	// first sample of first row
//System.err.println("Component "+c+" first sample of first row or first row after beginning of restart interval ... use predictorForFirstSample");
					prediction = predictorForFirstSample;
				}
				else {
//System.err.println("Component "+c+" other than first sample of first row or first row after beginning of restart interval ... use Ra (previous sample in row)");
					prediction = currentReconstructedRow[c][positionWithinRow[c]-1];	// Ra
				}
			}
			else if (positionWithinRow[c] == 0) {						// first sample of subsequent rows
//System.err.println("Component "+c+" first sample of subsequent rows");
				prediction = previousReconstructedRow[c][0];			// Rb for position 0
			}
			else {
				switch(predictorSelectionValue) {
					case 1:	prediction = currentReconstructedRow[c][positionWithinRow[c]-1];	// Ra
							break;
					case 2:	prediction = previousReconstructedRow[c][positionWithinRow[c]];		// Rb
							break;
					case 3:	prediction = previousReconstructedRow[c][positionWithinRow[c]-1];	// Rc
							break;
					case 4:	prediction = currentReconstructedRow[c][positionWithinRow[c]-1] + previousReconstructedRow[c][positionWithinRow[c]] - previousReconstructedRow[c][positionWithinRow[c]-1];		// Ra + Rb - Rc
							break;
					case 5:	prediction = currentReconstructedRow[c][positionWithinRow[c]-1] + ((previousReconstructedRow[c][positionWithinRow[c]] - previousReconstructedRow[c][positionWithinRow[c]-1])>>1);	// Ra + (Rb - Rc)/2
							break;
					case 6:	prediction = previousReconstructedRow[c][positionWithinRow[c]] + ((currentReconstructedRow[c][positionWithinRow[c]-1] - previousReconstructedRow[c][positionWithinRow[c]-1])>>1);	// Rb + (Ra - Rc)/2
							break;
					case 7: prediction = (currentReconstructedRow[c][positionWithinRow[c]-1] + previousReconstructedRow[c][positionWithinRow[c]])>>1;	// (Ra+Rb)/2
							break;
					default:
						throw new Exception("Unrecognized predictor selection value "+predictorSelectionValue);
				}
			}
//System.err.println("prediction ["+currentRowNumber[c]+","+positionWithinRow[c]+"] = "+prediction+" dec (0x"+Integer.toHexString(prediction)+")");
		}
			
		usingTable = htByClassAndIdentifer.get("0+"+Integer.toString(dcEntropyCodingTableSelector));

		final int ssss = decode();	// number of DC bits encoded next
		// see P&M Table 11-1 page 190
		int dcValue = 0;
		if (ssss == 0) {
			dcValue = 0;
		}
		else if (ssss == 16) {	// only occurs for lossless
			dcValue = 32768;
		}
		else {
			final int dcBits = getValueOfRequestedLength(ssss);
			dcValue = convertSignAndAmplitudeBitsToValue(dcBits,ssss);
		}
//System.err.println("encoded difference value ["+currentRowNumber[c]+","+positionWithinRow[c]+"] = "+dcValue+" dec (0x"+Integer.toHexString(dcValue)+")");
		
		int reconstructedValue = 0;
		
		if (decompressing) {
			reconstructedValue = (dcValue + prediction) & 0x0000ffff;
		
//System.err.println("reconstructedValue value ["+currentRowNumber[c]+","+positionWithinRow[c]+"] = "+reconstructedValue+" dec (0x"+Integer.toHexString(reconstructedValue)+")");
		
			currentReconstructedRow[c][positionWithinRow[c]] = reconstructedValue;
		
			++positionWithinRow[c];
			if (positionWithinRow[c] >= rowLength[c]) {
//System.err.println("Component "+c+" starting next row");
				positionWithinRow[c] = 0;
				++currentRowNumber[c];
				int[] holdRow = previousReconstructedRow[c];
				previousReconstructedRow[c] = currentReconstructedRow[c];
				currentReconstructedRow[c] = holdRow;	// values do not matter, will be overwritten, saves deallocating and reallocating
			}
		}
		
		return reconstructedValue;	// meaingless unless decompressing, but still need to have absorbed bits from input to stay in sync
	}
	
	// A "data unit" is the "smallest logical unit that can be processed", which in the case of DCT-based processes is one 8x8 block of coefficients (P&M page 101)
	private final void getOneDCTDataUnit(int dcEntropyCodingTableSelector,int acEntropyCodingTableSelector,boolean redact) throws Exception {
		usingTable = htByClassAndIdentifer.get("0+"+Integer.toString(dcEntropyCodingTableSelector));
		{
			final int ssss = decode();	// number of DC bits encoded next
			// see P&M Table 11-1 page 190
			int dcValue = 0;
			if (ssss == 0) {
				dcValue = 0;
			}
			else if (ssss == 16) {	// only occurs for lossless
				dcValue = 32768;
			}
			else {
				final int dcBits = getValueOfRequestedLength(ssss);
				dcValue = convertSignAndAmplitudeBitsToValue(dcBits,ssss);
			}
//System.err.println("Got DC value "+dcValue+" dec (0x"+Integer.toHexString(dcValue)+")");
		}
		
		usingTable = htByClassAndIdentifer.get("1+"+Integer.toString(acEntropyCodingTableSelector));
		
		final boolean restoreCopying = copying;
		if (redact && copying) {
			copying = false;
			writeEntropyCodedAllZeroACCoefficients();
		}
		
		int i=1;
		while (i<64) {
//System.err.println("AC ["+i+"]:");
			final int rrrrssss = decode();
			if (rrrrssss == 0) {
//System.err.println("AC ["+i+"]: "+"EOB");
				break; // EOB
			}
			else if (rrrrssss == 0xF0) {
//System.err.println("AC ["+i+"]: "+"ZRL: 16 zeroes");
				i+=16;
			}
			else {
				// note that ssss of zero is not used for AC (unlike DC) in sequential mode
				final int rrrr = rrrrssss >>> 4;
				final int ssss = rrrrssss & 0x0f;
//System.err.println("AC ["+i+"]: rrrr="+rrrr+" ssss="+ssss);
				final int acBits = getValueOfRequestedLength(ssss);
				final int acValue = convertSignAndAmplitudeBitsToValue(acBits,ssss);
//System.err.println("AC ["+i+"]: "+rrrr+" zeroes then value "+acValue);
				i+=rrrr;	// the number of zeroes
				++i;		// the value we read (ssss is always non-zero, so we always read something
			}
		}
		
		copying = restoreCopying;
	}
	
	private final boolean redactionDecision(int colMCU,int rowMCU,int thisHorizontalSamplingFactor,int thisVerticalSamplingFactor,int maxHorizontalSamplingFactor,int maxVerticalSamplingFactor,int h,int v,Vector<Shape> redactionShapes) {
		// only invoked for DCT so block size is always 8
		final int vMCUSize = 8 * maxVerticalSamplingFactor;
		final int hMCUSize = 8 * maxHorizontalSamplingFactor;
//System.err.println("MCUSize in pixels = "+hMCUSize+" * "+vMCUSize);
		
		final int hMCUOffset = colMCU * hMCUSize;
		final int vMCUOffset = rowMCU * vMCUSize;
//System.err.println("MCUOffset in pixels = "+hMCUOffset+" * "+vMCUOffset);
		
		final int hBlockSize = 8 * maxHorizontalSamplingFactor/thisHorizontalSamplingFactor;
		final int vBlockSize = 8 * maxVerticalSamplingFactor/thisVerticalSamplingFactor;
//System.err.println("BlockSize in pixels = "+hBlockSize+" * "+vBlockSize);
		
		final int xBlock = hMCUOffset + h * hBlockSize;
		final int yBlock = vMCUOffset + v * vBlockSize;
		
		Rectangle blockShape = new Rectangle(xBlock,yBlock,hBlockSize,vBlockSize);
//System.err.println("blockShape "+blockShape);
		
		boolean redact = false;
		if (redactionShapes != null) {
			for (Shape redactionShape : redactionShapes) {
				if (redactionShape.intersects(blockShape)) {
					redact = true;
					break;
				}
			}
		}
		return redact;
	}
	
	private final void writeDecompressedPixel(int c,int decompressedPixel) throws IOException {
		if (sof.getSamplePrecision() <= 8) {
			decompressedOutputPerComponent[c].writeByte(decompressedPixel);
		}
		else {
			// endianness handled by OutputArrayOrStream
			decompressedOutputPerComponent[c].writeShort(decompressedPixel);
		}
	}
	
	private final void getOneMinimumCodedUnit(int nComponents,int[] DCEntropyCodingTableSelector,int[] ACEntropyCodingTableSelector,int[] HorizontalSamplingFactor,int[] VerticalSamplingFactor,int maxHorizontalSamplingFactor,int maxVerticalSamplingFactor,int colMCU,int rowMCU,Vector<Shape> redactionShapes) throws Exception, IOException {
		for (int c=0; c<nComponents; ++c) {
			// See discussion of interleaving of data units within MCUs in P&M section 7.3.5 pages 101-105; always interleaved in sequential mode
			for (int v=0; v<VerticalSamplingFactor[c]; ++v) {
				for (int h=0; h<HorizontalSamplingFactor[c]; ++h) {
//System.err.println("Component "+c+" v "+v+" h "+h);
					boolean redact = redactionDecision(colMCU,rowMCU,HorizontalSamplingFactor[c],VerticalSamplingFactor[c],maxHorizontalSamplingFactor,maxVerticalSamplingFactor,h,v,redactionShapes);
					if (isDCT) {
						getOneDCTDataUnit(DCEntropyCodingTableSelector[c],ACEntropyCodingTableSelector[c],redact);
					}
					else if (isLossless) {
						int decompressedPixel = getOneLosslessValue(c,DCEntropyCodingTableSelector[c],colMCU,rowMCU);
						if (decompressing) {
							writeDecompressedPixel(c,decompressedPixel);
						}
					}
					else {
						throw new Exception("Only DCT or Lossless processes supported (not "+Markers.getAbbreviation(sof.getMarker())+" "+Markers.getDescription(sof.getMarker())+")");
					}
				}
			}
		}
	}
	
	private static final int max(int[] a) {
		int m = Integer.MIN_VALUE;
		for (int i : a) {
			if (i > m) m = i;
		}
		return m;
	}
	
	/**
	 * <p>Decode the supplied bytes that comprise a complete EntropyCodedSeqment and redact or copy them as required.</p>
	 *
	 * @param	bytesToDecompress	the bytes in the EntropyCodedSeqment
	 * @param	mcuCount			the number of MCUs encoded by this EntropyCodedSeqment
	 * @param	mcuOffset			the number of MCUs that have previously been read for the frame containing this EntropyCodedSeqment
	 * @return						the bytes in a copy of the EntropyCodedSeqment appropriately redacted
	 * @throws Exception			if bad things happen parsing the EntropyCodedSeqment, like running out of bits, caused by malformed input
	 * @throws IOException		if bad things happen reading or writing the bytes
	 */
	public final byte[] finish(byte[] bytesToDecompress,int mcuCount,int mcuOffset) throws Exception, IOException {
		this.bytesToDecompress = bytesToDecompress;
		availableBytes = this.bytesToDecompress.length;
		byteIndex = 0;
		bitIndex = 8;	// force fetching byte the first time
		haveBits = 0;	// don't have any bits to start with
		
		if (copying) {
			initializeWriteBits();		// will create a new ByteArrayOutputStream
		}

		if (rowNumberAtBeginningOfRestartInterval != null) {	// do not need to do this unless decompressiong lossless
			for (int c=0; c<nComponents; ++c) {
//System.err.println("Setting rowNumberAtBeginningOfRestartInterval["+c+"] to "+currentRowNumber[c]);
				rowNumberAtBeginningOfRestartInterval[c] = currentRowNumber[c];	// for lossless decompression predictor selection
			}
		}
		//try {
		
		for (int mcu=0; mcu<mcuCount; ++mcu) {
			int rowMCU = mcuOffset / nMCUHorizontally;
			int colMCU = mcuOffset % nMCUHorizontally;
//System.err.println("MCU ("+rowMCU+","+colMCU+")");
			getOneMinimumCodedUnit(nComponents,DCEntropyCodingTableSelector,ACEntropyCodingTableSelector,HorizontalSamplingFactor,VerticalSamplingFactor,maxHorizontalSamplingFactor,maxVerticalSamplingFactor,colMCU,rowMCU,redactionShapes);
			++mcuOffset;
		}

//System.err.println("Finished ...");
//System.err.println("availableBytes = "+availableBytes);
//System.err.println("byteIndex = "+byteIndex);
//System.err.println("bitIndex = "+bitIndex);
//System.err.println("currentByte = "+currentByte);
//System.err.println("currentBits = "+currentBits);
//System.err.println("haveBits = "+haveBits);
		
		//}
		//catch (Exception e) {
		//	e.printStackTrace(System.err);
		//}

		if (copying) {
			flushWriteBits();		// will pad appropriately to byte boundary
		}
		
		return copying ? copiedBytes.toByteArray() : null;
	}
		
	private final void dumpHuffmanTables() {
		System.err.print("\n");
		for (HuffmanTable ht : htByClassAndIdentifer.values()) {
			System.err.print(ht.toString());
		}
	}
	
	private final void dumpQuantizationTables() {
		System.err.print("\n");
		for (QuantizationTable qt : qtByIdentifer.values()) {
			System.err.print(qt.toString());
		}
	}
	
}

