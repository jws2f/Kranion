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
import java.nio.*;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import org.lwjgl.util.vector.*;


public class PlyFileReader extends Clippable {
    
    /** Creates a new instance of PlyFileReader */
    private int vertsID=0, normsID=0, indexID=0;
    
    private ShaderProgram shader = null;
   
    private String plyfile;
    private int vertcount, facecount;
 
    private FloatBuffer vertsBuffer, normsBuffer;
    private java.nio.IntBuffer indexBuffer;
    
    private Vector4f color = new Vector4f(0.8f, 0.8f, 0.6f, 1.0f);
    private Vector3f objCenter = new Vector3f();

    private float posX = 0f, posY = 0f, posZ = 0f;
    
    private float minX = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    private float minZ = Float.MAX_VALUE;
    private float maxZ = -Float.MAX_VALUE;
    
    public float getMinX() { return minX; }
    public float getMaxX() { return maxX; }
    public float getMinY() { return minY; }
    public float getMaxY() { return maxY; }
    public float getMinZ() { return minZ; }
    public float getMaxZ() { return maxZ; }
    
    private boolean isBinaryFormat = false;
    
    private boolean isVisible = true;

    
    /** Creates a new instance of PlyFileWriter */
    public PlyFileReader(String file) {
        plyfile = file;
    }
    
    public void setShader(ShaderProgram s) {
        shader = s;
    }
    
    public ShaderProgram getShader() { return shader; }
    
    public void setPos(float x, float y, float z) {
        if (posX != x || posY != y || posZ != z) {
            setIsDirty(true);
        }
    	posX = x;
    	posY = y;
        posZ = z;
    }
    
    public void setColor(float r, float g, float b, float a) {
        setIsDirty(true);
        color.x = r;
        color.y = g;
        color.z = b;
        color.w = a;
    }
    
    public float getXpos() { return posX; }
    public float getYpos() { return posY; }
    public float getZpos() { return posZ; }
    
    private String readLine(PushbackInputStream stream) throws IOException {
    	StringBuilder str = new StringBuilder();
        while (true) {
            int c = stream.read();
            if (c < 0) {
                break;
            }
            if (c == '\n') {
                // if possible consume another '\r'
                int peek = stream.read();
                if (peek != '\r' && peek >= 0){
                    stream.unread(peek);
                }
                break;
            }
            if (c == '\r') {
                // if possible consume another '\n'
                int peek = stream.read();
                if (peek != '\n' && peek >= 0){
                    stream.unread(peek);
                }
                break;
            }
            str.append((char) c);
        }
        return str.toString();    
    }
    
    public void readObject() throws FileNotFoundException, IOException {
    	
        InputStream rstm = this.getClass().getResourceAsStream(plyfile);
        PushbackInputStream stream = new PushbackInputStream(rstm);

        readPlyHeader(stream);
        
System.out.println("read ply header");
System.out.println("ply is bin? " + isBinaryFormat);
System.out.println("ply vert count = " + vertcount);

        vertsBuffer = BufferUtils.createFloatBuffer(vertcount * 3);
        normsBuffer = BufferUtils.createFloatBuffer(vertcount * 3);
        indexBuffer = BufferUtils.createIntBuffer(facecount * 3);

        if (!isBinaryFormat) {
            for (int i = 0; i < vertcount; i++) {
                readVertex(stream);
            }
            System.out.println("read vertices");
            for (int i = 0; i < facecount; i++) {
                readFace(stream);
            }
            System.out.println("read faces");
        } else {
            for (int i = 0; i < vertcount; i++) {
                readVertexBin(stream);
            }
            System.out.println("read vertices");
            for (int i = 0; i < facecount; i++) {
                readFaceBin(stream);
            }
            System.out.println("read faces");
        }
        
        objCenter.x /= (float)vertcount;
        objCenter.y /= (float)vertcount;
        objCenter.z /= (float)vertcount;
        
        System.out.println("Center = " + objCenter.x + ", " + objCenter.y + ", " + objCenter.z);
        
	vertsBuffer.flip();
        normsBuffer.flip();
        indexBuffer.flip();

        vertsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertsID);
        glBufferData(GL_ARRAY_BUFFER, vertsBuffer, GL_STATIC_DRAW);

        normsID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, normsID);
        glBufferData(GL_ARRAY_BUFFER, normsBuffer, GL_STATIC_DRAW);

        indexID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
//      Test code to check plyfile object bounding box values
//
//        minX = minY = minZ = Float.MAX_VALUE;
//        maxX = maxY = maxZ = -Float.MAX_VALUE;
//        vertsBuffer.rewind();
//        while (vertsBuffer.hasRemaining()) {
//            float x = vertsBuffer.get();
//            float y = vertsBuffer.get();
//            float z = vertsBuffer.get();
//            if (x < minX) {
//                minX = x;
//            }
//            if (y < minY) {
//                minY = y;
//            }
//            if (z < minZ) {
//                minZ = z;
//            }
//            if (x > maxX) {
//                maxX = x;
//            }
//            if (y > maxY) {
//                maxY = y;
//            }
//            if (z > maxZ) {
//                maxZ = z;
//            }
//        }
//        
//        System.out.println("Skull bounds:");
//        System.out.println("X: " + minX + ", " + maxX);
//        System.out.println("Y: " + minY + ", " + maxY);
//        System.out.println("Z: " + minZ + ", " + maxZ);
    
        indexBuffer = null;
        vertsBuffer = null;
        normsBuffer = null;
        
    }
    
    /*
            fw.write("ply\n");
            fw.write("format ascii 1.0\n");
            fw.write("element vertex " + mesh.getVertices().size()*3 + "\n");
            fw.write("property float x\n");
            fw.write("property float y\n");
            fw.write("property float z\n");
            fw.write("property float nx\n");
            fw.write("property float ny\n");
            fw.write("property float nz\n");
            fw.write("element face " + mesh.size() + "\n");
            fw.write("property list uchar int vertex_indices\n");
            fw.write("end_header\n");
     */
    
    private void readPlyHeader(PushbackInputStream stream) throws IOException {
        String line = "";
        
        do {
            line = readLine(stream);
            System.out.println(line);
            
            if (line.startsWith("element vertex")) {
                vertcount = Integer.parseInt(line.substring("element vertex".length()).trim());
                System.out.println(vertcount + " vertices");
                
            }
            if (line.startsWith("element face")) {
                facecount = Integer.parseInt(line.substring("element face".length()).trim());
                System.out.println(facecount + " faces");
            }
            
            if (line.startsWith("format binary_little_endian")) {
            	isBinaryFormat=true;
            }
            
        } while (line != null && !line.equals("end_header"));
    }
    
    private void readVertex(PushbackInputStream stream) throws IOException {
        String line = readLine(stream);
        StringTokenizer tok = new StringTokenizer(line, " ");
        Vector3f loc = new Vector3f();
        
        loc.x = Float.parseFloat(tok.nextToken());
        vertsBuffer.put(loc.x);
        loc.y = Float.parseFloat(tok.nextToken());
        vertsBuffer.put(loc.y);
        loc.z = Float.parseFloat(tok.nextToken());
        vertsBuffer.put(loc.z);
        
        if (loc.x < minX) minX = loc.x;
        if (loc.x > maxX) maxX = loc.x;
        if (loc.y < minY) minY = loc.y;
        if (loc.y > maxY) maxY = loc.y;
        if (loc.z < minZ) minZ = loc.z;
        if (loc.z > maxZ) maxZ = loc.z;
        
        objCenter.x += loc.x;
        objCenter.y += loc.y;
        objCenter.z += loc.z;

        Vector3f norm = new Vector3f();
        norm.x = Float.parseFloat(tok.nextToken());
        norm.y = Float.parseFloat(tok.nextToken());
        norm.z = Float.parseFloat(tok.nextToken());
        try {
        norm.normalise();
        }
        catch(Exception e) {
        	norm.set(0f, 0f, 1f);
        }
        normsBuffer.put(norm.x);
        normsBuffer.put(norm.y);
        normsBuffer.put(norm.z);
    }
    
    private void readVertexBin(PushbackInputStream stream) throws IOException {
        
    	// read vertex location
    	byte data[] = new byte[12];
        DataInputStream dis = new DataInputStream(stream);
//    	int nread = stream.read(data);
    	dis.readFully(data);
//    	if (nread != 12) {
//            System.out.println("Failed to completely read vert data.");
//            throw new IOException();
//        }
    	     
        Vector3f pos = new Vector3f();
        pos.x = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        pos.y = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        pos.z = ByteBuffer.wrap(data, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        
//        System.out.println(pos);

        vertsBuffer.put(pos.x);
        vertsBuffer.put(pos.y);
        vertsBuffer.put(pos.z);
        
        if (pos.x < minX) minX = pos.x;
        if (pos.x > maxX) maxX = pos.x;
        if (pos.y < minY) minY = pos.y;
        if (pos.y > maxY) maxY = pos.y;
        if (pos.z < minZ) minZ = pos.z;
        if (pos.z > maxZ) maxZ = pos.z;
        
        // read vertex normal. we need to make sure it is unit length

        dis.readFully(data);
//        nread = stream.read(data);
//    	if (nread != 12) throw new IOException();
       
        
        objCenter.x += pos.x;
        objCenter.y += pos.y;
        objCenter.z += pos.z;

        Vector3f norm = new Vector3f();
        norm.x = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        norm.y = ByteBuffer.wrap(data, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        norm.z = ByteBuffer.wrap(data, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        try {
        norm.normalise();
        }
        catch(Exception e) {
        	norm.set(0f, 0f, 1f);
        }
        normsBuffer.put(norm.x);
        normsBuffer.put(norm.y);
        normsBuffer.put(norm.z);
    }
    
    private void readFace(PushbackInputStream stream) throws IOException {           
        String line = readLine(stream);
        StringTokenizer tok = new StringTokenizer(line, " ");
         
        int count = Integer.parseInt(tok.nextToken());
        if (count != 3) {
            System.out.println("Only can deal with triangles!");
            return;
        }
        
        for (int i=0; i<count; i++) {
        	indexBuffer.put(Integer.parseInt(tok.nextToken()));
        }
    }
    
    private void readFaceBin(PushbackInputStream stream) throws IOException {
        int count = stream.read();
        
        if (count != 3) {
            System.out.println("Only can deal with triangles!");
            return;
        }
        
        DataInputStream dis = new DataInputStream(stream);
    	byte data[] = new byte[12];
        
        dis.readFully(data);
//    	int nread = stream.read(data);
//    	if (nread != 12) throw new IOException();
 
        for(int i=0; i<count; i++) {
	    	int index = ByteBuffer.wrap(data, i*4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
	
	        indexBuffer.put(index);
        }
    }
 
    public void render() {
        
        setIsDirty(false);
                
        if (!getVisible() || vertsID == 0 || normsID == 0) return;
        
        if (getClipped()) {
            setClipped(false);
            renderClipped();
            setClipped(true);
            return;
        }
        
        glMatrixMode(GL_MODELVIEW);
    	Main.glPushMatrix();
    	
    	glColor4f(color.x, color.y, color.z, color.w);
        
	FloatBuffer c = BufferUtils.createFloatBuffer(4);
	c.put(new float[] { color.x, color.y, color.z, color.w }).flip();
        glMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, c);
    	
//    		    glShadeModel(GL_SMOOTH);
//    		    glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );

        
    	glEnableClientState(GL_VERTEX_ARRAY);
    	glEnableClientState(GL_NORMAL_ARRAY);

    	glBindBuffer(GL_ARRAY_BUFFER, vertsID);
    	glVertexPointer(3, GL_FLOAT, 0, 0L);
    	
    	glBindBuffer(GL_ARRAY_BUFFER, normsID);
    	glNormalPointer(GL_FLOAT, 0, 0L);
    	
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexID);
        
        if (shader != null) {
            shader.start();
        }
            glDrawElements(GL_TRIANGLES, facecount*3, GL_UNSIGNED_INT, 0L);
            
        if (shader != null) {
            shader.stop();
        }

    	// turn off client state flags
    	glDisableClientState(GL_NORMAL_ARRAY);
    	glDisableClientState(GL_VERTEX_ARRAY);

    	// clean up
    	glBindBuffer(GL_ARRAY_BUFFER, 0);
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        // bounding box
//        if (shader != null) {
//            shader.start();
//        }
//        glBegin(GL_QUADS);
//            glNormal3f(-1f, 0f, 0f);
//            glVertex3f(minX, minY, minZ);
//            glVertex3f(minX, minY, maxZ);
//            glVertex3f(minX, maxY, maxZ);
//            glVertex3f(minX, maxY, minZ);
//            
//            glNormal3f(-1f, 0f, 0f);
//            glVertex3f(maxX, minY, minZ);
//            glVertex3f(maxX, minY, maxZ);
//            glVertex3f(maxX, maxY, maxZ);
//            glVertex3f(maxX, maxY, minZ);
//        glEnd();
//        
//        if (shader != null) {
//            shader.stop();
//        }

    	Main.glPopMatrix();
   }	
    public void release() {
        glDeleteBuffers(vertsID);
        glDeleteBuffers(normsID);
        glDeleteBuffers(indexID);
        
        if (shader != null) {
            shader.release();
            shader = null;
        }
    }
}
