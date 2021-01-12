/* $Id$
 *
 * Copyright 2007-2008 Cisco Systems Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


// Original CircularByteBuffer made available under the
// above Aparche 2 license.
//
// Addition of InputStream/OutputStream support
// Copyright 2106 Focused Ultrasound Foundation, John Snell
// https://opensource.org/licenses/MIT

package org.fusfoundation.dicom.net;

import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;

/**
 * Description of CircularByteBuffer.
 */
public class CircularByteBuffer {

    /**
     * Constructs the CircularByteBuffer.
     *
     * @param size
     */

    public CircularByteBuffer(int size) {
        this.size = size;
        buf = new byte[size];
    }

    private final int size;

    private final byte[] buf;

    private int length;

    private int nextGet;

    private int nextPut;

    public int size() {
        return size;
    }

    public int length() {
        return length;
    }

    public synchronized void clear() {
        length = 0;
        nextGet = 0;
        nextPut = 0;
    }

    public boolean isEmpty() {
        return length <= 0;
    }

    public boolean isFull() {
        return length >= size;
    }

    public byte get() throws EOFException {
        if (isEmpty()) {
            throw new EOFException();
        }

        length--;
        byte b = buf[nextGet++];
        if (nextGet >= size) {
            nextGet = 0;
        }
        return b;
    }

    public void put(byte b) throws BufferOverflowException {
        if (isFull()) {
            throw new BufferOverflowException();
        }

        length++;
        buf[nextPut++] = b;
        if (nextPut >= size) {
            nextPut = 0;
        }
    }

    // reasonalby thread safe access via Input/Output Streams
    private class CircularByteBufferInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            synchronized (CircularByteBuffer.this) {
                return get();
            }
        }

        @Override
        public int available() throws IOException {
            return length;
        }

    }

    private class CircularByteBufferOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            synchronized (CircularByteBuffer.this) {
                put((byte) (b & 0xff));
            }
        }

    }

    private CircularByteBufferInputStream inputStream;
    private CircularByteBufferOutputStream outputStream;

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
