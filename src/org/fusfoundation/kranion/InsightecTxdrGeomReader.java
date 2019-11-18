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

    private Vector4f[] channelPosArea;
    private Vector3f[] channelNormal;
    private boolean[] channelActive;
    private String name;

    public InsightecTxdrGeomReader(int channelCount) {
        channelPosArea = new Vector4f[channelCount];
        channelNormal = new Vector3f[channelCount];
        channelActive = new boolean[channelCount];        
    }
    
    public InsightecTxdrGeomReader(String filename) throws IOException {
        channelPosArea = null;
        channelNormal = null;
        channelActive = null;
        name = new String(filename);

        InputStream rstm = this.getClass().getResourceAsStream(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(rstm));
        //System.out.println(in.readLine());

        parseFile(in);

    }
    
    public InsightecTxdrGeomReader(File file) throws IOException {
        channelPosArea = null;
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

        channelPosArea = new Vector4f[elemCount];
        channelNormal = new Vector3f[elemCount];
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

            channelPosArea[channelNumber] = new Vector4f(
                    Float.parseFloat(tok.nextToken()),
                    Float.parseFloat(tok.nextToken()),
                    Float.parseFloat(tok.nextToken()) - 150f, // Insightec puts the origin at the bottom of the transducer, not the focus
                    Float.parseFloat(tok.nextToken())
            );
            
            // assume for now that all elements are normal to the origin which is the natural focus point
            // if we add normal specification option int he source file, we can add reading that info
            channelNormal[channelNumber] = new Vector3f(
                    -channelPosArea[channelNumber].x,
                    -channelPosArea[channelNumber].y,
                    -channelPosArea[channelNumber].z
            );
            channelNormal[channelNumber].normalise();

            Vector3f tmp = new Vector3f();
            tmp.x = channelPosArea[channelNumber].x;
            tmp.y = channelPosArea[channelNumber].y;
            tmp.z = channelPosArea[channelNumber].z;
            if (tmp.length() >= 155.0f) {
//                System.out.println(channelNumber + " - " + tmp);
//                System.out.println(tmp.length());
                channelActive[channelNumber] = false; // Set to true to see rays for hidden elements
                System.out.println("*** Inactive Channel found: " + channelNumber);
            }
            else {
                channelActive[channelNumber] = true;
            }
//            System.out.println("Channel desc " + channelNumber + " = " + channels[channelNumber]);
            channelCount++;
        }        
    }
    
    public void setChannelPos(int channelNum, Vector3f pos) {
        if (channelPosArea != null || channelNum >= 0 || channelNum < channelPosArea.length) {
            Vector4f ch = channelPosArea[channelNum];
            if (ch == null) {
                ch = new Vector4f();
            }
            ch.x = pos.x;
            ch.y = pos.y;
            ch.z = pos.z;
        }
        return;
    }
    
    public void setChannel(int channelNum, Vector4f pos_area) {
        if (channelPosArea != null || channelNum >= 0 || channelNum < channelPosArea.length) {
            channelPosArea[channelNum] = new Vector4f(pos_area);
        }
        return;
    }
    
    public void setChannelNorm(int channelNum, Vector3f norm) {
        if (channelPosArea != null || channelNum >= 0 || channelNum < channelPosArea.length) {
            channelNormal[channelNum] = new Vector3f(norm);
        }
        return;
    }

    public Vector4f getChannel(int channelNum) {
        if (channelPosArea != null || channelNum >= 0 || channelNum < channelPosArea.length) {
            return channelPosArea[channelNum];
        }
        return null;
    }
    
    public Vector3f getChannelNormal(int channelNum) {
        if (channelNormal == null || channelNum < 0 || channelNum >= channelNormal.length) {
            return null;
        }
        return channelNormal[channelNum];
    }
    
    public boolean getChannelActive(int channelNum) {
        if (channelActive == null || channelNum < 0 || channelNum >= channelPosArea.length) {
            return false;
        }
        return channelActive[channelNum];      
    }
    
    public void setChannelActive(int channelNum, boolean isActive) {
        if (channelActive == null || channelNum < 0 || channelNum >= channelPosArea.length) {
            return;
        }
        channelActive[channelNum] = isActive;
    }

    public int getChannelCount() {
        if (channelPosArea == null) {
            return 0;
        }
        return (channelPosArea.length);
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
