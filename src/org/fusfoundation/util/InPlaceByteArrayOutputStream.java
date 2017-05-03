/*
 * InPlaceByteArrayOutputStream.java
 *
 * Created on July 9, 2003, 3:28 PM
 */

package org.fusfoundation.util;

import java.io.OutputStream;

/**
 *
 * @author  jsnell
 */
public class InPlaceByteArrayOutputStream extends OutputStream {
    
    byte[] destination;
    short[] shortDestination;
    int[] intDestination;
    int position = 0;
    int bytePosition = 0;
    int mask = 0xff000000;
    
    /** Creates a new instance of InPlaceByteArrayOutputStream */
    public InPlaceByteArrayOutputStream(byte[] destinationArray) {
        destination = destinationArray;
        position = 0;
    }
    
    public InPlaceByteArrayOutputStream(short[] destinationArray) {
        shortDestination = destinationArray;
        position = 0;
    }
    
    public InPlaceByteArrayOutputStream(int[] destinationArray) {
        intDestination = destinationArray;
        position = 0;
    }
    
    public InPlaceByteArrayOutputStream(Object array) {
        position = 0;
        
        if (array instanceof byte[]) { // 8 bit grayscale
            destination = (byte[])array;
        }
        else if (array instanceof short[]) { // 16 bit grayscale
            shortDestination = (short[])array;
        }
        else if (array instanceof int[]) { // RGBA
            intDestination = (int[])array;
        }
        else {
            throw new RuntimeException("Unexpected object type passed");
        }
    }
    
    public void write(int b) throws java.io.IOException {
        if (destination != null) {
            destination[position++] = (byte)(b & 0xff);
        }
        else if (shortDestination != null) {
            shortDestination[position++] = (short)(b & 0xffff);
            /*
            shortDestination[position] |= (b & 0xff) << ((1 - bytePosition) * 8);
            
            bytePosition++;
            if (bytePosition == 2) {
                bytePosition = 0;
                position++;
            }
             **/
        }
        else {
            intDestination[position] |= (b & 0xff) << ((3 - bytePosition) * 8);
            
            bytePosition++;
            if (bytePosition == 4) {
                bytePosition = 0;
                position++;
            }
        }
    }
    
}
