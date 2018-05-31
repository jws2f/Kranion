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


import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Iterator;

/** Base interface for image volume classes. Implementing classes define the stategy
 * for handling voxel data allocation, although it is assumed by this interface that voxel
 * data is basically represented as a contiguous array of voxel values.
 * @author John W. Snell, Ph.D.
 * {@link mailto://jsnell@virginia.edu}
 */
public interface ImageVolume {
    
    /** Voxels are represented as unsigned bytes. */    
    public static final int UBYTE_VOXEL = 1;
    /** Voxels are represented as unsigned shorts. */    
    public static final int USHORT_VOXEL = 2;
    /** Voxels are represented as integers (32 bit, signed). */    
    public static final int INT_VOXEL = 4;
    /** Voxels are represented as floats (32 bit, signed). */    
    public static final int FLOAT_VOXEL = 8;
    /** Voxels are represented as integers (packed, 8 bits per r,g,b,a).
     * </BR>
     * <CODE>
     * r = 0x000000ff;</BR>
     * g = 0x0000ff00;</BR>
     * b = 0x00ff0000;</BR>
     * a = 0xff000000;</BR>
     * </CODE>
     */    
    public static final int RGBA_VOXEL = 16;
        
    public ImageVolume createMatchingVolume(int voxtype);
    public int addChannel(int voxtype);
    public void freeChannel(int channel);

    public int getDimensionality();
    public ImageDimension getDimension(int dimension);
    public int getVoxelOffset(int x, int y, int z);
    public int getVoxelType();
        
    /** Returns the voxel data as an array of the created voxel type. */    
    public Object getData();
    public Object getData(int channel);
    /** Returns the voxel data as a java.nio.ByteBuffer. This may (and probably will)
     * involve making a copy. ByteBuffer may or may not be direct. Main intent is to
     * provide a mechanism for passing voxel data to native code. In the event of a
     * copy, syncronization is up to the caller (for now).
     */    
    public ByteBuffer getByteBuffer();
    
    public void setAttribute(String name, Object value);
    public void setAttribute(String name, Object value, boolean isTransient);
    public boolean getIsAttributeTransient(String name);
    public Object getAttribute(String name);
    public Iterator getAttributeKeys();
    public void removeAttribute(String name);
        
    /** Return a BufferedImage representation of the selected 2D image volume slice.
     * Provides a mechanism for direct display and manipulation (drawing, etc.) of
     * slices via Java2D.
     * @return Returns an instance of java.awt.image.BufferdImage
     * @param slice indicates the desired ImageVolume slice (zero-based).
     */    
    public BufferedImage getBufferedImage(int slice);
    /** Returns a Graphics2D instance for the selected slice. Allows drawing on a slice
     * via Java2D.
     * @return Returns an instance of java.awt.Graphics2D.
     * @param slice Indicates the desired ImageVolume slice (zero-based).
     */    
    public Graphics2D getGraphics2D(int slice);
}
