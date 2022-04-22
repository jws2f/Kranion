/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
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

package org.fusfoundation.kranion;

/**
 *
 * @author john
 */

/*

The following code to estimate speed of sound in skull bone from CT HU data is based on:

A unified model for the speed of sound in cranial bone based on genetic algorithm optimization
December 2002 Physics in Medicine and Biology 47(22):3925-44
DOI10.1088/0031-9155/47/22/302

*/

public class CTSoundSpeed {
    private final static float[][] densityToSpeed = {
        {1000f,	1852.7f},
        {1100f,	2033f},
        {1200f,	2183.8f},
        {1300f,	2275.8f},
        {1400f,	2302.6f},
        {1500f,	2298.1f},
        {1600f,	2300.3f},
        {1700f,	2332.5f},
        {1800f,	2393.4f},
        {1900f,	2479f},
        {2000f,	2585.4f},
        {2100f,	2708.8f},
        {2200f,	2845.3f},
        {2300f,	2991.1f},
        {2400f,	3142.1f},
        {2500f,	3294.6f},
        {2600f,	3444.7f},
        {2700f,	3588.5f},
        {2800f,	3722.2f},
        {2900f,	3841.9f},
        {3000f,	3947.6f},
        {3100f,	4041.9f},
        {3200f,	4127.7f},
        {3300f,	4207.8f},
        {3400f,	4285.1f}       
    };
    
    public static float lookupSpeed(float density) {
        
        if (density < densityToSpeed[0][0]) {
            return densityToSpeed[0][1];
        }
        
        for (int i=0; i<densityToSpeed.length-1; i++) {
//                System.out.println(densityToSpeed[i][0] + " - " + densityToSpeed[i][1]);
                if (density >= densityToSpeed[i][0] && density < densityToSpeed[i+1][0]) {
                    
                    float x1 = densityToSpeed[i][0]; 
                    float x2 = densityToSpeed[i+1][0]; 
                    float y1 = densityToSpeed[i][1]; 
                    float y2 = densityToSpeed[i+1][1];
                    
                    return (density - x1)/(x2 - x1)*(y2 - y1) + y1;
                }
        }
        
        return densityToSpeed[densityToSpeed.length-1][1];
        
    }
    
    public static float getMinSpeed() {
        return densityToSpeed[0][1];
    }
    
    public static float getMaxSpeed() {
        return densityToSpeed[densityToSpeed.length-1][1];
    }
    
    public static void main(String[] params) {
        for (int i=0; i<densityToSpeed.length; i++) {
                System.out.println(densityToSpeed[i][0] + " - " + densityToSpeed[i][1]);
        }
        
        System.out.println();
        System.out.println("1950 - " + lookupSpeed(1950f));
        System.out.println("3250 - " + lookupSpeed(3250f));
        System.out.println("0 - " + lookupSpeed(0f));
        System.out.println("3450 - " + lookupSpeed(3450f));
   }
}
