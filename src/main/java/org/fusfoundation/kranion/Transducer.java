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

import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static org.lwjgl.opengl.GL11.*;

//import static org.lwjgl.opengl.GL12.*;
//import static org.lwjgl.opengl.GL13.*;
//import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
//import static org.lwjgl.opengl.GL20.*;
//import static org.lwjgl.util.glu.GLU.*;
//import org.lwjgl.opengl.GL30;

import org.lwjgl.util.vector.*;
import org.lwjgl.BufferUtils;

import java.nio.*;
import java.io.*;
import java.util.*;
import org.lwjgl.util.glu.GLU;
import static org.lwjgl.util.glu.GLU.gluLookAt;

public class Transducer extends Clippable {

    private int txdrElementVertsID=0, txdrElementNormsID=0;
    private int txdrRaysVertsID=0;

    private static ArrayList<Map.Entry<InsightecTxdrGeomReader, Renderable>> transducers;
    private InsightecTxdrGeomReader trans;

    private float transducerXTilt = 0f;
    private float transducerYTilt = 0f;
    
    private boolean showRays = false;
    private boolean clipRays = false;
    private boolean showFocalVolume = false;
    private boolean showSteeringVolume = false;

    // orientation and location, origin is assumed natural focus
    private Vector3f position = new Vector3f();
    private Matrix4f transform = (Matrix4f)new Matrix4f().setIdentity();

    // transducer elements are displayed as discs composed of this many
    // triangles in a triangle fan. Disc radius is proportional to element
    // area.
    private int tesselation = 20;
    
    private static ActionListener listener = null;
    private static RenderList defaultClinicalTransducer = new RenderList();
    
    private org.lwjgl.util.glu.Sphere steering;
    private org.lwjgl.util.glu.Sphere focalSpot;
    
    private boolean showFiducialPositions = false;
    
    private int transducerIndex = -1;

    public Transducer() {
    }
    
    public void allocateNew(String name, int channelCount) {
        trans = new InsightecTxdrGeomReader(channelCount);
        trans.setName(name);
        
        int matchIndex = -1;
        for (int i=0; i<transducers.size(); i++) {
            InsightecTxdrGeomReader t = transducers.get(i).getKey();
            if (t.getName().equals(name)) {
                matchIndex = i;
            }
        }
        
        if (matchIndex != -1) {
            transducers.set(matchIndex, new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(
                    trans,
                    new TransformationAdapter(new RenderList())));
            transducerIndex = matchIndex;
        } else {
            transducers.add(new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(
                    trans,
                    new TransformationAdapter(new RenderList()))
            );
            transducerIndex = transducers.size() - 1;
        }
    }
    
    public void addTransducerBodyModelMesh(PlyFileReader mesh) {
        this.getRenderList().add(mesh);
    }
    
    public void setName(String name) {
        trans.setName(name);
    }
    
    public String getName() {
        if (trans == null) {
            return null;
        }
        else {
            return trans.getName();
        }
    }
    
    public void setTransducerBodyModelTransform(Matrix4f m) {
        if (transducerIndex != -1) {
            Renderable r = transducers.get(transducerIndex).getValue();
            if (r instanceof TransformationAdapter) {
                ((TransformationAdapter)r).setTransform(m);
            }
        }
    }
    
    public Matrix4f getTransducerBodyModelTransform() {
        Matrix4f result = new Matrix4f();
        if (transducerIndex != -1) {
            Renderable r = transducers.get(transducerIndex).getValue();
            if (r instanceof TransformationAdapter) {
                result = ((TransformationAdapter)r).getTransform();
            }
        }
        return result;
    }
    
    public RenderList getRenderList() {
        if (transducerIndex != -1) {
            Renderable r = this.transducers.get(transducerIndex).getValue();
            if (r instanceof RenderList) {
                return (RenderList)r;
            }
            else if (r instanceof TransformationAdapter) {
                return (RenderList)((TransformationAdapter)r).getChild();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    public int getElementCount() {
        if (trans != null) {
            return trans.getChannelCount();
        }
        else {
            return 0;
        }
    }
    
    public void setElementPos(int n, float x, float y, float z) {
        if (trans != null) {
            trans.setChannelPos(n, new Vector3f(x, y, z));
        }
    }
    
    public Vector3f getElementPos(int n) {
        if (trans != null) {
            Vector4f elemPos = trans.getChannel(n);
            return new Vector3f(elemPos.x, elemPos.y, elemPos.z);
        }
        else {
            return null;
        }
    }
    
    public Vector3f getElementNormal(int n) {
        if (trans != null) {
            Vector3f elemNorm = trans.getChannelNormal(n);
            return new Vector3f(elemNorm.x, elemNorm.y, elemNorm.z);
        }
        else {
            return null;
        }
    }
    
    public void setElementNormal(int n, float x, float y, float z) {
        if (trans != null) {
            trans.setChannelNorm(n, new Vector3f(x, y, z));
        }
    }
    
    public float getElementArea(int n) {
        if (trans != null) {
            return trans.getChannel(n).w;
        }
        else {
            return 0;
        }
    }
    
    public void setElementArea(int n, float area) {
        if (trans != null) {
            Vector4f posArea = trans.getChannel(n);
            posArea.w = area;
            trans.setChannel(n, posArea);
        }
    }
        
    public static void setListener(ActionListener l) {
        listener = l;
    }
    
    public void setTransducerDefinitionIndex(int index) {
        if (index==-1) {
            transducerIndex = transducers.size()-1;
        }
        else {
            transducerIndex = index;
        }
        System.out.println("setTransducerDefinitionIndex="+transducerIndex);
        setIsDirty(true);
    }
    
    public int getTransducerDefinitionIndex() {
        return transducerIndex;
    }
    
    private StandardShader shader = new StandardShader();
    
//    public float getTransducerXTilt() { return transducerXTilt; }
//    public void setTransducerXTilt(float tiltDeg) { 
//        setTransducerTilt(tiltDeg, transducerYTilt);
//    }
//    
//    public float getTransducerYTilt() { return transducerYTilt; }
//    public void setTransducerYTilt(float tiltDeg) { 
//        setTransducerTilt(transducerXTilt, tiltDeg);
//    }
    
    public void setTransducerTilt(float xTiltDeg, float yTiltDeg) {
        if (transducerXTilt != xTiltDeg) {
            transducerXTilt = xTiltDeg;
            setIsDirty(true);
        }
        if (transducerYTilt != yTiltDeg) {
            transducerYTilt = yTiltDeg;
            setIsDirty(true);
        }
        
        transform.setIdentity();
        transform.rotate(transducerXTilt/180f*(float)Math.PI, new Vector3f(-1, 0, 0));
        transform.rotate(transducerYTilt/180f*(float)Math.PI, new Vector3f(0, -1, 0));
    
    }
    
    public void setRotation(Matrix3f rotmat) {
        transform.m00 = rotmat.m00;
        transform.m01 = rotmat.m01;
        transform.m02 = rotmat.m02;
        transform.m10 = rotmat.m10;
        transform.m11 = rotmat.m11;
        transform.m12 = rotmat.m12;
        transform.m20 = rotmat.m20;
        transform.m21 = rotmat.m21;
        transform.m22 = rotmat.m22;
        
        setIsDirty(true);        
    }
    
    public void setShowFiducialPositions(boolean bShow) {
        showFiducialPositions = bShow;
        setIsDirty(true);
    }
    
    public void setTransducerTilt(Vector3f xdir, Vector3f ydir) {
        Vector3f zdir = (Vector3f)Vector3f.cross(xdir, ydir, null);
        
        xdir.normalise();
        ydir.normalise();
        zdir.normalise();
        
        transform.m00 = xdir.x;
        transform.m01 = ydir.x;
        transform.m02 = zdir.x;
        transform.m03 = 0.0f;

        // Second row
        transform.m10 = xdir.y;
        transform.m11 = ydir.y;
        transform.m12 = zdir.y;
        transform.m13 = 0.0f;

        // Third row
        transform.m20 = xdir.z;
        transform.m21 = ydir.z;
        transform.m22 = zdir.z;
        transform.m23 = 0.0f;

        // Fourth row
        transform.m30 = 0;
        transform.m31 = 0;
        transform.m32 = 0;
        transform.m33 = 1.0f;


        
        setIsDirty(true);
    }
    
//    private Trackball trackball = null;

    public void release() {
        releaseBuffers();
        defaultClinicalTransducer.release();
    }
    
    private void releaseBuffers() {
        if (txdrElementVertsID != 0) {
            glDeleteBuffers(txdrElementVertsID);
            txdrElementVertsID = 0;
        }
        if (txdrElementNormsID != 0) {
            glDeleteBuffers(txdrElementNormsID);
            txdrElementNormsID = 0;
        }
        if (txdrRaysVertsID != 0) {
            glDeleteBuffers(txdrRaysVertsID);
            txdrRaysVertsID = 0;
        }        
    }

    public static InsightecTxdrGeomReader getTransducerDef(int i) {
        return transducers.get(i).getKey();
    }
    
    public static int getTransducerDefCount() {
        if (transducers == null) {
            buildTransducerList();
        }
        
        return transducers.size();
    }
    
    public Transducer(int index) throws IOException {
        
        if (transducers == null) {
            buildTransducerList();
        }
        
        if (index < 0 || index > transducers.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        
        transducerIndex = index;
        
        PlyFileReader transducerBody = new PlyFileReader("/org/fusfoundation/kranion/meshes/ClinicalTransducerBody.ply");
        transducerBody.setColor(0.1f, 0.1f, 0.1f, 1f);
        transducerBody.setClipColor(0.075f, 0.075f, 0.075f, 1f);
        
        PlyFileReader transducerRing = new PlyFileReader("/org/fusfoundation/kranion/meshes/ClinicalTransducerRing2.ply");
        transducerRing.setColor(0.40f, 0.30f, 0.05f, 1f);
        transducerRing.setClipColor(0.20f, 0.15f, 0.025f, 1f);
        
        PlyFileReader transducerRing2 = new PlyFileReader("/org/fusfoundation/kranion/meshes/ClinicalTransducerRing.ply");
        transducerRing2.setColor(0.1f, 0.1f, 0.1f, 1f);
        transducerRing2.setClipColor(0.075f, 0.075f, 0.075f, 1f);
        
        
        PlyFileReader transducerHousing1 = new PlyFileReader("/org/fusfoundation/kranion/meshes/ClinicalTransducerHousing1.ply");
        transducerHousing1.setColor(0.7f, 0.7f, 0.7f, 1f);
        transducerHousing1.setClipColor(0.5f, 0.5f, 0.5f, 1f);
        
        PlyFileReader transducerHousing2 = new PlyFileReader("/org/fusfoundation/kranion/meshes/ClinicalTransducerHousing2.ply");
        transducerHousing2.setColor(0.5f, 0.5f, 0.5f, 1f);
        transducerHousing2.setClipColor(0.5f, 0.5f, 0.5f, 1f);
        
//        defaultClinicalTransducer.add(housingGrp);
//        defaultClinicalTransducer.add(mountingRing1Grp);
//        defaultClinicalTransducer.add(mountingRing2Grp);
        defaultClinicalTransducer.add(transducerBody);
        defaultClinicalTransducer.add(transducerRing);
        defaultClinicalTransducer.add(transducerRing2);
        defaultClinicalTransducer.add(transducerHousing1);
        defaultClinicalTransducer.add(transducerHousing2);
        
        steering = new org.lwjgl.util.glu.Sphere();
        focalSpot = new org.lwjgl.util.glu.Sphere();

//<<<<<<< Upstream, based on origin/master
        setTransducerTilt(new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0));
        buildElements(transducers.get(index).getKey());
//=======
//        if (transducer_type == INSIGHTEC_650) {
//            trans = new InsightecTxdrGeomReader("TransducerInfo/XdNominalGeometry_7002.ini");
//            steering1 = new Hemisphere(15.0f, 1f);
//            steering2 = new Hemisphere(-15.0f, -1f);
//>>>>>>> a2c7a7e First functional check in. -SDR implemented as a reasonable first pass. -GUI controls are still almost non-existent. -Must restart to load different data.
//
//<<<<<<< Upstream, based on origin/master
    }
    
    public static int addDefaultTransducerDef(String name, InputStream is) {
        int indexToReturn = -1;
        if (transducers == null) {
            transducers = new ArrayList<Map.Entry<InsightecTxdrGeomReader, Renderable>>();            
        }
        try {
            // if entry exists with same name, remove it first
            Map.Entry<InsightecTxdrGeomReader, Renderable> searchResult = null;
            for (int i=0; i<transducers.size(); i++) {
                if (transducers.get(i).getKey().getName().equalsIgnoreCase(name)) {
                    searchResult = transducers.get(i);
                }
            }
            if (searchResult != null) {
                transducers.remove(searchResult);
            }
            
            InsightecTxdrGeomReader txdrGeom = new InsightecTxdrGeomReader(is);
            txdrGeom.setName(name);
            transducers.add(new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(
                    txdrGeom,
                    defaultClinicalTransducer));
        }
        catch(IOException e) {
            System.out.println("Transducer definision load failed: " + name);
            Logger.getGlobal().log(java.util.logging.Level.SEVERE, "Transducer definision load failed: " + name);
        }
        
        if (listener != null) {
            try {
            listener.actionPerformed(new ActionEvent(new Transducer(transducers.size()-1), ActionEvent.ACTION_PERFORMED, "transducerListUpdated"));
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        
        return transducers.size()-1;
    }
    
    private static void buildTransducerList() {
        try {
            transducers = new ArrayList<Map.Entry<InsightecTxdrGeomReader, Renderable>>();
            transducers.add(new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(
                    new InsightecTxdrGeomReader("TransducerInfo/GenericTransducerGeometry.ini"),
                    defaultClinicalTransducer)
            );

            File transducersDir = new File("transducers");
            if (transducersDir.isDirectory()) {
                File files[] = transducersDir.listFiles(new FilenameFilter() {
                    public boolean accept(File directory, String fileName) {
                        return fileName.endsWith(".ini");
                    }
                });
                for (int i = 0; i < files.length; i++) {
                    System.out.println(files[i].getPath());
                    transducers.add(new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(
                            new InsightecTxdrGeomReader(files[i]),
                            defaultClinicalTransducer)
                    );
                }
            } else {
                return;
            }

            if (transducersDir.isDirectory()) {
                File files[] = transducersDir.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory() && !files[i].getName().equalsIgnoreCase("Inactive")) {
                        System.out.println("Custom Transducer Directory: " + files[i].getName());
                        InsightecTxdrGeomReader customGeometry = null;
                        RenderList transducerRenderable = new RenderList();
                        File customFiles[] = files[i].listFiles();
                        for (int j = 0; j < customFiles.length; j++) {
                            if (customFiles[j].getName().equalsIgnoreCase("transducer.xml")) {
                                TransducerDesc desc = TransducerDesc.loadDescriptionXML(customFiles[j]);
                                if (desc != null) {
                                    customGeometry = new InsightecTxdrGeomReader(new File(customFiles[j].getParent() + File.separator + desc.getElementDescFilename()));
                                    if (customGeometry != null) {
                                        customGeometry.setName(files[i].getName());
                                    }
                                    Iterator<TransducerDesc.Mesh> mi = desc.getMeshIterator();
                                    while (mi.hasNext()) {
                                        TransducerDesc.Mesh m = mi.next();
                                        PlyFileReader r = new PlyFileReader(customFiles[j].getParent() + File.separator + m.getFilename());
                                        if (r != null) {
                                            Vector4f color = m.getColor();
                                            Vector4f clipColor = m.getClipColor();
                                            r.setColor(color.x, color.y, color.z, color.w);
                                            r.setClipColor(clipColor.x, clipColor.y, clipColor.z, clipColor.w);
                                            transducerRenderable.add(r);
                                        }
                                    }

                                    TransformationAdapter tdxrTransform = new TransformationAdapter(transducerRenderable);
                                    tdxrTransform.setTransform(desc.getTransform());

                                    transducers.add(new AbstractMap.SimpleImmutableEntry<InsightecTxdrGeomReader, Renderable>(customGeometry, tdxrTransform));
                                }

                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void buildElements(int index) {
        buildElements(transducers.get(index).getKey());
    }
    
    public Transducer buildElements(InsightecTxdrGeomReader txdr) {
        this.trans = txdr;
        
        FloatBuffer txdrElementVertsBuffer, txdrElementNormsBuffer, txdrRaysBuffer;

        txdrElementVertsBuffer = BufferUtils.createFloatBuffer((tesselation + 2) * 3 * trans.getChannelCount());
        txdrElementNormsBuffer = BufferUtils.createFloatBuffer((tesselation + 2) * 3 * trans.getChannelCount());
        txdrRaysBuffer = BufferUtils.createFloatBuffer(trans.getChannelCount() * 3 * 2);

        for (int e = 0; e < trans.getChannelCount(); e++) {
//System.out.println("Transducer.buildElements{" + txdr.getName() + ": " + e);
            Vector3f elementCenter = new Vector3f(trans.getChannel(e));

            Vector3f elementNorm = new Vector3f(trans.getChannel(e).x, trans.getChannel(e).y, trans.getChannel(e).z);
            elementNorm.normalise();
            
            // move the element visual position out 2mm along the normal so it isnt clipped by
            // tranducer itself. Assumes the the element center positions are coincident on the transdcuer outer surface
            elementCenter = Vector3f.add(elementCenter, (Vector3f)new Vector3f(elementNorm).scale(-2f), null);
            txdrElementVertsBuffer.put(elementCenter.x);
            txdrElementVertsBuffer.put(elementCenter.y);
            txdrElementVertsBuffer.put(elementCenter.z);

            // Calculate x and y unit vector perpendicular to the element facing direction
            Vector3f xvec = new Vector3f(), yvec = new Vector3f();
            Vector3f upvec = new Vector3f(1.0f, 1.0f, 1.0f); // hack

            Vector3f.cross(elementNorm, upvec, xvec);
            Vector3f.cross(xvec, elementNorm, yvec);
            xvec.normalise();
            yvec.normalise();
            
            // For first vertex normal in the fan 
            elementNorm.scale(-1f);
            txdrElementNormsBuffer.put(elementNorm.x);
            txdrElementNormsBuffer.put(elementNorm.y);
            txdrElementNormsBuffer.put(elementNorm.z);


            // element displayed with radius proportional to given element area
            float radius = (float) Math.sqrt(trans.getChannel(e).w / Math.PI) * 0.75f;

            for (int i = 0; i < tesselation + 1; i++) {
                float dx = -radius * (float) Math.cos(-(double) ((double) (i - 1) / (float) (tesselation) * 2.0 * Math.PI));
                float dy = radius * (float) Math.sin(-(double) ((double) (i - 1) / (float) (tesselation) * 2.0 * Math.PI));

                float vx = elementCenter.x + dx * xvec.x + dy * yvec.x;
                float vy = elementCenter.y + dx * xvec.y + dy * yvec.y;
                float vz = elementCenter.z + dx * xvec.z + dy * yvec.z;

                txdrElementVertsBuffer.put(vx);
                txdrElementVertsBuffer.put(vy);
                txdrElementVertsBuffer.put(vz);

                Vector3f vnorm = new Vector3f(vx, vy, (vz));
                vnorm.scale(-1);
                vnorm.normalise();

                // For each vert normal in the fan 
                txdrElementNormsBuffer.put(vnorm.x);
                txdrElementNormsBuffer.put(vnorm.y);
                txdrElementNormsBuffer.put(vnorm.z);
            }
        }

        for (int e = 0; e < trans.getChannelCount(); e++) {

            Vector3f elementCenter = new Vector3f(trans.getChannel(e));

            txdrRaysBuffer.put(elementCenter.x);
            txdrRaysBuffer.put(elementCenter.y);
            txdrRaysBuffer.put(elementCenter.z);

            // This is a hack to not draw inactive channel rays.
            // If inactive, loading identical start and end points for the line
            // so nothing gets drawn. Should do this better eventually.
            if (trans.getChannelActive(e) == true) {
                Vector3f rayTip = new Vector3f();
                Vector3f center = new Vector3f(0f, 0f, 0f);
                rayTip = (Vector3f)Vector3f.add(elementCenter, (Vector3f)Vector3f.sub(center, elementCenter, null).scale(0.96f), null);
                
                txdrRaysBuffer.put(rayTip.x);
                txdrRaysBuffer.put(rayTip.y);
                txdrRaysBuffer.put(rayTip.z);
            } else {
                txdrRaysBuffer.put(elementCenter.x);
                txdrRaysBuffer.put(elementCenter.y);
                txdrRaysBuffer.put(elementCenter.z);
            }
        }

        releaseBuffers();
        
        txdrElementVertsID = glGenBuffers();
        txdrElementVertsBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, txdrElementVertsID);
        glBufferData(GL_ARRAY_BUFFER, txdrElementVertsBuffer, GL_STATIC_DRAW);

        txdrElementNormsID = glGenBuffers();
        txdrElementNormsBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, txdrElementNormsID);
        glBufferData(GL_ARRAY_BUFFER, txdrElementNormsBuffer, GL_STATIC_DRAW);

        txdrRaysVertsID = glGenBuffers();
        txdrRaysBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, txdrRaysVertsID);
        glBufferData(GL_ARRAY_BUFFER, txdrRaysBuffer, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        return this;
    }

    @Override
    public void setTrackball(Trackball tb)
    {
        super.setTrackball(tb);
//        housingGrp.setTrackball(tb);
//        mountingRing1Grp.setTrackball(tb);
//        mountingRing2Grp.setTrackball(tb);
        defaultClinicalTransducer.setTrackball(tb);
        
        if (transducerIndex != -1) {
            Renderable r = transducers.get(transducerIndex).getValue();
            if (r instanceof Clippable) {
                ((Clippable) r).setTrackball(tb);
            }
        }
    }
    
    @Override
    public void setDolly(float cameraz, float dolly)
    {
        super.setDolly(cameraz, dolly);
        defaultClinicalTransducer.setDolly(cameraz, dolly);
        
        if (transducerIndex != -1) {
            Renderable r = transducers.get(transducerIndex).getValue();
            if (r instanceof Clippable) {
                ((Clippable) r).setDolly(cameraz, dolly);
            }
        }
    }
    
    public void setPosition(float x, float y, float z) {
        setPosition(new Vector3f(x, y, z));
    }

    public void setPosition(Vector3f p) {
        if (!position.equals(p)) {
            setIsDirty(true);
        }
        position.set(p);
        transform.m03 = p.x;
        transform.m13 = p.y;
        transform.m23 = p.z;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Matrix4f getTransform() {
        return new Matrix4f(transform);
    }

    public void setShowRays(Boolean show) {
        if (showRays != show) {
            setIsDirty(true);
        }
        showRays = show;
    }

    public boolean getShowRays() {
        return showRays;
    }

    public void setClipRays(Boolean bClip) {
        if (clipRays != bClip) {
            setIsDirty(true);
        }
        clipRays = bClip;
    }

    public boolean getClipRays() {
        return clipRays;
    }
    
    public boolean getShowFocalVolume() { return this.showFocalVolume; }
    public void setShowFocalVolume(boolean bShow) {
        if (bShow != showFocalVolume) {
            setIsDirty(true);
        }
        this.showFocalVolume = bShow; 
    }
    public boolean getShowSteeringVolume() { return this.showSteeringVolume; }
    public void setShowSteeringVolume(boolean bShow) {
        if (bShow != showSteeringVolume) {
            setIsDirty(true);
        }
        this.showSteeringVolume = bShow;
    }

//    public void renderclip() {
//        glPushAttrib(GL_ENABLE_BIT);
//        
//        glEnable(GL_CULL_FACE);
//        
//        glCullFace(GL_BACK);
//        renderPass(); ////////////////////
//        
//        
//        // Turn stencil buffer on
//        glClear(GL_STENCIL_BUFFER_BIT);
//        glEnable(GL_STENCIL_TEST);
//        glStencilMask(0xff);
//        
//        glStencilFunc(GL_ALWAYS, 1, 0xff);        
//        glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);        
//        glCullFace(GL_FRONT);
//        renderPass(); /////////////////////
//        
//        glDisable(GL_CULL_FACE);
//        
//        glStencilFunc(GL_NOTEQUAL, 0, 0xff);
//        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
//        
//        
//        //End cap
//        glDisable(GL_CLIP_PLANE0);
//        glDisable(GL_CLIP_PLANE1);
//        glDisable(GL_DEPTH_TEST);
//        glColor4f(0.4f, 0.4f, 0.4f, 1f);
//        
//        glMatrixMode(GL_MODELVIEW);
//        glPushMatrix();
//        
//        glLoadIdentity();
//        
//        // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
//        gluLookAt(0.0f, 0.0f, -600.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
//        // camera dolly in/out
//        glTranslatef(0.0f, 0.0f, 400);
//
//        if (trackball != null) {
//            //glTranslatef(0f, 0f, 150f);
//            //trackball.renderOpposite();
//        }
//        
//        glBegin(GL_QUADS);
//
//            glNormal3d(0.0, 0.0, -1.0);
//
//            glVertex3d(-400, 400, 0.0);
//            glVertex3d(400, 400, 0.0);
//            glVertex3d(400, -400, 0.0);
//            glVertex3d(-400, -400, 0.0);
//        glEnd();
//        
//        glPopMatrix();
//        
//        glDisable(GL_STENCIL_TEST);
//        
//        glPopAttrib();
//    }
    
    
    private void renderElements() {
       //
        //
        //
        //Draw element fans
        /////
        glColor3f(0.0f, 0.6f, 0.6f);
        shader.setDiffusetColor(0.0f, 0.6f, 0.6f, 1f);
        shader.setAmbientColor(0.0f, 0.1f, 0.1f, 1f);
        shader.setSpecularCoefficient(50f);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);

        glBindBuffer(GL_ARRAY_BUFFER, txdrElementVertsID);
        glVertexPointer(3, GL_FLOAT, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, txdrElementNormsID);
        glNormalPointer(GL_FLOAT, 0, 0);

        shader.start();
        for (int i = 0; i < trans.getChannelCount(); i++) {
            if (trans.getChannelActive(i) == true) {
                glDrawArrays(GL_TRIANGLE_FAN, i * (tesselation + 2), (tesselation + 2));
            }
        }
        shader.stop();
        /////
        //
        //
        //

        // clean up
        glBindBuffer(GL_ARRAY_BUFFER, 0);    
    }
    
    public void renderRays() {
        if (showRays) {
            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
                glTranslatef(position.x, position.y, position.z);

                Main.glPushAttrib(GL_ENABLE_BIT);

                    if (clipRays) {
                        glEnable(GL_CLIP_PLANE0);
                        glEnable(GL_CLIP_PLANE1);
                    }

                    // Render element rays
                    glDisable(GL_LIGHTING);
                    glEnable(GL_BLEND);
                    glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glColor4f(1f, 0.3f, 0.0f, 0.5f);
                    glDisableClientState(GL_NORMAL_ARRAY);
                    glEnableClientState(GL_VERTEX_ARRAY);
                    glBindBuffer(GL_ARRAY_BUFFER, txdrRaysVertsID);
                    glVertexPointer(3, GL_FLOAT, 0, 0);
                    glDrawArrays(GL_LINES, 0, trans.getChannelCount() * 2);
                    glEnable(GL_LIGHTING);
                    glBindBuffer(GL_ARRAY_BUFFER, 0);

                Main.glPopAttrib();
            Main.glPopMatrix();
        }
    }
    
    @Override
    public void setClipped(boolean clipped) {
        super.setClipped(clipped);
//        housingGrp.setClipped(clipped);
//        mountingRing1Grp.setClipped(clipped);
//        mountingRing2Grp.setClipped(clipped);
        for (int i=0; i<transducers.size(); i++) {
            Renderable r = transducers.get(this.transducerIndex).getValue();
            if (r instanceof Clippable) {
                ((Clippable)r).setClipped(clipped);
            }
        }        
    }
        
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);
        
//        if (getClipped()) {
//            setClipped(false);
//            renderClipped();
//            setClipped(true);
//            return;
//        }

        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();

 //           glTranslatef(0, 0, 150);
//            glRotatef(transducerTilt, 1, 0, 0);
//            glTranslatef(0, 0, -150);


                FloatBuffer matBuf = BufferUtils.createFloatBuffer(16);
		transform.store(matBuf);
		matBuf.flip();
		glMultMatrix(matBuf);

//            Translation is stored in the Matrix4f now, so dont need:                
//            glTranslatef(position.x, position.y, position.z);

            if (getClipped()) {
                glEnable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);
                renderElements();
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);
            }
            else {
                renderElements();
            }

            this.transducers.get(transducerIndex).getValue().render();
//            housingGrp.render();
//            mountingRing1Grp.render();
//            mountingRing2Grp.render();
            
            //TODO: remove, just debugging some registration code
            if (showFiducialPositions) {
                Main.glPushAttrib(GL_ENABLE_BIT);
                glDisable(GL_LIGHTING);
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);
                glColor3f(0f, 1f, 0f);
                glBegin(GL_LINES);
                    glVertex3f(-80f, -80f,  3.37f);
                    glVertex3f(80f, -80f,  3.37f);

                    glVertex3f(80f, -80f,  3.37f);
                    glVertex3f(80f, 80f,  3.37f);

                    glVertex3f(80f, 80f,  3.37f);
                    glVertex3f(-80f, 80f,  3.37f);

                    glVertex3f(-80f, 80f,  3.37f);
                    glVertex3f(-80f, -80f,  3.37f);
                glEnd();
                Main.glPopAttrib();
            }

    //        renderRays();
                 
        Main.glPopMatrix();
    }
    
    public void renderFocalSpot() {
        glMatrixMode(GL_MODELVIEW);
         Main.glPushMatrix();

            glTranslatef(position.x, position.y, position.z);

            Main.glPushAttrib(GL_ENABLE_BIT);

    //            glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glEnable(GL_CULL_FACE);

                glCullFace(GL_FRONT);
                
                if (showSteeringVolume) {
//                    steering1.render();
//                    steering2.render();
                    glColor4f(0f, 1f, 0f, 0.4f);
                    steering.setDrawStyle(GLU.GLU_FILL);
                    steering.setNormals(GLU.GLU_SMOOTH);
                    steering.draw(15f, 32, 16);
                    
                }
                if (showFocalVolume) {
//                    focalSpot1.render();
//                    focalSpot2.render();
                        
                    glColor4f(1f, 0f, 0f, 0.6f);
                    focalSpot.setDrawStyle(GLU.GLU_FILL);
                    focalSpot.setNormals(GLU.GLU_SMOOTH);
    
                    focalSpot.draw(1f, 32, 16);
                }

                glCullFace(GL_BACK);
                
                if (showSteeringVolume) {
//                    steering1.render();
//                    steering2.render();
                    glColor4f(0f, 1f, 0f, 0.4f);
                    steering.setDrawStyle(GLU.GLU_FILL);
                    steering.setNormals(GLU.GLU_SMOOTH);
                    steering.draw(15f, 32, 16);
                }
                if (showFocalVolume) {
//                    focalSpot1.render();
//                    focalSpot2.render();
                    glColor4f(1f, 0f, 0f, 0.6f);
                    focalSpot.setDrawStyle(GLU.GLU_FILL);
                    focalSpot.setNormals(GLU.GLU_SMOOTH);
                    focalSpot.draw(1f, 32, 16);
                }

                glDisable(GL_CULL_FACE);
                


            Main.glPopAttrib();
                
        Main.glPopMatrix();       
    }
    
//    public void renderClipped(Vector4f color) {
//        glPushMatrix();
//
//            glTranslatef(position.x, position.y, position.z);
//
//            renderElements();
//
////            housingGrp.renderClipped(new Vector4f(0.25f, 0.25f, 0.25f, 1f));
////            mountingRing1Grp.renderClipped(new Vector4f(0.05f, 0.05f, 0.05f, 1f));
////            mountingRing2Grp.renderClipped(new Vector4f(0.2f, 0.16f, 0.025f, 1f));
//               
//        glPopMatrix();        
//    }
        
    public Vector4f getElement(int i) {
//        return Matrix4f.transform(this.transducerRot, trans.getChannel(i), null);
        return trans.getChannel(i);
    }
    
    public void setElement(int i, Vector4f posArea) {
        trans.setChannel(i, posArea);
    }
    
    public boolean getElementActive(int i) {
        return trans.getChannelActive(i);
    }

    public void setElementActive(int i, boolean active) {
        trans.setChannelActive(i, active);
    }

}
