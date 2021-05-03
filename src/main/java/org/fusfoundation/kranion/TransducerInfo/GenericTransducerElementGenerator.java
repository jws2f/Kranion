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
package org.fusfoundation.kranion.TransducerInfo;

/**
 *
 * @author john
 */
public class GenericTransducerElementGenerator {
    public static void main(String[] args) {
        
        int channelCount = 128; //1024;
        double focalLength = 50; //150;
        double elementArea = 5;// 113.54;
        double fractionOfHemisphereHeight = 0.1; //1;
        
        System.out.println("NUM_OF_X_CHANNELS	=	" + channelCount);
        
        double phi = (Math.sqrt(5.0) - 1.0)/2.0;
        //double ga = phi * 2.0 * Math.PI;
        double ga = 2.0 * Math.PI * (1.0 - 1/phi);
        
//        double scale = (Math.PI*Math.PI/4.0)/channelCount;
        
        for (int n=1; n<=channelCount; n++) {
            
            double c = channelCount/(1.0 + Math.cos((Math.PI/2) * 1.025));
            double rn = Math.acos((c - n)/c) * fractionOfHemisphereHeight;
            double thetan = ga * n;
            
            double x = focalLength * Math.sin(rn) * Math.cos(thetan);
            double y = focalLength * Math.sin(rn) * Math.sin(thetan);
            double z = focalLength * Math.cos(rn);
            
            System.out.print("XCH" + (n-1) + "	=	");
//            System.out.println(x + " " + y + " " + z + " 113.64");
            
            System.out.printf("%4.4f %4.4f %4.4f %4.4f", x, y, 150d - z, elementArea);
            System.out.println();
        }
    }
}
