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

/**
 *
 * @author john
 */
public class FloatAnimator extends Renderable implements Animator {
    private AnimatorUtil anim = new AnimatorUtil();
    private FloatParameter parameter = null;
    
    public FloatAnimator() {
        anim.set(0f, 0f, 0f);
    }
    
    public FloatAnimator(FloatParameter animatedParam, float start, float end, float durationSecs)
    {
        set(animatedParam, start, end, durationSecs);
    }
    
    public void set(FloatParameter animatedParam, float start, float end, float durationSecs) {
       parameter = animatedParam;
       anim.set(start, end, durationSecs);
    }
    
    public void setAnimatedParameter(FloatParameter animatedParam) {
        parameter = animatedParam;
    }

    @Override
    public void cancelAnimation() { anim.cancelAnimation(); }
    
    @Override
    public boolean isAnimationDone() {
        return anim.isAnimationDone();
    }

    @Override
    public void advanceFrame()
    {
        if (isAnimationDone()) return;
        
        anim.advanceFrame();
        
        if (parameter != null) {
            parameter.setValue(anim.getCurrent());
        }
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
