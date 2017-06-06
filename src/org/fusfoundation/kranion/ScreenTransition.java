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

import org.lwjgl.opengl.Display;
import org.lwjgl.LWJGLException;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
//import static org.lwjgl.opengl.GL32.*;

/**
 *
 * @author John Snell
 */
public class ScreenTransition extends Renderable implements Animator, Resizeable {

    private Framebuffer fb = new Framebuffer();
    private AnimatorUtil animator = new AnimatorUtil();
    
    public ScreenTransition() {
        doLayout();
    }
    
    public void doTransition(float duration) {
        animator.set(0f, 1f, duration);
        
        //copy the main framebuffer to FBO
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);  
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fb.getFBOId());  
        glBlitFramebuffer(0, 0, fb.getWidth()-1, fb.getHeight()-1, 0, 0, fb.getWidth()-1, fb.getHeight()-1,  GL_COLOR_BUFFER_BIT, GL_NEAREST); 
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);       
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
    }
    
    @Override
    public void render() {
        if (animator.isAnimationDone()) {
            return;
        }
        else {
//            System.out.println("Transition " + animator.getCurrent());
            
            Main.glPushAttrib(GL_ENABLE_BIT);
            
//            glEnable(GL_BLEND);
//            glDisable(GL_DEPTH_TEST);
//            glDisable(GL_STENCIL_TEST);
//            glDisable(GL_CLIP_PLANE0);
//            glDisable(GL_CLIP_PLANE1);
            glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            
            fb.render(1f - animator.getCurrent());
            
            Main.glPopAttrib();
        }
    }

    @Override
    public boolean getIsDirty() {
        return !animator.isAnimationDone();
    }
    
    @Override
    public void release() {
        fb.release();
    }

    @Override
    public boolean isAnimationDone() {
        return animator.isAnimationDone();
    }

    @Override
    public void cancelAnimation() {
        animator.cancelAnimation();
    }

    @Override
    public void advanceFrame() {
        animator.advanceFrame();
    }

    @Override
    public void doLayout() {
        try {
            fb.build(Display.getWidth(), Display.getHeight());
        }
        catch(LWJGLException e) {
            e.printStackTrace();
        }
    }
    
}
