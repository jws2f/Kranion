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
package org.fusfoundation.kranion.compute;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;


import javax.swing.JFileChooser;
import org.fusfoundation.kranion.Trackball;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glMapBufferRange;

/**
 *
 * @author john
 */
public class ComputeShaderTest {
    
    public static int shaderprogram = 0;
    private static int DISPLAY_WIDTH = 1280;
    private static int DISPLAY_HEIGHT = 800;
    
    private int posSSBo=0;
    private int colSSBo=0;
    private boolean mouseButton1Drag=false;
    Trackball trackball = new Trackball(DISPLAY_HEIGHT / 2, DISPLAY_WIDTH / 2, DISPLAY_HEIGHT / 2f);
    float dolly = 0f;
    
    public ComputeShaderTest() {
        
  
    }
    
    private String readResourceFile(String filename) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filename);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
    
    private void initShader() {
        int v = glCreateShader(GL_COMPUTE_SHADER);

        String shaderSrc = new String();

        try {
            shaderSrc = readResourceFile("ParticleSystem.comp.glsl");
            System.out.println(shaderSrc);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(-1);
        }

        System.out.println("Compiling shaders");

        glShaderSource(v, shaderSrc);
        glCompileShader(v);                 
        printShaderInfoLog(v);

        System.out.println("Attaching and linking shader objects");

        shaderprogram = glCreateProgram();
        glAttachShader(shaderprogram, v);

        glLinkProgram(shaderprogram);
        glValidateProgram(shaderprogram);       
        IntBuffer resultValidate = BufferUtils.createIntBuffer(1);
        glGetProgram(shaderprogram, GL_VALIDATE_STATUS, resultValidate);
        
        if (resultValidate.get() == 0) {
            System.out.println("Shader 1 validaiton error:");
            System.out.println(GetProgramInfoLog(shaderprogram));
            System.exit(-1);
        }         
    }
    
    private void printShaderInfoLog(int shaderid) {
             System.out.println(GetShaderInfoLog(shaderid));
    }
    
    public String GetShaderInfoLog(int shader) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(java.nio.ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        glGetShaderInfoLog(shader, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }
    
    public String GetProgramInfoLog(int shader) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(java.nio.ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(java.nio.ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        glGetProgramInfoLog(shader, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }
     
    public static void main(String[] args) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            File file = new File("C:/Users/John/Desktop/MR for Cad1 and Cad2/A/Z3284");
            


            ComputeShaderTest test = new ComputeShaderTest();
            test.create();
            test.run();
            
//            glUseProgram(test.shaderprogram);
//            
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
//            org.lwjgl.opengl.GL43.glDispatchCompute(128, 1, 1);
//            org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    
    public void create() throws LWJGLException {
        //Display
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        
        DisplayMode chosenMode = null;
        for (int i=0; i<modes.length; i++) {
            DisplayMode current = modes[i];
            System.out.println(current.getWidth() + "x" + current.getHeight() + "x" +
                    current.getBitsPerPixel() + " " + current.getFrequency() + "Hz");
            if (current.getBitsPerPixel() == 32 && current.getWidth() == 1920 && current.getFrequency() == 60)
                chosenMode = current;
        }
        
        DisplayMode mode = new DisplayMode(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        System.out.println("Display: " + mode.getBitsPerPixel() + " bpp");
        Display.setDisplayMode(mode);
        Display.setFullscreen(true);
        Display.setResizable(true);
        Display.setTitle("Compute Shader Demo");
        
        PixelFormat pixelFormat = new PixelFormat(24, 8, 24, 8, 1);
        org.lwjgl.opengl.ContextAttribs contextAtrigs = new ContextAttribs(2, 1);
        
        Display.create(pixelFormat, contextAtrigs);
        
        System.out.println("GL Vendor: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR));
        System.out.println("GL Version: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
        System.out.println("GLSL Language Version: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("GL Renderer: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
            
        initShader();
        
//        IntBuffer buffer = ByteBuffer.allocateDirect(1*4).order(ByteOrder.nativeOrder()).asIntBuffer(); // allocate a 1 int byte buffer
//        buffer.rewind();

        FloatBuffer floatPosBuffer = ByteBuffer.allocateDirect(1024*1024*4*8).order(ByteOrder.nativeOrder()).asFloatBuffer(); // allocate a 1 int byte buffer
        for (int i=0; i<1024*1024; i++) {
            Vector4f v = new Vector4f((float)Math.random()*400f-200f, (float)Math.random()*400f-200f, (float)Math.random()*100f-50f, 1f);
            floatPosBuffer.put(v.x);
            floatPosBuffer.put(v.y);
            floatPosBuffer.put(v.z);
            floatPosBuffer.put(v.w);
            
            Vector4f npos = new Vector4f(v.x, v.y, v.z, v.w);
            npos.normalise();
            
            Vector3f dist = new Vector3f(v.x, v.y, v.z);
            float mag = v.x*v.x + v.y*v.y + v.z*v.z;
            //mag = (float)Math.sqrt(mag);
            
            float s = (float)Math.random()*.00f + 0.1f;
            Vector3f vel = Vector3f.cross(new Vector3f(npos.x*s, npos.y*s, npos.z*s), new Vector3f(0f, 0f, 1f), null);
            floatPosBuffer.put(vel.x);
            floatPosBuffer.put(vel.y);
            floatPosBuffer.put(vel.z);
            floatPosBuffer.put(0f);
            
        }
        floatPosBuffer.flip();
        
        FloatBuffer floatColBuffer = ByteBuffer.allocateDirect(1024*1024*4*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); // allocate a 1 int byte buffer
        for (int i=0; i<1024*1024; i++) {
            //color
            floatColBuffer.put(1f);
            floatColBuffer.put(1f);
            floatColBuffer.put(1f);
            floatColBuffer.put(0.4f);
        }
        floatColBuffer.flip();
        
        posSSBo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, posSSBo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, floatPosBuffer, GL_STATIC_DRAW);
        
        colSSBo = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, colSSBo);
        glBufferData(GL_SHADER_STORAGE_BUFFER, floatColBuffer, GL_STATIC_DRAW);
        
//        int flags = GL_MAP_WRITE_BIT | GL_MAP_UNSYNCHRONIZED_BIT;
//        ByteBuffer mappedBuf = glMapBufferRange(GL_SHADER_STORAGE_BUFFER, 0, 1024*1024*4*12, flags, null);
//        FloatBuffer fbuf = mappedBuf.asFloatBuffer();
//        
//        for (int i=0; i<1024*1024; i++) {
//            Vector4f v = new Vector4f((float)Math.random()*200f-100f, (float)Math.random()*200f-100f, 0f, 1f);
//            floatPosBuffer.put(v.x);
//            floatPosBuffer.put(v.y);
//            floatPosBuffer.put(v.z);
//            floatPosBuffer.put(v.w);
//            
//            v = new Vector4f((float)Math.random()*10f-5f, (float)Math.random()*10f-5f, 0f, 1f);
//            floatPosBuffer.put(v.x);
//            floatPosBuffer.put(v.y);
//            floatPosBuffer.put(v.z);
//            floatPosBuffer.put(v.w);
//
//        }
//        
//        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
         
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        
        //Keyboard
        Keyboard.create();

        //Mouse
        Mouse.setGrabbed(false);
        Mouse.create();

        //OpenGL
        initGL();
        resizeGL();
   }

     public void resizeGL() throws org.lwjgl.LWJGLException {
        //2D Scene
        System.out.println("Viewport: " + Display.getWidth() + ", " + Display.getHeight());
        
        glViewport(0, 0, Display.getWidth(), Display.getHeight());
        
        trackball.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
       
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(40.0f, (float) Display.getWidth() / (float) Display.getHeight(), 100.0f, 3000.0f);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        Display.update();
    }
     
    public void initGL() {
//        initLightArrays();

        //2D Initialization
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }
    
    public void render() {
        // clear the framebuffer
        glClearColor(0.0f, 0.0f, 0.0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        
        glViewport(0, 0, Display.getWidth(), Display.getHeight());

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        
        float viewportAspect = (float) Display.getWidth() / (float) Display.getHeight();
        gluPerspective(40.0f, viewportAspect, 10.0f, 60000.0f);
       
        // looking down the z-axis
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        
        // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
        gluLookAt(0.0f, 0.0f, -800.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);
        
        // camera dolly in/out
        glTranslatef(0.0f, 0.0f, dolly);
        
        trackball.render();
        
            glUseProgram(shaderprogram);
            
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, posSSBo);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, colSSBo);
            for (int i=0; i<10; i++) {
                //org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
                org.lwjgl.opengl.GL43.glDispatchCompute(1024*1024/256, 1, 1);
                org.lwjgl.opengl.GL42.glMemoryBarrier(org.lwjgl.opengl.GL42.GL_ALL_BARRIER_BITS);
            }
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, 0);
            
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0.8f, 0.2f, 1.0f);
        glBegin(GL_LINE_LOOP);
            glVertex3f(-100f, -100f, -100f);
            glVertex3f(100f, -100f, -100f);
            glVertex3f(100f, 100f, -100f);
            glVertex3f(-100f, 100f, -100f);
        glEnd();
        glBegin(GL_LINE_LOOP);
            glVertex3f(-100f, -100f, 100f);
            glVertex3f(100f, -100f, 100f);
            glVertex3f(100f, 100f, 100f);
            glVertex3f(-100f, 100f, 100f);
        glEnd();
        glBegin(GL_LINE_LOOP);
            glVertex3f(-1000f, -1000f, 0f);
            glVertex3f(1000f, -1000f, 0f);
            glVertex3f(1000f, 1000f, 0f);
            glVertex3f(-1000f, 1000f, 0f);
        glEnd();
        glBegin(GL_LINE_LOOP);
            glVertex3f(-10000f, -10000f, 0f);
            glVertex3f(10000f, -10000f, 0f);
            glVertex3f(10000f, 10000f, 0f);
            glVertex3f(-10000f, 10000f, 0f);
        glEnd();
        
        glUseProgram(0);
            
        glBindBuffer(GL_ARRAY_BUFFER, posSSBo); 
        glVertexPointer(4, GL_FLOAT, 32, 0);
        glBindBuffer(GL_ARRAY_BUFFER, colSSBo); 
        glColorPointer(4, GL_FLOAT, 16, 0);
        
        glEnableClientState(GL_VERTEX_ARRAY); 
        glEnableClientState(GL_COLOR_ARRAY); 
        
        glDrawArrays(GL_POINTS, 0, 1024*1024);
        
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0); 
        

    }
    
    public void processMouse() {
        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();
        int dwheel = Mouse.getDWheel();

        dolly -= dwheel*5f; // zoom with mouse wheel
        
        if (mouseButton1Drag == true && Mouse.isButtonDown(0)) {
            trackball.mouseDragged(mouseX, mouseY);
        }

        if (mouseButton1Drag == true && !Mouse.isButtonDown(0)) {
            mouseButton1Drag = false;
            Mouse.setGrabbed(false);
            //System.out.println("Mouse drag end");
            trackball.mouseReleased(mouseX, mouseY);
        }
        while (Mouse.next()) {
            if (Mouse.getEventButtonState()) {
                if (Mouse.isButtonDown(0) && !mouseButton1Drag) {
                    Mouse.setGrabbed(true);
                    mouseButton1Drag = true;
                    //System.out.println("Mouse drag start");
                    trackball.mousePressed(mouseX, mouseY);
                }

            }
        }}
    
    public void processKeyboard() {
        
    }
    
    public void update() {

    }
    
    public void run() {
        while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            if (Display.isVisible()) {
                if (Display.wasResized()) {
                    try {
                    resizeGL();
                    }
                    catch(org.lwjgl.LWJGLException e) {
                        System.out.println(e);
                        System.exit(0);
                    }
                }
                processKeyboard();
                processMouse();
                update();
                render();
            } else {
                if (Display.isDirty()) {
                    render();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
            Display.update();
            Display.sync(60);
        }

    }
}
