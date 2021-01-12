/*
 * The MIT License
 *
 * Copyright 2020 Focused Ultrasound Foundation.
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

import static org.fusfoundation.kranion.Main.glPopMatrix;
import static org.fusfoundation.kranion.Main.glPushMatrix;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.fusfoundation.kranion.view.DefaultView;
import org.fusfoundation.kranion.view.View;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import org.lwjgl.util.Display;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author jsnell
 */
public class ImageLandmark extends Landmark {

    private String name = "";
    private Vector4f color = new Vector4f(1, 0, 0, 1);
    private Sphere sphere = new Sphere(0.5f);
    private ImageVolume image;
    private Vector3f currentTarget = new Vector3f();
    private Vector3f currentSteering = new Vector3f();
    
    private boolean mouseIsGrabbed = false;
    private Vector3f grabStart = new Vector3f();
    private Vector3f locationStart = new Vector3f();
    private boolean isArmed = false;
    
    private boolean showOnlyIfSelected = false;
    
    private ImageLandmarkConstraint lmConstraint = null;

// Debugging ray based 3D picking    
//    private Vector3f debugPt1 = new Vector3f();
//    private Vector3f debugPt2 = new Vector3f();
    
    public ImageLandmark(ImageVolume img) {
        image = img;
        sphere.setColor(1, 0, 0);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setImage(ImageVolume img) {
        if (img != image) {
            setIsDirty(true);
        }
        image = img;
    }
    
    public void setShowOnlyIfSelected(boolean show) {
        if (show != showOnlyIfSelected) {
            setIsDirty(true);
        }
        showOnlyIfSelected = show;
    }
    public void setColor(float r, float g, float b, float a) {
        setIsDirty(true);
        color.set(r, g, b, a);
        sphere.setColor(r, g, b, a);
    }

    @Override
    public void setLocation(Vector3f loc) {
        if (image == null) {
            super.setLocation(loc);
        }
        else {
            
            if (loc == null || currentTarget == null || currentSteering == null) {
//                System.out.println("Null pointer here");
                return;
            }
            else {
                super.setLocation(loc);
                //super.setLocation(ImageVolumeUtil.pointFromWorldToImage(image, Vector3f.add(loc, Vector3f.add(currentTarget, currentSteering, null), null)));
                //super.setLocation(ImageVolumeUtil.pointFromWorldToImage(image, loc));
            }
        }
    }
    
    public Vector3f getWorldLocation() {
        return ImageVolumeUtil.pointFromImageToWorld(image, location);
    }
    
    public void setWorldLocation(Vector3f world_location) {
        super.setLocation(ImageVolumeUtil.pointFromWorldToImage(image, world_location));
    }
    
    public void setContraint(ImageLandmarkConstraint constraint) {
        lmConstraint = constraint;
    }
    
    public ImageLandmarkConstraint getConstraint() {
        return lmConstraint;
    }

    @Override
    public void render() {
        setIsDirty(false);
        if (isVisible) {
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            
                if (image != null) {
                    glTranslatef(   -currentTarget.x - currentSteering.x,
                                    -currentTarget.y - currentSteering.y,
                                    -currentTarget.z - currentSteering.z);

                    sphere.setLocation(ImageVolumeUtil.pointFromImageToWorld(image, this.location));
                }
                else {
                    sphere.setLocation(this.location);
                }

                if (showOnlyIfSelected && this.isArmed) {
                    sphere.render();
                }
                else if (!showOnlyIfSelected) {
                    sphere.render();
                }

// Debugging ray based 3D picking                
//                Vector3f draw1 = ImageVolumeUtil.pointFromImageToWorld(image, debugPt1);
//                Vector3f draw2 = ImageVolumeUtil.pointFromImageToWorld(image, debugPt2);
//                
//                if (draw1 != null && draw2 != null) {
//                    glBegin(GL_LINES);
//                        glVertex3f(draw1.x, draw1.y, draw1.z);
//                        glVertex3f(draw2.x, draw2.y, draw2.z);
//                    glEnd();
//                }
                
            glPopMatrix();
        }
    }
    
    @Override
    public void update(Object newValue) {
        try {
            Vector3f target = new Vector3f((Vector3f)newValue);
            setLocation(target);
//            System.out.println("ImageLandmark.update");
        }
        catch(Exception e) {
            System.out.println(this + ": Wrong or NULL new value.");
        }
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        View view = Main.getView();
        Model model = Main.getModel();
        
        if (getVisible() && model != null && view != null && model.getCtImage() != null) {
            Vector3f pickRay = new Vector3f();
            Vector3f pt = view.doRayPick((int)x, (int)y, pickRay);
            if (pt != null) {
                Vector3f pt2 = new Vector3f().set(pt);
                
                // maybe draw the vector to see if it looks right
                
                Vector3f.add(pt2, this.currentTarget, pt2);
                Vector3f.add(pt2, this.currentSteering, pt2);
                Vector3f.add(pt2, (Vector3f)pickRay, pt2);
                Vector3f imagePt2 = ImageVolumeUtil.pointFromWorldToImage(image, pt2);
//                debugPt2.set(imagePt2);
                
                Vector3f.add(pt, this.currentTarget, pt);
                Vector3f.add(pt, this.currentSteering, pt);
                Vector3f imagePt = ImageVolumeUtil.pointFromWorldToImage(image, pt);
//                debugPt1.set(imagePt);
                                
                Vector3f closestIntercept = GetClosestPoint(imagePt, imagePt2, getLocation()); // image points already in image coords!
                imagePt.set(closestIntercept); //ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), closestIntercept);
                
                
                float dist = Vector3f.sub(closestIntercept, getLocation(), null).length();
//                System.out.println("close = " + closestIntercept);
//                System.out.println("ac = " + getLocation());
//                System.out.println("pt = " + pt);
//                System.out.println("imagePt = " + imagePt);
//                System.out.println("ac dist = " + dist);

                if (dist<sphere.getRadius() && !button1down) {
                    sphere.setColor(1, 1, 0, 1);
                    setIsDirty(true);
                    isArmed = true;
                }
                else if(!button1down && dist >= sphere.getRadius()) {
                    sphere.setColor(color.x, color.y, color.z, color.w);
                    setIsDirty(true);
                    isArmed = false;
                }
                
                if (isArmed && !mouseIsGrabbed && button1down) {
                    mouseIsGrabbed = true;
                    grabStart.set(pt);
                    locationStart.set(location);
                    setIsDirty(true);
                    return true;
                }
                
                if (mouseIsGrabbed) {
                    //return dwheel == 0; // let mouse wheel events fall through
                    if (button1down) {
                        Vector3f start = ImageVolumeUtil.pointFromImageToWorld(image, locationStart);
                        Vector3f newWorldLoc = Vector3f.add(start, Vector3f.sub(pt, grabStart, null), null);
                        
                        if (lmConstraint != null) {
                            newWorldLoc = lmConstraint.filterImagePoint(ImageVolumeUtil.pointFromWorldToImage(image, newWorldLoc));
                        }
                        else {
                            newWorldLoc = ImageVolumeUtil.pointFromWorldToImage(image, newWorldLoc);
                        }
                        
                        // newWorldLoc is actually now in image space; 
                        
                        location.set(newWorldLoc);
                        if (this.getCommand() != null && !getCommand().isEmpty()) {
                            model.setAttribute(getCommand(), new Vector3f(location));
                        }
                        return true;                
                    }
                    else {
                        mouseIsGrabbed = false;
                    }
                }
            }
        }
        
        return false;
    }
    
// Closest point on a line AB to point P    
    Vector3f GetClosestPoint(Vector3f A, Vector3f B, Vector3f P)
    {
        Vector3f AP = Vector3f.sub(P, A, null); //P - A;
        Vector3f AB = Vector3f.sub(B, A, null); //B - A;
        float ab2 = AB.x*AB.x + AB.y*AB.y + AB.z*AB.z;
        float ap_ab = AP.x*AB.x + AP.y*AB.y + AP.z*AB.z;
        float t = ap_ab / ab2;

//        System.out.println("t=" + t);

    //    t = Math.max(0, t); // clamp(t, 0, t);

        Vector3f Closest = Vector3f.add(A, (Vector3f)AB.scale(t), null); // A + AB * t;

//        System.out.println("Closest: " + Closest);

        return Closest;
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
        }
    }
}
