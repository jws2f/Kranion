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

import java.nio.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 *
 * @author john
 */
public class Framebuffer extends Renderable implements Resizeable {

    private int myFBOId = -1;
    private int renderedTextureID;
    private int depthRenderBufferID;
    private int width, height;
    private int nMSAAsamples = 0;
    
    private static ShaderProgram shader = null;
    
    public void Framebuffer() {
        width = 0;
        height = 0;
    }
          
    public int getFBOId() { return myFBOId; }
    
    public void setMultiSamples(int samples) {
        nMSAAsamples = samples;
    }
    
    public void build(int w, int h) throws LWJGLException {
        
        if (w <=0 || h <=0) return;
        
        if (w == width && h == height && myFBOId != -1) return;
        
        nMSAAsamples = 1;
                
        release();
        
        width = w;
        height = h;
        
        IntBuffer buffer = ByteBuffer.allocateDirect(1*4).order(ByteOrder.nativeOrder()).asIntBuffer(); // allocate a 1 int byte buffer

        buffer.rewind();
        glGenTextures(buffer);
        renderedTextureID = buffer.get();
 
        // Create color buffer texture to bind to FBO
        glBindTexture(GL_TEXTURE_2D, renderedTextureID);
  
        // Filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        
        // create texture
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        // create FBO
        buffer.rewind();
        glGenFramebuffers( buffer ); // generate 
        myFBOId = buffer.get();        
        glBindFramebuffer(GL_FRAMEBUFFER, myFBOId);

        // The depth buffer
        buffer.rewind();
        glGenRenderbuffers(buffer);
        depthRenderBufferID = buffer.get();
        
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferID);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        
        // Set "renderedTexture" as our colour attachement #0
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTextureID, 0);
        
        glDrawBuffer(GL_NONE);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE){
            System.out.println("Frame buffer created sucessfully.");
        }
        else {
            System.out.println("An error occured creating the frame buffer.");
            throw new LWJGLException("An error occured creating the frame buffer.");
        }
    
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
       
        glClearColor(0f, 0f, 0f, 0.0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        
        //glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
      
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public void build(int w, int h, int samples) throws LWJGLException {
        
        if (w <=0 || h <=0) return;
        
        if (w == width && h == height && samples == nMSAAsamples && myFBOId != -1) return;
    
        if (samples == 1) {
            build(w, h);
            return;
        }
        
        release();
        
        nMSAAsamples = samples;
         
        width = w;
        height = h;
        
        IntBuffer buffer = ByteBuffer.allocateDirect(1*4).order(ByteOrder.nativeOrder()).asIntBuffer(); // allocate a 1 int byte buffer

        buffer.rewind();
        glGenTextures(buffer);
        renderedTextureID = buffer.get();
 
        if (nMSAAsamples == 1 || Main.OpenGLVersion < 3f) {
            // Create color buffer texture to bind to FBO
            glBindTexture(GL_TEXTURE_2D, renderedTextureID);

            // Filtering
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_WRAP_T, GL_CLAMP);

            // create texture
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
            
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        else {
            // Create color buffer texture to bind to FBO
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, renderedTextureID);

            // Filtering
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
    //        glTexParameteri(GL_TEXTURE_2D_MULTISAMPLE, GL_TEXTURE_WRAP_T, GL_CLAMP);

            // create texture
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, nMSAAsamples, GL_RGBA8, width, height, true);
            
            glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
        }
        
        
        // create FBO
        buffer.rewind();
        glGenFramebuffers( buffer ); // generate 
        myFBOId = buffer.get();        
        glBindFramebuffer(GL_FRAMEBUFFER, myFBOId);

        // The depth buffer
        buffer.rewind();
        glGenRenderbuffers(buffer);
        depthRenderBufferID = buffer.get();
        
     
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderBufferID);
        
        if (nMSAAsamples == 1 || Main.OpenGLVersion < 3f) {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        }
        else {
            glRenderbufferStorageMultisample(GL_RENDERBUFFER, nMSAAsamples, GL_DEPTH24_STENCIL8, width, height);
        }
        
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthRenderBufferID);
        
        // Set "renderedTexture" as our colour attachement #0
        if (nMSAAsamples == 1 || Main.OpenGLVersion < 3f) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedTextureID, 0);
        }
        else {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, renderedTextureID, 0);
        }
        
        glDrawBuffer(GL_NONE);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE){
            System.out.println("Frame buffer created sucessfully.");
        }
        else {
            System.out.println("An error occured creating the frame buffer.");
            throw new LWJGLException("An error occured creating the frame buffer.");
        }
    
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
       
        glClearColor(0f, 0f, 0f, 0.0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        
        //glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
      
    }
    
    // this.bind() first!
    public void clear() {
        glClearColor(0f, 0f, 0f, 0.0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);        
    }
    
    public void bind() {
        if (myFBOId == 0) return;
        
        glBindFramebuffer(GL_FRAMEBUFFER, myFBOId);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
    }
    
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
//        glDrawBuffer(0);
    }
    
    @Override
    public void render() {
        render(1f);
    }
    
    public void render(float opacity) {
        
        if (!getVisible()) return;
        setIsDirty(false);
        
        if (shader == null) {
            shader = new ShaderProgram();
            shader.addShader(GL_VERTEX_SHADER, "shaders/framebuffer.vs.glsl");
            shader.addShader(GL_FRAGMENT_SHADER, "shaders/framebuffer.fs.glsl");
            shader.compileShaderProgram();
        }
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_VIEWPORT_BIT | GL_TRANSFORM_BIT);
                
// This is probably faster than texture approach, but can't do alpha blending
//        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.myFBOId);
//        glReadBuffer(GL_COLOR_ATTACHMENT0);
//        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
//        //glDrawBuffers(...)
//        glDrawBuffer(GL_BACK);
//        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
//        
//        glReadBuffer(0);
//        glDrawBuffer(0);
//        glBindFramebuffer(GL_FRAMEBUFFER, 0);        
         
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);      
        glEnable(GL_BLEND);
        glDisable(GL_CLIP_PLANE0);
        glDisable(GL_CLIP_PLANE1);
        //glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        glEnable(GL_TEXTURE_2D);        
        glBindTexture(GL_TEXTURE_2D, renderedTextureID);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
       //glTexEnvi(GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_COMBINE_ALPHA, GL_REPLACE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        
        glViewport(0, 0, width, height);
        
        // Go into ORTHO projection, but save any 
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
        glLoadIdentity();
        
        org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, width, 0.0f, height);

        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
        glLoadIdentity();
        
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            
        glMatrixMode(GL_MODELVIEW);
        
        glColor4f(1f, 1.0f, 1f, opacity);
        
        if (shader != null) {
                shader.start();
                int uniformLoc = glGetUniformLocation(shader.getShaderProgramID(), "fb_tex");
                glUniform1i(uniformLoc, 0);
                uniformLoc = glGetUniformLocation(shader.getShaderProgramID(), "alpha");
                glUniform1f(uniformLoc, opacity);             
        }
        
        glBegin(GL_QUADS);
            glTexCoord2f(0f, 0f);
            glVertex2f(0f, 0f);
            
            glTexCoord2f(0f, 1f);
            glVertex2f(0f, (float)height);
            
            glTexCoord2f(1f, 1f);
            glVertex2f((float)width, (float)height);
            
            glTexCoord2f(1f, 0f);
            glVertex2f((float)width, 0f);
        glEnd();
        
        if (shader != null) {
            shader.stop();
        }
        
        // Restore matrices
        glMatrixMode(GL_MODELVIEW);
        Main.glPopMatrix();
        
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
        
        glMatrixMode(GL_MODELVIEW);
        
        // unbind the framebuffer texture
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);        
        
        // reset gl attributes
        Main.glPopAttrib();
    }
    
    public void render_MSAA(Framebuffer fb) {
        Main.glPushAttrib(GL_ENABLE_BIT | GL_VIEWPORT_BIT);
        
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);      
//        glEnable(GL_BLEND);
//        glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.myFBOId);
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fb.getFBOId());
        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        
        glReadBuffer(0);
        glDrawBuffer(0);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);        
//        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
//        glEnable(GL_TEXTURE_2D);        
//        glBindTexture(GL_TEXTURE_2D, renderedTextureID);
//        
//        glViewport(0, 0, width-1, height-1);
//        
//        // Go into ORTHO projection, but save any 
//        glMatrixMode(GL_PROJECTION);
//        glPushMatrix();
//        glLoadIdentity();
//        
//        org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, width-1, 0.0f, height-1);
//
//        glMatrixMode(GL_MODELVIEW);
//        glPushMatrix();
//        glLoadIdentity();
//        
//            glMatrixMode(GL_TEXTURE);
//            glLoadIdentity();
//            
//        glMatrixMode(GL_MODELVIEW);
//        
//        glColor4f(1f, 1.0f, 1f, 1.0f);
//        glBegin(GL_QUADS);
//            glTexCoord2f(0f, 0f);
//            glVertex2f(0f, 0f);
//            
//            glTexCoord2f(0f, 1f);
//            glVertex2f(0f, (float)height-1);
//            
//            glTexCoord2f(1f, 1f);
//            glVertex2f((float)width-1, (float)height-1);
//            
//            glTexCoord2f(1f, 0f);
//            glVertex2f((float)width-1, 0f);
//        glEnd();
//        
//        // Restore matrices
//        glMatrixMode(GL_MODELVIEW);
//        glPopMatrix();
//        
//        glMatrixMode(GL_PROJECTION);
//        glPopMatrix();
//        
//        glMatrixMode(GL_MODELVIEW);
//        
//        // unbind the framebuffer texture
//        glBindTexture(GL_TEXTURE_2D, 0);
        
        // reset gl attributes
        Main.glPopAttrib();
    }

    public int getPixelRGBasInt(int x, int y) { // TODO:
        glBindFramebuffer(GL_FRAMEBUFFER, myFBOId);
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        
        ByteBuffer buf = BufferUtils.createByteBuffer(4);
        
//        System.out.println("reading framebuffer pixel " + x + ", " + y);
        glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
                
        byte b1 = buf.get();
        byte b2 = buf.get();
        byte b3 = buf.get();
        
        return (b3 & 0xff) << 16 |(b2 & 0xff) << 8 | (b1 & 0xff);
        
    }
    
    @Override
    public void release() {
        glDeleteTextures(renderedTextureID);
        glDeleteRenderbuffers(depthRenderBufferID);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(myFBOId);
    }

    @Override
    public void doLayout() {
        try {
            this.build(Display.getWidth(), Display.getHeight());
        }
        catch(LWJGLException e) {
            e.printStackTrace();
        }
    }
    
}
