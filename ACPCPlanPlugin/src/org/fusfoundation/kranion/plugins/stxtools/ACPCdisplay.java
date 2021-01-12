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
package org.fusfoundation.kranion.plugins.stxtools;

import org.fusfoundation.kranion.GUIControl;
import org.fusfoundation.kranion.ImageLandmark;
import org.fusfoundation.kranion.ImageLandmarkConstraint;
import org.fusfoundation.kranion.Main;
import static org.fusfoundation.kranion.Main.glPopAttrib;
import static org.fusfoundation.kranion.Main.glPopMatrix;
import static org.fusfoundation.kranion.Main.glPushAttrib;
import static org.fusfoundation.kranion.Main.glPushMatrix;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.fusfoundation.kranion.view.View;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE1;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_POINT_BIT;
import static org.lwjgl.opengl.GL11.GL_POLYGON_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author jsnell
 */
public class ACPCdisplay extends GUIControl {
    
    private Vector3f ac, pc, sup;
    private Vector3f acpcUpVec = new Vector3f();
    private Vector3f currentTarget = new Vector3f();
    private Vector3f currentSteering = new Vector3f();
    
    private Model model;
    private View view;
    private ImageVolume image;
    private ImageLandmark ac_lm, pc_lm, sup_lm;
        
    private ImageLandmark[] tb_landmarks = new ImageLandmark[6];
    private ImageLandmarkConstraint[] tb_landmark_constr = new ImageLandmarkConstraint[6];
    
    public ACPCdisplay() {
        ac = pc = sup = null;
        this.setVisible(true);
                
        for (int i=0; i<tb_landmarks.length; i++) {
            tb_landmarks[i] = new ImageLandmark(null);
            
            tb_landmark_constr[i] = new ImageLandmarkConstraint();
            tb_landmark_constr[i].setOffset(20.0f);
            tb_landmarks[i].setContraint(tb_landmark_constr[i]);
        }        
    }
    
    public void setModel(Model m, View v) {
        model = m;
        view = v;
        
        if (ac_lm == null) {
            ac_lm = new ImageLandmark(model.getCtImage());
            ac_lm.setColor(1, 0, 0, 1);
            ac_lm.setPropertyPrefix("Model.Attribute");
            ac_lm.setName("AC");
            ac_lm.setCommand("AC");
            ac_lm.setShowOnlyIfSelected(true);
            model.addObserver(ac_lm);
            model.setAttribute("AC", new Vector3f(0, -10, 0));            
        }
        if (pc_lm == null) {
            pc_lm = new ImageLandmark(model.getCtImage());
            pc_lm.setColor(0, 1, 0, 1);
            pc_lm.setPropertyPrefix("Model.Attribute");
            pc_lm.setName("PC");
            pc_lm.setCommand("PC");
            pc_lm.setShowOnlyIfSelected(true);
            model.addObserver(pc_lm);
            model.setAttribute("PC", new Vector3f(0, 10, 0));
        }
        if (sup_lm == null) {
            sup_lm = new ImageLandmark(model.getCtImage());
            sup_lm.setColor(0.3f, 0.3f, 1, 1);
            sup_lm.setPropertyPrefix("Model.Attribute");
            sup_lm.setName("ACPCSup");
            sup_lm.setCommand("ACPCSup");
            sup_lm.setShowOnlyIfSelected(false);
            model.addObserver(sup_lm);
            model.setAttribute("ACPCSup", new Vector3f(0, 0, 20));
        }
                
        for (int i=0; i<tb_landmarks.length; i++) {
            if (tb_landmarks[i] != null) {
                tb_landmarks[i].setPropertyPrefix("Model.Attribute");
                model.addObserver(tb_landmarks[i]);
            }
        }
    }

    @Override
    public Renderable setIsDirty(boolean dirty) {
        super.setIsDirty(dirty);
        ac_lm.setIsDirty(dirty);
        pc_lm.setIsDirty(dirty);
        sup_lm.setIsDirty(dirty);
        
        return this;
    }
    
    @Override
    public boolean getIsDirty() {
        return super.getIsDirty() || ac_lm.getIsDirty() || pc_lm.getIsDirty() || sup_lm.getIsDirty();
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
//        if (this.model != null && this.view != null && this.model.getCtImage() != null) {
//            Vector3f pickRay = new Vector3f();
//            Vector3f pt = view.doRayPick((int)x, (int)y, pickRay);
//            if (pt != null) {
//                Vector3f pt2 = new Vector3f(pt);
//                Vector3f.add(pt2, this.currentTarget, pt2);
//                Vector3f.add(pt2, this.currentSteering, pt2);
//                Vector3f.add(pt2, pickRay, pt2);
//                Vector3f imagePt2 = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), pt2);
//                
//                Vector3f.add(pt, this.currentTarget, pt);
//                Vector3f.add(pt, this.currentSteering, pt);
//                Vector3f imagePt = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), pt);
//                                
//                Vector3f closestIntercept = GetClosestPoint(imagePt, imagePt2, ac_lm.getLocation());
//                imagePt = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), closestIntercept);
//                
//                float dist = Vector3f.sub(closestIntercept, ac_lm.getLocation(), null).length();
//                System.out.println("close = " + closestIntercept);
//                System.out.println("ac = " + ac_lm.getLocation());
//                System.out.println("pt = " + pt);
//                System.out.println("imagePt = " + imagePt);
//                System.out.println("ac dist = " + dist);
//                if (dist < 2) {
//                    ac_lm.setColor(1, 1, 1, 1);
//                    setIsDirty(true);
//                    return true;
//                }
//                else {
//                    ac_lm.setColor(1, 0 , 0, 1);
//                    setIsDirty(true);
//                }
//            }
//        }

        if (getVisible()) {
            if (ac_lm.OnMouse(x, y, button1down, button2down, dwheel)) {
                setIsDirty(true);
                return true;
            } else if (pc_lm.OnMouse(x, y, button1down, button2down, dwheel)) {
                setIsDirty(true);
                return true;
            } else if (sup_lm.OnMouse(x, y, button1down, button2down, dwheel)) {
                setIsDirty(true);
                return true;
            } else {
                for (int i=0; i<6; i++) {
                    if (this.tb_landmarks[i].OnMouse(x, y, button1down, button2down, dwheel)) {
                        setIsDirty(true);
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void render() {
        setIsDirty(false);
        
        if (!isVisible) {
            return;
        }
        
        if (ac != null && pc != null) {
        ImageVolume ctimage = Main.getModel().getCtImage();
        
        glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_POINT_BIT);
                glDisable(GL_CLIP_PLANE0);
                glDisable(GL_CLIP_PLANE1);
                glDisable(GL_DEPTH_TEST);
                if (ac_lm != null) {
                    ac_lm.setImage(ctimage);
                    ac_lm.render();
                }
                if (pc_lm != null) {
                    pc_lm.setImage(ctimage);
                    pc_lm.render();
                }
                
                for (int i=0; i<6; i++) {
                    tb_landmarks[i].setImage(ctimage);
                    tb_landmarks[i].render();
                }
                               
                // Gets a little confusing with perspective if no depth test
                // for the superior point
                glEnable(GL_DEPTH_TEST);
                if (sup_lm != null) {
                    sup_lm.setImage(ctimage);
                    sup_lm.render();
                }
        glPopAttrib();
        
        glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_POINT_BIT);
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
                glMatrixMode(GL_MODELVIEW);
                glPushMatrix();
                    glTranslatef(-currentTarget.x - currentSteering.x,
                                -currentTarget.y - currentSteering.y,
                                -currentTarget.z - currentSteering.z);
//                    sphere.setColor(1f, 1f, 0.3f, 1f);
//    //                Vector3f color = getColor(selectedTarget);
//    //                sphere.setColor(color.x, color.y, color.z, 1f);
//                    sphere.render();
                glLineWidth(2);
                glBegin(GL_LINES);
                    glColor4f(1f, 0.5f, 0.5f, 0.9f);
                    
                    Vector3f acpt=null, pcpt=null;
                    if (ctimage != null) {
                        acpt = ImageVolumeUtil.pointFromImageToWorld(ctimage, ac);
                        glVertex3f(acpt.x, acpt.y, acpt.z);
                        
                        pcpt = ImageVolumeUtil.pointFromImageToWorld(ctimage, pc);
                        glVertex3f(pcpt.x, pcpt.y, pcpt.z);
                    }
                
//                    // Draw a verticle line from AC upward aligned with ACPC z-axis
//                    if (sup != null) {
//                        glColor4f(0.5f, 0.5f, 1f, 0.9f);
//                        Vector3f pt = ImageVolumeUtil.pointFromImageToWorld(ctimage, ac);
//                        glVertex3f(pt.x, pt.y, pt.z);
//                                                
//                        Vector3f upvector = new Vector3f(acpcUpVec);
//                        upvector.scale(80f);
//
//                        Vector3f.add(pt, upvector, upvector);
//                        
//                        glVertex3f(upvector.x, upvector.y, upvector.z);
//
//                    }
                glEnd();
                
                Vector3f refPt = (Vector3f)Main.getModel().getAttribute("acpcRefPoint");
                Vector3f targetPerpendicularPt = (Vector3f)Main.getModel().getAttribute("acpcPerpendicularPoint");
                Vector3f target = (Vector3f)Main.getModel().getAttribute("acpcTarget");
                
                Vector3f acpcnorm = null;
                Vector3f perpnorm = null;
                if (acpt != null && pcpt != null) {
                    acpcnorm = Vector3f.sub(acpt, pcpt, null).normalise(null);
                }
                
                perpnorm = (Vector3f)Main.getModel().getAttribute("acpcCoordRight");                    
                                
                if (targetPerpendicularPt != null && target != null) {
                    if (ctimage != null) {
                        glLineWidth(2);
                        glBegin(GL_LINES);
                            glColor4f(1f, 0.5f, 0.5f, 0.9f);
                    
                            Vector3f pt1 = ImageVolumeUtil.pointFromImageToWorld(ctimage, targetPerpendicularPt);
                            glVertex3f(pt1.x, pt1.y, pt1.z);
                            
                            Vector3f pt2 = ImageVolumeUtil.pointFromImageToWorld(ctimage, target);
                            glVertex3f(pt2.x, pt2.y, pt2.z);
                            
                            if (acpcnorm != null) {
                                Vector3f latMarker1 = Vector3f.add(pt1, acpcnorm, null);
                                Vector3f latMarker2 = Vector3f.sub(pt1, acpcnorm, null);

                                glVertex3f(latMarker1.x, latMarker1.y, latMarker1.z);
                                glVertex3f(latMarker2.x, latMarker2.y, latMarker2.z);
                            }
                            
                            if (perpnorm != null) {
                                Vector3f latMarker1 = Vector3f.add(acpt, perpnorm, null);
                                Vector3f latMarker2 = Vector3f.sub(acpt, perpnorm, null);

                                glVertex3f(latMarker1.x, latMarker1.y, latMarker1.z);
                                glVertex3f(latMarker2.x, latMarker2.y, latMarker2.z);
                                
                                latMarker1 = Vector3f.add(pcpt, perpnorm, null);
                                latMarker2 = Vector3f.sub(pcpt, perpnorm, null);

                                glVertex3f(latMarker1.x, latMarker1.y, latMarker1.z);
                                glVertex3f(latMarker2.x, latMarker2.y, latMarker2.z);
                            }
                            
                        glEnd();
                        
                        glPointSize(8);
                        glBegin(GL_POINTS);
                            glColor4f(0.5f, 0.5f, 1f, 0.9f);
                            pt1 = ImageVolumeUtil.pointFromImageToWorld(ctimage, refPt);
                            glVertex3f(pt1.x, pt1.y, pt1.z);
                        glEnd();
                    }
                }
                
                drawAtlasBox();
                
                glPopMatrix();
                

                
         glPopAttrib();
         
            
        }
        
        setIsDirty(false);
    }
    
    private void drawAtlasBox() {
        ImageVolume ctimage = Main.getModel().getCtImage();
        Vector3f superior = (Vector3f)model.getAttribute("acpcCoordSup");
        Vector3f right = (Vector3f)model.getAttribute("acpcCoordRight");
        Vector3f posterior = (Vector3f)model.getAttribute("acpcCoordPost");
        Vector3f ac = (Vector3f)model.getAttribute("AC");
               
        Vector3f[] corners = new Vector3f[24];
        
        right = ImageVolumeUtil.pointFromWorldToImageRotationOnly(ctimage, right);
        superior = ImageVolumeUtil.pointFromWorldToImageRotationOnly(ctimage, superior);
        posterior = ImageVolumeUtil.pointFromWorldToImageRotationOnly(ctimage, posterior);
        
        corners[0] = new Vector3f((Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()));
        Vector3f.add(corners[0], (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset()), corners[0]);
        Vector3f.add(corners[0], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[0]);
        
        corners[1] = new Vector3f((Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()));
        Vector3f.add(corners[1], (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset()), corners[1]);
        Vector3f.add(corners[1], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[1]);
        
        corners[2] = new Vector3f((Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()));
        Vector3f.add(corners[2], (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset()), corners[2]);
        Vector3f.add(corners[2], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[2]);
        
        corners[3] = new Vector3f((Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()));
        Vector3f.add(corners[3], (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset()), corners[3]);
        Vector3f.add(corners[3], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[3]);
        
        
        corners[4] = new Vector3f((Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()));
        Vector3f.add(corners[4], (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset()), corners[4]);
        Vector3f.add(corners[4], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[4]);
        
        corners[5] = new Vector3f((Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()));
        Vector3f.add(corners[5], (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset()), corners[5]);
        Vector3f.add(corners[5], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[5]);
        
        corners[6] = new Vector3f((Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()));
        Vector3f.add(corners[6], (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset()), corners[6]);
        Vector3f.add(corners[6], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[6]);
        
        corners[7] = new Vector3f((Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()));
        Vector3f.add(corners[7], (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset()), corners[7]);
        Vector3f.add(corners[7], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[7]);
        
               
        corners[8] = (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset());
        Vector3f.add(corners[8], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[8]);
        
        corners[9] = (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset());
        Vector3f.add(corners[9], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[9]);
        
        corners[10] = (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset());
        Vector3f.add(corners[10], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[10]);
        
        corners[11] = (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset());
        Vector3f.add(corners[11], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[11]);
        
        
        corners[12] = (Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset());
        Vector3f.add(corners[12], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[12]);
        
        corners[13] = (Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset());
        Vector3f.add(corners[13], (Vector3f)new Vector3f(superior).scale(-tb_landmark_constr[5].getOffset()), corners[13]);
        
        corners[14] = (Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset());
        Vector3f.add(corners[14], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[14]);
        
        corners[15] = (Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset());
        Vector3f.add(corners[15], (Vector3f)new Vector3f(superior).scale(tb_landmark_constr[4].getOffset()), corners[15]);
        
        
        corners[16] = (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset());
        Vector3f.add(corners[16], (Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()), corners[16]);
        
        corners[17] = (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset());
        Vector3f.add(corners[17], (Vector3f)new Vector3f(right).scale(-tb_landmark_constr[1].getOffset()), corners[17]);
        
        corners[18] = (Vector3f)new Vector3f(posterior).scale(-tb_landmark_constr[3].getOffset());
        Vector3f.add(corners[18], (Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()), corners[18]);
        
        corners[19] = (Vector3f)new Vector3f(posterior).scale(tb_landmark_constr[2].getOffset());
        Vector3f.add(corners[19], (Vector3f)new Vector3f(right).scale(tb_landmark_constr[0].getOffset()), corners[19]);
        
        
        
        glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_POINT_BIT | GL_POLYGON_BIT);
            glDisable(GL_BLEND);
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_LIGHTING);
            glMatrixMode(GL_MODELVIEW);
            
//            glPushMatrix();
//                glTranslatef(-currentTarget.x - currentSteering.x,
//                            -currentTarget.y - currentSteering.y,
//                            -currentTarget.z - currentSteering.z);

                glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                glBegin(GL_QUADS);
                    glColor4f(0.5f, 0.9f, 0.5f, 1f);

                    Vector3f origin = (Vector3f)Vector3f.add(ac, pc, null).scale(0.5f);//new Vector3f(ac);//ImageVolumeUtil.pointFromImageToWorld(ctimage, ac);
                    for (int i=0; i<8; i++) {
                        submitAtlasBoxVertex(i, origin, corners, ctimage);
                    }
                    
                    for (int i=8; i<20; i++) {
                        submitAtlasBoxVertex(i, origin, corners, ctimage);
                    }
                    
                    
                    submitAtlasBoxVertex(0, origin, corners, ctimage);
                    submitAtlasBoxVertex(1, origin, corners, ctimage);
                    submitAtlasBoxVertex(5, origin, corners, ctimage);
                    submitAtlasBoxVertex(4, origin, corners, ctimage);

                    submitAtlasBoxVertex(2, origin, corners, ctimage);
                    submitAtlasBoxVertex(3, origin, corners, ctimage);
                    submitAtlasBoxVertex(7, origin, corners, ctimage);
                    submitAtlasBoxVertex(6, origin, corners, ctimage);

                glEnd();

//                glPopMatrix();
        glPopAttrib();
        
    }
    
    private void submitAtlasBoxVertex(int i, Vector3f origin, Vector3f[] corners, ImageVolume ctimage) {
        Vector3f pt1 = Vector3f.add(origin, corners[i], null);
        pt1 = ImageVolumeUtil.pointFromImageToWorld(ctimage, pt1);
        glVertex3f(pt1.x, pt1.y, pt1.z);
    }

    @Override
    public void update(String propertyName, Object newValue) {
        
        switch (propertyName) {
            case "currentTargetPoint":
//                System.out.println("ACPCdisplay: currentTarget update");
                if (newValue instanceof Vector3f) {                    
                    currentTarget.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
            case "currentTargetSteering":
//                System.out.println("ACPCdisplay: currentSteering update");
                if (newValue instanceof Vector3f) {                    
                    currentSteering.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
            case "acpcCoordSup":
                if (newValue instanceof Vector3f) {                    
                    acpcUpVec.set((Vector3f)newValue);
                    setIsDirty(true);
                    tb_landmarks[4].setImage(model.getCtImage());
                    tb_landmarks[5].setImage(model.getCtImage());
                    
                    if (ac != null & pc != null) {
                        Vector3f ac_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), ac);
                        Vector3f pc_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), pc);

                        ac_world = (Vector3f) Vector3f.add(ac_world, pc_world, ac_world).scale(0.5f);

                        tb_landmarks[4].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(tb_landmark_constr[4].getOffset()), null)));
                        tb_landmarks[5].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(-tb_landmark_constr[5].getOffset()), null)));

                        tb_landmark_constr[4].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue)));
                        tb_landmark_constr[5].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue).negate(null)));

                    }
                }                
                break;
            case "acpcCoordRight":
                if (newValue instanceof Vector3f) {
                    tb_landmarks[0].setImage(model.getCtImage());
                    tb_landmarks[1].setImage(model.getCtImage());

                    if (ac != null & pc != null) {
                        Vector3f ac_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), ac);
                        Vector3f pc_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), pc);

                        ac_world = (Vector3f)Vector3f.add(ac_world, pc_world, ac_world).scale(0.5f);

                        tb_landmarks[0].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(tb_landmark_constr[0].getOffset()), null)));
                        tb_landmarks[1].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(-tb_landmark_constr[1].getOffset()), null)));

                        tb_landmark_constr[0].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue)));
                        tb_landmark_constr[1].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue).negate(null)));
                    }
                }
                break;
            case "acpcCoordPost":
                if (newValue instanceof Vector3f) {
                    tb_landmarks[2].setImage(model.getCtImage());
                    tb_landmarks[3].setImage(model.getCtImage());

                    if (ac != null & pc != null) {
                        Vector3f ac_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), ac);
                        Vector3f pc_world = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), pc);

                        ac_world = (Vector3f) Vector3f.add(ac_world, pc_world, ac_world).scale(0.5f);
                        
                        tb_landmarks[2].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(tb_landmark_constr[2].getOffset()), null)));
                        tb_landmarks[3].setLocation(ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), Vector3f.add(ac_world, (Vector3f) new Vector3f((Vector3f) newValue).scale(-tb_landmark_constr[3].getOffset()), null)));

                        tb_landmark_constr[2].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue)));
                        tb_landmark_constr[3].setDirection(ImageVolumeUtil.pointFromWorldToImageRotationOnly(model.getCtImage(), ((Vector3f) newValue).negate(null)));

                    }
                }
                break;
                
            case "AC":
                System.out.println("ACPCdisplay: AC update");
                if (newValue instanceof Vector3f) {
                    if (ac == null) ac = new Vector3f();
                    ac.set(new Vector3f((Vector3f)newValue));
                    if (ac_lm != null && model != null && model.getCtImage() != null) {
                        ac_lm.setImage(model.getCtImage());
//                        ac_lm.setLocation(ImageVolumeUtil.pointFromImageToWorld(image, new Vector3f((Vector3f)newValue)));
                        ac_lm.setLocation(new Vector3f((Vector3f)newValue));
                        
                        if (ac != null & pc != null) {
                            Vector3f acpcmid = (Vector3f) Vector3f.add(ac, pc, null).scale(0.5f);
                            for (int l = 0; l < tb_landmark_constr.length; l++) {
                                tb_landmark_constr[l].setPoint(acpcmid);
                            }
                        }
                    }
                }
                else {
                    ac = null;
                }
                setIsDirty(true);
                break;
            case "PC":
                System.out.println("ACPCdisplay: PC update");
                if (newValue instanceof Vector3f) {
                    if (pc == null) pc = new Vector3f();
                    pc.set(new Vector3f((Vector3f)newValue));
                    if (pc_lm != null && model != null && model.getCtImage() != null) {
                        pc_lm.setImage(model.getCtImage());
//                        pc_lm.setLocation(ImageVolumeUtil.pointFromImageToWorld(image, new Vector3f((Vector3f)newValue)));
                        pc_lm.setLocation(new Vector3f((Vector3f)newValue));
                        
                        if (ac != null & pc != null) {
                            Vector3f acpcmid = (Vector3f) Vector3f.add(ac, pc, null).scale(0.5f);
                            for (int l = 0; l < tb_landmark_constr.length; l++) {
                                tb_landmark_constr[l].setPoint(acpcmid);
                            }
                        }
                    }
                }
                else {
                    pc = null;
                }
                setIsDirty(true);
                break;
            case "ACPCSup":
                if (newValue instanceof Vector3f) {
                    if (sup == null) sup = new Vector3f();
                    sup.set((Vector3f)newValue);
                    if (sup_lm != null && model != null && model.getCtImage() != null) {
                        sup_lm.setImage(model.getCtImage());
//                        sup_lm.setLocation(ImageVolumeUtil.pointFromImageToWorld(image, new Vector3f((Vector3f)newValue)));
                        sup_lm.setLocation(new Vector3f((Vector3f)newValue));
                    }
                }
                else {
                    sup = null;
                }
                setIsDirty(true);
                break;                
        }                
    }
    
//    void updateTalairachBox() {
//        for (int i=0; i<tb_landmarks.length; i++) {
//                tb_landmarks[i].setImage(model.getCtImage());
//                tb_landmarks[i].setLocation(Vector3f.add(ac, (Vector3f)new Vector3f((Vector3f)newValue).scale(tailarachBox[i]), null));
//        }
//    }
    
}
