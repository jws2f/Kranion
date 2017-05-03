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
 * @author  jsnell
 */
public class DecompressInfo {
    
    /** Creates a new instance of DecompressInfo */
    public DecompressInfo() {
        for (int i=0; i<4; i++)
            dcHuffTblPtrs[i] = new HuffmanTable();
    }
    /*
     * Image width, height, and image data precision (bits/sample)
     * These fields are set by ReadFileHeader or ReadScanHeader
     */ 
    public int imageWidth;
    public int imageHeight;
    public int dataPrecision;

    /*
     * compInfo[i] describes component that appears i'th in SOF
     * numComponents is the # of color components in JPEG image.
     */
    public JpegComponentInfo[] compInfo;
    public int numComponents;

    /*
     * *curCompInfo[i] describes component that appears i'th in SOS.
     * compsInScan is the # of color components in current scan.
     */
    public JpegComponentInfo curCompInfo[] = new JpegComponentInfo[4];
    public int compsInScan;

    /*
     * MCUmembership[i] indexes the i'th component of MCU into the
     * curCompInfo array.
     */
    public int MCUmembership[] = new int[10];

    /*
     * ptrs to Huffman coding tables, or NULL if not defined
     */
    public HuffmanTable dcHuffTblPtrs[] = new HuffmanTable[4];

    /* 
     * prediction seletion value (PSV) and point transform parameter (Pt)
     */
    public int Ss;
    public int Pt;

    /*
     * In lossless JPEG, restart interval shall be an integer
     * multiple of the number of MCU in a MCU row.
     */
    public int restartInterval;/* MCUs per restart interval, 0 = no restart */
    public int restartInRows; /*if > 0, MCU rows per restart interval; 0 = no restart*/

    /*
     * these fields are private data for the entropy decoder
     */
    public int restartRowsToGo;	/* MCUs rows left in this restart interval */
    public int nextRestartNum;	/* # of next RSTn marker (0..7) */
    
    public String toString() {
        return imageWidth + " x " + imageHeight + " x " + dataPrecision + " bits";
    }
}
