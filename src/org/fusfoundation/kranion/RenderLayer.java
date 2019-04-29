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

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import org.lwjgl.util.vector.Vector4f;
import java.util.Iterator;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author john
 */
public class RenderLayer extends GUIControl implements Resizeable, Pickable {

    private Framebuffer frameBuffer = new Framebuffer();
    private Framebuffer frameBufferMSAA = new Framebuffer();
    private int oversample = 1;
    private Vector4f clearColor = new Vector4f(0f, 0f, 0f, 0f);
    private boolean is2D = false;
    
//    private boolean isDirty = true;
    
    public RenderLayer() {
        setIsDirty(false);
    }
    
    public RenderLayer(int nMSAA) {
        setOversample(nMSAA);
        setIsDirty(false);
    }
    
    private void setOversample(int nMSAA) {
        if (oversample != nMSAA && children.size() > 0) {
            setIsDirty(true);
        }
        oversample = Math.max(1, nMSAA);
    }
    
    public void setClearColor(float r, float g, float b, float a) {
        if (clearColor.x != r || clearColor.y != g || clearColor.z != b || clearColor.w != a) {
            setIsDirty(true);
        }
        clearColor.set(r, g, b, a);
    }
            
    public void  setSize(int width, int height) {
        if (frameBuffer.getWidth() != width || frameBuffer.getHeight() != height) {
            setIsDirty(true);
        }
        System.out.println(this + ":setSize");
        try {
            setBounds(0, 0, width, height);
            frameBuffer.build(width, height);
            frameBufferMSAA.build(width, height, oversample);
        }
        catch (LWJGLException e) {
            e.printStackTrace();
        }
    }
    
    public void setIs2d(boolean is2D) {
        this.is2D = is2D;
    }
    
    public boolean getIs2D() { return this.is2D; }
    
    public void  setSize(int width, int height, int nMSAA) {
//        System.out.println("Render Layer Resize " + this.getTag());
        setOversample(nMSAA);
        setSize(width, height);
    }
    
    @Override
    public void render() {
        advanceChildren();
        
        if (getIsDirty()) {
//            System.out.println("Render Layer Dirty " + this.getTag());

//            glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT);
        
            if (is2D) {
                glMatrixMode(GL_PROJECTION);
                Main.glPushMatrix();
                glLoadIdentity();

                org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, Display.getWidth(), 0.0f, Display.getHeight());

                glMatrixMode(GL_MODELVIEW);
                Main.glPushMatrix();
                glLoadIdentity();
            }
                
                
//            System.out.println(this);
            frameBufferMSAA.bind();
            
            
            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
            glClearStencil(0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            
            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
        
            renderChildren();

            frameBufferMSAA.unbind();
            frameBufferMSAA.render_MSAA(frameBuffer);

            setIsDirty(false);
            
            if (is2D) {
                glMatrixMode(GL_MODELVIEW);
                Main.glPopMatrix();
                glMatrixMode(GL_PROJECTION);
                Main.glPopMatrix();
            }
//            glPopAttrib();
            
            glMatrixMode(GL_MODELVIEW);
        }
        

        
        frameBuffer.render();
    }

    @Override
    public void release() {
        frameBuffer.release();
        frameBufferMSAA.release();
        super.release();
    }

    @Override
    public void addChild(Renderable child) {
        setIsDirty(true);
        super.addChild(child);
    }

    @Override
    public void doLayout() {

        setSize(Display.getWidth(), Display.getHeight());
        
        // Check for resizeable children
        Iterator<Renderable> i = this.children.iterator();
        while(i.hasNext()) {
            Renderable r = i.next();
            if (r instanceof Resizeable) {
                ((Resizeable)r).doLayout();
            }
        }
    }

    @Override
    public void renderPickable() {
        if (!is2D) { // picking is just for 3D renderables
            renderPickableChildren();
        }
    }
 
}
