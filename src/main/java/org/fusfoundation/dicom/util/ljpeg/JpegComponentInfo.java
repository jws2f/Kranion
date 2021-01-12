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
public class JpegComponentInfo {
    
    /** Creates a new instance of JpegComponentInfo */
    public JpegComponentInfo() {
    }
    
    /*
     * These values are fixed over the whole image.
     * They are read from the SOF marker.
     */
    public int componentId;		/* identifier for this component (0..255) */
    public int componentIndex;	/* its index in SOF or cPtr->compInfo[]   */

    /*
     * Downsampling is not normally used in lossless JPEG, although
     * it is permitted by the JPEG standard (DIS). We set all sampling 
     * factors to 1 in this program.
     */
    public int hSampFactor;		/* horizontal sampling factor */
    public int vSampFactor;		/* vertical sampling factor   */

    /*
     * Huffman table selector (0..3). The value may vary
     * between scans. It is read from the SOS marker.
     */
    public int dcTblNo;    
}
