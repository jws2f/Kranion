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

import java.io.*;
import javax.swing.*;

/**
 *
 * @author  jsnell
 */
public class LJPEGDecoder {
    
    public static final int MinPrecisionValue = 2;
    public static final int MaxPrecisionValue = 65535;
    public static final int MinPrecisionBits = 2;
    public static final int MaxPrecisionBits = 16;
    
    public static final int M_SOF0 = 0xc0;
    public static final int M_SOF1 = 0xc1;
    public static final int M_SOF2 = 0xc2;
    public static final int M_SOF3 = 0xc3;
    
    public static final int M_SOF5 = 0xc5;
    public static final int M_SOF6 = 0xc6;
    public static final int M_SOF7 = 0xc7;
    
    public static final int M_JPG = 0xc8;
    public static final int M_SOF9 = 0xc9;
    public static final int M_SOF10 = 0xca;
    public static final int M_SOF11 = 0xcb;
    
    public static final int M_SOF13 = 0xcd;
    public static final int M_SOF14 = 0xce;
    public static final int M_SOF15 = 0xcf;
    
    public static final int M_DHT = 0xc4;
    
    public static final int M_DAC = 0xcc;
    
    public static final int M_RST0 = 0xd0;
    public static final int M_RST1 = 0xd1;
    public static final int M_RST2 = 0xd2;
    public static final int M_RST3 = 0xd3;
    public static final int M_RST4 = 0xd4;
    public static final int M_RST5 = 0xd5;
    public static final int M_RST6 = 0xd6;
    public static final int M_RST7 = 0xd7;
    
    public static final int M_SOI = 0xd8;
    public static final int M_EOI = 0xd9;
    public static final int M_SOS = 0xda;
    public static final int M_DQT = 0xdb;
    public static final int M_DNL = 0xdc;
    public static final int M_DRI = 0xdd;
    public static final int M_DHP = 0xde;
    public static final int M_EXP = 0xdf;
    
    public static final int M_APP0 = 0xe0;
    public static final int M_APP15 = 0xef;
    
    public static final int M_JPG0 = 0xf0;
    public static final int M_JPG13 = 0xfd;
    public static final int M_COM = 0xfe;
    
    public static final int M_TEM = 0x01;
    
    public static final int M_ERROR = 0x100;
    
    public static final int RST0 = 0xD0;
    
    private PushbackInputStream inputStream=null;
    private OutputStream outputStream=null;
    
    private DecompressInfo dcPtr = new DecompressInfo();
    private int[] mcuROW1[], mcuROW2[];
    
    // Huffman variables
    private static final int BITS_PER_LONG	= (8*8);
    private static final int  MIN_GET_BITS = (BITS_PER_LONG-7);	   /* max value for long getBuffer */
    
    private int bitsLeft=0;
    private long getBuffer=0;		/* current bit-extraction buffer */
    private  int bitMask[] = {  0xffffffff, 0x7fffffff, 0x3fffffff, 0x1fffffff,
    0x0fffffff, 0x07ffffff, 0x03ffffff, 0x01ffffff,
    0x00ffffff, 0x007fffff, 0x003fffff, 0x001fffff,
    0x000fffff, 0x0007ffff, 0x0003ffff, 0x0001ffff,
    0x0000ffff, 0x00007fff, 0x00003fff, 0x00001fff,
    0x00000fff, 0x000007ff, 0x000003ff, 0x000001ff,
    0x000000ff, 0x0000007f, 0x0000003f, 0x0000001f,
    0x0000000f, 0x00000007, 0x00000003, 0x00000001};
    /*
     * bmask[n] is mask for n rightmost bits
     */
    private int bmask[] = {0x0000,
    0x0001, 0x0003, 0x0007, 0x000F,
    0x001F, 0x003F, 0x007F, 0x00FF,
    0x01FF, 0x03FF, 0x07FF, 0x0FFF,
    0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};
    
    /** Creates a new instance of LJPEGDecoder */
    public LJPEGDecoder() {
    }
    
    public DecompressInfo getDecompressInfo() { return dcPtr; }
    
    private int getByte() throws IOException {
        return inputStream.read() & 0xff;
    }
    
    private void putByte(int c) throws IOException {
        inputStream.unread(c);
    }
    
    public void setInputStream(InputStream is) {
        inputStream = new PushbackInputStream(is, 256);
    }
    
    public void setOutputStream(OutputStream os) {outputStream = os;}
    
    private void putc(int v) throws IOException {
       // outputStream.write(v/*+97*/);
       //   voxelData[offset + v] = (short)((sliceData[v*2] & 0xff) << 8 | (sliceData[v*2 + 1] & 0xff));
        //outputStream.write(0);
        outputStream.write(((v ) >> 8) + 128);
        outputStream.write((v & 0xff/*+97*/) + 128);
        //outputStream.write((v >> 8) & 0xff/*+97*/);
    }
    
    private int GetJpegChar() throws IOException {
        return getByte();
    }
    
    private void UnGetJpegChar(int c) throws IOException {
        putByte(c);
    }
    
    private int Get2bytes() throws IOException {
        int a;
        
        a = getByte();
        return (a << 8) + getByte();
    }
    
    
 /*
  *--------------------------------------------------------------
  *
  * GetSoi --
  *
  *	Process an SOI marker
  *
  * Results:
  *	None.
  *
  * Side effects:
  *	Bitstream is parsed.
  *	Exits on error.
  *
  *--------------------------------------------------------------
  */
    private void GetSoi() throws IOException {
        //System.out.println("GetSoi()");
        
    /*
     * Reset all parameters that are defined to be reset by SOI
     */
        dcPtr.restartInterval = 0;
    }
    
 /*
  *--------------------------------------------------------------
  *
  * GetSof --
  *
  *	Process a SOFn marker
  *
  * Results:
  *	None.
  *
  * Side effects:
  *	Bitstream is parsed
  *	Exits on error
  *	dcPtr structure is filled in
  *
  *--------------------------------------------------------------
  */
    private void GetSof(int code) throws IOException
    
    {
        //System.out.println("GetSof()");
        
        int length;
        short ci;
        int c;
        JpegComponentInfo compptr;
        
        length = Get2bytes();
        
        dcPtr.dataPrecision = GetJpegChar();
        dcPtr.imageHeight = Get2bytes();
        dcPtr.imageWidth = Get2bytes();
        dcPtr.numComponents = GetJpegChar();
                
    /*
     * We don't support files in which the image height is initially
     * specified as 0 and is later redefined by DNL.  As long as we
     * have to check that, might as well have a general sanity check.
     */
        if ((dcPtr.imageHeight <= 0 ) ||
        (dcPtr.imageWidth <= 0) ||
        (dcPtr.numComponents <= 0)) {
            System.out.println("Empty JPEG image (DNL not supported)");
            System.exit(1);
        }
        
        if ((dcPtr.dataPrecision<MinPrecisionBits) ||
        (dcPtr.dataPrecision>MaxPrecisionBits)) {
            System.out.println("Unsupported JPEG data precision");
            System.exit(1);
        }
        
        if (length != (dcPtr.numComponents * 3 + 8)) {
            System.out.println("Bogus SOF length");
            System.exit(1);
        }
        
        dcPtr.compInfo = new JpegComponentInfo[dcPtr.numComponents];
        
        for (ci = 0; ci < dcPtr.numComponents; ci++) {
            dcPtr.compInfo[ci] = new JpegComponentInfo();
            
            compptr = dcPtr.compInfo[ci];
            compptr.componentIndex = ci;
            compptr.componentId = GetJpegChar();
            c = getByte();
            compptr.hSampFactor = (c >>> 4) & 15;
            compptr.vSampFactor = (c) & 15;
            getByte();   /* skip Tq */
        }
    }
    
    
/*
 *--------------------------------------------------------------
 *
 * GetDht --
 *
 *	Process a DHT marker
 *
 * Results:
 *	None
 *
 * Side effects:
 *	A huffman table is read.
 *	Exits on error.
 *
 *--------------------------------------------------------------
 */
    private void GetDht() throws IOException
    
    {
        
        //System.out.println("GetDht()");
        
        int length;
        int bits[] = new int[17];
        int huffval[] = new int[256];
        int i, index, count;
        HuffmanTable htblptr = null;
        
        length = Get2bytes() - 2;
        
        while (length > 0) {
            index = GetJpegChar();
            
            bits[0] = 0;
            count = 0;
            for (i = 1; i <= 16; i++) {
                bits[i] = GetJpegChar();
                count += bits[i];
                System.out.println("bits[" + i + "]=" + bits[i]);
            }
            
            if (count > 256) {
                System.out.println("Bogus DHT counts");
                System.exit(1);
            }
            
            for (i = 0; i < count; i++) {
                huffval[i] = GetJpegChar();
                System.out.println("huffval["+i+"]="+huffval[i]);
            }
            
            length -= 1 + 16 + count;
            
            if ((index & 0x10) > 0) {	/* AC table definition */
                System.out.println("Huffman table for lossless JPEG is not defined.");
            } else {		/* DC table definition */
                htblptr = dcPtr.dcHuffTblPtrs[index];
            }
            
            if (index < 0 || index >= 4) {
                System.out.println("Bogus DHT index " + index);
                System.exit(1);
            }
            
            if (htblptr == null) {
                htblptr = new HuffmanTable();
                if (htblptr == null) {
                    System.out.println("Can't malloc HuffmanTable");
                    System.exit(-1);
                }
            }
            
            System.arraycopy(bits, 0, htblptr.bits, 0, bits.length);
            System.arraycopy(huffval, 0, htblptr.huffval, 0, huffval.length);
        }
    }
    
/*
 *--------------------------------------------------------------
 *
 * GetDri --
 *
 *	Process a DRI marker
 *
 * Results:
 *	None
 *
 * Side effects:
 *	Exits on error.
 *	Bitstream is parsed.
 *
 *--------------------------------------------------------------
 */
    private void GetDri() throws IOException {
        //System.out.println("GetDri()");
        
        if (Get2bytes() != 4) {
            System.out.println("Bogus length in DRI");
            System.exit(1);
        }
        
        dcPtr.restartInterval = Get2bytes();
    }
    
/*
 *--------------------------------------------------------------
 *
 * GetApp0 --
 *
 *	Process an APP0 marker.
 *
 * Results:
 *	None
 *
 * Side effects:
 *	Bitstream is parsed
 *
 *--------------------------------------------------------------
 */
    public void GetApp0() throws IOException {
        //System.out.println("GetApp0");
        
        int length;
        
        length = Get2bytes() - 2;
        while (length-- > 0)	/* skip any remaining data */
            GetJpegChar();
    }
    
/*
 *--------------------------------------------------------------
 *
 * GetSos --
 *
 *	Process a SOS marker
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Bitstream is parsed.
 *	Exits on error.
 *
 *--------------------------------------------------------------
 */
    public void GetSos() throws IOException {
        int length;
        int i, ci, n, c, cc;
        JpegComponentInfo compptr;
        
        length = Get2bytes();
        
    /*
     * Get the number of image components.
     */
        n = GetJpegChar();
        dcPtr.compsInScan = n;
        length -= 3;
        
        if (length != (n * 2 + 3) || n < 1 || n > 4) {
            System.out.println("Bogus SOS length");
            System.exit(1);
        }
        
        
        for (i = 0; i < n; i++) {
            cc = GetJpegChar();
            c = GetJpegChar();
            length -= 2;
            
            for (ci = 0; ci < dcPtr.numComponents; ci++)
                if (cc == dcPtr.compInfo[ci].componentId) {
                    break;
                }
            
            if (ci >= dcPtr.numComponents) {
                System.out.println("Invalid component number in SOS");
                System.exit(1);
            }
            
            compptr = dcPtr.compInfo[ci];
            dcPtr.curCompInfo[i] = compptr;
            compptr.dcTblNo = (c >>> 4) & 15;
        }
        
    /*
     * Get the PSV, skip Se, and get the point transform parameter.
     */
        dcPtr.Ss = GetJpegChar();
        GetJpegChar();
        c = GetJpegChar();
        dcPtr.Pt = c & 0x0F;
        
        //System.out.println("PSV = " + dcPtr.Ss + ", PTx = " + dcPtr.Pt);
    }
    
/*
 *--------------------------------------------------------------
 *
 * ReadScanHeader --
 *
 *	Read the start of a scan (everything through the SOS marker).
 *
 * Results:
 *	1 if find SOS, 0 if find EOI
 *
 * Side effects:
 *	Bitstream is parsed, may exit on errors.
 *
 *--------------------------------------------------------------
 */
    public int ReadScanHeader() throws IOException {
        int c;
        
    /*
     * Process markers until SOS or EOI
     */
        c = ProcessTables();
        
        switch (c) {
            case M_SOS:
                GetSos();
                return 1;
                
            case M_EOI:
                return 0;
                
            default:
                System.out.println("Unexpected marker " + c);
                break;
        }
        return 0;
    }
    
    public void readHeader() throws IOException {
        int c, c2;
        
    /*
     * Demand an SOI marker at the start of the file --- otherwise it's
     * probably not a JPEG file at all.
     */
        c = getByte();
        c2 = getByte();
        if ((c != 0xFF) || (c2 != M_SOI)) {
            System.out.println("Not a JPEG file");
            return;
        }
        
        System.out.println("Looks like a JPEG file");
        
        
        GetSoi();		/* OK, process SOI */
        
    /*
     * Process markers until SOF
     */
        c = ProcessTables();
        
        switch (c) {
            case M_SOF0:
            case M_SOF1:
            case M_SOF3:
                GetSof(c);
                break;
                
            default:
                System.out.println("Unsupported SOF marker type " + c);
                break;
        }
    }
    
    
    public int readTag() throws IOException {
        return Get2bytes();
    }
    
    private void SkipVariable() throws IOException {
        int length;
        
        length = Get2bytes() - 2;
        
        while (length-- > 0) {
            getByte();
        }
    }
    
    private int NextMarker() throws IOException {
        int c, nbytes;
        
        nbytes = 0;
        do {
        /*
         * skip any non-FF bytes
         */
            do {
                nbytes++;
                c = getByte();
            } while (c != 0xFF);
        /*
         * skip any duplicate FFs without incrementing nbytes, since
         * extra FFs are legal
         */
            do {
                c = getByte();
            } while (c == 0xFF);
        } while (c == 0);		/* repeat if it was a stuffed FF/00 */
        
        return c;
    }
    
    private int ProcessTables() throws IOException {
        int c;
        
        while (true) {
            c = NextMarker();
            
            switch (c) {
                case M_SOF0:
                case M_SOF1:
                case M_SOF2:
                case M_SOF3:
                case M_SOF5:
                case M_SOF6:
                case M_SOF7:
                case M_JPG:
                case M_SOF9:
                case M_SOF10:
                case M_SOF11:
                case M_SOF13:
                case M_SOF14:
                case M_SOF15:
                case M_SOI:
                case M_EOI:
                case M_SOS:
                    return (c);
                    
                case M_DHT:
                    GetDht();
                    break;
                    
                case M_DQT:
                    System.out.println("Not a lossless JPEG file.");
                    break;
                    
                case M_DRI:
                    GetDri();
                    break;
                    
                case M_APP0:
                    GetApp0();
                    break;
                    
                case M_RST0:		/* these are all parameterless */
                case M_RST1:
                case M_RST2:
                case M_RST3:
                case M_RST4:
                case M_RST5:
                case M_RST6:
                case M_RST7:
                case M_TEM:
                    System.out.println("Warning: unexpected marker " + c);
                    break;
                    
                default:		/* must be DNL, DHP, EXP, APPn, JPGn, COM, or RESn */
                    SkipVariable();
                    break;
            }
        }
    }
    
/*
 *--------------------------------------------------------------
 *
 * DecoderStructInit --
 *
 *	Initalize the rest of the fields in the decompression
 *	structure.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *--------------------------------------------------------------
 */
    public void DecoderStructInit() {
        int ci,i;
        JpegComponentInfo compPtr;
        int mcuSize;
        
    /*
     * Check sampling factor validity.
     */
        for (ci = 0; ci < dcPtr.numComponents; ci++) {
            compPtr = dcPtr.compInfo[ci];
            if ((compPtr.hSampFactor != 1) || (compPtr.vSampFactor != 1)) {
                System.out.println("Error: Downsampling is not supported.");
                System.exit(-1);
            }
        }
        
    /*
     * Prepare array describing MCU composition
     */
        if (dcPtr.compsInScan == 1) {
            dcPtr.MCUmembership[0] = 0;
        } else {
            
            if (dcPtr.compsInScan > 4) {
                System.out.println("Too many components for interleaved scan");
                System.exit(1);
            }
            
            for (ci = 0; ci < dcPtr.compsInScan; ci++) {
                dcPtr.MCUmembership[ci] = ci;
            }
        }
        
    /*
     * Initialize mucROW1 and mcuROW2 which buffer two rows of
     * pixels for predictor calculation.
     */
        
        mcuSize=dcPtr.compsInScan * 2;
        
        mcuROW1 = new int[dcPtr.imageWidth][mcuSize];
        mcuROW2 = new int[dcPtr.imageWidth][mcuSize];
        
    }
    
/*
 *--------------------------------------------------------------
 *
 * HuffDecoderInit --
 *
 *	Initialize for a Huffman-compressed scan.
 *	This is invoked after reading the SOS marker.
 *
 * Results:
 *	None
 *
 * Side effects:
 *	None.
 *
 *--------------------------------------------------------------
 */
    public void HuffDecoderInit() {
        short ci;
        JpegComponentInfo compptr;
        
    /*
     * Initialize static variables
     */
        bitsLeft = 0;
        
        for (ci = 0; ci < dcPtr.compsInScan; ci++) {
            compptr = dcPtr.curCompInfo[ci];
        /*
         * Make sure requested tables are present
         */
            
            if (dcPtr.dcHuffTblPtrs[compptr.dcTblNo] == null) {
                System.out.println("Error: Use of undefined Huffman table");
                System.exit(1);
            }
            
        /*
         * Compute derived values for Huffman tables.
         * We may do this more than once for same table, but it's not a
         * big deal
         */
            FixHuffTbl(dcPtr.dcHuffTblPtrs[compptr.dcTblNo]);
        }
        
    /*
     * Initialize restart stuff
     */
        dcPtr.restartInRows = (dcPtr.restartInterval)/(dcPtr.imageWidth);
        dcPtr.restartRowsToGo = dcPtr.restartInRows;
        dcPtr.nextRestartNum = 0;
    }
    
/*
 *--------------------------------------------------------------
 *
 * FixHuffTbl --
 *
 *      Compute derived values for a Huffman table one the DHT marker
 *      has been processed.  This generates both the encoding and
 *      decoding tables.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      None.
 *
 *--------------------------------------------------------------
 */
    private void FixHuffTbl(HuffmanTable htbl) {
        int p, i, l, lastp, si;
        int huffsize[] = new int[257];
        int huffcode[] = new int[257];
        int code;
        int size;
        int value, ll, ul;
        
    /*
     * Figure C.1: make table of Huffman code length for each symbol
     * Note that this is in code-length order.
     */
        p = 0;
        for (l = 1; l <= 16; l++) {
            for (i = 1; i <= (int)htbl.bits[l]; i++)
                huffsize[p++] = l;
        }
        huffsize[p] = 0;
        lastp = p;
        
        System.out.println("lastp = " + lastp);
        
        
    /*
     * Figure C.2: generate the codes themselves
     * Note that this is in code-length order.
     */
        code = 0;
        si = huffsize[0];
        p = 0;
        while (huffsize[p] > 0) {
            while (((int)huffsize[p]) == si) {
                huffcode[p++] = code;
                code++;
            }
            code <<= 1;
            si++;
        }
        
    /*
     * Figure C.3: generate encoding tables
     * These are code and size indexed by symbol value
     * Set any codeless symbols to have code length 0; this allows
     * EmitBits to detect any attempt to emit such symbols.
     */
        //MEMSET(htbl.ehufsi, 0, sizeof(htbl.ehufsi));
        for (int asize = 0; asize<htbl.ehufsi.length; asize++)
            htbl.ehufsi[asize] = 0;
        
        for (p = 0; p < lastp; p++) {
            htbl.ehufco[htbl.huffval[p]] = huffcode[p];
            htbl.ehufsi[htbl.huffval[p]] = huffsize[p];
        }
        
    /*
     * Figure F.15: generate decoding tables
     */
        p = 0;
        for (l = 1; l <= 16; l++) {
            if (htbl.bits[l] > 0) {
                htbl.valptr[l] = p;
                htbl.mincode[l] = huffcode[p];
                p += htbl.bits[l];
                htbl.maxcode[l] = huffcode[p - 1];
            } else {
                htbl.maxcode[l] = -1;
            }
        }
        
    /*
     * We put in this value to ensure HuffDecode terminates.
     */
        htbl.maxcode[17] = 0xFFFFF;
        
    /*
     * Build the numbits, value lookup tables.
     * These table allow us to gather 8 bits from the bits stream,
     * and immediately lookup the size and value of the huffman codes.
     * If size is zero, it means that more than 8 bits are in the huffman
     * code (this happens about 3-4% of the time).
     */
        //bzero (htbl.numbits, sizeof(htbl.numbits));
        for (int asize=0; asize<htbl.numbits.length; asize++)
            htbl.numbits[asize] = 0;
        
        for (p=0; p<lastp; p++) {
            size = huffsize[p];
            if (size <= 8) {
                value = htbl.huffval[p];
                code = huffcode[p];
                
                ll = code << (8-size);
                if (size < 8) {
                    ul = ll | bitMask[24+size];
                } else {
                    ul = ll;
                }
                
                for (i=ll; i<=ul; i++) {
                    htbl.numbits[i] = size;
                    htbl.value[i] = value;
                }
            }
        }
    }
    
/*
 *--------------------------------------------------------------
 *
 * FillBitBuffer --
 *
 *	Load up the bit buffer with at least nbits
 *	Process any stuffed bytes at this time.
 *
 * Results:
 *	None
 *
 * Side effects:
 *	The bitwise global variables are updated.
 *
 *--------------------------------------------------------------
 */
    private void FillBitBuffer(int nbits) throws IOException {
        int c, c2;
        
        while (bitsLeft < MIN_GET_BITS) {
            c = GetJpegChar();
            
        /*
         * If it's 0xFF, check and discard stuffed zero byte
         */
            if (c == 0xFF) {
                c2 = GetJpegChar();
                
                if (c2 != 0) {
                    
                /*
                 * Oops, it's actually a marker indicating end of
                 * compressed data.  Better put it back for use later.
                 */
                    UnGetJpegChar(c2);
                    UnGetJpegChar(c);
                    
                /*
                 * There should be enough bits still left in the data
                 * segment; if so, just break out of the while loop.
                 */
                    if (bitsLeft >= nbits)
                        break;
                    
                /*
                 * Uh-oh.  Corrupted data: stuff zeroes into the data
                 * stream, since this sometimes occurs when we are on the
                 * last show_bits(8) during decoding of the Huffman
                 * segment.
                 */
                    c = 0;
                }
            }
        /*
         * OK, load c into getBuffer
         */
            getBuffer = (getBuffer << 8) | c;
            bitsLeft += 8;
        }
    }
    
    private int show_bits8() throws IOException {
        if (bitsLeft < 8) FillBitBuffer(8);
        return (int)(getBuffer >>> (bitsLeft-8)) & 0xff;
    }
    
    private void flush_bits(int nbits) {
        bitsLeft -= (nbits);
    }
    
    private int get_bits(int nbits) throws IOException {
        if (bitsLeft < nbits) FillBitBuffer(nbits);
        return  (int)((getBuffer >>> (bitsLeft -= (nbits)))) & bmask[nbits];
    }
    
    private int get_bit() throws IOException {
        if (bitsLeft <= 0) FillBitBuffer(1);
        return (int)(getBuffer >>> (--bitsLeft)) & 1;
    }
    
    
    
    
/*
 *--------------------------------------------------------------
 *
 * HuffDecode --
 *
 *	Taken from Figure F.16: extract next coded symbol from
 *	input stream.  This should becode a macro.
 *
 * Results:
 *	Next coded symbol
 *
 * Side effects:
 *	Bitstream is parsed.
 *
 *--------------------------------------------------------------
 */
    private int HuffDecode(HuffmanTable htbl) throws IOException {
        int l, code, temp, rv=0;
        
    /*
     * If the huffman code is less than 8 bits, we can use the fast
     * table lookup to get its value.  It's more than 8 bits about
     * 3-4% of the time.
     */
        code = show_bits8();
        if (htbl.numbits[code] > 0) {
            flush_bits(htbl.numbits[code]);
            rv=htbl.value[code];
        }  else {
            flush_bits(8);
            l = 8;
            while (code > htbl.maxcode[l]) {
                temp = get_bit();
                code = (code << 1) | temp;
                l++;
            }
            
        /*
         * With garbage input we may reach the sentinel value l = 17.
         */
                       
            if (l > 16) {
                System.out.println("Corrupt JPEG data: bad Huffman code");
                rv = 0;		/* fake a zero as the safest result */
            } else {
                rv = htbl.huffval[htbl.valptr[l] +
                ((int)(code - htbl.mincode[l]))];
            }
        }
        
        return rv;
    }
    
/*
 *--------------------------------------------------------------
 *
 * HuffExtend --
 *
 *	Code and table for Figure F.12: extend sign bit
 *
 * Results:
 *	The extended value.
 *
 * Side effects:
 *	None.
 *
 *--------------------------------------------------------------
 */
    
    //jws2f - add the 16th value to the following 2 arrays
    // because some images were causing s to equal 16 for HuffExtend.
    // still not sure this is correct...
    private int extendTest[] =	/* entry n is 2**(n-1) */
    {0, 0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080,
     0x0100, 0x0200, 0x0400, 0x0800, 0x1000, 0x2000, 0x4000, 0x8000};
     
     private int extendOffset[] =	/* entry n is (-1 << n) + 1 */
     {0,
         ((-1) << 1) + 1,
         ((-1) << 2) + 1,
         ((-1) << 3) + 1,
         ((-1) << 4) + 1,
        ((-1) << 5) + 1,
        ((-1) << 6) + 1,
        ((-1) << 7) + 1,
        ((-1) << 8) + 1,
        ((-1) << 9) + 1,
        ((-1) << 10) + 1,
        ((-1) << 11) + 1,
        ((-1) << 12) + 1,
        ((-1) << 13) + 1,
        ((-1) << 14) + 1,
        ((-1) << 15) + 1,
        ((-1) << 16) + 1};
      
      private int HuffExtend(int x, int s) {
          if ((x) < extendTest[s]) {
              (x) += extendOffset[s];
          }
          return x;
      }
      
/*
 *--------------------------------------------------------------
 *
 * DecodeFirstRow --
 *
 *	Decode the first raster line of samples at the start of
 *      the scan and at the beginning of each restart interval.
 *	This includes modifying the component value so the real
 *      value, not the difference is returned.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Bitstream is parsed.
 *
 *--------------------------------------------------------------
 */
      private void DecodeFirstRow(int[][] curRowBuf) throws IOException {
          short curComp;
          int ci;
          int s,col,compsInScan,numCOL;
          JpegComponentInfo compptr;
          int Pr,Pt,d;
          HuffmanTable dctbl;
          
          Pr=dcPtr.dataPrecision;
          Pt=dcPtr.Pt;
          compsInScan=dcPtr.compsInScan;
          numCOL=dcPtr.imageWidth;
          
          System.out.println("DecodeFirstRow");
          System.out.println("PR = " + Pr + ", PT = " + Pt);
          
    /*
     * the start of the scan or at the beginning of restart interval.
     */
          for (curComp = 0; curComp < compsInScan; curComp++) {
              ci = dcPtr.MCUmembership[curComp];
              compptr = dcPtr.curCompInfo[ci];
              dctbl = dcPtr.dcHuffTblPtrs[compptr.dcTblNo];
              
        /*
         * Section F.2.2.1: decode the difference
         */
              s = HuffDecode(dctbl);
              if (s>0) {
                  d = get_bits(s);
                  d = HuffExtend(d,s);
              } else {
                  d = 0;
              }
              
              
        /*
         * Add the predictor to the difference.
         */
              curRowBuf[0][curComp]=(d+(1<<(Pr-Pt-1)));
              
              System.out.println("d=" + d + ", val="+curRowBuf[0][curComp]);
          }
          
    /*
     * the rest of the first row
     */
          for (col=1; col<numCOL; col++) {
              for (curComp = 0; curComp < compsInScan; curComp++) {
                  ci = dcPtr.MCUmembership[curComp];
                  compptr = dcPtr.curCompInfo[ci];
                  dctbl = dcPtr.dcHuffTblPtrs[compptr.dcTblNo];
                  
            /*
             * Section F.2.2.1: decode the difference
             */
                  s = HuffDecode(dctbl);
                  if (s==16) System.out.println("?");
                  if (s>0) {
                      d = get_bits(s);
                      d = HuffExtend(d,s);
                  } else {
                      d = 0;
                  }
                  
            /*
             * Add the predictor to the difference.
             */
                  curRowBuf[col][curComp]=(d+curRowBuf[col-1][curComp]);
              }
          }
          
          if (dcPtr.restartInRows > 0) {
              (dcPtr.restartRowsToGo)--;
          }
      }
      
/*
 *--------------------------------------------------------------
 *
 * PmPutRow --
 *
 *      Output one row of pixels stored in RowBuf.
 *
 * Results:
 *      None
 *
 * Side effects:
 *      One row of pixels are write to file pointed by outFile.
 *
 *--------------------------------------------------------------
 */
      private void PmPutRow(int[][] RowBuf,int numComp, int numCol, int Pt) throws IOException {
          int col,v;
          if (numComp==1) { /*pgm*/
              for (col = 0; col < numCol; col++) {
                  v=RowBuf[col][0]<<Pt;
                  putc(v);
              }
          } else { /*ppm*/
              for (col = 0; col < numCol; col++) {
                  v=RowBuf[col][0]<<Pt;
                  putc(v);
                  v=RowBuf[col][1]<<Pt;
                  putc(v);
                  v=RowBuf[col][2]<<Pt;
                  putc(v);
              }
          }
      }
/*
 *--------------------------------------------------------------
 *
 * QuickPredict --
 *
 *      Calculate the predictor for sample curRowBuf[col][curComp].
 *	It does not handle the special cases at image edges, such
 *      as first row and first column of a scan. We put the special
 *	case checkings outside so that the computations in main
 *	loop can be simpler. This has enhenced the performance
 *	significantly.
 *
 * Results:
 *      predictor is passed out.
 *
 * Side effects:
 *      None.
 *
 *--------------------------------------------------------------
 */
      private int QuickPredict(int col, int curComp, int[][]curRowBuf, int[][] prevRowBuf, int psv) {
          int left,upper,diag,leftcol;
          
          leftcol=col-1;
          upper=prevRowBuf[col][curComp];
          left=curRowBuf[leftcol][curComp];
          diag=prevRowBuf[leftcol][curComp];
          
          int predictor = 0;
          
    /*
     * All predictor are calculated according to psv.
     */
          switch (psv) {
              case 0:
                  predictor = 0;
                  break;
              case 1:
                  predictor = left;
                  break;
              case 2:
                  predictor = upper;
                  break;
              case 3:
                  predictor = diag;
                  break;
              case 4:
                  predictor = left+upper-diag;
                  break;
              case 5:
                  predictor = left+((upper-diag)>>1);
                  break;
              case 6:
                  predictor = upper+((left-diag)>>1);
                  break;
              case 7:
                  predictor = (left+upper)>>1;
                  break;
              default:
                  System.out.println("Warning: Undefined PSV");
                  predictor = 0;
          }
          
          return predictor;
      }
      
/*
 *--------------------------------------------------------------
 *
 * ProcessRestart --
 *
 *	Check for a restart marker & resynchronize decoder.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	BitStream is parsed, bit buffer is reset, etc.
 *
 *--------------------------------------------------------------
 */
      private void ProcessRestart() throws IOException {
          int c, nbytes;
          int ci;
          
          System.out.println("ProcessRestart()");
          
    /*
     * Throw away any unused bits remaining in bit buffer
     */
          nbytes = bitsLeft / 8;
          bitsLeft = 0;
          
    /*
     * Scan for next JPEG marker
     */
          do {
              do {			/* skip any non-FF bytes */
                  nbytes++;
                  c = GetJpegChar();
              } while (c != 0xFF);
              do {			/* skip any duplicate FFs */
            /*
             * we don't increment nbytes here since extra FFs are legal
             */
                  c = GetJpegChar();
              } while (c == 0xFF);
          } while (c == 0);		/* repeat if it was a stuffed FF/00 */
          
          if (c != (RST0 + dcPtr.nextRestartNum)) {
              
        /*
         * Uh-oh, the restart markers have been messed up too.
         * Just bail out.
         */
              System.out.println("Error: Corrupt JPEG data.  Exiting...");
              System.exit(-1);
          }
          
    /*
     * Update restart state
     */
          dcPtr.restartRowsToGo = dcPtr.restartInRows;
          dcPtr.nextRestartNum = (dcPtr.nextRestartNum + 1) & 7;
      }
      
/*
 *--------------------------------------------------------------
 *
 * DecodeImage --
 *
 *      Decode the input stream. This includes modifying
 *      the component value so the real value, not the
 *      difference is returned.
 *
 * Results:
 *      None.
 *
 * Side effects:
 *      Bitstream is parsed.
 *
 *--------------------------------------------------------------
 */
      private void DecodeImage() throws IOException {
          int s,d,col,row;
          short curComp;
          int ci;
          HuffmanTable dctbl;
          JpegComponentInfo compptr;
          int predictor;
          int numCOL,numROW,compsInScan;
          /*MCU*/ int prevRowBuf[][], curRowBuf[][];
          int imagewidth,Pt,psv;
          
          numCOL=imagewidth=dcPtr.imageWidth;
          numROW=dcPtr.imageHeight;
          compsInScan=dcPtr.compsInScan;
          Pt=dcPtr.Pt;
          psv=dcPtr.Ss;
          prevRowBuf=mcuROW2;
          curRowBuf=mcuROW1;
          
          
    /*
     * Decode the first row of image. Output the row and
     * turn this row into a previous row for later predictor
     * calculation.
     */
          DecodeFirstRow(curRowBuf);
          PmPutRow(curRowBuf,compsInScan,numCOL,Pt);
          
          // swap(prevRowBuf,curRowBuf);
          int[][] tmp = prevRowBuf;
          prevRowBuf = curRowBuf;
          curRowBuf = tmp;
          
          for (row=1; row<numROW; row++) {
              System.out.println("Row = " + row);
              
        /*
         * Account for restart interval, process restart marker if needed.
         */
              if (dcPtr.restartInRows > 0) {
                  if (dcPtr.restartRowsToGo == 0) {
                      ProcessRestart();
                      
              /*
               * Reset predictors at restart.
               */
                      DecodeFirstRow(curRowBuf);
                      PmPutRow(curRowBuf,compsInScan,numCOL,Pt);
                      //swap(prevRowBuf,curRowBuf);
                      tmp = prevRowBuf;
                      prevRowBuf = curRowBuf;
                      curRowBuf = tmp;
                      continue;
                  }
                  dcPtr.restartRowsToGo--;
              }
              
        /*
         * The upper neighbors are predictors for the first column.
         */
              for (curComp = 0; curComp < compsInScan; curComp++) {
                  ci = dcPtr.MCUmembership[curComp];
                  compptr = dcPtr.curCompInfo[ci];
                  dctbl = dcPtr.dcHuffTblPtrs[compptr.dcTblNo];
                  
            /*
             * Section F.2.2.1: decode the difference
             */
                  s = HuffDecode(dctbl);
                  if (s==16) System.out.println("@");
                  if (s>0) {
                      d = get_bits(s);
                      d = HuffExtend(d,s);
                  } else {
                      d = 0;
                  }
                  
                  curRowBuf[0][curComp]=(d+prevRowBuf[0][curComp]);
              }
              
        /*
         * For the rest of the column on this row, predictor
         * calculations are base on PSV.
         */
              for (col=1; col<numCOL; col++) {
                  for (curComp = 0; curComp < compsInScan; curComp++) {
                      ci = dcPtr.MCUmembership[curComp];
                      compptr = dcPtr.curCompInfo[ci];
                      dctbl = dcPtr.dcHuffTblPtrs[compptr.dcTblNo];
                      
                /*
                 * Section F.2.2.1: decode the difference
                 */
                      s = HuffDecode(dctbl);
                      if (s==16) System.out.println("#");
                      if (s>0) {
                          d = get_bits(s);
                          d = HuffExtend(d,s);
                      } else {
                          d = 0;
                      }
                      predictor = QuickPredict(col,curComp,curRowBuf,prevRowBuf,psv);
                      
                      curRowBuf[col][curComp]=(d+predictor);
                  }
              }
              PmPutRow(curRowBuf,compsInScan,numCOL,Pt);
              //swap(prevRowBuf,curRowBuf);
              tmp = prevRowBuf;
              prevRowBuf = curRowBuf;
              curRowBuf = tmp;
          }
      }
      
      private void  swap(int a[][], int b[][]) {
          int[][] c=a;
          a=b;
          b=c;
      }
      
/*
 *--------------------------------------------------------------
 *
 * WritePmHeader --
 *
 *	Output Portable Pixmap (PPM) or Portable
 *	Graymap (PGM) image header.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *      The PPM or PGM header is written to file
 *	pointed by outFile.
 *
 *--------------------------------------------------------------
 */
      private void
      WritePmHeader() {
          PrintWriter pw = new PrintWriter(outputStream);
          
          switch(dcPtr.numComponents) {
              case 1: /* pgm */
                  if (dcPtr.dataPrecision==8) {
                      pw.println("P5");
                      pw.println(dcPtr.imageWidth + " " + dcPtr.imageHeight);
                      pw.println("255");
                  } else {
                      pw.println("P5");
                      pw.println(dcPtr.imageWidth + " " + dcPtr.imageHeight);
                      pw.println(((1<<dcPtr.dataPrecision)-1));
                  }
                  break;
              case 3: /* ppm */
                  if (dcPtr.dataPrecision==8) {
                      pw.println("P6");
                      pw.println(dcPtr.imageWidth + " " + dcPtr.imageHeight);
                      pw.println("255");
                  } else {
                      pw.println("P6");
                      pw.println(dcPtr.imageWidth + " " + dcPtr.imageHeight);
                      pw.println(((1<<dcPtr.dataPrecision)-1));
                  }
                  break;
              default:
                  System.out.println("Error: Unsupported image format.");
                  System.exit(-1);
          }
          
          pw.flush();
      }
      
      public void readFileTest(File file) throws IOException, FileNotFoundException {
          inputStream = new java.io.PushbackInputStream(new FileInputStream(file), 512);
          readHeader();
          
        /*
         * Loop through each scan in image. ReadScanHeader returns
         * 0 once it consumes and EOI marker.
         */
          if (ReadScanHeader() <= 0) {
              System.out.println("Empty JPEG file");
              System.exit(1);
          }
          
          WritePmHeader();
          
          DecoderStructInit();
          HuffDecoderInit();
          DecodeImage();
          
      }
      
      public void decodeToPNM() throws IOException {
          readHeader();
          
        /*
         * Loop through each scan in image. ReadScanHeader returns
         * 0 once it consumes and EOI marker.
         */
          if (ReadScanHeader() <= 0) {
              System.out.println("Empty JPEG file");
              System.exit(1);
          }
          
          WritePmHeader();
          
          DecoderStructInit();
          HuffDecoderInit();
          DecodeImage();
      }
      
      public void decodeImageHeader() throws IOException {
          readHeader();
          
        /*
         * Loop through each scan in image. ReadScanHeader returns
         * 0 once it consumes and EOI marker.
         */
          if (ReadScanHeader() <= 0) {
              System.out.println("Empty JPEG file");
              System.exit(1);
          }
      }
      
      public void decodeImageData() throws IOException {
          DecoderStructInit();
          HuffDecoderInit();
          DecodeImage();
      }
      
      public static void main(String[] args) throws IOException, FileNotFoundException {
          LJPEGDecoder decoder = new LJPEGDecoder();
          
          // Set the output stream
          OutputStream os = new BufferedOutputStream(new FileOutputStream(new File("/home/jsnell/testljpeg.pnm")));
          decoder.setOutputStream(os);
          
          // Set the input stream
          JFileChooser fileChooser = new JFileChooser();
          fileChooser.showOpenDialog(null);
          decoder.setInputStream(new BufferedInputStream(new FileInputStream(fileChooser.getSelectedFile())));
          decoder.decodeImageHeader();
          System.out.println(decoder.getDecompressInfo());
          
          decoder.setInputStream(new BufferedInputStream(new FileInputStream(fileChooser.getSelectedFile())));
          
          // decode the input stream
          decoder.decodeToPNM();
          
          System.out.println("Done.");
          
          os.flush();
          os.close();
          
          System.exit(0);
      }
      
}
