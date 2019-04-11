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
import org.fusfoundation.kranion.Main;
import static org.fusfoundation.kranion.Main.glPopAttrib;
import static org.fusfoundation.kranion.Main.glPopMatrix;
import static org.fusfoundation.kranion.Main.glPushAttrib;
import static org.fusfoundation.kranion.Main.glPushMatrix;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import static org.lwjgl.opengl.GL11.GL_BLEND;
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
    
    public ACPCdisplay() {
        ac = pc = sup = null;
        this.setVisible(true);
    }

    @Override
    public void render() {
        if (!isVisible) {
            setIsDirty(false);
            return;
        }
        
        if (ac != null && pc != null) {
        glPushAttrib(GL_ENABLE_BIT | GL_LINE_BIT | GL_POINT_BIT);
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
                glMatrixMode(GL_MODELVIEW);
                glPushMatrix();
                    glTranslatef(-currentTarget.x, -currentTarget.y, -currentTarget.z);
//                    sphere.setColor(1f, 1f, 0.3f, 1f);
//    //                Vector3f color = getColor(selectedTarget);
//    //                sphere.setColor(color.x, color.y, color.z, 1f);
//                    sphere.render();
                glBegin(GL_LINES);
                    glColor4f(1f, 0.5f, 0.5f, 0.9f);
                    
                    ImageVolume ctimage = Main.getModel().getCtImage();
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
                }
                break;
            case "acpcCoordSup":
                if (newValue instanceof Vector3f) {                    
                    acpcUpVec.set((Vector3f)newValue);
                }                
                break;
            case "AC":
                System.out.println("ACPCdisplay: AC update");
                if (newValue instanceof Vector3f) {
                    if (ac == null) ac = new Vector3f();
                    ac.set((Vector3f)newValue);
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
                    pc.set((Vector3f)newValue);
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
                }
                else {
                    sup = null;
                }
                setIsDirty(true);
                break;                
        }                
    }
    
}
