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

import java.util.HashMap;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.*;
import java.beans.PropertyChangeEvent;
import java.io.Serializable;
import java.nio.*;
import java.util.*;
import java.util.Observable;


/**
 *
 * @author  jsnell
 */
public class ImageVolume4D extends Observable implements ImageVolume, Serializable {
    
    private ImageDimension[] dims = new ImageDimension[4];
    private int voxelCount;
    private Vector voxelData = new Vector();
    private Vector<Integer> voxelType = new Vector();
    private HashMap attributes = new HashMap();
    
    /** Creates a new instance of ImageVolume4D */
    public ImageVolume4D(int voxelType, int x, int y, int z, int t) {
        dims[0] = new ImageDimension(x);
        dims[1] = new ImageDimension(y);
        dims[2] = new ImageDimension(z);
        dims[3] = new ImageDimension(t);
        
        voxelData.setSize(1);
        
        alloc(0, voxelType);
    }
    
    public ImageVolume createMatchingVolume(int voxtype) {
        ImageVolume4D image = new ImageVolume4D(voxtype,  dims[0].getSize(), dims[1].getSize(), dims[2].getSize(), dims[3].getSize());
        
        // copy sample geometry
        // assumes regular sample spacing/widths for x and y
        image.setPixelSize(getPixelWidth(), getPixelHeight());
        
        // handle irregular geometry in z (variable slice position/thickness for CT)
        image.getDimension(2).setSampleWidth(dims[2].getSampleWidth());
        if (dims[2].getRegularSampleSpacing()) {
            image.getDimension(2).setSampleSpacing(dims[2].getSampleWidth());
        }
        else {
            for (int i=0; i<dims[2].getSize(); i++) {
                image.getDimension(2).setSamplePosition(i, dims[2].getSamplePosition(i));
            }
        }
        
        return image;
    }
    
    public int addChannel(int voxtype) {
        int channelNum = voxelData.size();
        voxelData.setSize(channelNum+1);
        alloc(channelNum, voxtype);
        System.out.println("added channel " + channelNum);
        return channelNum;
    }
    
    public void setPixelSize(float width, float height) {
        dims[0].setSampleSpacing(width);
        dims[0].setSampleWidth(width);
        dims[1].setSampleSpacing(height);
        dims[1].setSampleWidth(height);
    }
    
    public float getPixelWidth() { return dims[0].getSampleWidth(); }
    public float getPixelHeight() { return dims[1].getSampleWidth(); }
    
    
    private void alloc(int channel, int type) {
        voxelCount = 1;
        for (int i=0; i<dims.length; i++) {
            voxelCount *= dims[i].getSize();
        }
        
        switch (type) {
            case UBYTE_VOXEL:
                voxelData.set(channel, new byte[voxelCount]);
                break;
            case USHORT_VOXEL:
                voxelData.set(channel, new short[voxelCount]);
                break;
            case INT_VOXEL:
            case RGBA_VOXEL:
                voxelData.set(channel, new int[voxelCount]);
                break;
            case FLOAT_VOXEL:
                voxelData.set(channel, new float[voxelCount]);
                break;
        };
        
        try {
            voxelType.set(channel, type);
        }
        catch(ArrayIndexOutOfBoundsException e) {
            voxelType.add(channel, type);
        }
        
        System.out.println("alloc: channel " + channel);
    }
    
    
    private int getDataSize() { return getDataSize(0); }
    
    private int getDataSize(int channel) {
        switch(voxelType.get(channel)) {
            case ImageVolume.FLOAT_VOXEL:
            case ImageVolume.RGBA_VOXEL:
            case ImageVolume.INT_VOXEL:
                return voxelCount * 4;
            case ImageVolume.UBYTE_VOXEL:
                return voxelCount;
            case ImageVolume.USHORT_VOXEL:
                return voxelCount * 2;
            default:
                throw new RuntimeException("Undefined voxel data type");
        }
    }
    
    
    public ByteBuffer getByteBuffer() { return getByteBuffer(0); }
    
    public ByteBuffer getByteBuffer(int channel) {
        
        int datasize = getDataSize();
        
        ByteBuffer buf = ByteBuffer.allocateDirect(datasize);
        buf.order(ByteOrder.nativeOrder());
        
        switch(voxelType.get(channel)) {
            case ImageVolume.FLOAT_VOXEL:
                FloatBuffer fb = buf.asFloatBuffer();
                fb.put((float[])voxelData.get(channel));
                break;
            case ImageVolume.RGBA_VOXEL:
            case ImageVolume.INT_VOXEL:
                IntBuffer ib = buf.asIntBuffer();
                ib.put((int[])voxelData.get(channel));
                break;
            case ImageVolume.UBYTE_VOXEL:
                buf.put((byte[])voxelData.get(channel));
                break;
            case ImageVolume.USHORT_VOXEL:
                ShortBuffer sb = buf.asShortBuffer();
                sb.put((short[])voxelData.get(channel));
                break;
            default:
                throw new RuntimeException("Undefined voxel data type");            
        }
          
        return buf;
    }
    
    public Object getData() { return getData(0); }
    
    public Object getData(int channel) {
        return voxelData.get(channel);
    }
    
    public ImageDimension getDimension(int dimension) {
        return dims[dimension];
    }
    
    public int getDimensionality() {
        return dims.length;
    }
    
    public BufferedImage getBufferedImage(int slice) {
        DataBuffer db;
        int width = dims[0].getSize();
        int height = dims[1].getSize();
        int sliceSize = width * height;
        
        int voxType = voxelType.get(0);
        
        if (voxType == ImageVolume.USHORT_VOXEL) {
            short[] pixelData = (short[])this.getData();
            db = new DataBufferUShort(pixelData, sliceSize, slice * sliceSize);
        }
        else if (voxType == ImageVolume.UBYTE_VOXEL) {
            byte[] pixelData = (byte[])this.getData();
            db = new DataBufferByte(pixelData, sliceSize, slice * sliceSize);
        }
        else {
            return null;
        }
        
        int[] bandoffsets = {0};
                
        WritableRaster wr = Raster.createBandedRaster(db, width, height, width, bandoffsets, bandoffsets, new Point(0,0));
        
        ColorSpace cs = ColorSpace.getInstance(ICC_ColorSpace.CS_GRAY);
        
        ComponentColorModel cm;
        
        if (voxType == ImageVolume.USHORT_VOXEL) {
            cm = new ComponentColorModel(cs, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
        }
        else if (voxType == ImageVolume.UBYTE_VOXEL) {
            cm = new ComponentColorModel(cs, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
        }
        else {
            return null;
        }
        
        return new BufferedImage(cm, wr, false, null);
    }
    
    public Graphics2D getGraphics2D(int slice) {
        return (Graphics2D)getBufferedImage(slice).getGraphics();
    }
    
    public int getVoxelOffset(int x, int y, int z) {
        int rowsize = dims[0].getSize();
        int slicesize = rowsize * dims[1].getSize();
        
        return x + y*rowsize + z*slicesize;
    }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
            //notify
        setChanged();
        notifyObservers(new PropertyChangeEvent(this, "Attribute["+name+"]", null, value));
    }
    
    public int getVoxelType() { return voxelType.get(0); }
    public int getVoxelType(int channel) { return voxelType.get(channel); }
    
    public Iterator getAttributeKeys() {
        return attributes.keySet().iterator();
    }    
    
}
