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
import java.beans.PropertyChangeEvent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import static org.fusfoundation.kranion.Main.glPopMatrix;
import static org.fusfoundation.kranion.Main.glPushMatrix;
import static org.fusfoundation.kranion.Trackball.toMatrix4f;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE1;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_REPLACE;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_SHORT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_MODE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRANSFORM_BIT;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexEnvf;
import static org.lwjgl.opengl.GL11.glTexImage1D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.glMultiDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import org.lwjgl.opengl.GL20;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE0;
import static org.lwjgl.opengl.GL30.GL_CLIP_DISTANCE1;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_R16;
import static org.lwjgl.opengl.GL30.GL_R16F;
import static org.lwjgl.opengl.GL30.GL_R32F;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class FibImageLoader extends GUIControl {
    
    // texture names
    int odfVertsTexName = 0;
    int[] indexTexName;
    int[] faTexName;
    
    private static ShaderProgram fiberTrackShader, renderShader;
    private int vao;
    private int seedCount = 0;
    private int seedSSBo=0; // buffer of seed locations
    private int fiberSSBo=0; // buffer of tracked fiber points
    private int fiberColorsSSBo=0; // buffer of tracked fiber points
    private int fiberCountsSSB0=0; // number of tracked points per fiber
    private int fiberFilteredCountsSSB0=0; // number of tracked points per fiber
//    private int fiberStartsSSB0=0; // number of tracked points per fiber
    
    // fib image volume size
    private int nx, ny, nz;
    private float rx, ry, rz;
    private int odfVertsSize = 0;
    
    private Vector3f currentTarget = new Vector3f();
    private Vector3f currentSteering = new Vector3f();

    private boolean clipFibers = false;
    private float   clipThickness = 2f;
    
    private ImageVolume4D trackedImage = null;
    private Vector3f renderTranslation = new Vector3f();
    private Quaternion renderOrientation = new Quaternion().setIdentity();

    private boolean bShowFilteredFibers = false;
    private List<Vector4f> startPoints = new ArrayList<>();
    private List<Vector4f> endPoints = new ArrayList<>();
    private static Sphere sphere = new Sphere(1f);//(2.5f);

    public void addStartPoint(Vector4f pt) {
        startPoints.add(new Vector4f(pt));
        setIsDirty(true);
    }
    
    public void addEndPoint(Vector4f pt) {
        endPoints.add(new Vector4f(pt));
        setIsDirty(true);
    }
    
    public void clearAllROIPoints() {
        startPoints.clear();
        endPoints.clear();
        bShowFilteredFibers = false;
        setIsDirty(true);
    }
    
    public void setClipped(boolean doClip) {
        if (doClip != clipFibers) {
            setIsDirty(true);
        }
        clipFibers = doClip;
    }
    
    public void setClipThickness(float thickness_mm) {
        if (thickness_mm != clipThickness) {
            clipThickness = thickness_mm;
            setIsDirty(true);
        }
    }
    
    public static void main(String[] args) {
        
//        JFileChooser chooser = new JFileChooser();
//        chooser.setDialogTitle("Choose FIB file");
//        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        chooser.setMultiSelectionEnabled(false);
//        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
{
//                File file = chooser.getSelectedFile();
                File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/dti_ecc.src.gz.odf8.f8.csfc.012fy.rdi.gqi.1.25.fib");
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
                    if (entry != null) {
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
                }
                catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
        }
    }
    
    public void initShader() {
        if (fiberTrackShader == null) {
            fiberTrackShader = new ShaderProgram();
            fiberTrackShader.addShader(GL_COMPUTE_SHADER, "shaders/DeterministicFiberTracking.cs.glsl");
            //shader.addShader(GL_COMPUTE_SHADER, "shaders/ImageGradientVolume5x5.cs.glsl"); // seems overly smooth
            fiberTrackShader.compileShaderProgram();  // TODO: should provide check for successful shader compile
        }
        
        if (renderShader == null) {
            renderShader  = new ShaderProgram();
            renderShader.addShader(GL_VERTEX_SHADER, "/org/fusfoundation/kranion/shaders/linesToTubes.vs.glsl");
            renderShader.addShader(GL_GEOMETRY_SHADER, "/org/fusfoundation/kranion/shaders/linesToTubes.gs.glsl");
            renderShader.addShader(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/linesToTubes.fs.glsl");
//            glBindAttribLocation(renderShader.shaderProgramID, 0, "position");
//            glBindAttribLocation(renderShader.shaderProgramID, 1, "color");
            renderShader.compileShaderProgram();
            
        }
    }
    
    public void doFiberTracking() {
        
        // init list of fiber tracking seed points
        /////////////////////////////////////////////
        seedCount = 64000;

        FloatBuffer floatPosBuffer = ByteBuffer.allocateDirect(seedCount*4*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        
        if (vao == 0) {
            vao = glGenVertexArrays();
        }
        glBindVertexArray(vao);
        
        for (int i=0; i<seedCount; i++) {
            Vector4f pos;
            pos = new Vector4f();
            
            pos.x = (float)(nx * rx)/2f;
            pos.y = (float)(ny * ry)/2f;
            pos.z = (float)(nz * rz)/2f;
            pos.w = 1f;
                
            if (i>0) {
                pos.x += (float)((Math.random() - 0.5) * nx * rx / 2f);
                pos.y += (float)((Math.random() - 0.5) * ny * ry / 2f);
                pos.z += (float)((Math.random() - 0.5) * nz * rz / 1.25f);
                pos.w = 1f;
            }
                    
            floatPosBuffer.put(pos.x);
            floatPosBuffer.put(pos.y);
            floatPosBuffer.put(pos.z);
            floatPosBuffer.put(pos.w);            
        }
        
        floatPosBuffer.flip();
                
        seedSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, seedSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        
        // fiber points output buffer for 1K fiber tracking points per seed
        floatPosBuffer = ByteBuffer.allocateDirect(seedCount * 4*4 * 1024).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i< seedCount * 1024; i++) {
            floatPosBuffer.put(0);
            floatPosBuffer.put(0);
            floatPosBuffer.put(0);
            floatPosBuffer.put(1);
        }
        floatPosBuffer.flip();
        
        fiberSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, fiberSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);
        
        // fiber point color output buffer for 1K fiber tracking points per seed
        floatPosBuffer = ByteBuffer.allocateDirect(seedCount * 4*4 * 1024).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i=0; i< seedCount * 1024; i++) {
            floatPosBuffer.put(0);
            floatPosBuffer.put(0);
            floatPosBuffer.put(0);
            floatPosBuffer.put(1);
        }
        floatPosBuffer.flip();
        
        fiberColorsSSBo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, fiberColorsSSBo);
        glBufferData(GL_ARRAY_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        
        GL20.glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
        
        glBindVertexArray(0);
        
        
        IntBuffer intPosBuffer = ByteBuffer.allocateDirect(seedCount*2*4).order(ByteOrder.nativeOrder()).asIntBuffer();
        for (int i=0; i< seedCount*2; i++) {
            intPosBuffer.put(0);
        }
        intPosBuffer.flip();
        
        fiberCountsSSB0 = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, fiberCountsSSB0);
        glBufferData(GL_ARRAY_BUFFER, intPosBuffer, GL_STATIC_DRAW);        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        fiberFilteredCountsSSB0 = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, fiberFilteredCountsSSB0);
        glBufferData(GL_ARRAY_BUFFER, intPosBuffer, GL_STATIC_DRAW);        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
//        intPosBuffer = ByteBuffer.allocateDirect(seedCount*4).order(ByteOrder.nativeOrder()).asIntBuffer();
//        for (int i=0; i< seedCount; i++) {
//            intPosBuffer.put(0);
//        }
//        intPosBuffer.flip();
//        
//        fiberStartsSSB0 = glGenBuffers();
//        glBindBuffer(GL_ARRAY_BUFFER, fiberStartsSSB0);
//        glBufferData(GL_ARRAY_BUFFER, intPosBuffer, GL_STATIC_DRAW);        
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
//        
       
        
        int shaderID = fiberTrackShader.getShaderProgramID();
        fiberTrackShader.start();
        

        // Pass texture info
        setupImageTexture1D(odfVertsTexName, 0);
        int texLoc = glGetUniformLocation(shaderID, "odfVerts");
        glUniform1i(texLoc, 0);
        
        for (int i=0; i<8; i++) {
            setupImageTexture3D(this.faTexName[i], 1+i);            
            texLoc = glGetUniformLocation(shaderID, "faValue["+i+"]");
            glUniform1i(texLoc, 1+i);
            
            setupImageTexture3D(this.indexTexName[i], 9+i);            
            texLoc = glGetUniformLocation(shaderID, "fdIndex["+i+"]");
            glUniform1i(texLoc, 9+i);
        }

        int uloc = glGetUniformLocation(shaderID, "seedCount");
        glUniform1i(uloc, seedCount);
        
        uloc = glGetUniformLocation(shaderID, "odfVertsSize");
        glUniform1i(uloc, odfVertsSize);

        uloc = glGetUniformLocation(shaderID, "voxelSize");
        glUniform3f(uloc, rx, ry, rz);
        
        uloc = glGetUniformLocation(shaderID, "voxelDim");
        glUniform3f(uloc, nx, ny, nz);

        // Bind our output buffer
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outSSBo);
        // run compute shader
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, seedSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, fiberSSBo);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, fiberCountsSSB0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, fiberColorsSSBo);
        
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        org.lwjgl.opengl.GL43.glDispatchCompute(seedCount/64, 1, 1);
        org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 3, 0);

        // Clean up
        //glUseProgram(0);
        fiberTrackShader.stop();

        // clean up texture units
        for (int i=0; i<17; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_3D, 0);
        }
        glDisable(GL_TEXTURE_3D);
        
//        glBindBuffer(GL_ARRAY_BUFFER, fiberSSBo);
//        ByteBuffer distances = glMapBuffer(GL_ARRAY_BUFFER,GL_READ_WRITE, null);
//        FloatBuffer floatDistances = distances.asFloatBuffer();
//        for (int i=0; i<1024; i++) {
//            Vector4f v = new Vector4f(floatDistances.get(), floatDistances.get(), floatDistances.get(), floatDistances.get());
//            System.out.println(v);
//        }
//        
//        glUnmapBuffer(GL_ARRAY_BUFFER);
//        
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        setIsDirty(true);
        
   }

    public void loadTextures() {
        try {
            FibImageLoader.mat4header entry = getEntryHeader("dimension");
            System.out.println("Dimensions = " + entry.rows + " x " + entry.cols);
            ShortBuffer sbuf = getEntryData("dimension").asShortBuffer();

            System.out.println(nx = sbuf.get());
            System.out.println(ny = sbuf.get());
            System.out.println(nz = sbuf.get());

            entry = getEntryHeader("voxel_size");
            System.out.println("Voxel size = " + entry.rows + " x " + entry.cols);
            FloatBuffer fbuf = getEntryData("voxel_size").asFloatBuffer();

            System.out.println(rx = fbuf.get());
            System.out.println(ry = fbuf.get());
            System.out.println(rz = fbuf.get());

            // load odf direction lut
            /////////////////////////////
            entry = getEntryHeader("odf_vertices");
            System.out.println("odf_vertices = " + entry.rows + " x " + entry.cols);
            fbuf = getEntryData("odf_vertices").asFloatBuffer();

            // need a direct buffer for opengl, so make a copy
            FloatBuffer dirFloatBuf = BufferUtils.createFloatBuffer(entry.cols * 4);
            for (int i=0; i<entry.cols; i++) {
                float dx = fbuf.get();
                float dy = fbuf.get();
                float dz = fbuf.get();
                
                dirFloatBuf.put(dx);
                dirFloatBuf.put(dy);
                dirFloatBuf.put(dz);
                dirFloatBuf.put(1);
                
                System.out.println(dx + ", " + dy + ", " + dz);
            }
            dirFloatBuf.flip();

            // store as a 1D RGB opengl texture
            odfVertsSize = entry.cols;
            odfVertsTexName = buildLutTexture(odfVertsSize, dirFloatBuf);
            System.out.println("odfVertsTexName = " + odfVertsTexName);

            // load fiber indexed direction volumes
            //////////////////////////////////////////
            indexTexName = new int[8];

            for (int i = 0; i < indexTexName.length; i++) {
                entry = getEntryHeader("index" + i);
                sbuf = getEntryData("index" + i).asShortBuffer();
                indexTexName[i] = buildIndexTexture(nx, ny, nz, sbuf);
                System.out.println("index" + i + "TexName = " + indexTexName[i]);
            }

            // load FA volumes
            //////////////////////////////////////////
            faTexName = new int[8];

            for (int i = 0; i < faTexName.length; i++) {
                entry = getEntryHeader("fa" + i);
                fbuf = getEntryData("fa" + i).asFloatBuffer();
                faTexName[i] = buildFATexture(nx, ny, nz, fbuf);
                System.out.println("fa" + i + "TexName = " + faTexName[i]);
            }

        } catch (Exception ex) {
            Logger.getLogger(FibImageLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void setupImageTexture3D(int textureName, int textureUnit) {
        if (textureName == 0) {
            return;
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);

        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_3D);
        glBindTexture(GL_TEXTURE_3D, textureName);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);


        // Build transformation of Texture matrix
        ///////////////////////////////////////////
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);

        Main.glPopAttrib();
    }
    
    private void setupImageTexture1D(int textureName, int textureUnit) {
        if (textureName == 0) {
            return;
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT);

        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_1D);
        glBindTexture(GL_TEXTURE_1D, textureName);
        glTexParameterf(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_1D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);


        // Build transformation of Texture matrix
        ///////////////////////////////////////////
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);

        Main.glPopAttrib();
    }
            
    public int buildIndexTexture(int x, int y, int z, ShortBuffer pixelBuf) {
        int indexTextureName = 0;
        
        if (indexTextureName == 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(4);
            IntBuffer texName = buf.asIntBuffer();

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenTextures(texName);
            indexTextureName = texName.get(0);
        }
        
        glBindTexture(GL_TEXTURE_3D, indexTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        
        FloatBuffer fbuf = BufferUtils.createFloatBuffer(pixelBuf.capacity());
        
        int count=0;
        int mark = x*y*z/2 + x/2*y;
        while(pixelBuf.hasRemaining()) {
            short v =  pixelBuf.get();
            fbuf.put(v);
            if(count>mark && count<mark+256) {
                System.out.println("index val = " + v);
                count++;
            }
            count++;
        }
        fbuf.flip();
        
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R32F, x, y, z, 0, GL_RED, GL_FLOAT, fbuf);

        glBindTexture(GL_TEXTURE_3D, 0);
        
        return indexTextureName;
    }
    
    public int buildFATexture(int x, int y, int z, FloatBuffer pixelBuf) {
        int faTextureName = 0;
        
        if (faTextureName == 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(4);
            IntBuffer texName = buf.asIntBuffer();

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenTextures(texName);
            faTextureName = texName.get(0);
        }
        
        glBindTexture(GL_TEXTURE_3D, faTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R32F, x, y, z, 0, GL_RED, GL_FLOAT, pixelBuf);

        glBindTexture(GL_TEXTURE_3D, 0);
        
        return faTextureName;
    }    
    
    public int  buildLutTexture(int size, FloatBuffer pixelBuf) {
        
        int lutTextureName = 0;
        
        if (lutTextureName == 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(4);
            IntBuffer texName = buf.asIntBuffer();

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenTextures(texName);
            lutTextureName = texName.get(0);
        }
        
        glBindTexture(GL_TEXTURE_1D, lutTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        
        glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA16F, size, 0, GL_RGBA, GL_FLOAT, pixelBuf);
        
        glBindTexture(GL_TEXTURE_1D, 0);
        
        return lutTextureName;                    
    }
    
    private File theFile;
    private Map<String, mat4header> keys = new HashMap<>();

    public FibImageLoader() {
        theFile = null;
    }
    
    public FibImageLoader(File file) {
        theFile = file;
    }
    
    public void setFile(File file) {
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
                result = ByteBuffer.allocateDirect(bytecount).order(hd.m == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
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
    
    public void filterFibers() {
            if (fiberSSBo != 0) {

                IntBuffer countBuf = BufferUtils.createIntBuffer(seedCount * 2);

                glBindBuffer(GL_ARRAY_BUFFER, fiberSSBo);
                ByteBuffer bfibers = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_ONLY, null);
                FloatBuffer fibers = bfibers.asFloatBuffer();
                glUnmapBuffer(GL_ARRAY_BUFFER);
                
                glBindBuffer(GL_ARRAY_BUFFER, fiberCountsSSB0);
                ByteBuffer bcounts = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_ONLY, null);
                IntBuffer counts = bcounts.asIntBuffer();
                glUnmapBuffer(GL_ARRAY_BUFFER);
                
                glBindBuffer(GL_ARRAY_BUFFER, fiberFilteredCountsSSB0);
                ByteBuffer bfiltcounts = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_WRITE, null);
                IntBuffer filtcounts = bfiltcounts.asIntBuffer();
                
                float tmp[] = new float[1024*4]; // 512 * vec4
                
                for (int i=0; i<this.seedCount; i++) {
                    boolean keepFiber1 = false;
                    boolean keepFiber2 = false;
                    int origCount1 = counts.get();
                    int origCount2 = counts.get();
                    
                    fibers.get(tmp, 0, 1024*4);
                    
                    for (int j=1; j<origCount1 && keepFiber1==false; j++) {
                        int first = (j-1)*4;
                        int second = (j)*4;
                        Vector3f p1 = new Vector3f(tmp[first], tmp[first+1], tmp[first+2]);
                        Vector3f p2 = new Vector3f(tmp[second], tmp[second+1], tmp[second+2]);
                        
                        Iterator<Vector4f> rois = startPoints.iterator();
                        while (rois.hasNext() && keepFiber1==false) {
                            Vector4f roi = rois.next();
                            if (lineSegmentSphereIntersection(p1, p2, new Vector3f(roi.x, roi.y, roi.z), roi.w)) {
                                keepFiber1 = true;
                            }
                        }
                        rois = endPoints.iterator();
                        while (rois.hasNext() && keepFiber1==false) {
                            Vector4f roi = rois.next();
                            if (lineSegmentSphereIntersection(p1, p2, new Vector3f(roi.x, roi.y, roi.z), roi.w)) {
                                keepFiber1 = true;
                            }
                        }
                   }
                    
                   for (int j=1; j<origCount2 && keepFiber2==false; j++) {
                        int first = (j-1 + 512)*4;
                        int second = (j + 512)*4;
                        Vector3f p1 = new Vector3f(tmp[first], tmp[first+1], tmp[first+2]);
                        Vector3f p2 = new Vector3f(tmp[second], tmp[second+1], tmp[second+2]);
                        
                        Iterator<Vector4f> rois = startPoints.iterator();
                        while (rois.hasNext() && keepFiber2==false) {
                            Vector4f roi = rois.next();
                            if (lineSegmentSphereIntersection(p1, p2, new Vector3f(roi.x, roi.y, roi.z), roi.w)) {
                                keepFiber2 = true;
                            }
                        }
                        rois = endPoints.iterator();
                        while (rois.hasNext() && keepFiber2==false) {
                            Vector4f roi = rois.next();
                            if (lineSegmentSphereIntersection(p1, p2, new Vector3f(roi.x, roi.y, roi.z), roi.w)) {
                                keepFiber2 = true;
                            }
                        }                        
                   }
                   
                   if (keepFiber1 || keepFiber2) {
                       filtcounts.put(origCount1);
                       filtcounts.put(origCount2);
                   }
                   else {
                       filtcounts.put(0);
                       filtcounts.put(0);
                   }
                    
                }
                
                
                glUnmapBuffer(GL_ARRAY_BUFFER);
            }
            
            bShowFilteredFibers = true;
            setIsDirty(true);
    }
    
    public void trackWithImage(ImageVolume4D image) {
        trackedImage = image;
        image.addObserver(this);
    }
    
    @Override
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);

        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT);
        
            if (fiberSSBo != 0) {

                IntBuffer countBuf = BufferUtils.createIntBuffer(seedCount * 2);

                if (bShowFilteredFibers) {
                    glBindBuffer(GL_ARRAY_BUFFER, fiberFilteredCountsSSB0);
                }
                else {
                    glBindBuffer(GL_ARRAY_BUFFER, fiberCountsSSB0);
                }
                
                ByteBuffer bcounts = glMapBuffer(GL_ARRAY_BUFFER, GL_READ_ONLY, null);
                IntBuffer counts = bcounts.asIntBuffer();
                glUnmapBuffer(GL_ARRAY_BUFFER);

                IntBuffer firstBuf = BufferUtils.createIntBuffer(seedCount * 2);

                for (int i = 0; i < seedCount * 2; i++) {
                    int val = counts.get();

                    firstBuf.put(i * 512);
                    countBuf.put(val);
    //            System.out.println("fiber count " + i + " = " + (int)val);
                }
                firstBuf.flip();
                countBuf.flip();

                glMatrixMode(GL_MODELVIEW);
                Main.glPushMatrix();

    //            glDisable(GL_LIGHTING);
                if (clipFibers) {
                    glEnable(GL_CLIP_DISTANCE0);
                    glEnable(GL_CLIP_DISTANCE1);
                    glEnable(GL_CLIP_PLANE0);
                    glEnable(GL_CLIP_PLANE1);
                } else {
                    glDisable(GL_CLIP_DISTANCE0);
                    glDisable(GL_CLIP_DISTANCE1);
                    glDisable(GL_CLIP_PLANE0);
                    glDisable(GL_CLIP_PLANE1);
                }

                glDisable(GL_BLEND);
                glDisable(GL_CULL_FACE);
                glLineWidth(2);

                //        glTranslatef(-120, -120, -80); // Fix later with image based transformation, should track with src image volume
                glTranslatef(-currentTarget.x - currentSteering.x,
                        -currentTarget.y - currentSteering.y,
                        -currentTarget.z - currentSteering.z);

                // sync transformation with tracked image volume (fa0 for now)
                glTranslatef(-renderTranslation.x, -renderTranslation.y, -renderTranslation.z);

                FloatBuffer matBuf = BufferUtils.createFloatBuffer(16);
                Trackball.toMatrix4f(renderOrientation.negate(null)).store(matBuf);
                matBuf.flip();
                glMultMatrix(matBuf);

    //            glBindBuffer(GL_ARRAY_BUFFER, this.fiberSSBo);
    //            glEnableClientState(GL_VERTEX_ARRAY);
    //            glVertexPointer(4, GL_FLOAT, 0, 0);
    //            
    //            glBindBuffer(GL_ARRAY_BUFFER, this.fiberColorsSSBo);
    //            glEnableClientState(GL_COLOR_ARRAY);
    //            glColorPointer(4, GL_FLOAT, 0, 0);
                renderShader.start();

                glBindVertexArray(vao);
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);

                FloatBuffer tempFloatBuf = BufferUtils.createFloatBuffer(16);

                glGetFloat(GL_MODELVIEW_MATRIX, tempFloatBuf);
                int uLoc = glGetUniformLocation(renderShader.shaderProgramID, "model");
                tempFloatBuf.rewind();
    //            while(tempFloatBuf.hasRemaining()) {
    //                System.out.println("modelmat " + tempFloatBuf.get());
    //            }
    //            tempFloatBuf.rewind();
                GL20.glUniformMatrix4(uLoc, false, tempFloatBuf);

                tempFloatBuf.rewind();
                Matrix4f normMat = new Matrix4f();
                normMat.load(tempFloatBuf);
                normMat = (Matrix4f) normMat.invert();
                normMat = normMat.transpose(null);
                tempFloatBuf.rewind();
                normMat.store(tempFloatBuf);
                tempFloatBuf.rewind();
                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "normal");
                GL20.glUniformMatrix4(uLoc, false, tempFloatBuf);

                glGetFloat(GL_PROJECTION_MATRIX, tempFloatBuf);
                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "projection");
                tempFloatBuf.rewind();
    //            while(tempFloatBuf.hasRemaining()) {
    //                System.out.println("projmat " + tempFloatBuf.get());
    //            }
    //            tempFloatBuf.rewind();
                GL20.glUniformMatrix4(uLoc, false, tempFloatBuf);

                Matrix4f ident = new Matrix4f();
                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "view");
                ident.store(tempFloatBuf);
                tempFloatBuf.rewind();
    //            while(tempFloatBuf.hasRemaining()) {
    //                System.out.println("viewmat " + tempFloatBuf.get());
    //            }
    //            tempFloatBuf.rewind();
                GL20.glUniformMatrix4(uLoc, false, tempFloatBuf);

                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "lightPos");
                glUniform3f(uLoc, 0, 0, -600);

                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "viewPos");
                glUniform3f(uLoc, 0, 0, -600);

                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "currentTarget");
                glUniform3f(uLoc,
                        currentTarget.x + currentSteering.x + renderTranslation.x,
                        currentTarget.y + currentSteering.y + renderTranslation.y,
                        currentTarget.z + currentSteering.z + renderTranslation.z);

                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "orientation");
                Trackball.toMatrix4f(renderOrientation).store(tempFloatBuf);
                tempFloatBuf.rewind();
                GL20.glUniformMatrix4(uLoc, false, tempFloatBuf);

                uLoc = glGetUniformLocation(renderShader.shaderProgramID, "clipThickness");
                glUniform1f(uLoc, clipThickness);

                glMultiDrawArrays(GL_LINE_STRIP, firstBuf, countBuf);
                renderShader.stop();

                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glBindVertexArray(0);

    //            glDisableClientState(GL_VERTEX_ARRAY);
    //            glDisableClientState(GL_COLOR_ARRAY);
    //
    //            glBindBuffer(GL_ARRAY_BUFFER, 0);
                Main.glPopMatrix();
            }
                
            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
            
            glEnable(GL_BLEND);
            
            Iterator<Vector4f> i = startPoints.iterator();
            while(i.hasNext()) {
                Vector4f p = i.next();
                
                glPushMatrix();
                    glTranslatef(p.x-currentTarget.x, p.y-currentTarget.y, p.z-currentTarget.z);
                    sphere.setRadius(p.w);
                    sphere.setColor(0.35f, 0.65f, 0.35f, 0.7f);
                    sphere.render();
                glPopMatrix();
            }
            
            i = endPoints.iterator();
            while(i.hasNext()) {
                Vector4f p = i.next();
                
                glPushMatrix();
                    glTranslatef(p.x-currentTarget.x, p.y-currentTarget.y, p.z-currentTarget.z);
                    sphere.setRadius(p.w);
                    sphere.setColor(0.65f, 0.35f, 0.35f, 0.7f);
                    sphere.render();
                glPopMatrix();
            }
                
        Main.glPopAttrib();
                
    }

    @Override
    public void release() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Observable o, Object arg) {
        super.update(o, arg);
        
        // all of the following machinery is to keep the transformation of the tracked fibers
        // synchronized with a given image volume, which currently is fa0. That way we can use
        // registration tools on fa0 and the fibers will move with it.
        if (o instanceof ImageVolume4D && o == trackedImage) {
//            System.out.println("FibImageLoader.update - " + arg);
            if (arg != null && arg instanceof PropertyChangeEvent) {

                // If the update is coming from other than the main (OpenGL) thread
                // then queue it for later when it can be handled on main thread
                if (myThread != Thread.currentThread()) {
                    this.updateEventQueue.push(o, arg);
                    return;
                }

                PropertyChangeEvent propEvt = (PropertyChangeEvent) arg;
                
                switch (propEvt.getPropertyName()) {
                    case "Attribute[ImageTranslation]":
                        try {
                            Vector3f translation = (Vector3f)trackedImage.getAttribute("ImageTranslation");
                            if (translation != null) {
                                this.renderTranslation.set(translation);
                                setIsDirty(true);
                            }
                        }
                        catch(NullPointerException e) {
                            
                        }
                        break;
                    case "Attribute[ImageOrientationQ]":
                        try {
                            Quaternion orientation = (Quaternion)trackedImage.getAttribute("ImageOrientationQ");
                            if (orientation != null) {
                                this.renderOrientation.set(orientation);
                                setIsDirty(true);
                            }
                        }
                        catch(NullPointerException e) {
                            
                        }
                        break;
                }
            }
        }
    }
    
    @Override
    public void update(String propertyName, Object newValue) {
    //    System.out.println("FibImageLoader update: " + propertyName);
        
        switch (propertyName) {
            case "currentTargetPoint":
                System.out.println("FIBImageLoader: currentTarget update");
                if (newValue instanceof Vector3f) {                    
                    currentTarget.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
            case "currentTargetSteering":
                System.out.println("FIBImageLoader: currentSteering update");
                if (newValue instanceof Vector3f) {                    
                    currentSteering.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
        }
    }
    
    private boolean lineSegmentSphereIntersection(Vector3f p1, Vector3f p2, Vector3f c, float radius) {
        float d1 = Vector3f.sub(p1, c, null).lengthSquared();
        float d2 = Vector3f.sub(p2, c, null).lengthSquared();
        float r2 = radius*radius;
        
        if ( ((d1<=r2) && (d2<=r2)) || ((d1>r2) && (d2<=r2)) || ((d1<=r2) && (d2>r2)) ) {
            return true;
        }
        else {
            return false;
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
