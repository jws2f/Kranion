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

import org.fusfoundation.kranion.controller.DefaultController;
import org.fusfoundation.kranion.controller.Controller;
import org.fusfoundation.kranion.plugin.PluginFinder;
import org.fusfoundation.kranion.plugin.Plugin;
import org.fusfoundation.kranion.view.View;
import org.fusfoundation.kranion.view.DefaultView;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import org.lwjgl.BufferUtils;
import java.nio.*;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;

import org.lwjgl.PointerBuffer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.opencl.CL;

import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLContext;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10GL.*;

import java.lang.reflect.Constructor;

import java.io.File;
import java.awt.event.*;
import javax.swing.*;
import java.util.StringTokenizer;
import java.util.List;

import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.model.image.*;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;

import java.awt.SplashScreen;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import org.fusfoundation.util.CLI;
import org.lwjgl.opencl.CL10GL;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLDeviceCapabilities;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;

public class Main implements ProgressListener {

    private Model model;
    private Controller controller;
    private View view;

    private int currentBuffer = 0;
    private boolean bufferNeedsRendering[] = new boolean[3];


    public static final int DISPLAY_HEIGHT = 1024;
    public static final int DISPLAY_WIDTH = 1680;
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static float OpenGLVersion = -1f;

    public static CLContext CLcontext = null;
    public static CLCommandQueue CLqueue = null;
    
    public static final boolean GL_DEBUG = false;

    private float viewportAspect = 1f;
    private static List<Plugin> plugins = new ArrayList<>();           
    private static List<BackgroundWorker> workers = new ArrayList<>();
    public static void addBackgroundWorker(BackgroundWorker w) {
        workers.add(w);
    }
    public static void removeBackgroundWorker(BackgroundWorker w) {
        workers.remove(w);
    }
    public static void startBackgroundWorker(String name) {
        Iterator<BackgroundWorker> i = workers.iterator();
        while(i.hasNext()) {
            BackgroundWorker w = i.next();
            if (w.getName().compareTo(name) == 0) {
                w.start();
            }
        }
    }
    
    public static void stopBackgroundWorker(String name) {
        Iterator<BackgroundWorker> i = workers.iterator();
        while(i.hasNext()) {
            BackgroundWorker w = i.next();
            if (w.getName().compareTo(name) == 0) {
                w.stop();
            }
        }
    }
    
    private void doBackgroundWorkers() {
        Iterator<BackgroundWorker> i = workers.iterator();
        while (i.hasNext()) {
            BackgroundWorker w = i.next();
            w.doWorkStep();
        }
    }

    public static Model getModel() { return Main.main.model; }
    
    public static void setModel(Model model) {
        
        if (model == null) {
            model = new Model();
        }
        
        for (int i = 0; i < plugins.size(); i++) {
            plugins.get(i).release();
        }
        
        //disconnect old model if exists
        if (Main.main.model != null) {
            Main.main.model.deleteObservers();
            Main.main.model.clear();
        }
        
        
        Main.main.model = model;
        Main.main.controller.setModel(model);
        Main.main.view.setModel(model);
        
        // reconnect plugins to new model
        for (int i = 0; i < plugins.size(); i++) {
            plugins.get(i).init(Main.main.controller);
        }
            
        // reconnect observer controls to new model
        Iterator<Renderable> controls = Renderable.iterator();
        while (controls.hasNext()) {
            Renderable control = controls.next();
            if (control instanceof GUIControl) {
                model.addObserver((GUIControl)control);
            }
        }
        
        // update workers
        Iterator<BackgroundWorker> i = workers.iterator();
        while(i.hasNext()) {
            BackgroundWorker w = i.next();
            w.setModel(model);
        }
    }
    
    public static List<Plugin> getPlugins() {
        return plugins;
    }
    
    public static void updateAllControlBindings() {
        if (Main.main.model != null) {
            Iterator<Renderable> controls = Renderable.iterator();
            while (controls.hasNext()) {
                Renderable control = controls.next();
                if (control instanceof GUIControlModelBinding) {
                    ((GUIControlModelBinding)control).doBinding(Main.main.model);
                }
            }
        }        
    }
    
    public static View getView() { return Main.main.view; }
    public static void setView(View view) {
        if (Main.main.view != null) {
            Main.main.view.setModel(null);
        }
        view.setModel(Main.main.model);
        Main.main.view = view;
    }

    static {
        try {
            LOGGER.addHandler(new FileHandler("errors.log", true));
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.toString(), ex);
        }
    }

    private static Main main;
    private static long mainThreadId = Thread.currentThread().getId();

    public static void main(String[] args) {
        try {
            main = new Main();
            
            System.out.println("This is build " + getRbTok("app.version") + ":" + getRbTok("app.build"));
            System.out.println("JVM version: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));

            main.create();
            

            SplashScreen mySplash = SplashScreen.getSplashScreen();

            if (mySplash != null) {
                mySplash.close();
            }
            Thread t = new CLI(main);
            t.start();
            main.run();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        } finally {
            if (main != null) {
                main.destroy();
            }
        }
    }

    public Main() {

    }
    
    public static void setTitle(String title) {
        if (title == null || title.length() == 0) {
            Display.setTitle("Kranion");
        }
        else {
            Display.setTitle("Kranion - " + title);
        }
    }

    public static void update() {
        // If the caller is in the same thread and thus blocking the main loop,
        // we do one cycle of the main loop without input checking.
        // Main idea is to allow a progress indicator to be updated while
        // some lengthy process is running in the main thread calling this update()
        // function periodically.
        if (Thread.currentThread().getId() == mainThreadId) {
            main.handleResize();
            main.nextFrame();

            Main.checkForGLError();
        }
    }

    public void create() throws LWJGLException {

        model = new Model();

        controller = new DefaultController();
        controller.setModel(model);

        view = new DefaultView();
        
        view.setModel(model);
        view.setPropertyPrefix("Model.Attribute");
        view.setController(controller);
        view.setTag("DefaultView");

        controller.setView(view);
        
        //Display
        DisplayMode[] modes = Display.getAvailableDisplayModes();

        DisplayMode chosenMode = null;
        int maxDisplayWidth = 0;
        for (int i = 0; i < modes.length; i++) {
            DisplayMode current = modes[i];
            System.out.println(current.getWidth() + "x" + current.getHeight() + "x"
                    + current.getBitsPerPixel() + " " + current.getFrequency() + "Hz");
            if (current.getBitsPerPixel() == 32 && current.getWidth() == 2560 && current.getHeight() == 1440 && current.getFrequency() == 60) {
                chosenMode = current;
            }
            else if (current.getBitsPerPixel() == 32 && current.getFrequency() >= 60) {
                if (current.getWidth() > maxDisplayWidth) {
                    maxDisplayWidth = current.getWidth();
                    chosenMode = current;
                    if (maxDisplayWidth >= DISPLAY_WIDTH) {
                        break;
                    }
                }
            }
        }
        DisplayMode mode = null;
        
        if (chosenMode == null) {
            System.out.println("Didn't find a display mode we like. Trying default...");
            mode = new DisplayMode(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        }
        else {
            mode = chosenMode;
        }
        
     
//        mode = chosenMode;
        System.out.println("Display: " + mode.getBitsPerPixel() + " bpp");
        Display.setDisplayMode(mode);
//        Display.setFullscreen(true);
        System.out.println("Display: mode set");
        Display.setResizable(true);
        Display.setTitle("Kranion");

        System.out.println("Setting pixel format...");
        PixelFormat pixelFormat = new PixelFormat(24, 8, 24, 8, 1);
        org.lwjgl.opengl.ContextAttribs contextAtribs = new ContextAttribs(2, 1);
        contextAtribs.withForwardCompatible(true);

        try {
            ByteBuffer[] list = new ByteBuffer[3];
            InputStream rstm = this.getClass().getResourceAsStream("/org/fusfoundation/kranion/images/icon32.png");
            BufferedImage img = ImageIO.read(rstm);
            list[0] = this.convertToByteBuffer(img);
            rstm = this.getClass().getResourceAsStream("/org/fusfoundation/kranion/images/icon64.png");
            img = ImageIO.read(rstm);
            list[1] = this.convertToByteBuffer(img);
            rstm = this.getClass().getResourceAsStream("/org/fusfoundation/kranion/images/icon256.png");
            img = ImageIO.read(rstm);
            list[2] = this.convertToByteBuffer(img);
            Display.setIcon(list);
        } catch (Exception e) {
            System.out.println("Failed to set window icon.");
        }

        System.out.println("Creating display...");
        Display.create(pixelFormat, contextAtribs);
        //Display.create();

        System.out.println("GL Vendor: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR));
        System.out.println("GL Version: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
        System.out.println("GLSL Language Version: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION));
        System.out.println("GL Renderer: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));

        checkGLSupport();

        checkCLSupport();

        //Keyboard
        Keyboard.create();

        //Mouse
        Mouse.setGrabbed(false);
        Mouse.create();

        //OpenGL
        initGL();
        resizeGL();

        view.create();

        // load plugins
        try {
            PluginFinder pluginFinder = new PluginFinder();
            pluginFinder.search("plugins");
            plugins = pluginFinder.getPluginCollection();
            for (int i = 0; i < plugins.size(); i++) {
                plugins.get(i).init(controller);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        resizeGL();
//        resizeGL();

    }

    private ByteBuffer convertToByteBuffer(BufferedImage image) {
        byte[] buffer = new byte[image.getWidth() * image.getHeight() * 4];
        int counter = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                int colorSpace = image.getRGB(j, i);
                buffer[counter + 0] = (byte) ((colorSpace << 8) >> 24);
                buffer[counter + 1] = (byte) ((colorSpace << 16) >> 24);
                buffer[counter + 2] = (byte) ((colorSpace << 24) >> 24);
                buffer[counter + 3] = (byte) (colorSpace >> 24);
                counter += 4;
            }
        }
        return ByteBuffer.wrap(buffer);
    }

    private void checkCLSupport() {
        try {
            // Initialize OpenCL and create a context and command queue
            CL.create();
            System.out.println("\n****************");
            System.out.println("CL created");

            System.out.println(CLPlatform.getPlatforms().size() + " platforms found.");
            CLPlatform platform = null;
            List<CLDevice> devices = null;
            CLDevice selectedDevice = null;
            IntBuffer errcode_ret = BufferUtils.createIntBuffer(1);
                        
            boolean success = false;
            for (int p = 0; p < CLPlatform.getPlatforms().size(); p++) {
                platform = CLPlatform.getPlatforms().get(p);
                System.out.println("Platform : " + platform.getInfoString(CL_PLATFORM_NAME) + " version: " + platform.getInfoString(CL_PLATFORM_VERSION));

//                PointerBuffer ctxProps = BufferUtils.createPointerBuffer(3);
//                ctxProps.put(CL_CONTEXT_PLATFORM).put(platform).put(0).flip();

                devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
                
                for (int d = 0; d < devices.size(); d++) {

                    System.out.println(devices.size() + " GPU devices found.");

                    // long context = clCreateContext(platform, devices, null, null, null);
                    //CLContext context = org.lwjgl.opencl.CLContext.createFromType(platform, Thread.currentThread().getId(), null, Display.getDrawable(), errcode_ret);
                    try {
                        List<CLDevice> deviceCandidate = new ArrayList<CLDevice>();
                        deviceCandidate.add(devices.get(d));

                        CLcontext = org.lwjgl.opencl.CLContext.create(platform, deviceCandidate, null, Display.getDrawable(), errcode_ret);
                        org.lwjgl.opencl.Util.checkCLError(errcode_ret.get(0));
                    } catch (Exception e) {
                        System.out.println("Couldn't create shareable OpenCL context.");
                    }
                    if (errcode_ret.get(0) == CL_SUCCESS) {
                        System.out.println("CL context created");
                        selectedDevice = devices.get(d);
                        success = true;
                    }
                    
                    if (success && selectedDevice != null) {
                        System.out.println("Device : " + selectedDevice.getInfoString(CL_DEVICE_NAME));
                        System.out.println("Device CL extensions: " + selectedDevice.getInfoString(CL_DEVICE_EXTENSIONS));

                        try {
                            CLDeviceCapabilities abilities = CLCapabilities.getDeviceCapabilities(selectedDevice);
                            System.out.println("3d_image_writes: " + abilities.CL_KHR_3d_image_writes);
                            System.out.println("gl_sharing: " + abilities.CL_KHR_gl_sharing);

                            errcode_ret.clear();

                            org.lwjgl.opencl.CLCommandQueue queue = org.lwjgl.opencl.CL10.clCreateCommandQueue(CLcontext, selectedDevice, 0 /*org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE*/, errcode_ret);
                            // checkCLError throw a CLExcepetion if the error code does not equal CL_SUCCESS. This exception should be caught and all currently created resources released. See later. 
                            org.lwjgl.opencl.Util.checkCLError(errcode_ret.get(0));
                            CLqueue = queue;

                        } catch (Exception e) {
                            System.out.println("Failed to create a sharable CL ccommand queue with this device.");
                            success = false;
                        }
                    }
                }

                if (success) {
                    break;
                }
            }
           


 
   
//            org.lwjgl.opencl.CL10.clReleaseContext(CLcontext);    
//            org.lwjgl.opencl.CL.destroy();
//            System.out.println("CL context released, CL shutdown");

            System.out.println("****************\n");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("*** Problem initializing OpenCL");
        }

    }

    private void checkGLSupport() {
        String vendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
        String version = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        int nMaxTexUnits = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        int nMaxCombinedTexUnits = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);

        try {
            float versionVal = 0f;
            System.out.println("  Texture unit count = " + nMaxTexUnits + ", Max combined textures = " + nMaxCombinedTexUnits);
            StringTokenizer tok = new StringTokenizer(version, ". ");
            if (tok.hasMoreElements()) {
                versionVal = Float.parseFloat(tok.nextToken());
            }
            if (tok.hasMoreElements()) {
                versionVal += Float.parseFloat(tok.nextToken()) / 10f;
            }

            Main.OpenGLVersion = versionVal;

            if (versionVal < 4.5f) {
                JOptionPane.showMessageDialog(null, "OpenGL 4.5 or later required.\n\nYou have:\n" + vendor + "\n" + version);
//            System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void destroy() {
        //Methods already check if created before destroying.
        Mouse.destroy();
        Keyboard.destroy();
        Display.destroy();
    }

    //----------- Variables added for Lighting Test -----------//
    private FloatBuffer matSpecular;
    private FloatBuffer lightPosition;
    private FloatBuffer whiteLight;
    private FloatBuffer lModelAmbient;

    //------- Added for Lighting Test----------//
    private void initLightArrays() {
        matSpecular = BufferUtils.createFloatBuffer(4);
        matSpecular.put(0.3f).put(0.3f).put(0.3f).put(1.0f).flip();

        lightPosition = BufferUtils.createFloatBuffer(4);
        lightPosition.put(150.0f).put(150.0f).put(400.01f).put(0.0f).flip();
        //lightPosition.put(0.0f).put(700.0f).put(700.01f).put(0.0f).flip();

        whiteLight = BufferUtils.createFloatBuffer(4);
        whiteLight.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();

        lModelAmbient = BufferUtils.createFloatBuffer(4);
        lModelAmbient.put(0.1f).put(0.1f).put(0.1f).put(1.0f).flip();
    }

    public void initGL() {
        initLightArrays();

        //2D Initialization
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
//        glEnable(GL_POLYGON_SMOOTH); // EVIL
//        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        glShadeModel(GL_SMOOTH);
        glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);  // sets specular material color
        glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 50.0f);  // sets shininess

        // lighting setup
        glLight(GL_LIGHT0, GL_POSITION, lightPosition);  // sets light position
        glLight(GL_LIGHT0, GL_SPECULAR, whiteLight);  // sets specular light to white
        glLight(GL_LIGHT0, GL_DIFFUSE, whiteLight);  // sets diffuse light to white
        glLightModel(GL_LIGHT_MODEL_AMBIENT, lModelAmbient);  // global ambient light 
        glEnable(GL_LIGHTING);  // enables lighting
        // enables light0
        glEnable(GL_LIGHT0);

        glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
        glLightModeli(GL_LIGHT_MODEL_LOCAL_VIEWER, GL_TRUE);

        glEnable(GL_COLOR_MATERIAL);  // enables opengl to use glColor3f to define material color
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);  // tell opengl glColor3f effects the ambient and diffuse properties of material

    }

    public void resizeGL() throws org.lwjgl.LWJGLException {
        //2D Scene
        System.out.println("Viewport: " + Display.getWidth() + ", " + Display.getHeight());

        if (Display.getWidth() <= 0 || Display.getHeight() <= 0) {
            return;
        }

        glViewport(0, 0, Display.getWidth(), Display.getHeight());

//        trackball.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
//        registerBall.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
//        registerBall2.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        viewportAspect = (float) Display.getWidth() / (float) Display.getHeight();
        gluPerspective(40.0f, viewportAspect, 100.0f, 100000.0f);
        
//        org.lwjgl.util.vector.Matrix4f perspective = new org.lwjgl.util.vector.Matrix4f();
//        FloatBuffer matbuf = BufferUtils.createFloatBuffer(16);
//        glGetFloat(GL_PROJECTION_MATRIX, matbuf);
//        perspective.load(matbuf);        
//        System.out.println("MAIN GL perspective mat:\n" + perspective);
 
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        view.doLayout();        
    }

    public void run() {
        if (view.doOkCancelMessageBox("RESEARCH USE ONLY", "Kranion is intended for research purposes only.")) {
            while (true) {

//                handleResize();
//
//                view.processInput();
//
//                nextFrame();
//
//                Main.checkForGLError();
                messageLoop();

                if (Display.isCloseRequested()) {
//                int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit Kranion", JOptionPane.YES_NO_OPTION);
//                if (result == JOptionPane.YES_OPTION) {
//                    break;
//                }
                    if (view.okToExit()) {
                        break;
                    }
                }

            }
        };

        view.release();
        
        if (CLcontext != null) {
            org.lwjgl.opencl.CL10.clReleaseContext(CLcontext);
        }
        org.lwjgl.opencl.CL.destroy();

    }

    public void handleResize() {
        if (Display.wasResized()) {
            try {
                resizeGL();
            } catch (org.lwjgl.LWJGLException e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }

    public static void processNextFrame() {
        // Spin the event loop for one frame.
        // Modal dialog boxes and others need this
        if (Main.main != null) {
            Main.main.messageLoop();
        }
    }
    
    protected void messageLoop() {
            handleResize();

            view.processInput();

            nextFrame();

            Main.checkForGLError();    
    }
    
    public void nextFrame() {
        boolean wasRendered = false;
        
        if (Display.isVisible()) {
            // manage rendering into double buffer
            if (view.getIsDirty()) {
                bufferNeedsRendering[0] = true;
                bufferNeedsRendering[1] = true;
                bufferNeedsRendering[2] = true;
            }

            if (bufferNeedsRendering[currentBuffer]) {
                bufferNeedsRendering[currentBuffer] = false;

                // Render the scene
                view.render();
                
                wasRendered = true;
            }
            else {
                doBackgroundWorkers();                
            }

        }
        
        // This needs to be here for window resizing to get handled correctly
        // swap display buffers
        Display.update(); 
        
        
//        Display.processMessages(); // Unnecessary

        currentBuffer = (currentBuffer + 1) % 3; // keep track of front/back buffers

        try {
            if (!wasRendered) { // if no rendering updates, sleep a bit
                Thread.sleep(50);
            }
            else {
                Display.sync(60); // not sure this makes any difference
            }
        }
        catch(Exception e) {}        
    }
    
    public static void checkForGLErrorAndThrow() {
        int error = checkForGLError();
        if (error != GL_NO_ERROR) {
            throw new org.lwjgl.opengl.OpenGLException(error);
        }
    }

    public static void printStackTrace() {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        for (int i = 2; i < traces.length; i++) {
            System.out.println("\t" + traces[i]);
        }
    }

    public static int checkForGLError() {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.out.println("GL ERROR DETECTED.");
            switch (error) {
                case GL_INVALID_ENUM:
                    System.out.println("GL_INVALID_ENUM");
                    break;
                case GL_INVALID_VALUE:
                    System.out.println("GL_INVALID_VALUE");
                    break;
                case GL_INVALID_OPERATION:
                    System.out.println("GL_INVALID_OPERATION");
                    break;
                case org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION:
                    System.out.println("GL_INVALID_FRAMEBUFFER_OPERATION");
                    break;
                case GL_OUT_OF_MEMORY:
                    System.out.println("GL_OUT_OF_MEMORY");
                    break;
                case GL_STACK_OVERFLOW:
                    System.out.println("GL_STACK_OVERFLOW");
                    break;
                case GL_STACK_UNDERFLOW:
                    System.out.println("GL_STACK_UNDERFLOW");
                    break;
                default:
                    System.out.println("UNKNOWN GL ERROR: " + error);
            }
            printStackTrace();
        }
        return error;
    }
    
    @Override
    public void percentDone(String msg, int percent) {
//        System.out.println(msg + " - " + percent + "%");
    }

    public static void glPushMatrix() {
        org.lwjgl.opengl.GL11.glPushMatrix();

        if (GL_DEBUG && Main.checkForGLError() != GL_NO_ERROR) {
            System.out.println("MODELVIEW stack depth: " + glGetInteger(GL_MODELVIEW_STACK_DEPTH));
            System.out.println("MODELVIEW max stack depth: " + glGetInteger(GL_MAX_MODELVIEW_STACK_DEPTH));
            System.out.println("PROJECTIONVIEW stack depth: " + glGetInteger(GL_PROJECTION_STACK_DEPTH));
            System.out.println("PROJECTIONVIEW max stack depth: " + glGetInteger(GL_MAX_PROJECTION_STACK_DEPTH));
        }
    }

    public static void glPopMatrix() {
        org.lwjgl.opengl.GL11.glPopMatrix();

        if (GL_DEBUG && Main.checkForGLError() != GL_NO_ERROR) {
            System.out.println("MODELVIEW stack depth: " + glGetInteger(GL_MODELVIEW_STACK_DEPTH));
            System.out.println("MODELVIEW max stack depth: " + glGetInteger(GL_MAX_MODELVIEW_STACK_DEPTH));
            System.out.println("PROJECTIONVIEW stack depth: " + glGetInteger(GL_PROJECTION_STACK_DEPTH));
            System.out.println("PROJECTIONVIEW max stack depth: " + glGetInteger(GL_MAX_PROJECTION_STACK_DEPTH));
        }
    }

    public static void glPushAttrib(int bitmask) {
        org.lwjgl.opengl.GL11.glPushAttrib(bitmask);

        if (GL_DEBUG && Main.checkForGLError() != GL_NO_ERROR) {
            System.out.println("ATTRIB stack depth: " + glGetInteger(GL_ATTRIB_STACK_DEPTH));
            System.out.println("ATTRIB max stack depth: " + glGetInteger(GL_MAX_ATTRIB_STACK_DEPTH));
        }
    }

    public static void glPopAttrib() {
        org.lwjgl.opengl.GL11.glPopAttrib();

        if (GL_DEBUG && Main.checkForGLError() != GL_NO_ERROR) {
            System.out.println("ATTRIB stack depth: " + glGetInteger(GL_ATTRIB_STACK_DEPTH));
            System.out.println("ATTRIB max stack depth: " + glGetInteger(GL_MAX_ATTRIB_STACK_DEPTH));
        }
    }
    
    final static ResourceBundle appResourceBundle
            = ResourceBundle.getBundle("version");

    public static final String getRbTok(String propToken) {
        String msg = "";
        try {
            msg = appResourceBundle.getString(propToken);
        } catch (MissingResourceException e) {
            System.err.println("Token ".concat(propToken).concat(" not in Propertyfile!"));
        }
        return msg;
    }
}
