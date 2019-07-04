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
package org.fusfoundation.kranion;

import java.io.*;
import java.util.*;
import org.lwjgl.util.vector.*;

public class InsightecTxdrGeomReader {

    private Vector4f[] channels;
    private boolean[] channelActive;
    private String name;

    public InsightecTxdrGeomReader(String filename) throws IOException {
        channels = null;
        channelActive = null;
        name = new String(filename);

        InputStream rstm = this.getClass().getResourceAsStream(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(rstm));
        //System.out.println(in.readLine());

        parseFile(in);

    }
    
    public InsightecTxdrGeomReader(File file) throws IOException {
        channels = null;
        channelActive = null;
        name = new String(file.getName());

        InputStream rstm = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(rstm));
        //System.out.println(in.readLine());

        parseFile(in);

    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    private void parseFile(BufferedReader in) throws IOException {
        String line;
        do {
            line = in.readLine();
        } while (!line.startsWith("NUM_OF_X_CHANNELS"));
        //System.out.println(line);

        String[] tokens = line.split("=");
        int elemCount = Integer.parseInt(tokens[1].trim());
        //System.out.println("Element count = " + elemCount);

        channels = new Vector4f[elemCount];
        channelActive = new boolean[elemCount];

        int channelCount = 0;
        while (channelCount < elemCount) {
            do {
                line = in.readLine();
            } while (!line.trim().startsWith("XCH"));
            //System.out.println(line);

            tokens = line.split("=");

            int channelNumber = Integer.parseInt(tokens[0].trim().substring(3));
            //System.out.println("Channel number = " + channelNumber);

            //System.out.println(tokens[1].trim());
            StringTokenizer tok = new StringTokenizer(tokens[1].trim());

            channels[channelNumber] = new Vector4f(
                    Float.parseFloat(tok.nextToken()),
                    Float.parseFloat(tok.nextToken()),
                    Float.parseFloat(tok.nextToken()) - 150f, // Insightec puts the origin at the bottom of the transducer, not the focus
                    Float.parseFloat(tok.nextToken())
            );

            Vector3f tmp = new Vector3f();
            tmp.x = channels[channelNumber].x;
            tmp.y = channels[channelNumber].y;
            tmp.z = channels[channelNumber].z;
            if (tmp.length() > 151.0f) {
//                System.out.println(channelNumber + " - " + tmp);
//                System.out.println(tmp.length());
                channelActive[channelNumber] = false; // Set to true to see rays for hidden elements
            }
            else {
                channelActive[channelNumber] = true;
            }
//            System.out.println("Channel desc " + channelNumber + " = " + channels[channelNumber]);
            channelCount++;
        }        
    }

    public Vector4f getChannel(int channelNum) {
        if (channels == null || channelNum < 0 || channelNum >= channels.length) {
            return null;
        }
        return channels[channelNum];
    }
    
    public boolean getChannelActive(int channelNum) {
          if (channels == null || channelNum < 0 || channelNum >= channels.length) {
            return false;
        }
        return channelActive[channelNum];      
    }
    
    public void setChannelActive(int channelNum, boolean isActive) {
        channelActive[channelNum] = isActive;
    }

    public int getChannelCount() {
        if (channels == null) {
            return 0;
        }
        return (channels.length);
    }
    
    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        try {
            InsightecTxdrGeomReader reader = new InsightecTxdrGeomReader("TransducerInfo/XdNominalGeometry_7002.ini");
            InsightecTxdrGeomReader reader2 = new InsightecTxdrGeomReader("TransducerInfo/NominalTransducerGeometry_6002.ini");
        } catch (Exception e) {
            System.out.println("InsightecTxdrGeomReader failed to open or read the file.");
            System.out.println(e);
        }
    }
}
