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

import java.io.Serializable;
/**
 *
 * @author  jsnell
 */
public class ImageDimension implements Serializable {
    private static final long  serialVersionUID = 4003385660612642331L;
    
    public static final int DIMENSION_TYPE_SPATIAL = 0;
    public static final int DIMENSION_TYPE_TEMPORAL = 1;
    public static final int DIMENSION_TYPE_SPECTRAL = 2;
    
    private int dimensionSize;
    private int type;
    
    private boolean regularSampleSpacing;
    private Float regularSpacing;
    private float[] irregularPositions;
    
    private boolean regularSampleWidth;
    private Float regularWidth;
    private float[] irregularWidths;
    
    
    /** Creates a new instance of ImageDimension */
    
    public ImageDimension() {
        dimensionSize = 1;
        
        type = DIMENSION_TYPE_SPATIAL;
        
        regularSampleWidth = true;
        regularWidth = new Float(1.0f);
        regularSampleSpacing = true;
        regularSpacing = new Float(1.0f);
    }
    
    public ImageDimension(int size) {
        dimensionSize = Math.max(1, size);
        
        type = DIMENSION_TYPE_SPATIAL;
        
        regularSampleWidth = true;
        regularWidth = new Float(1.0f);
        regularSampleSpacing = true;
        regularSpacing = new Float(1.0f);
    }
    
    public ImageDimension(int size, float spacing, float width) {
        dimensionSize = size;
        
        type = DIMENSION_TYPE_SPATIAL;
        
        regularSampleWidth = true;
        regularWidth = new Float(width);
        regularSampleSpacing = true;
        regularSpacing = new Float(spacing);
    }
    
    public ImageDimension(int size, float spacing, float width, int dimtype) {
        dimensionSize = size;
        
        type = dimtype;
        
        regularSampleWidth = true;
        regularWidth = new Float(width);
        regularSampleSpacing = true;
        regularSpacing = new Float(spacing);
    }
    
    public int getSize() { return dimensionSize; }
    public void setSize(int size) { dimensionSize = size; }
    
    public int getType() { return type; }
    public void setType(int t) { type = t; }
    
    public boolean getRegularSampleSpacing() { return regularSampleSpacing; }
    public boolean getRegularSampleWidth() { return regularSampleWidth; }
    
    public float getSampleSpacing() { return regularSpacing.floatValue(); }
    
    public float getSamplePosition(int i) {
        if (regularSampleSpacing && regularSpacing != null) {
            return regularSpacing.floatValue() * (i + 0.5f);
        }
        else {
            return irregularPositions[i];
        }
    }
    
    public float getSampleWidth() { return regularWidth.floatValue(); }
    public float getSampleWidth(int i) {
        if (regularSampleWidth) {
            return regularWidth.floatValue();
        }
        else {
            return irregularWidths[i];
        }
    }
    
    public void setSampleWidth(float width) {
        regularWidth = new Float(width);
        irregularWidths = null;
        regularSampleWidth = true;
    }
    
    public void setSampleWidth(int n, float width) {
        if (regularSampleWidth == true) {
            irregularWidths = new float[dimensionSize];
            for (int i=0; i<dimensionSize; i++) {
                irregularWidths[i] = regularWidth.floatValue();
            }
            regularWidth = null;
            regularSampleWidth = false;
        }
        irregularWidths[n] = width;
    }
    
    public void setSampleSpacing(float spacing) {
        regularSpacing = new Float(spacing);
        irregularPositions = null;
        regularSampleSpacing = true;
    }
    
    public void setSamplePosition(int n, float position) {
        if (regularSampleSpacing == true) {
            irregularPositions = new float[dimensionSize];
            for (int i=0; i<dimensionSize; i++) {
                irregularPositions[i] = getSamplePosition(i);
            }
            regularSpacing = null;
            this.regularSampleSpacing = false;
        }
        irregularPositions[n] = position;
    }
    
}
