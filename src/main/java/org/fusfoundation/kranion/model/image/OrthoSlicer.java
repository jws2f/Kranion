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
package org.fusfoundation.kranion.model.image;

import java.io.*;
import java.net.*;
import java.math.*;
import java.awt.image.*;
import java.util.*;

public class OrthoSlicer {
    private ImageVolume inputImageVolume;
    private int imageChannel=0, imageTime=0;
    private ImageVolume xySlice, zySlice, zxSlice;
    private int currentX, currentY, currentZ;
    private int sliceCount = 1, timeCount = 1;
    
    private Collection listeners = new ArrayList();
    
    public void OrthoSlicer() {}
    
    public void setInputImageVolume(ImageVolume img) {
        int x, y, z, t;
        
        inputImageVolume = img;
        
        if (inputImageVolume == null || inputImageVolume.getVoxelType() != ImageVolume.USHORT_VOXEL) {
//            System.out.println("OrhtoSlicer: nulling input and outputs");
            inputImageVolume = null;
            xySlice = null;
            zySlice = null;
            zxSlice = null;
            return;
        }
        
        x = inputImageVolume.getDimension(0).getSize();
        y = inputImageVolume.getDimension(1).getSize();
        z = inputImageVolume.getDimension(2).getSize();
        sliceCount = z;
        timeCount = Math.max(1, inputImageVolume.getDimension(3).getSize());
        
        xySlice = new ImageVolume4D(ImageVolume.USHORT_VOXEL, x, y, 0, 0);
        
        float xres = inputImageVolume.getDimension(0).getSamplePosition(1) - inputImageVolume.getDimension(0).getSamplePosition(0);
        float yres = inputImageVolume.getDimension(1).getSamplePosition(1) - inputImageVolume.getDimension(1).getSamplePosition(0);
        float zres = inputImageVolume.getDimension(2).getSamplePosition(1) - inputImageVolume.getDimension(2).getSamplePosition(0);
        
//        System.out.println(inputImageVolume.getDimension(2).getSamplePosition(0));
//        System.out.println(inputImageVolume.getDimension(2).getSamplePosition(1));
//        System.out.println("voxel size = " + xres + " x " + yres + " x " + zres);
        
        xySlice.getDimension(0).setSampleSpacing(xres);
        xySlice.getDimension(1).setSampleSpacing(yres);
        xySlice.getDimension(0).setSampleWidth(xres);
        xySlice.getDimension(1).setSampleWidth(yres);
        
        zySlice = new ImageVolume4D(ImageVolume.USHORT_VOXEL, z, y, 0, 0);
        zySlice.getDimension(0).setSampleSpacing(zres);
        zySlice.getDimension(1).setSampleSpacing(yres);
        zySlice.getDimension(0).setSampleWidth(zres);
        zySlice.getDimension(1).setSampleWidth(yres);
        
        zxSlice = new ImageVolume4D(ImageVolume.USHORT_VOXEL, z, x, 0, 0);
        zxSlice.getDimension(0).setSampleSpacing(zres);
        zxSlice.getDimension(1).setSampleSpacing(xres);
        zxSlice.getDimension(0).setSampleWidth(zres);
        zxSlice.getDimension(1).setSampleWidth(xres);
        
        // copy image attributes
        Iterator iter = inputImageVolume.getAttributeKeys();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            xySlice.setAttribute(key, inputImageVolume.getAttribute(key));
            zySlice.setAttribute(key, inputImageVolume.getAttribute(key));
            zxSlice.setAttribute(key, inputImageVolume.getAttribute(key));
        }
        
        setPosition(x/2, y/2, z/2, false);
        
    }
    
    public void setImageChannel(int channel) {
        imageChannel = channel;
        DoXY(currentZ);
        DoZX(currentY);
        DoZY(currentX);
    }
    
    public int getTime() { return imageTime; }
    public void setTime(int value) {
        if (value != imageTime) {
            if (inputImageVolume != null && inputImageVolume.getDimension(3).getSize() > 1) {
                imageTime = value;
                DoXY(currentZ);
                DoZX(currentY);
                DoZY(currentX);
            }
            else {
                imageTime = 0;
            }
        }
    }
    
    public ImageVolume getXY() { return xySlice; }
    public ImageVolume getZY() { return zySlice; }
    public ImageVolume getZX() { return zxSlice; }
    
    public void setPosition(int x, int y, int z, boolean drag) {
      //  System.out.println("setPosition: " + x + ", " + y + ", " + z);
        
        if (z < 0) z = 0;
        if (z >= sliceCount) z = sliceCount-1;
        
        if (x<0) x =0;
        if (x>255) x=255;
        
        if (y<0) y=0;
        if (y>255) y = 255;
        
        if (x != currentX) {
            DoZY(x);
            currentX = x;
        }
        if (y != currentY) {
            DoZX(y);
            currentY = y;
        }
        if (z != currentZ) {
            DoXY(z);
            currentZ =z;
        }
    }
    
    public int getXPos() { return currentX; }
    public int getYPos() { return currentY; }
    public int getZPos() { return currentZ; }
    
    private void DoXY(int z) {
        if (this.inputImageVolume == null) return;
     //   System.out.println("DoXY: " + z );
        
        int width, height;
        short in[] = (short[])inputImageVolume.getData(imageChannel);
        short out[] = (short[])xySlice.getData();
        
        width = xySlice.getDimension(0).getSize();
        height = xySlice.getDimension(1).getSize();
        
        int framesize = width*height;
        
        int sliceOffset = z * framesize + imageTime * framesize * sliceCount;
        
    /*
    int inOffset, outOffset;
    for (int y=0; y<height; y++) {
      inOffset = y*width + sliceOffset;
      outOffset = y*width;
      for (int x=0; x<width; x++) {
        out[x + outOffset] = in[x + inOffset];
      }
    }
     */
        System.arraycopy(in, sliceOffset, out, 0, width*height);
    }
    
    private void DoZY(int x) {
        if (this.inputImageVolume == null) return;
        
      //  System.out.println("DoZY: " + x );
        
        int inwidth, inheight, outwidth, outheight, sliceOffset;
        short in[] = (short[])inputImageVolume.getData(imageChannel);
        short out[] = (short[])zySlice.getData();
        
        inwidth = inputImageVolume.getDimension(0).getSize();
        inheight = inputImageVolume.getDimension(1).getSize();
        
        int framesize = inwidth * inheight;
        
        sliceOffset = framesize;
        int timeOffset = imageTime * framesize * sliceCount;
        
        outwidth = zySlice.getDimension(0).getSize();
        outheight = zySlice.getDimension(1).getSize();
        
        int inOffset, outOffset;
        for (int y=0; y<outheight; y++) {
            inOffset = y*inwidth + x;
            outOffset = y*outwidth;
            for (int z=0; z<outwidth; z++) {
                out[z + outOffset] = in[z*sliceOffset + inOffset + timeOffset];
            }
        }
    }
    
    private void DoZX(int y) {
        if (this.inputImageVolume == null) return;
      //  System.out.println("DoZX: " + y );
        
        int inwidth, inheight, outwidth, outheight, sliceOffset;
        short in[] = (short[])inputImageVolume.getData(imageChannel);
        short out[] = (short[])zxSlice.getData();
        
        inwidth = inputImageVolume.getDimension(0).getSize();
        inheight = inputImageVolume.getDimension(1).getSize();
        
        int framesize = inwidth * inheight;
        
        sliceOffset = framesize;
        int timeOffset = imageTime * framesize * sliceCount;
        
        outwidth = zxSlice.getDimension(0).getSize();
        outheight = zxSlice.getDimension(1).getSize();
        
        int inOffset, outOffset;
        for (int x=0; x<outheight; x++) {
            inOffset = x + y * inwidth;
            outOffset = x*outwidth;
            for (int z=0; z<outwidth; z++) {
                out[z + outOffset] = in[z*sliceOffset + inOffset + timeOffset];
            }
        }
    }
    
}