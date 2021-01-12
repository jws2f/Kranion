/* Copyright (c) 2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.imageio;

// follow the pattern described in "http://docs.oracle.com/javase/1.5.0/docs/guide/imageio/spec/extending.fm3.html"

import com.pixelmed.codec.jpeg.Markers;
import com.pixelmed.codec.jpeg.Utilities;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class JPEGLosslessImageReaderSpi extends ImageReaderSpi {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/imageio/JPEGLosslessImageReaderSpi.java,v 1.5 2016/01/16 15:07:52 dclunie Exp $";

	static final String vendorName = "PixelMed Publishing, LLC.";
	static final String version = "0.01";
	static final String readerClassName = "com.pixelmed.imageio.JPEGLosslessImageReader";
	static final String description = "PixelMed JPEG Lossless Image Reader";
	
	public static final Class<?>[] inputTypes = { ImageInputStream.class };	// current JavaDoc says STANDARD_INPUT_TYPE is deprecated
	
	static final String[] names = { "jpeg-lossless" };			// this is what Sun JIIO JAI codecs use to recognize JPEG lossless
	static final String[] suffixes = { "ljpeg", "jpl" };		// not "jls", which is JPEG-LS; "ljpeg" was used by USF Mammo
	static final String[] MIMETypes = null;						// current JavaDoc says null or empty array OK
	
	static final String nativeImageMetadataFormatName = "com.pixelmed.imageio.JPEGLosslessMetadata_0.1";
	static final String nativeImageMetadataFormatClassName = "com.pixelmed.imageio.JPEGLosslessMetadata";
	
	public JPEGLosslessImageReaderSpi() {
		super(
			vendorName,
			version,
			names,
			suffixes,
			MIMETypes,
			readerClassName,
			inputTypes,
			null/*writerSpiNames*/,
			false/*supportsStandardStreamMetadataFormat*/,
			null/*nativeStreamMetadataFormatName*/,
			null/*nativeStreamMetadataFormatClassName*/,
			null/*extraStreamMetadataFormatNames*/,
			null/*extraStreamMetadataFormatClassNames*/,
			false/*supportsStandardImageMetadataFormat*/,
			nativeImageMetadataFormatName,
			nativeImageMetadataFormatClassName,
			null/*extraImageMetadataFormatNames*/,
			null/*extraImageMetadataFormatClassNames*/);
	}
	
	public boolean canDecodeInput(Object input) throws IOException {
		// Need SOI Start of Image
		// May be intervening table/misc segments
		// Need SOF3 Huffman Lossless Sequential length variable 0x0b
		boolean canDecode = false;
		try {
			if (input instanceof ImageInputStream) {
				ImageInputStream stream = (ImageInputStream)input;
				byte[] b = new byte[4];
				stream.mark();
				stream.readFully(b,0,2);
				if (b[0] == (byte)0xff && b[1] == (byte)0xd8) {		// have SOI
					int markerprefix = stream.read();
					while (markerprefix == 0xff) {			// keep reading until we have an SOF or until not a marker segment
						int marker = stream.read();
						marker|=0xff00;						// convention is to express them with the leading ff, so that is what we look up
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): have marker "+Utilities.toPaddedHexString(marker,4)+" "+Markers.getAbbreviation(marker));
						// should not have to worry about stuffed bytes in ECS or padding because we never get that far in the stream
						if (Markers.isSOF(marker)) {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): have some type of SOF marker");
							if (marker == Markers.SOF3) {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): have SOF3");
								canDecode = true;
							}
							break;		// stop reading after any SOF
						}
						else if (marker == Markers.SOS) {
							break;		// stop reading at SOS since too late to get SOF3
						}
						else if (Markers.isVariableLengthJPEGSegment(marker)) {
							stream.readFully(b,0,2);
							int length=((b[0]&0xff)<<8) + (b[1]&0xff);	// big endian
							if (length > 2) {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): skipping variable length marker segment length="+length);
								stream.skipBytes(length-2);
							}
							else {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): variable length marker segment with invalid length="+length);
								break;
							}
						}
						else if (Markers.isNoLengthJPEGSegment(marker)) {
						}
						else {
							int length=Markers.isFixedLengthJPEGSegment(marker);
							if (length == 0) {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): stopping on unrecognized marker segment");
								break;
							}
							else {
//System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput(): skipping fixed length marker segment length="+length);
								stream.skipBytes(length-2);
							}
						}
						markerprefix=stream.read();
					}
				}
				// else no SOI so not JPEG
				stream.reset();
System.err.println("JPEGLosslessImageReaderSpi.canDecodeInput() = "+canDecode);
			}
		}
		catch (IOException e) {
		}
		return canDecode;
	}
	
	public String getDescription(Locale locale) {
		return description;
	}
	
	public ImageReader createReaderInstance(Object extension) {
		return new JPEGLosslessImageReader(this);
	}

}
