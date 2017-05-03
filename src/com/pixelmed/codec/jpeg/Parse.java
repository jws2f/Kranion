/* Copyright (c) 2014-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.awt.Rectangle;
import java.awt.Shape;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * <p>A class to parse a JPEG bitstream.</p>
 *
 * <p>Includes the ability to selectively redact blocks and leave other blocks alone, to permit "lossless" redaction.</p>
 *
 * <p>Includes the ability to decompress lossless JPEG.</p>
 *
 * <p>Development of this class was supported by funding from MDDX Research and Informatics.</p>
 *
 * @author	dclunie
 */
public class Parse {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/Parse.java,v 1.17 2016/01/16 13:30:09 dclunie Exp $";
	
	private static int getLargestSamplingFactor(int[] factors) {
		int largest = 0;
		for (int factor : factors) {
			if (factor > largest) {
				largest = factor;
			}
		}
		return largest;
	}
	
	private static final void writeMarkerAndLength(OutputStream out, int marker,int length) throws IOException {
		out.write(0xff);
		out.write(marker&0xff);
		out.write((length>>>8)&0xff);
		out.write(length&0xff);
	}
	
	private static final void writeVariableLengthMarkerSegment(OutputStream out, int marker,int length,byte[] b) throws IOException {
		writeMarkerAndLength(out,marker,length);
		out.write(b,0,length-2);
	}

	public static class DecompressedOutput {
		private int nComponents;
		private OutputArrayOrStream[] decompressedOutputPerComponent;
		private File fileBasis;
		private ByteOrder order;
		
		public DecompressedOutput() {
		}
		
		/*
		 * @param	fileBasis	will be used literally if one component, with an appended suffix _n before the file extension (if any), where n is the component number from 0
		 */
		public DecompressedOutput(File fileBasis,ByteOrder order) {
			this.fileBasis = fileBasis;
			this.order = order;
		}
		
		public OutputArrayOrStream[] getDecompressedOutputPerComponent() { return decompressedOutputPerComponent; }
		
		public void configureDecompressedOutput(MarkerSegmentSOF sof) throws IOException {
			nComponents = sof.getNComponentsInFrame();
			decompressedOutputPerComponent = new OutputArrayOrStream[nComponents];
			if (fileBasis == null) {
				int length = sof.getNSamplesPerLine() * sof.getNLines();
				for (int c=0; c<nComponents; ++c) {
					decompressedOutputPerComponent[c] = new OutputArrayOrStream();
					if (sof.getSamplePrecision() <= 8) {
						decompressedOutputPerComponent[c].allocateByteArray(length);
					}
					else {
						decompressedOutputPerComponent[c].allocateShortArray(length);
					}
				}
			}
			else {
				if (nComponents == 1) {
					decompressedOutputPerComponent[0] = new OutputArrayOrStream(new FileOutputStream(fileBasis),order);
				}
				else {
					File parent = fileBasis.getParentFile();	// may be null
					String baseFileName = fileBasis.getName();
					String prefix;
					String suffix;
					int periodPosition = baseFileName.lastIndexOf('.');
					if (periodPosition > -1) {
						if (periodPosition > 0) {
							prefix = baseFileName.substring(0,periodPosition);		// copies from 0 to periodPosition-1
						}
						else {
							prefix = "";
						}
						suffix = baseFileName.substring(periodPosition);			// copies the period to the end
					}
					else {
						prefix = baseFileName;
						suffix = "";
					}
					for (int c=0; c<nComponents; ++c) {
						String componentFileName = prefix + c + suffix;
//System.err.println("Parse.DecompressedOutput.configureDecompressedOutput(): componentFileName["+c+"] = "+componentFileName);
						decompressedOutputPerComponent[c] = new OutputArrayOrStream(new FileOutputStream(new File(parent,componentFileName)),order);	// OK if parent is null
					}
				}
			}
		}
		
		public void close() throws IOException {
			for (int c=0; c<nComponents; ++c) {
				decompressedOutputPerComponent[c].close();
			}
		}

	}

	public static class MarkerSegmentsFoundDuringParse {
		private MarkerSegmentSOS sos;
		private MarkerSegmentSOF sof;
		private Map<String,HuffmanTable> htByClassAndIdentifer;
		private Map<String,QuantizationTable> qtByIdentifer;
		
		public MarkerSegmentSOS getSOS() { return sos; }
		public MarkerSegmentSOF getSOF() { return sof; }
		public Map<String,HuffmanTable>  getHuffmanTableByClassAndIdentifer() { return htByClassAndIdentifer; }
		public Map<String,QuantizationTable>  getQuantizationTableByIdentifer() { return qtByIdentifer; }
		
		public MarkerSegmentsFoundDuringParse(MarkerSegmentSOS sos,MarkerSegmentSOF sof,Map<String,HuffmanTable> htByClassAndIdentifer,Map<String,QuantizationTable> qtByIdentifer) {
			this.sos = sos;
			this.sof = sof;
			this.htByClassAndIdentifer = htByClassAndIdentifer;
			this.qtByIdentifer = qtByIdentifer;
		}
	}
	
	// follows pattern of dicom3tools appsrc/misc/jpegdump.cc
	
	/**
	 * <p>Parse a JPEG bitstream and either copy to the output redacting any blocks that intersect with the specified locations, or decompress.</p>
	 *
	 * <p>Parsing and redaction is implemented only for baseline (8 bit DCT Huffman).</p>
	 *
	 * <p>Parsing and decompression is implemented only for lossless sequential Huffman.</p>
	 *
	 * @param	in							the input JPEG bitstream
	 * @param	copiedRedactedOutputStream	the output JPEG bitstream, redacted as specified
	 * @param	redactionShapes				a Vector of Shape that are Rectangle
	 * @param	decompressedOutput			the decompressed output (with specified or default endianness if precision &gt; 8)
	 * @return								the marker segments found during parsing
	 * @exception Exception			if bad things happen parsing the JPEG bit stream, caused by malformed input
	 * @exception IOException		if bad things happen reading or writing
	 */
	public static MarkerSegmentsFoundDuringParse parse(InputStream in,OutputStream copiedRedactedOutputStream,Vector<Shape> redactionShapes,DecompressedOutput decompressedOutput) throws Exception, IOException {
		boolean dumping = copiedRedactedOutputStream == null && decompressedOutput == null;
		//boolean dumping = true;
		boolean copying = copiedRedactedOutputStream != null;
		boolean decompressing = decompressedOutput != null;
	
		EntropyCodedSegment ecs = null;					// lazy instantiation of EntropyCodedSegment ... wait until we have relevant marker segments for its constructor
		
		ByteArrayOutputStream byteAccumulator = null;	// recreated for first byte of each EntropyCodedSegment (at start and at each subsequent restart interval)
		
		MarkerSegmentSOS sos = null;
		MarkerSegmentSOF sof = null;
		Map<String,HuffmanTable> htByClassAndIdentifer = new HashMap<String,HuffmanTable>();
		Map<String,QuantizationTable> qtByIdentifer = new HashMap<String,QuantizationTable>();
		int restartinterval = 0;
		
		int mcuOffset = 0;
		int nMCUHorizontally = 0;
		int nMCUVertically = 0;
		int mcuCountPerEntropyCodedSegment = 0;

		int offset=0;
		int markerprefix = in.read();
		while (true) {
			if (markerprefix == -1) {
				if (dumping) System.err.print("End of file\n");
				break;
			}
			if (markerprefix != 0xff) {		// byte of entropy-coded segment
				if (byteAccumulator == null) {
					if (dumping) System.err.print("Offset "+Utilities.toPaddedHexString(offset,4)+" Starting new Entropy Coded Segment\n");
					byteAccumulator = new ByteArrayOutputStream();
				}
				byteAccumulator.write(markerprefix);
				++offset;
				markerprefix=in.read();
				continue;
			}
			int marker=in.read();
			if (marker == -1) {
				if (dumping) System.err.print("End of file immediately after marker flag 0xff ... presumably was padding\n");
				break;
			}
			else if (marker == 0xff) {		// 0xff byte of padding
				if (dumping) System.err.print("Offset "+Utilities.toPaddedHexString(offset,4)+" Fill byte 0xff\n");
				++offset;
				markerprefix=marker;		// the first 0xff is padding, the 2nd may be the start of a marker
				continue;
			}
			// ignore doing_jpeg2k_tilepart for now :(
			else if (marker == 0) {			// 0xff byte of entropy-coded segment ... ignore following zero byte
				if (dumping) System.err.print("Offset "+Utilities.toPaddedHexString(offset,4)+" Encoded 0xff in entropy-coded segment followed by stuffed zero byte\n");
				if (byteAccumulator == null) {
					if (dumping) System.err.print("Offset "+Utilities.toPaddedHexString(offset,4)+" Starting new Entropy Coded Segment\n");
					byteAccumulator = new ByteArrayOutputStream();
				}
				byteAccumulator.write(markerprefix);
				markerprefix=in.read();
				offset+=2;
				continue;
			}
			// ignore doing_jpegls and zero stuffed bit instead of byte for now :(

			// Definitely have a marker ...
			
			if (byteAccumulator != null) {
				// process any Entropy Coded Segment bytes accumulated so far ...
				if (ecs == null) {
					// need to figure out the sampling factors if this is the first Entropy Coded Segment, so that EntropyCodedSegment.finish() knows how many to process and where it is at
					
					if (sof == null) {
						throw new Exception("Error - compressed data without preceding SOF marker segment");
					}
					
					int blockSize = Markers.isDCT(sof.getMarker()) ? 8 : 1;
					
					int horizontalSamplesPerMCU = blockSize * getLargestSamplingFactor(sof.getHorizontalSamplingFactor());
//System.err.println("horizontalSamplesPerMCU "+horizontalSamplesPerMCU);
					nMCUHorizontally = (sof.getNSamplesPerLine()-1)/horizontalSamplesPerMCU + 1;
//System.err.println("nMCUHorizontally "+nMCUHorizontally);
		
					int verticalSamplesPerMCU = blockSize * getLargestSamplingFactor(sof.getVerticalSamplingFactor());
//System.err.println("verticalSamplesPerMCU "+verticalSamplesPerMCU);
					nMCUVertically = (sof.getNLines()-1)/verticalSamplesPerMCU + 1;					// may need to update this from DNL marker :(
//System.err.println("nMCUVertically "+nMCUVertically);
		
//System.err.println("restartinterval "+restartinterval);
					mcuCountPerEntropyCodedSegment = (restartinterval == 0) ? nMCUHorizontally * nMCUVertically : restartinterval;
//System.err.println("mcuCountPerEntropyCodedSegment "+mcuCountPerEntropyCodedSegment);
					mcuOffset = 0;

					ecs = new EntropyCodedSegment(sos,sof,htByClassAndIdentifer,qtByIdentifer,nMCUHorizontally,redactionShapes,copying,dumping,decompressing,decompressedOutput);
				}
				byte[] bytesToDecompress = byteAccumulator.toByteArray();
//System.err.println("bytesToDecompress length "+bytesToDecompress.length);
//System.err.println("mcuOffset "+mcuOffset);
				int mcuStillNeeded = (nMCUHorizontally * nMCUVertically) - mcuOffset;
//System.err.println("mcuStillNeeded "+mcuStillNeeded);
				int mcuNeededThisInterval = mcuCountPerEntropyCodedSegment > mcuStillNeeded ? mcuStillNeeded : mcuCountPerEntropyCodedSegment;		// Do NOT attempt to read beyond what is needed
//System.err.println("mcuNeededThisInterval "+mcuNeededThisInterval);
				byte[] bytesToCopy = ecs.finish(bytesToDecompress,mcuNeededThisInterval,mcuOffset);
				if (copying) {
					copiedRedactedOutputStream.write(bytesToCopy);		// NB. EntropyCodedSegment.finish() has already done the zero byte stuffing after 0xff values
				}
				byteAccumulator = null;
				mcuOffset += mcuCountPerEntropyCodedSegment;
			}

			marker|=0xff00;			// convention is to express them with the leading ff, so that is what we look up
			
			if (dumping) System.err.print("Offset "+Utilities.toPaddedHexString(offset,4)+" Marker "+Utilities.toPaddedHexString(marker,4)+" "+Markers.getAbbreviation(marker)+" "+Markers.getDescription(marker)+" ");

			offset+=2;	// wait till after we have printed it to increment it
			
			if (Markers.isVariableLengthJPEGSegment(marker)) {
				int length=Utilities.read16be(in);
				if (length == -1) {
					throw new Exception("Error - variable length marker without length at Offset "+Utilities.toPaddedHexString(offset,4));
				}
				else {
					offset+=2;
					if (dumping) System.err.print("length variable "+Utilities.toPaddedHexString(length,2)+" ");
				}
				
				if (length > 2) {
					byte[] b = new byte[length-2];
					int count = in.read(b,0,length-2);
					if (count != length-2) {
						throw new Exception("Error - couldn't read variable length parameter sequence at Offset "+Utilities.toPaddedHexString(offset,4));
					}
					else {
						switch (marker) {
							case Markers.SOS:
								sos = new MarkerSegmentSOS(b,length-2);
								if (dumping) System.err.print(sos);
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							case Markers.SOF0:
							case Markers.SOF1:
							case Markers.SOF2:
							case Markers.SOF3:
							case Markers.SOF5:
							case Markers.SOF6:
							case Markers.SOF7:
							case Markers.SOF9:
							case Markers.SOFA:
							case Markers.SOFB:
							case Markers.SOFD:
							case Markers.SOFE:
							case Markers.SOFF:
							case Markers.SOF55:
								sof = new MarkerSegmentSOF(marker,b,length-2);
								if (dumping) System.err.print(sof);
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								if (decompressing) decompressedOutput.configureDecompressedOutput(sof);
								break;
							case Markers.DHT:
								MarkerSegmentDHT dht = new MarkerSegmentDHT(b,length-2);
								dht.addToMapByClassAndIdentifier(htByClassAndIdentifer);	// hokey, but sometimes multiple tables in one segment, sometimes multiple segments
								if (dumping) System.err.print(dht);
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							case Markers.DQT:
								MarkerSegmentDQT dqt = new MarkerSegmentDQT(b,length-2);
								dqt.addToMapByIdentifier(qtByIdentifer);					// hokey, but sometimes multiple tables in one segment, sometimes multiple segments
								if (dumping) System.err.print(dqt);
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							//case Markers.LSE
							//	break;
							case Markers.DRI:
								if (length == 4) {
									restartinterval = Utilities.extract16be(b,0);
								}
								else if (length == 5) {
									restartinterval = (int)Utilities.extract24be(b,0);
								}
								else if (length == 6) {
									restartinterval = (int)Utilities.extract32be(b,0);
								}
								else {
									throw new Exception("Illegal length "+length+" of restart interval at Offset "+Utilities.toPaddedHexString(offset,4));
								}
								if (dumping) System.err.print("\n\tDRI - Define Restart Interval = "+Utilities.toPaddedHexString(restartinterval,4)+"\n");
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							case Markers.DNL:
								long numberoflines;
								if (length == 4) {
									numberoflines = Utilities.extract16be(b,0);
								}
								else if (length == 5) {
									numberoflines = Utilities.extract24be(b,0);
								}
								else if (length == 6) {
									numberoflines = Utilities.extract32be(b,0);
								}
								else {
									throw new Exception("Illegal length "+length+" of number of lines at Offset "+Utilities.toPaddedHexString(offset,4));
								}
								if (dumping) System.err.print("\n\tDNL - Define Number of Lines = "+Utilities.toPaddedHexString(numberoflines,4)+"\n");
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							//case Markers.COD:
							//	break;
							//case Markers.COM:
							//	// do NOT copy COM marker segments ... may leak identity
							//	break;
							case Markers.APP0:
							case Markers.APP1:
							case Markers.APP2:
								String magic = "";
								{
									StringBuffer magicbuf = new StringBuffer();
									for (int i=0; i<b.length && b[i] != 0; ++i) {
										magicbuf.append(Character.valueOf((char)b[i]));
									}
									magic = magicbuf.toString();
								}
								if (dumping) System.err.print(magic);
								if (marker == Markers.APP0 && magic.equals("JFIF")) {
									if (dumping) System.err.print(new MarkerSegmentAPP0JFIF(b,length-2));
									//if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								}
								// may want to consider not copying unrecognized APPn segments ... may leak identity ... copy everything for now :(
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
							default:
								// may want to consider not copying unrecognized segments ... may leak identity ... copy everything for now :(
								if (copying) writeVariableLengthMarkerSegment(copiedRedactedOutputStream,marker,length,b);
								break;
						}
					}
				}
				else {
					if (dumping) System.err.print("Warning - variable length marker without \"zero\" length (really 2)");
				}
				offset+=(length-2);
			}
			else if (Markers.isNoLengthJPEGSegment(marker)) {
				if (copying) { copiedRedactedOutputStream.write(0xff); copiedRedactedOutputStream.write(marker&0xff);}
				if (marker == Markers.EOI) {
					// stop rather than process padding to end of file, so as not to create spurious empty EntropyCodedSegment
					if (dumping) System.err.print("\n");
					break;
				}
			}
			else {
				int length=Markers.isFixedLengthJPEGSegment(marker);
				switch (length) {
					case 0:
						break;
					case 3:
						{
							int value = in.read();
							if (value != -1) {
								offset+=1;
								if (dumping) System.err.print("length fixed 3 value "+Utilities.toPaddedHexString(value,2)+" ");
								if (copying) { writeMarkerAndLength(copiedRedactedOutputStream,marker,length); copiedRedactedOutputStream.write(value&0xff); }
							}
							else {
								throw new Exception("Error - fixed length 3 marker without value at Offset "+Utilities.toPaddedHexString(offset,4));
							}
						}
						break;
					case 4:
						{
							int value=Utilities.read16be(in);
							if (value != -1) {
								offset+=2;
								if (dumping) System.err.print("length fixed 4 value "+Utilities.toPaddedHexString(value,2)+" ");
								if (copying) { writeMarkerAndLength(copiedRedactedOutputStream,marker,length); copiedRedactedOutputStream.write((value>>>8)&0xff); copiedRedactedOutputStream.write(value&0xff); }
							}
							else {
								throw new Exception("Error - fixed length 4 marker without value at Offset "+Utilities.toPaddedHexString(offset,4));
							}
						}
						break;
					default:
						throw new Exception("Error - fixed length marker with unexpected length "+length+" at Offset "+Utilities.toPaddedHexString(offset,4));
						//break;
				}
			}

			if (dumping) System.err.print("\n");
			markerprefix=in.read();
		}
		
		if (copying) {
			copiedRedactedOutputStream.close();
		}
		if (decompressing) {
			decompressedOutput.close();
		}
		return new MarkerSegmentsFoundDuringParse(sos,sof,htByClassAndIdentifer,qtByIdentifer);
	}

	/**
	 * <p>Parse a JPEG bitstream and copying to the output redacting any blocks that intersect with the specified locations.</p>
	 *
	 * <p>Parsing and redaction is implemented only for baseline (8 bit DCT Huffman).</p>
	 *
	 * @param	in							the input JPEG bitstream
	 * @param	copiedRedactedOutputStream	the output JPEG bitstream, redacted as specified
	 * @param	redactionShapes				a Vector of Shape that are Rectangle
	 * @return								the marker segments found during parsing
	 * @exception Exception			if bad things happen parsing the JPEG bit stream, caused by malformed input
	 * @exception IOException		if bad things happen reading or writing
	 */
	public static MarkerSegmentsFoundDuringParse parse(InputStream in,OutputStream copiedRedactedOutputStream,Vector<Shape> redactionShapes) throws Exception, IOException {
		return parse(in,copiedRedactedOutputStream,redactionShapes,null/*decompressedOutput*/);
	}
	/**
	 * <p>Test utility to read and write a JPEG file to check parsing is sound.</p>
	 *
	 * <p>If only an input file is supplied, will dump rather than copy.</p>
	 *
	 * <p>If a decompressed output file is supplied, will write in big endian if precision greater than 8, and will appended component number before file extension iff more than one component.</p>
	 *
	 * @param	arg	two or three parameters, the input file, the copied compressed output file, and the decompressed output file
	 */
	public static void main(String arg[]) {
		try {
			InputStream in = new FileInputStream(arg[0]);
			OutputStream copiedCompressedOutput = arg.length > 1 && arg[1].length() > 0 ? new FileOutputStream(arg[1]) : null;
			DecompressedOutput decompressedOutput = arg.length > 2 && arg[2].length() > 0 ? new DecompressedOutput(new File(arg[2]),ByteOrder.BIG_ENDIAN) : null;
			long startTime = System.currentTimeMillis();
			parse(in,copiedCompressedOutput,null,decompressedOutput);
			long currentTime = System.currentTimeMillis();
			long runTime = currentTime-startTime;
System.err.println("Took = "+runTime+" ms");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

}

