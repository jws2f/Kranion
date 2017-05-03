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

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import org.fusfoundation.kranion.Trackball;

/**
 *
 * @author john
 */
public class OrientationAnimator extends Renderable implements Animator {
    private AnimatorUtil anim = new AnimatorUtil();
    private Quaternion startQ = new Quaternion(), endQ = new Quaternion(), currentQ = new Quaternion();
    private Trackball trackball = null;
    
    public OrientationAnimator() {        
    }
    
    public OrientationAnimator(Quaternion start, Quaternion end, float durationSecs)
    {
       set(start, end, durationSecs);
    }
    
    public void set(Quaternion start, Quaternion end, float durationSecs)
    {
       anim.set(0, 1, durationSecs);
       startQ.set(start.x, start.y, start.z, start.w);
       currentQ.set(start.x, start.y, start.z, start.w);
       endQ.set(end.x, end.y, end.z, end.w);
    }
    
    public Quaternion getCurrent() { return currentQ; }
    
    public void setTrackball(Trackball tb) {
        trackball = tb;
    }
    
    @Override
    public void advanceFrame()
    {
        if (isAnimationDone()) return;
                
        anim.advanceFrame();
        currentQ = slerp(startQ, endQ, anim.getCurrent());
         
        if (trackball != null) {
            trackball.setCurrent(this.getCurrent());
        }
    }
        
    @Override
    public void cancelAnimation() { anim.cancelAnimation(); }
    
    @Override
    public boolean isAnimationDone() {
        return anim.isAnimationDone();
    }
    
    private Quaternion lerp(float t)
    {
        Vector4f a = new Vector4f(startQ.x, startQ.y, startQ.z, startQ.w);
        Vector4f b = new Vector4f(endQ.x, endQ.y, endQ.z, endQ.w);
        return new Quaternion(Vector4f.add((Vector4f)(a.scale(1f-t)), (Vector4f)(b.scale(t)), null).normalise(null));
    }
    
    private Quaternion slerp(Quaternion qa, Quaternion qb, double t) {
	// quaternion to return
	Quaternion qm = new Quaternion();
	// Calculate angle between them.
	double cosHalfTheta = qa.w * qb.w + qa.x * qb.x + qa.y * qb.y + qa.z * qb.z;
	// if qa=qb or qa=-qb then theta = 0 and we can return qa
	if (Math.abs(cosHalfTheta) >= 1.0){
		qm.w = qa.w;qm.x = qa.x;qm.y = qa.y;qm.z = qa.z;
		return qm;
	}
	// Calculate temporary values.
	double halfTheta = Math.acos(cosHalfTheta);
	double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta*cosHalfTheta);
	// if theta = 180 degrees then result is not fully defined
	// we could rotate around any axis normal to qa or qb
	if (Math.abs(sinHalfTheta) < 0.001){ // fabs is floating point absolute
		qm.w = (qa.w * 0.5f + qb.w * 0.5f);
		qm.x = (qa.x * 0.5f + qb.x * 0.5f);
		qm.y = (qa.y * 0.5f + qb.y * 0.5f);
		qm.z = (qa.z * 0.5f + qb.z * 0.5f);
		return qm;
	}
	float ratioA = (float)(Math.sin((1d - t) * halfTheta) / sinHalfTheta);
	float ratioB = (float)(Math.sin(t * halfTheta) / sinHalfTheta); 
	//calculate Quaternion.
	qm.w = (qa.w * ratioA + qb.w * ratioB);
	qm.x = (qa.x * ratioA + qb.x * ratioB);
	qm.y = (qa.y * ratioA + qb.y * ratioB);
	qm.z = (qa.z * ratioA + qb.z * ratioB);
	return qm;
}

    @Override
    public void render() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean getVisible() {
        return true;
    }

    @Override
    public void setVisible(boolean visible) {
    }

    @Override
    public boolean getIsDirty() {
        return !isAnimationDone();
    }

    @Override
    public void setIsDirty(boolean dirty) {
    }
}
