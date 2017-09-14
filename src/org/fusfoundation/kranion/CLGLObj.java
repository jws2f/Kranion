/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.kranion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.lwjgl.BufferUtils;
import org.lwjgl.opencl.api.Filter;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.Sys;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import static org.lwjgl.opencl.CL10.CL_DEVICE_NAME;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_LOG;
import static org.lwjgl.opencl.CL10.CL_PROGRAM_BUILD_STATUS;
import static org.lwjgl.opencl.CL10.CL_QUEUE_PROFILING_ENABLE;
import static org.lwjgl.opencl.CL10.CL_SUCCESS;
import static org.lwjgl.opencl.CL10.clEnqueueNDRangeKernel;
import static org.lwjgl.opencl.CL10.clEnqueueWaitForEvents;
import static org.lwjgl.opencl.CL10.clFinish;
import static org.lwjgl.opencl.CL10.clReleaseMemObject;
import org.lwjgl.opencl.CL10GL;
import static org.lwjgl.opencl.CL10GL.clEnqueueAcquireGLObjects;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLDeviceCapabilities;
import org.lwjgl.opencl.CLEvent;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import static org.lwjgl.opencl.KHRGLEvent.clCreateEventFromGLsyncKHR;
import static org.lwjgl.opengl.ARBCLEvent.glCreateSyncFromCLeventARB;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glTexCoord3f;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
import static org.lwjgl.opengl.GL32.glFenceSync;
import static org.lwjgl.opengl.GL32.glWaitSync;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

/*
 * @author labramson
 */
public class CLGLObj {
    private Drawable drawable; //drawable for the CL context
    private CLPlatform platform; //platform for the CL context
    private Filter<CLDevice> glSharingFilter, write3DImgFilter;  //filters for the devices used in the GPU
    private List<CLDevice> devices; //list of all the devices available
    private static int deviceType = CL10.CL_DEVICE_TYPE_GPU; //default device type
    private CLContext context; //CL context
    private CLCommandQueue queue; //command queue needed for the context
    private CLKernel kernel; //kernel used to run the source code that does the shading
    private CLProgram program; //program used to run the kernel
    private static CLMem[] glBuffers;  //array of clm for input texture
    private static CLMem[] glBuffersOut;  //array of clm for output texture
    private CLEvent glEvent;  //gl event 
    private CLEvent[] clEvents; //array of cl events for tracking
    private GLSync glSync; //glsync so that cl & gl dont cause race condition
    private GLSync[] clSyncs; //array of gl sync objects for 
    private boolean syncCLtoGL = false; // true if we can make CL wait on sync objects generated from GL.
    private boolean syncGLtoCL = false; // true if we can make GL wait on events generated from CL queues.
    private final boolean doublePrecision = false;  //doubles used instead of floats
    private final boolean useTextures = false;  //indication of if textures or bufferes are used for the texture
    private static boolean buffersInitialized;  //buffers for the textrue initialized
    private boolean drawSeparator;  //DID NOT USE, NOT NECESSARY
    private boolean rebuild;  //boolean for rerendering
    private final PointerBuffer kernel2DGlobalWorkSize = BufferUtils.createPointerBuffer(3);  //the global work size of the kernel
    private final PointerBuffer syncBuffer = BufferUtils.createPointerBuffer(1);  //buffer for dealing with gl cl sync

    public CLGLObj() {
        try {
//            CL.create();
//
//            this.drawable = Display.getDrawable();
//
//            this.platform = CLPlatform.getPlatforms().get(0);
//
//            this.glSharingFilter = (final CLDevice device) -> {
//                CLDeviceCapabilities abilities = CLCapabilities.getDeviceCapabilities(device);
//                System.out.println("APPLE_gl_sharing: " + abilities.CL_APPLE_gl_sharing + "  KHR_gl_sharing" + abilities.CL_KHR_gl_sharing);
//                if (abilities.CL_APPLE_gl_sharing) {
//                    return abilities.CL_APPLE_gl_sharing;
//                } else if (abilities.CL_KHR_gl_sharing) {
//                    return abilities.CL_KHR_gl_sharing;
//                } else {
//                    return false;
//                }
//            };
//            this.write3DImgFilter = (final CLDevice device) -> {
//                CLDeviceCapabilities abilities = CLCapabilities.getDeviceCapabilities(device);
//                System.out.println("3d_image_writes: " + abilities.CL_KHR_3d_image_writes);
//                return abilities.CL_KHR_gl_sharing;
//            };
//
//            this.devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU, glSharingFilter);
//            checkDevices(devices);

//            this.context = CLContext.create(platform, devices, null, drawable, null);
            this.context = Main.CLcontext;

            this.queue = Main.CLqueue; //CL10.clCreateCommandQueue(context, context.getInfoDevices().get(0), 0/*CL_QUEUE_PROFILING_ENABLE*/, null);
            this.queue.checkValid();

            buildProgram(context);

            this.kernel = CL10.clCreateKernel(this.program, "imgTest2", null);
        } catch (Exception e) {
            System.out.println("*** Problem creating CL context");
            e.printStackTrace();
        }
        
        syncStatus(this.context);
    }

    private void checkDevices(List<CLDevice> devices) {
        if (devices == null) {
            deviceType = CL10.CL_DEVICE_TYPE_CPU;
            devices = platform.getDevices(deviceType, glSharingFilter);
            if (devices == null) {
                throw new RuntimeException("No OpenCL devices found with gl_sharing support.");
            }
        }
        System.out.println(devices.size() + " GPU devices found.");
    }

    private void buildProgram(CLContext context) {
        if (this.program != null) {
            CL10.clReleaseProgram(this.program);
        }

        String kernel_3DImage = getKernelSource("shaders/ImageGradientVolume.kernel.txt");
        
        System.out.println(kernel_3DImage);

        this.program = CL10.clCreateProgramWithSource(context, kernel_3DImage, null);

        final CLDevice device = queue.getCLDevice();
        final StringBuilder options = new StringBuilder(useTextures ? "-D USE_TEXTURE" : "");
        final CLDeviceCapabilities caps = CLCapabilities.getDeviceCapabilities(device);

        if (doublePrecision) {
            //cl_khr_fp64 verson of double precision floating point math
            options.append(" -D DOUBLE_FP");

            //amd's verson of double precision floating point math
            if (!caps.CL_KHR_fp64 && caps.CL_AMD_fp64) {
                options.append(" -D AMD_FP\n");
            } else {
                options.append("\n");
            }
        }
        System.out.println("Compiling kernel for: " + device.getInfoString(CL_DEVICE_NAME));
        System.out.println("OpenCL COMPILER OPTIONS: " + options);
        
        try {
            int error = CL10.clBuildProgram(this.program, device, options, null);
            if (error != CL_SUCCESS) {
                System.out.println("clBuildProgram failed: " + error);
            }
        } finally {
            System.out.println("BUILD LOG: " + this.program.getBuildInfoString(device, CL_PROGRAM_BUILD_STATUS) + "\n" + this.program.getBuildInfoString(device, CL_PROGRAM_BUILD_LOG));
        }
    }

    public void syncStatus(CLContext context) {
        final ContextCapabilities abilities = GLContext.getCapabilities();

        // GL3.2 or ARB_sync implied
        syncGLtoCL = false;//abilities.GL_ARB_cl_event;
        if (syncGLtoCL) {
            clEvents = new CLEvent[1];
            clSyncs = new GLSync[1];
            System.out.println("\nGL to CL sync: Using OpenCL events");
        } else {
            System.out.println("\nGL to CL sync: Using clFinish");
        }

        // Detect CLtoGL synchronization method
        syncCLtoGL = false; //abilities.OpenGL32 || abilities.GL_ARB_sync;
        if (syncCLtoGL) {
            for (CLDevice device : context.getInfoDevices()) {
                if (!CLCapabilities.getDeviceCapabilities(device).CL_KHR_gl_event) {
                    syncCLtoGL = false;
                    break;
                }
            }
        }
        if (syncCLtoGL) {
            System.out.println("CL to GL sync: Using OpenGL sync objects");
        } else {
            System.out.println("CL to GL sync: Using glFinish");
        }
    }

    private String getKernelSource(String fileName) {
        InputStream inputStream = this.getClass().getResourceAsStream(fileName);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (IOException ex) {
            Logger.getLogger(CLGLObj.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void setKernelParams(CLMem glBuffers, CLMem glBuffersOut, float xres, float yres, float zres) {
        kernel.setArg(0, glBuffers)
                .setArg(1, glBuffersOut)
                .setArg(2, xres)
                .setArg(3, yres)
                .setArg(4, zres);
    }

    public static void initCLTexture(CLContext context, int textureName, String textureType, ImageVolume image) {
        if (glBuffers == null) {
            glBuffers = new CLMem[1];
        } else {
            for (CLMem mem : glBuffers) {
//                clReleaseMemObject(mem);
            }
        }
        
        if (glBuffersOut == null) {
            glBuffersOut = new CLMem[1];
        } else {
            for (CLMem mem : glBuffersOut) {
                if (mem != null) {
//                    clReleaseMemObject(mem);
                }
            }
        }

        int iWidth = image.getDimension(0).getSize();
        int iHeight = image.getDimension(1).getSize();
        int iDepth = image.getDimension(2).getSize();

        if (textureType.compareToIgnoreCase("input") == 0) {
            glBuffers[0] = CL10GL.clCreateFromGLTexture3D(context, CL10.CL_MEM_READ_ONLY, GL_TEXTURE_3D, 0, textureName, null);
            //Texture texture = new Texture(iWidth, iHeight, iDepth, GL_R16F, null);
            //inTexture = texture;
        } else if (textureType.compareToIgnoreCase("output") == 0) {
            glBuffersOut[0] = CL10GL.clCreateFromGLTexture3D(context, CL10.CL_MEM_WRITE_ONLY, GL_TEXTURE_3D, 0, textureName, null);
            buffersInitialized = true;
            //Texture texture = new Texture(iWidth, iHeight, iDepth, GL_RGBA16F, null);
            //outTexture = texture;
        }
        
        //UNBINDS THE TEXTURE
        glBindTexture(GL_TEXTURE_3D, 0);
    }

    //DISPLAYS THE RESULTS
    public void display(int textureName, String textureType, ImageVolume image){
        //CHECKS TO MAKE SURE ALL GL EVENTS HAVE COMPLETED
        if (syncCLtoGL && glEvent != null) {
            clEnqueueWaitForEvents(queue, glEvent);
        } else {
            glFinish();
        }

        //IF THE GL TEXTURE BUFFERS HAVE NOT BEEN INITIALIZED
        if (!buffersInitialized) {
            initCLTexture(context, textureName, textureType, image);
            setKernelParams(glBuffers[0], glBuffersOut[0], image.getDimension(0).getSampleWidth(0), image.getDimension(1).getSampleWidth(0), image.getDimension(2).getSampleWidth(0));
        }

        //IF CHANGES OCCURED, AND NEEDS TO REBUILD PROGRAM & KERNEL
        if (rebuild) {
            buildProgram(context);
            setKernelParams(glBuffers[0], glBuffersOut[0], image.getDimension(0).getSampleWidth(0), image.getDimension(1).getSampleWidth(0), image.getDimension(2).getSampleWidth(0));
        }

        int iWidth = image.getDimension(0).getSize();
        int iHeight = image.getDimension(1).getSize();
        int iDepth = image.getDimension(2).getSize();
        //SETS THE WORKSIZE OF THE KERNEL
        kernel2DGlobalWorkSize.put(0, iWidth).put(1, iHeight).put(2, iDepth);
        long start = 0;
        //GETS THE GL OBJECTS 
        for (int i = 0; i < 1; i++) {
            // acquire GL objects, and enqueue a kernel with a probe from the list
            clEnqueueAcquireGLObjects(queue, glBuffers[i], null, null);
            clEnqueueAcquireGLObjects(queue, glBuffersOut[i], null, null);
            start = (Sys.getTime() * 1000) / Sys.getTimerResolution();
            clEnqueueNDRangeKernel(queue, kernel, 3,
                    null,
                    kernel2DGlobalWorkSize,
                    null,
                    null, null);
            CL10GL.clEnqueueReleaseGLObjects(queue, glBuffers[i], null, syncGLtoCL ? syncBuffer : null);
            CL10GL.clEnqueueReleaseGLObjects(queue, glBuffersOut[i], null, syncGLtoCL ? syncBuffer : null);
            if (syncGLtoCL) {
                clEvents[i] = queue.getCLEvent(syncBuffer.get(0));
                clSyncs[i] = glCreateSyncFromCLeventARB(queue.getParent(), clEvents[i], 0);
            }
        }

        // block until done (important: finish before doing further gl work)
        if (!syncGLtoCL) {
            for (int i = 0; i < 1; i++) {
                clFinish(queue);
            }
        }

        //RENDER THE TEXTURE
        //render(start, textureName);
    }

    private void render(long start, int textureName) {
        if (syncGLtoCL) {
            glWaitSync(clSyncs[0], 0, 0);
        }

        //SETS GL SETTINGS
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); //CLEARS SCREEN EACH LOOP
        glEnable(GL_DEPTH_TEST); //DISABLES DEPTH TEST
        glEnable(GL_TEXTURE_3D); //ENABLES GL_TEXTURE_3D

        //BIND THE TEXTURE
        glBindTexture(GL_TEXTURE_3D, textureName);

        //DRAW A CUBE WITH MAPPED TEXTURE
        glBegin(GL_QUADS);

        glTexCoord3f(0f, 0f, 0.5f);
        glVertex3f(250, 250, 0); //upper left
        glTexCoord3f(0f, 1.0f, 0.5f);
        glVertex3f(250, 450, 0);  //upper right
        glTexCoord3f(1.0f, 1.0f, 0.5f);
        glVertex3f(450, 450, 0); //bottom right
        glTexCoord3f(1.0f, 0f, 0.5f);
        glVertex3f(450, 250, 0); //bottom left

        glEnd();

        glBindTexture(GL_TEXTURE_3D, 0);
        glDisable(GL_TEXTURE_3D);
        System.out.println("Time: " + ((Sys.getTime() * 1000) / Sys.getTimerResolution() - start));

        //CHECKING SYNC
        if (syncCLtoGL) {
            glSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            glEvent = clCreateEventFromGLsyncKHR(context, glSync, null);
        }
    }

    public CLContext getContext() {
        return context;
    }

    public CLCommandQueue getQueue() {
        return queue;
    }
    
    public CLMem[] getGLBuffers(){
        return glBuffers;
    }
    
    public CLMem[] getGLBuffersOut(){
        return glBuffersOut;
    }
}
