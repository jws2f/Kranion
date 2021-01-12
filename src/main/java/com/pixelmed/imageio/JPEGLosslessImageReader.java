/* Copyright (c) 2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.imageio;

// follow the pattern described in "http://docs.oracle.com/javase/1.5.0/docs/guide/imageio/spec/extending.fm3.html"

import com.pixelmed.codec.jpeg.MarkerSegmentSOF;
import com.pixelmed.codec.jpeg.OutputArrayOrStream;
import com.pixelmed.codec.jpeg.Parse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.nio.ByteOrder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.awt.Point;
import java.awt.Transparency;

import java.awt.color.ColorSpace;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.imageio.ImageReader;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class JPEGLosslessImageReader extends ImageReader {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/imageio/JPEGLosslessImageReader.java,v 1.8 2015/10/19 15:34:42 dclunie Exp $";

	ImageInputStream stream = null;
	
	int width;
	int height;
	int bitDepth;
	
	Parse.DecompressedOutput decompressedOutput = null;
	
	boolean gotEverything = false;

	public JPEGLosslessImageReader(ImageReaderSpi originatingProvider) {
		super(originatingProvider);
	}
	
	public void reset() {
System.err.println("reset()");
		super.reset();
		stream = null;
		gotEverything = false;
		decompressedOutput = null;
	}

	public void setInput(Object input, boolean isStreamable,boolean ignoreMetadata) {	// contrary to docs, need to override three argument method
//System.err.println("JPEGLosslessImageReader.setInput("+input+","+isStreamable+"/*isStreamable*/,"+ignoreMetadata+"/*ignoreMetadata*/)");
		super.setInput(input,isStreamable,ignoreMetadata);
		if (input == null) {
			this.stream = null;
			return;
		}
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream)input;
		}
		else {
			throw new IllegalArgumentException("bad input");
		}
		// just in case we don't call reset() before reusing reader ...
		gotEverything = false;
		decompressedOutput = null;
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		return 1; // format can only encode a single image
	}

	private void checkIndex(int imageIndex) {
		// format can only encode a single image
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException("bad index");
		}
	}

	public int getWidth(int imageIndex) throws IIOException {
		checkIndex(imageIndex); // will throw an exception if != 0
		readEverything();
		return width;
	}

	public int getHeight(int imageIndex) throws IIOException {
		checkIndex(imageIndex); // will throw an exception if != 0
		readEverything();
		return height;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IIOException {
		checkIndex(imageIndex);
		readEverything();
		
		ImageTypeSpecifier imageType = null;
		List l = new ArrayList<ImageTypeSpecifier>();
		imageType = ImageTypeSpecifier.createGrayscale(
			bitDepth,
			bitDepth <= 8 ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT,
			false/*isSigned*/);	// have no way to determine from the JPEG lossless bitstream if signed or not
		l.add(imageType);
		return l.iterator();
	}
	
	private final class WrapImageInputStreamAsInputStream extends InputStream {
		private final ImageInputStream iis;
		
		private WrapImageInputStreamAsInputStream() {
			iis = null;
		}
		
		public WrapImageInputStreamAsInputStream(ImageInputStream iis) {
			this.iis = iis;
		}
		
		public final int available() { return 0; }	// no such method in ImageInputStream
		
		public final void close() throws IOException { iis.close(); }
		
		public final void mark(int readlimit) { iis.mark(); }		// ImageInputStream has no readlimit
		
		public final boolean markSupported() { return true; }		// always supported
		
		public final int read() throws IOException { return iis.read(); }
		
		public final int read(byte[] b) throws IOException { return iis.read(b); }
		
		public final int read(byte[] b, int off, int len) throws IOException { return iis.read(b,off,len); }
		
		public final void reset() throws IOException { iis.reset(); }
		
		public final long skip(long n) throws IOException { return iis.skipBytes(n); }
	}
	
	public void readEverything() throws IIOException {
		if (gotEverything) {
			return;
		}
		gotEverything = true;
		
		if (stream == null) {
			throw new IllegalStateException("No input stream");
		}
		decompressedOutput = new Parse.DecompressedOutput();		// allocation to byte or short, and setting of correct size, will be done by com.pixelmed.codec.jpeg.Parse
		try {
			Parse.MarkerSegmentsFoundDuringParse markerSegments = Parse.parse(new WrapImageInputStreamAsInputStream(stream),null,null,decompressedOutput);
			MarkerSegmentSOF sof = markerSegments != null ? markerSegments.getSOF() : null;
			if (sof != null) {
				if (sof.getNComponentsInFrame() != 1 && sof.getNComponentsInFrame() != 3) {
					throw new IIOException("Error reading JPEG stream - only single component (grayscale) or three component supported)");
				}
				width = sof.getNSamplesPerLine();
				height = sof.getNLines();
				bitDepth = sof.getSamplePrecision();
			}
			else {
				throw new IIOException("Error reading JPEG stream - no SOS or SOF marker segment parsed");
			}
		}
		catch (Exception e) {
			throw new IIOException("Error reading JPEG stream",e);
		}
	}

	public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
		checkIndex(imageIndex);
		readEverything();
		
		BufferedImage image = null;
		
		OutputArrayOrStream[] decompressedOutputPerComponent = decompressedOutput.getDecompressedOutputPerComponent();
		
		ComponentColorModel cm = null;
		ComponentSampleModel sm = null;
		DataBuffer buf = null;
		if (decompressedOutputPerComponent.length == 1) {
			if (bitDepth <= 8) {
				// copied from com.pixelmed.display.SourceImage.createByteGrayscaleImage() ...
				cm=new ComponentColorModel(
										   ColorSpace.getInstance(ColorSpace.CS_GRAY),
										   new int[] {8},
										   false,		// has alpha
										   false,		// alpha premultipled
										   Transparency.OPAQUE,
										   DataBuffer.TYPE_BYTE
										   );
				sm = new ComponentSampleModel(
											  DataBuffer.TYPE_BYTE,
											  width,
											  height,
											  1,
											  width,
											  new int[] {0}
											  );
				buf = new DataBufferByte(decompressedOutputPerComponent[0].getByteArray(),width,0);
			}
			else {
				// copied from com.pixelmed.display.SourceImage.createUnsignedShortGrayscaleImage() ...
				cm=new ComponentColorModel(
										   ColorSpace.getInstance(ColorSpace.CS_GRAY),
										   new int[] {16},
										   false,		// has alpha
										   false,		// alpha premultipled
										   Transparency.OPAQUE,
										   DataBuffer.TYPE_USHORT
										   );
				sm = new ComponentSampleModel(
											  DataBuffer.TYPE_USHORT,
											  width,
											  height,
											  1,
											  width,
											  new int[] {0}
											  );
				buf = new DataBufferUShort(decompressedOutputPerComponent[0].getShortArray(),width,0);
			}
		}
		else if (decompressedOutputPerComponent.length == 3) {
			// the decompressedOutput has separated the input into separate arrays, each of which we can use as a bank and use a band interleaved model
			if (bitDepth <= 8) {
				// copied from com.pixelmed.display.SourceImage.createBandInterleavedByteRGBImage(), except that we have three rather than one banks ...
				cm=new ComponentColorModel(
										   ColorSpace.getInstance(ColorSpace.CS_sRGB),	// lie if YCbCr (we don't know at this point) :(
										   new int[] {8,8,8},
										   false,		// has alpha
										   false,		// alpha premultipled
										   Transparency.OPAQUE,
										   DataBuffer.TYPE_BYTE
										   );
				sm = new ComponentSampleModel(
											  DataBuffer.TYPE_BYTE,
											  width,
											  height,
											  1/*pixelStride*/,
											  width/*scanlineStride*/,
											  new int[] {0,1,2}/*bankIndices*/,
											  new int[] {0,0,0}/*bandOffsets*/
											  );
				buf = new DataBufferByte(
					new byte[][] {
						decompressedOutputPerComponent[0].getByteArray(),
						decompressedOutputPerComponent[1].getByteArray(),
						decompressedOutputPerComponent[2].getByteArray(),
					},
					width*height);
			}
			else {
				// not really expecting to see > 8 bit color per channel, but no reason not to build it ... probably not tested yet though :(
				cm=new ComponentColorModel(
										   ColorSpace.getInstance(ColorSpace.CS_sRGB),	// lie if YCbCr (we don't know at this point) :(
										   new int[] {16,16,16},
										   false,		// has alpha
										   false,		// alpha premultipled
										   Transparency.OPAQUE,
										   DataBuffer.TYPE_USHORT
										   );
				sm = new ComponentSampleModel(
											  DataBuffer.TYPE_USHORT,
											  width,
											  height,
											  1/*pixelStride*/,
											  width/*scanlineStride*/,
											  new int[] {0,1,2}/*bankIndices*/,
											  new int[] {0,0,0}/*bandOffsets*/
											  );
				buf = new DataBufferUShort(
					new short[][] {
						decompressedOutputPerComponent[0].getShortArray(),
						decompressedOutputPerComponent[1].getShortArray(),
						decompressedOutputPerComponent[2].getShortArray(),
					},
					width*height);
			}
		}
		
		if (buf != null) {
			WritableRaster wr = Raster.createWritableRaster(sm,buf,new Point(0,0));
			image = new BufferedImage(cm,wr,true,null);	// no properties hash table
		}
		
		return image;
	}

	JPEGLosslessMetadata metadata = null;

	public IIOMetadata getStreamMetadata() throws IIOException {
		return null;
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IIOException {
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException("imageIndex != 0!");
		}
		readMetadata();
		return metadata;
	}
	
	public void readMetadata() throws IIOException {
		if (metadata != null) {
			return;
		}
		readEverything();
		this.metadata = new JPEGLosslessMetadata();
		//try {
		//}
		//catch (IOException e) {
		//	throw new IIOException("Exception reading metadata", e);
		//}
	}
}