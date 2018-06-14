/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
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

import java.awt.Color;
import java.util.Random;
import static org.fusfoundation.kranion.Main.glPopAttrib;
import static org.fusfoundation.kranion.Main.glPopMatrix;
import static org.fusfoundation.kranion.Main.glPushAttrib;
import static org.fusfoundation.kranion.Main.glPushMatrix;
import org.fusfoundation.kranion.model.Model;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ENABLE_BIT;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author john
 */
public class TargetRenderer extends DirtyFollower {

    private static Sphere sphere = new Sphere(1f);//(2.5f);
    
    private static Color[] colors = new Color[36];
    
    private static Random rand = new Random();
    
    public TargetRenderer(Renderable toBeFollowed) {
        super(toBeFollowed);
        
        rand.setSeed(31415L); // just to get the same color series every time
        for (int i=0; i<colors.length; i++) {
            colors[i] = Color.getHSBColor(rand.nextFloat(),//random hue, color
                1.0f,//full saturation, 1.0 for 'colorful' colors, 0.0 for grey
                1.0f //1.0 for bright, 0.0 for black
                );
        }
    }
    
    private Vector3f getColor(int index) {
        Vector3f color = new Vector3f(
//            colors[index%colors.length].getRed()/255f,
//            colors[index%colors.length].getGreen()/255f,
//            colors[index%colors.length].getBlue()/255f
              0f, 0.9f, 0.9f
        );
        return color;
    }

    @Override
    public void render() {
        
        if (!isVisible) return;
        
        Model model = Main.getModel();
        int targetCount = model.getSonicationCount();
        Integer selectedTarget = (Integer)model.getAttribute("currentSonication");
        
        Vector3f vec = (Vector3f)model.getAttribute("currentTargetPoint");
        Vector3f currentTarget = null;
        if (vec != null) {
            currentTarget = new Vector3f(vec);
        }
        else {
            return;
        }
        
        if (currentTarget == null) return;
        
        Vector3f tmp = (Vector3f)model.getAttribute("currentTargetSteering");
        if (tmp == null) tmp = new Vector3f();
        Vector3f currentSteering = new Vector3f(tmp);
        
        Vector3f.add(currentTarget, currentSteering, currentTarget);
        
        
        glPushAttrib(GL_ENABLE_BIT);
        glEnable(GL_BLEND);
        
        // Render the selected target opaque first
        if (selectedTarget != null && selectedTarget >=0 && selectedTarget < targetCount) {
            Vector3f target = new Vector3f(model.getSonication(selectedTarget).getNaturalFocusLocation());
            
            tmp = (Vector3f)model.getAttribute("currentTargetSteering");
            if (tmp == null) tmp = new Vector3f();
            Vector3f steering = new Vector3f(tmp);
            
            Vector3f.add(target, steering, target);
            
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
                glTranslatef(target.x-currentTarget.x, target.y-currentTarget.y, target.z-currentTarget.z);
                sphere.setColor(1f, 1f, 0.3f, 1f);
//                Vector3f color = getColor(selectedTarget);
//                sphere.setColor(color.x, color.y, color.z, 1f);
                sphere.render();
            glPopMatrix();
        }
        
        // Render other targets with transparency
        for (int i=0; i<targetCount; i++) {
            Vector3f target = new Vector3f(model.getSonication(i).getNaturalFocusLocation());
            Vector3f steering = new Vector3f(model.getSonication(i).getFocusSteering());
            Vector3f.add(target, steering, target);
            
            glColor4f(1f, 0f, 0f, 1f);
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
                glTranslatef(target.x-currentTarget.x, target.y-currentTarget.y, target.z-currentTarget.z);
                if (selectedTarget == null || (selectedTarget != null && i!=selectedTarget)) {
                    //sphere.setColor(0.5f, 0.2f, 0.2f, 0.8f);
                    Vector3f color = getColor(i);
                    sphere.setColor(color.x, color.y, color.z, 0.6f);
                }
                sphere.render();
            glPopMatrix();
        }
        glPopAttrib();
    }

    @Override
    public void release() {
    }

    @Override
    public Renderable setIsDirty(boolean dirty) {
        super.setIsDirty(dirty); //To change body of generated methods, choose Tools | Templates.
        
        return this;
    }
    
}
