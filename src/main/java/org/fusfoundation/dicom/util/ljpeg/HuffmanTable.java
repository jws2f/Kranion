/* 
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.dicom.util.ljpeg;

/**
 *
 */
public class HuffmanTable {
    
    /** Creates a new instance of HuffmanTable */
    public HuffmanTable() {
    }
    
    /*
     * These two fields directly represent the contents of a JPEG DHT
     * marker
     */
    public int bits[] = new int[17];
    public int huffval[] = new int[256];

    /*
     * This field is used only during compression.  It's initialized
     * FALSE when the table is created, and set TRUE when it's been
     * output to the file.
     */
    public int sentTable;

    /*
     * The remaining fields are computed from the above to allow more
     * efficient coding and decoding.  These fields should be considered
     * private to the Huffman compression & decompression modules.
     */
    public int ehufco[] = new int[256];
    public int ehufsi[] = new int[256];

    public int mincode[] = new int[17];
    public int maxcode[] = new int[18];
    public int valptr[] = new int[17];
    public int numbits[] = new int[256];
    public int value[] = new int[256];    
}
