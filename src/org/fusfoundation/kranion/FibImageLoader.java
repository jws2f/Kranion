/*
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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

import com.jmatio.io.MatFile;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.sun.scenario.effect.impl.BufferUtil;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Math.log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author jsnell
 */
public class FibImageLoader {
    
    public static void main(String[] args) {
        
//        JFileChooser chooser = new JFileChooser();
//        chooser.setDialogTitle("Choose FIB file");
//        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        chooser.setMultiSelectionEnabled(false);
//        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
{
//                File file = chooser.getSelectedFile();
                File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/data.src.gz.odf8.f8.qsdr.1.25.R52.fib");
                try {
                    FibImageLoader fibloader = new FibImageLoader(file);
                    fibloader.parse();
                    
                    Iterator<String> iter = fibloader.keys.keySet().iterator();
                    while (iter.hasNext()) {
                        String keyname = iter.next();
                        mat4header hd = fibloader.getEntryHeader(keyname);
                        System.out.println(keyname + ": " + hd.rows + " x " + hd.cols + " : m=" + hd.m + " p=" + hd.p + " t=" + hd.t);

                    }
                    
                    mat4header entry = fibloader.getEntryHeader("dimension");
                    System.out.println("Dimensions = " + entry.rows + " x " + entry.cols);
                    ShortBuffer sbuf = fibloader.getEntryData("dimension").asShortBuffer();
                    System.out.println(sbuf.get());
                    System.out.println(sbuf.get());
                    System.out.println(sbuf.get());
                    
                    entry = fibloader.getEntryHeader("voxel_size");
                    System.out.println("Voxel size = " + entry.rows + " x " + entry.cols);
                    FloatBuffer fbuf = fibloader.getEntryData("voxel_size").asFloatBuffer();
                    System.out.println(fbuf.get());
                    System.out.println(fbuf.get());
                    System.out.println(fbuf.get());
                    
                    entry = fibloader.getEntryHeader("odf_vertices");
                    System.out.println("odf_vertices = " + entry.rows + " x " + entry.cols);
                    fbuf = fibloader.getEntryData("odf_vertices").asFloatBuffer();
                    for (int i=0; i<entry.cols; i++) {
                        System.out.println(i + ") " + fbuf.get() + ", " + fbuf.get() + ", " + fbuf.get());
                    }
                    
                    entry = fibloader.getEntryHeader("fa0");
                    System.out.println("fa0 = " + entry.rows + " x " + entry.cols);
                    System.out.println("datasize = " + entry.dataSize);
                    fbuf = fibloader.getEntryData("fa0").asFloatBuffer();
                    
                    
                    entry = fibloader.getEntryHeader("trans");
                    System.out.println("trans = " + entry.rows + " x " + entry.cols);
                    fbuf = fibloader.getEntryData("trans").asFloatBuffer();
                    float[] mat = new float[16];
                    for (int i=0; i<16; i++) {
                        mat[i] = fbuf.get();
                    }
                    for (int i=0; i<4; i++) {
                        for (int j=0; j<4; j++) {
                            System.out.print(mat[i*4+j] + ", ");
                        }
                        System.out.println();
                    }
                }
                catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
        }
    }
    
    private File theFile;
    private Map<String, mat4header> keys = new HashMap<>();

    public FibImageLoader(File file) {
        theFile = file;
    }
    
    public Iterator<String> getKeyIterator() {
        return keys.keySet().iterator();
    }
    
    public mat4header getEntryHeader(String key) {
        return keys.get(key);
    }
    
    public ByteBuffer getEntryData(String key) {
        ByteBuffer result = null;
        mat4header hd = getEntryHeader(key);
        if (theFile != null && hd != null) {
            int bytecount = hd.dataSize * hd.rows * hd.cols;
            if (hd.imaginary) {
                bytecount *= 2;
            }
            try {
                FileInputStream inFile = new FileInputStream(theFile);
                FileChannel inChannel = inFile.getChannel();
                result = ByteBuffer.allocate(bytecount).order(hd.m == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                inChannel.position(hd.dataOffset);
                inChannel.read(result);
                result.flip();
                inChannel.close();
                inFile.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
    
    public void parse() throws FileNotFoundException {

        keys.clear();
        
        FileInputStream inFile = new FileInputStream(theFile);
        FileChannel inChannel = inFile.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

        try {
            while (inChannel.read(buf) != -1) {
                try {
                    mat4header hd = new mat4header();

                    buf.flip();

                    IntBuffer intbuf = (IntBuffer) buf.asIntBuffer();

                    hd.one = intbuf.get();
                    hd.two = intbuf.get();
                    hd.three = intbuf.get();
                    hd.four = intbuf.get();
                    hd.five = intbuf.get();
                    
                    hd.imaginary = (hd.four == 1);

                    ByteBuffer namebytes = ByteBuffer.allocate(hd.five);
                    inChannel.read(namebytes);
                    String name = new String(namebytes.array()).trim();
                    
                    hd.dataOffset = inChannel.position();

                    hd.m = hd.one / 1000;
                    hd.one -= hd.m * 1000;
                    hd.z = hd.one / 100;
                    hd.one -= hd.z * 100;
                    hd.p = hd.one / 10;
                    hd.one -= hd.p * 10;
                    hd.t = hd.one;
                    
                    if (hd.z!=0) {
                        throw new IOException("Bad file format");
                    }

                    hd.rows = hd.two;
                    hd.cols = hd.three;

                    switch (hd.p) {
                        case 0:
                            hd.dataSize = 8; // double
                            break;
                        case 1:
                            hd.dataSize = 4; //float
                            break;
                        case 2:
                            hd.dataSize = 4; // signed int
                            break;
                        case 3:
                            hd.dataSize = 2; // signed short
                            break;
                        case 4:
                            hd.dataSize = 2; // unsigned short
                            break;
                        case 5:
                            hd.dataSize = 1; // unsigned byte
                            break;
                        default:
                            throw new IOException("Bad file format: illlegal data type");
                    }

                    inChannel.position(inChannel.position() + hd.dataSize * hd.rows * hd.cols * (hd.imaginary ? 2 : 1));
                    
                    keys.put(name, hd);
                    
                    System.out.println(name + ": " + hd.rows + " x " + hd.cols + " : m=" + hd.m + " p=" + hd.p + " t=" + hd.t);


                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            
            inChannel.close();
            inFile.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        

    }
    
    public static class mat4header {
        public int one;
        public int two;
        public int three;
        public int four;
        public int five;
        public long dataOffset;
        public int rows, cols;
        public int dataSize;
        public int m, p, t, z;
        public boolean imaginary;
    }
}
