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

    private int txdrElementVertsID, txdrElementNormsID;
    private int txdrRaysVertsID;

    private static ArrayList<InsightecTxdrGeomReader> transducers;
    private InsightecTxdrGeomReader trans;

    public static int GENERIC_220 = 0;
    public static int INSIGHTEC_220 = 1;
    public static int INSIGHTEC_650 = 2;

    private float transducerXTilt = 0f;
    private float transducerYTilt = 0f;
    
    private boolean showRays = false;
    private boolean clipRays = false;
    private boolean showFocalVolume = false;
    private boolean showSteeringVolume = false;

    private Vector3f position = new Vector3f();
    private Matrix4f transducerRot = (Matrix4f)new Matrix4f().setIdentity();


    private int tesselation = 20;
    private Hemisphere housing = new Hemisphere(151f, -1f);
    private Hemisphere housing2 = new Hemisphere(165.0f, 1f);
    private Cylinder outerHousing = new Cylinder(170.0f, 110.0f, 0.0f, 1f);
    private Ring housingRing = new Ring(170.0f, 19.0f, 0.0f, -1f);
    private Ring housingRing2 = new Ring(170.0f, 47.0f, 110.0f, 1f);
    
    private RenderList housingGrp = new RenderList();

    private Cylinder mountingRing = new Cylinder(152.0f, 15.0f, -15.0f, -1f);
    private Cylinder mountingRingInner = new Cylinder(162.0f, 15.0f, -15.0f, 1f);
    private Ring mountingRingUpper = new Ring(162.0f, 10.0f, -15.0f, -1f);
    private Ring mountingRingLower = new Ring(162.0f, 10.0f, -0.01f, 1f);
    
    private RenderList mountingRing1Grp = new RenderList();

    private Cylinder mountingRing2 = new Cylinder(176.0f, 25.0f, -25.0f, 1f);
    private Cylinder mountingRingInner2 = new Cylinder(162.05f, 25.0f, -25.0f, -1f);
    private Ring mountingRingUpper2 = new Ring(176.0f, 14.0f, -25.0f, -1f);
    private Ring mountingRingLower2 = new Ring(176.0f, 14.0f, -0.01f, 1f);
    
    private RenderList mountingRing2Grp = new RenderList();
    
    private org.lwjgl.util.glu.Sphere steering;
    private org.lwjgl.util.glu.Sphere focalSpot;
    
    private boolean showFiducialPositions = false;

    public Transducer() {
    }
    
    private StandardShader shader = new StandardShader();
    
    public float getTransducerXTilt() { return transducerXTilt; }
    public void setTransducerXTilt(float tiltDeg) { 
        setTransducerTilt(tiltDeg, transducerYTilt);
    }
    
    public float getTransducerYTilt() { return transducerYTilt; }
    public void setTransducerYTilt(float tiltDeg) { 
        setTransducerTilt(transducerXTilt, tiltDeg);
    }
    
    public void setTransducerTilt(float xTiltDeg, float yTiltDeg) {
        if (transducerXTilt != xTiltDeg) {
            transducerXTilt = xTiltDeg;
            setIsDirty(true);
        }
        if (transducerYTilt != yTiltDeg) {
            transducerYTilt = yTiltDeg;
            setIsDirty(true);
        }
        
        transducerRot.setIdentity();
        transducerRot.rotate(transducerXTilt/180f*(float)Math.PI, new Vector3f(-1, 0, 0));
        transducerRot.rotate(transducerYTilt/180f*(float)Math.PI, new Vector3f(0, 1, 0));
    
    }
    
    public void setShowFiducialPositions(boolean bShow) {
        showFiducialPositions = bShow;
        setIsDirty(true);
    }
    
    public void setTransducerTilt(Vector3f xdir, Vector3f ydir) {
        Vector3f zdir = (Vector3f)Vector3f.cross(xdir, ydir, null);
        
        // TODO: remove or fix for real the following
        //zdir = zdir.negate(null);
               
        xdir.normalise();
        ydir.normalise();
        zdir.normalise();
        
        transducerRot.m00 = xdir.x;
        transducerRot.m01 = ydir.x;
        transducerRot.m02 = zdir.x;
        transducerRot.m03 = 0.0f;

        // Second row
        transducerRot.m10 = xdir.y;
        transducerRot.m11 = ydir.y;
        transducerRot.m12 = zdir.y;
        transducerRot.m13 = 0.0f;

        // Third row
        transducerRot.m20 = xdir.z;
        transducerRot.m21 = ydir.z;
        transducerRot.m22 = zdir.z;
        transducerRot.m23 = 0.0f;

        // Fourth row
        transducerRot.m30 = 0;
        transducerRot.m31 = 0;
        transducerRot.m32 = 0;
        transducerRot.m33 = 1.0f;


        
        setIsDirty(true);
    }
    
//    private Trackball trackball = null;

    public void release() {        
        housingGrp.release();
        mountingRing1Grp.release();
        mountingRing2Grp.release();

    }

    public static InsightecTxdrGeomReader getTransducerDef(int i) {
        return transducers.get(i);
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

        housing.setColor(0.2f, 0.2f, 0.2f);
        housing2.setColor(0.3f, 0.3f, 0.3f);

        outerHousing.setColor(0.7f, 0.7f, 0.7f);
        housingRing.setColor(0.1f, 0.1f, 0.1f);
        housingRing2.setColor(0.7f, 0.7f, 0.7f);
        
        housingGrp.add(housing);
        housingGrp.add(housing2);
        housingGrp.add(housingRing);
        housingGrp.add(housingRing2);
        housingGrp.add(outerHousing);

        mountingRing.setColor(0.1f, 0.1f, 0.1f);
        mountingRingInner.setColor(0.1f, 0.1f, 0.1f);
        mountingRingUpper.setColor(0.1f, 0.1f, 0.1f);
        mountingRingLower.setColor(0.1f, 0.1f, 0.1f);
        
        mountingRing1Grp.add(mountingRing);
        mountingRing1Grp.add(mountingRingInner);
        mountingRing1Grp.add(mountingRingUpper);
        mountingRing1Grp.add(mountingRingLower);

        mountingRing2.setColor(0.4f, 0.35f, 0.05f);
        mountingRingInner2.setColor(0.4f, 0.35f, 0.05f);
        mountingRingUpper2.setColor(0.4f, 0.35f, 0.05f);
        mountingRingLower2.setColor(0.4f, 0.35f, 0.05f);
        
        mountingRing2Grp.add(mountingRing2);
        mountingRing2Grp.add(mountingRingInner2);
        mountingRing2Grp.add(mountingRingUpper2);
        mountingRing2Grp.add(mountingRingLower2);
        
        housingGrp.setClipColor(0.25f, 0.25f, 0.25f, 1f);
        mountingRing1Grp.setClipColor(0.05f, 0.05f, 0.05f, 1f);
        mountingRing2Grp.setClipColor(0.2f, 0.16f, 0.025f, 1f);
        
        steering = new org.lwjgl.util.glu.Sphere();
        focalSpot = new org.lwjgl.util.glu.Sphere();

//<<<<<<< Upstream, based on origin/master
        setTransducerTilt(new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0));
        buildElements(transducers.get(index));
//=======
//        if (transducer_type == INSIGHTEC_650) {
//            trans = new InsightecTxdrGeomReader("TransducerInfo/XdNominalGeometry_7002.ini");
//            steering1 = new Hemisphere(15.0f, 1f);
//            steering2 = new Hemisphere(-15.0f, -1f);
//>>>>>>> a2c7a7e First functional check in. -SDR implemented as a reasonable first pass. -GUI controls are still almost non-existent. -Must restart to load different data.
//
//<<<<<<< Upstream, based on origin/master
    }
    
    private static void buildTransducerList() {
        try {
            transducers = new ArrayList<InsightecTxdrGeomReader>();
            transducers.add(new InsightecTxdrGeomReader("TransducerInfo/GenericTransducerGeometry.ini"));
            
            File transducersDir = new File("transducers");
            if (transducersDir.isDirectory()) {
                File files[] = transducersDir.listFiles(new FilenameFilter() {
                    public boolean accept(File directory, String fileName) {
                        return fileName.endsWith(".ini");
                    }
                });
                for (int i=0; i<files.length; i++) {
                    System.out.println(files[i].getPath());
                    transducers.add(new InsightecTxdrGeomReader(files[i]));
                }
            }
            else {
                return;
            }
//=======
//            focalSpot1 = new Hemisphere(1f, 1f);
//            focalSpot2 = new Hemisphere(-1f, -1f);
//        } else if (transducer_type == INSIGHTEC_220) {
//            trans = new InsightecTxdrGeomReader("TransducerInfo/NominalTransducerGeometry_6002.ini");
//            steering1 = new Hemisphere(25.0f, 1f);
//            steering2 = new Hemisphere(-25.0f, -1f);
//
//            focalSpot1 = new Hemisphere(2.5f, 1f);
//            focalSpot2 = new Hemisphere(-2.5f, -1f);
//        } else {
//            throw new FileNotFoundException();
//>>>>>>> a2c7a7e First functional check in. -SDR implemented as a reasonable first pass. -GUI controls are still almost non-existent. -Must restart to load different data.
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    public void buildElements(int index) {
        buildElements(transducers.get(index));
    }
    
    public Transducer buildElements(InsightecTxdrGeomReader txdr) {
        this.trans = txdr;
        
        FloatBuffer txdrElementVertsBuffer, txdrElementNormsBuffer, txdrRaysBuffer;

        txdrElementVertsBuffer = BufferUtils.createFloatBuffer((tesselation + 2) * 3 * trans.getChannelCount());
        txdrElementNormsBuffer = BufferUtils.createFloatBuffer((tesselation + 2) * 3 * trans.getChannelCount());
        txdrRaysBuffer = BufferUtils.createFloatBuffer(trans.getChannelCount() * 3 * 2);

        for (int e = 0; e < trans.getChannelCount(); e++) {

            Vector3f elementCenter = new Vector3f(trans.getChannel(e));

            txdrElementVertsBuffer.put(elementCenter.x);
            txdrElementVertsBuffer.put(elementCenter.y);
            txdrElementVertsBuffer.put(elementCenter.z);

            Vector3f elementNorm = new Vector3f(trans.getChannel(e).x, trans.getChannel(e).y, trans.getChannel(e).z);
            elementNorm.normalise();

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
        housingGrp.setTrackball(tb);
        mountingRing1Grp.setTrackball(tb);
        mountingRing2Grp.setTrackball(tb);
    }
    
    @Override
    public void setDolly(float c, float d)
    {
        super.setDolly(c, d);
        housingGrp.setDolly(c,d);
        mountingRing1Grp.setDolly(c,d);
        mountingRing2Grp.setDolly(c,d);
    }
    
    public void setPosition(float x, float y, float z) {
        setPosition(new Vector3f(x, y, z));
    }

    public void setPosition(Vector3f p) {
        if (!position.equals(p)) {
            setIsDirty(true);
        }
        position = p;
    }

    public Vector3f getPosition() {
        return position;
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
        housingGrp.setClipped(clipped);
        mountingRing1Grp.setClipped(clipped);
        mountingRing2Grp.setClipped(clipped);
    }
        
    public void render() {
        
        if (!getVisible()) return;
        
        setIsDirty(false);
        
        if (getClipped()) {
            isClipped = false;
            renderClipped();
            isClipped = true;
            return;
        }

        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();

 //           glTranslatef(0, 0, 150);
//            glRotatef(transducerTilt, 1, 0, 0);
//            glTranslatef(0, 0, -150);


                FloatBuffer matBuf = BufferUtils.createFloatBuffer(16);//.newFloatBuffer(16);
		transducerRot.store(matBuf);
		matBuf.flip();
		glMultMatrix(matBuf);
            
            glTranslatef(position.x, position.y, position.z);

            renderElements();


            housingGrp.render();
            mountingRing1Grp.render();
            mountingRing2Grp.render();
            
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
    
    public int getElementCount() { return trans.getChannelCount(); }
    public Vector4f getElement(int i) {
//        return Matrix4f.transform(this.transducerRot, trans.getChannel(i), null);
        return trans.getChannel(i);
    }
    
    public boolean getElementActive(int i) { return trans.getChannelActive(i); }

}
