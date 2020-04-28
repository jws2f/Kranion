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
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_POINT_BIT;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPointSize;
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
    
    public ACPCdisplay() {
        ac = pc = sup = null;
        this.setVisible(true);
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
                                
                glPopMatrix();
                

                
         glPopAttrib();
            
        }
        
        setIsDirty(false);
    }

    @Override
    public void update(String propertyName, Object newValue) {
        
        switch (propertyName) {
            case "currentTargetPoint":
                System.out.println("ACPCdisplay: currentTarget update");
                if (newValue instanceof Vector3f) {                    
                    currentTarget.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
            case "currentTargetSteering":
                System.out.println("ACPCdisplay: currentSteering update");
                if (newValue instanceof Vector3f) {                    
                    currentSteering.set((Vector3f)newValue);
                    setIsDirty(true);
                }
                break;
            case "acpcCoordSup":
                if (newValue instanceof Vector3f) {                    
                    acpcUpVec.set((Vector3f)newValue);
                    setIsDirty(true);
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
    
}
