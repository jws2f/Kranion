/* Copyright (c) 2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.codec.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteOrder;

/**
 * <p>A class that allows writing to either an {@link java.io.OutputStream OutputStream}
 * or a byte[] or short[] of preallocated size.</p>
 *
 * <p>An unallocated instance may be constructed but any attempt to write to it will
 * fail until either an OutputStream is assigned or an array of the appropriate type
 * is allocated. This allows, for example, the instance to be created and later
 * allocated based on size information, e.g., as header information is encountered
 * while decompressing before decompressed pixel values need to be written.</p>
 *
 * @author	dclunie
 */
public class OutputArrayOrStream {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/codec/jpeg/OutputArrayOrStream.java,v 1.5 2016/01/16 13:30:09 dclunie Exp $";
	
	protected OutputStream out = null;
	protected ByteOrder order = null;
	protected byte[] byteValues = null;
	protected short[] shortValues = null;
	protected int byteOffset = 0;
	protected int shortOffset = 0;
	
	public OutputArrayOrStream() {
		// lazy allocation
	}
	
	public OutputArrayOrStream(OutputStream out,ByteOrder order) {
		this.out = out;
		this.order = order;
	}
	
	public OutputArrayOrStream(byte[] byteValues) {
		this.byteValues = byteValues;
		byteOffset = 0;
	}
	
	public OutputArrayOrStream(short[] shortValues) {
		this.shortValues = shortValues;
		shortOffset = 0;
	}
	
	public void setOutputStream(OutputStream out,ByteOrder order) throws IOException {
		if (this.out != null || this.byteValues != null || this.shortValues != null) {
			throw new IOException("Destination already allocated");
		}
		this.out = out;
		this.order = order;
	}
	
	/**
	 * <p>Retrieves the OutputStream's byte order used when writing short values.</p>
	 *
	 * @return	The OutputStream's byte order, or null if no OutputStream
	 */
	public ByteOrder order() {
		return out == null ? null : order;
	}
	
	/**
	 * <p>Modifes the OutputStream's byte order used when writing short values.</p>
	 *
	 * @param	order		the new byte order, either BIG_ENDIAN or LITTLE_ENDIAN
	 * @throws	IOException	if no OutputStream assigned
	 */
	public void order(ByteOrder order) throws IOException {
		if (out == null) {
			throw new IOException("Cannot assign byte order if no OutputStream");
		}
		this.order = order;
	}
	
	public void allocateByteArray(int length) throws IOException {
		if (this.out != null || this.byteValues != null || this.shortValues != null) {
			throw new IOException("Destination already allocated");
		}
		this.byteValues = new byte[length];
		byteOffset = 0;
	}
	
	public void allocateShortArray(int length) throws IOException {
		if (this.out != null || this.byteValues != null || this.shortValues != null) {
			throw new IOException("Destination already allocated");
		}
		this.shortValues = new short[length];
		shortOffset = 0;
	}
	
	public OutputStream getOutputStream() {
		return out;
	}
	
	public byte[] getByteArray() {
		return byteValues;
	}
	
	public short[] getShortArray() {
		return shortValues;
	}

    /**
     * Writes the specified <code>byte</code> to this output.
     *
     * @param      b   the <code>byte</code>.
     * @throws  IOException  if an I/O error occurs.
     */
    public void writeByte(int b) throws IOException {
		if (out != null) {
			out.write(b);
		}
		else if (byteValues != null) {
			byteValues[byteOffset++] = (byte)b;
		}
		else if (shortValues != null) {
			throw new IOException("Cannot write byte value to short array");
		}
		else {
			throw new IOException("Byte array not allocated yet");
		}
    }

    /**
     * Writes the specified <code>short</code> to this output.
     *
     * @param      s   the <code>short</code>.
     * @throws  IOException  if an I/O error occurs.
     */
    public void writeShort(int s) throws IOException {
		if (out != null) {
			if (order == ByteOrder.LITTLE_ENDIAN) {
				out.write(s);
				out.write(s>>8);
			}
			else {
				out.write(s>>8);
				out.write(s);
			}
		}
		else if (shortValues != null) {
			shortValues[shortOffset++] = (short)s;
		}
		else if (byteValues != null) {
			throw new IOException("Cannot write short value to byte array");
		}
		else {
			throw new IOException("Short array not allocated yet");
		}
    }
	
    /**
     * <p>Closes any assigned OutputStream.</p>
     *
     * <p>Does nothing if arrays allocated instead of an OutputStream (i.e., does NOT release them).</p>
     *
     * @throws  IOException  if an I/O error occurs.
     */
	public void close() throws IOException {
		if (out != null) {
			out.close();
		}
	}

}
