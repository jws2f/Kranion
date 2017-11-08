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

/*
 * ImageCanvasjava
 *
 * Copyright 2005, John W. Snell
 */
//import com.sun.prism.impl.BufferUtil;

import java.io.*;
import java.nio.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.math.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
//import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.BufferUtils;

import org.fusfoundation.kranion.model.image.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

/**
 *
 * @author jsnell
 */
public class ImageCanvas3D extends GUIControl {

    private ImageVolume CTimage, MRimage, TransferFunction, OverlayImage=null;
    private int imageChannel = 0;
    private BufferedImage overlay;
//    private float rotx = 0.0f;
//    private float roty = 0.0f;
    private float zoom = 1.0f;
//    private int ct_textureName = -1;
//    private int mr_textureName = -1;
//    private int[] lut = new int[256 * 256];
//    private Object pixels;
    private int ct_center = 1024, ct_window = 1024;
    private int mr_center = 1024, mr_window = 1024;
    private float ct_rescale_slope = 1f, ct_rescale_intercept = 0f;
    private float mr_rescale_slope = 1f, mr_rescale_intercept = 0f;
    private int imageTime = 0;
    private double panx = 0.0, pany = 0.0;
    private double rotation = 0.0;
    private float ct_threshold = 0.0f, mr_threshold = 0.0f;
    private int currentOverlayFrame = 0;
//    private int iWidth, iHeight, texWidth, texHeight, texDepth;
    private int width, height;
//    private boolean needsTexture = true;
//    private boolean needsRendering = true;
//    private boolean needsLUT = true;
//    private float[] target = new float[3];
    
    public FloatBuffer matrixBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f ctTexMatrix = new Matrix4f();
    public Matrix4f mrTexMatrix = new Matrix4f();

    
//    public Vector3f ctImageOrientationX = new Vector3f();
//    public Vector3f ctImageOrientationY = new Vector3f();
//    public Vector3f ctImageOrientationZ = new Vector3f();    
//    public Vector3f ctImageOriginPosition = new Vector3f();    
//    private Quaternion ctImageOrientation = new Quaternion();
//
//    public Vector3f mrImageOrientationX = new Vector3f();
//    public Vector3f mrImageOrientationY = new Vector3f();
//    public Vector3f mrImageOrientationZ = new Vector3f();    
//    public Vector3f mrImageOriginPosition = new Vector3f();    
//    private Quaternion mrImageOrientation = new Quaternion();
    
    private int lutTextureName=0;

    // we use this to rotate our imaging plane in the 3D texture space
    private Trackball trackball;
    private Vector3f centerOfRotation = new Vector3f(0, 0, 0);

    private static int centerUniform = 0, windowUniform = 0;
    private static int mr_thresholdUniform = 0, ct_thresholdUniform = 0;
//    private static int shaderprogram = 0;
//    private static int shaderprogram2D = 0;
    private ShaderProgram shader, pressureShader, thermometryShader, shader2D;
    
    private boolean doVolumeRender = false;
    private boolean showMR = true;
    private int fgVolSlices = 0;
    
    private boolean showPressure = false;
    private boolean showThermometry = false;

    private int orientation = 0; // temp hack for now, playing with tex matrix ops
    
    // moving volume rendering slices to VBOs
    private int vertsID, texcoordsID;


    /**
     * Creates a new instance of ImageCanvasGL
     */
    public ImageCanvas3D() {
        TransferFunction = null;
        
        renderOverlay();
        
    }
   
    public void setTransferFunction(ImageVolume4D lut) {
        if (TransferFunction != lut) {
            setIsDirty(true);
        }
        TransferFunction = lut; // TODO: should check for dimensionality and size
    }
    
//    public void updateTransferFunctionAlpha(float[] values)
//    {
//        float[] pixels = (float[])TransferFunction.getData();
//        for (int i=0; i<pixels.length && i<values.length; i++) {
//            pixels[i*4+3] = values[i];
//        }
//    }

    public void setOrientation(int orient) {
        if (orientation != orient) {
            setIsDirty(false);
        }
        orientation = orient;
    }
    
    public void setCTThreshold(float value) {
        if (ct_threshold != value) {
            setIsDirty(false);
        }
        ct_threshold = value;
    }
    
    public void setMRThreshold(float value) {
        if (mr_threshold != value) {
            setIsDirty(true);
        }
        mr_threshold = value;
    }

    public void setTrackball(Trackball tb) {
        trackball = tb;
    }
    
    public void setTextureRotatation(Vector3f rotationOffset, Trackball tb) {
        if (trackball == null || !centerOfRotation.equals(rotationOffset) || trackball.getCurrent() != tb.getCurrent()) {
            setIsDirty(true);
        }
        centerOfRotation = rotationOffset;
        trackball = tb;
    }

//    public void setTarget(float x, float y, float z) {
//        if (target[0] != x || target[1] != y || target[2] != z) {
//            setIsDirty(true);
//        }
//        target[0] = x;
//        target[1] = y;
//        target[2] = z;
//        if (this.CTimage != null) {
//            CTimage.setAttribute("volumeTarget", target);
//        }
//        //////TODO////////////this.workspace.repaint();
//    }
    
    public void setVolumeRender(boolean f) {
        if (doVolumeRender != f) {
            setIsDirty(true);
        }
        doVolumeRender = f;
    }
    
    public boolean getVolumeRender() { return doVolumeRender; }

//    public float[] getTarget() {
//        return target;
//    }
    
    public void setShowMR(boolean bShow) {
        if (showMR != bShow) {
            setIsDirty(true);
        }
        showMR = bShow;
    }
    
    public boolean getShowMR() {
        return showMR;
    }
    
    public void setShowPressure(boolean bShow) {
        if (showPressure != bShow) {
            setIsDirty(true);
        }
        showPressure = bShow;
        if (showPressure) {
            showThermometry = false;
        }
    }
    
    public void setShowThermometry(boolean bShow) {
        if (showThermometry != bShow) {
            setIsDirty(true);
        }
        showThermometry = bShow;
        if (showThermometry) {
            showPressure = false;
        }
    }
    
    public void setCurrentOverlayFrame(int frame) {
        currentOverlayFrame = frame;
    }
    
    public boolean getShowPressure() { return showPressure; }
    public boolean getShowThermometry() { return showThermometry; }
    
    public void setForegroundVolumeSlices(int fgs) {
        if (fgVolSlices != fgs) {
            setIsDirty(true);
        }
        fgVolSlices = fgs;
    }
    
    public int getForegroundVolumeSlices() {
        return fgVolSlices;
    }

    public void setCTImage(ImageVolume image) {
        //System.out.println("ImageCanvasGL::setCTImage()");
        if (CTimage != image) {
            setIsDirty(true);
        }
        CTimage = image;
        

//        theImage.setAttribute("textureName", null); ///////HACK
        Integer tn = (Integer) CTimage.getAttribute("textureName");
        System.out.println("texname = " + tn);
                
        ImageVolumeUtil.setupImageOrientationInfo(image);

        renderOverlay();

    }
    
    public void setOverlayImage(ImageVolume image) {
        
//        releaseTexture(OverlayImage);

        if (OverlayImage != image) {
            
            if (image != null) {
                ImageVolumeUtil.releaseTextures(image);
            }
            setIsDirty(true);
        }
        
        System.out.println("ImageCanvasGL::setOverlayImage()");
        OverlayImage = image;
        
        if (OverlayImage == null) return;
        
//        theImage.setAttribute("textureName", null); ///////HACK
        Integer tn = (Integer) OverlayImage.getAttribute("textureName");
        System.out.println("texname = " + tn);
        
        ImageVolumeUtil.setupImageOrientationInfo(image);
//        OverlayImage.setAttribute("ImageOrientationQ", new Quaternion().setIdentity());

//        OverlayImage.setAttribute("ImageTranslation", new Vector3f(0f,0f,0f));
             
//        ImageVolumeUtil.buildTexture(OverlayImage, false);
//        setupImageTexture(OverlayImage, 5);
        

        if (tn == null || tn == 0) {
//            needsTexture = true;
        } else {
//            needsTexture = false;
//            mr_textureName = tn.intValue();
            System.out.println("setImage: found existing texture = " + tn);
        }
//        needsRendering = true;
//        needsLUT = true;

//        if (needsTexture) {
//            buildTexture();
//        }
        // Really just a test for now of combining Java2D
        // graphics with GL via glDisplayPixels()
        // renderOverlay();
    }
    
    public void setMRImage(ImageVolume image) {
        System.out.println("ImageCanvasGL::setMRImage()");
        if (MRimage != image) {
            setIsDirty(true);
        }
        MRimage = image;
        
//        theImage.setAttribute("textureName", null); ///////HACK
        Integer tn = (Integer) MRimage.getAttribute("textureName");
        System.out.println("texname = " + tn);
                
        ImageVolumeUtil.setupImageOrientationInfo(image);

        renderOverlay();
    }
        


    public void setImageChannel(int channel) {
        if (imageChannel != channel) {
            setIsDirty(true);
        }
        imageChannel = channel;
//        needsRendering = true;
    }

    private void renderOverlay() {

        overlay = new BufferedImage(256, 64, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) overlay.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        gc.setColor(new Color(0.2f, 0.2f, 0.5f, 0.5f));
        gc.fillRect(0, 0, overlay.getWidth() - 1, overlay.getHeight() - 1);
        gc.setColor(new Color(0.2f, 0.2f, 0.5f, 0.8f));
        gc.drawRect(0, 0, overlay.getWidth() - 1, overlay.getHeight() - 1);

        gc.setFont(new Font("Helvetica", Font.BOLD | Font.TRUETYPE_FONT, 15));

        if (CTimage != null) {
            int ypos = 16;

            try {

                String value = CTimage.getAttribute("PatientName").toString();
//value = "Patient Name"; // TODO: anonymizing for now
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("PatientID").toString();
//value = "123456"; // TODO: anonymizing for now
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);

                value = CTimage.getAttribute("PatientSex").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 86, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 85, ypos);
                ypos += 15;

                value = "DOB: " + CTimage.getAttribute("PatientBirthDate").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("InstitutionName").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("AcquisitionDate").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("AcquisitionTime").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

            } catch (Exception e) {
            }

        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTime() {
        return imageTime;
    }

    public void setTime(int value) {
        if (value != imageTime) {
            if (CTimage != null && CTimage.getDimension(3).getSize() > 1) {
//                needsRendering = true;
                imageTime = value;
            } else {
                imageTime = 0;
            }
        }
    }

    public void draw() {

            if (CTimage != null)
                ImageVolumeUtil.buildTexture(CTimage); ///HACK
            if (MRimage != null)
                ImageVolumeUtil.buildTexture(MRimage); ///HACK
            if (OverlayImage != null);
                ImageVolumeUtil.buildTexture(OverlayImage, false, currentOverlayFrame);

            display();
    }

    @Override
    public void render() {
        if (!getVisible()) return;
        
        draw();
        
        setIsDirty(false);
    }

    public ImageVolume getImage() {
        return CTimage;
    }

    @Override
    public void release() {
        ImageVolumeUtil.releaseTexture(CTimage);
        ImageVolumeUtil.releaseTexture(MRimage);
        ImageVolumeUtil.releaseTexture(OverlayImage);
        
        if (lutTextureName != 0) {
            if (glIsTexture(lutTextureName)) {
                ByteBuffer texName = ByteBuffer.allocateDirect(4);
                texName.asIntBuffer().put(0, lutTextureName);
                texName.flip();
                glDeleteTextures(texName.asIntBuffer());
            }                
        }
        
        if (shader != null) {
            shader.release();
            shader = null;
        }
        
        if (pressureShader != null) {
            pressureShader.release();
            pressureShader = null;
        }
        
        if (shader2D != null) {
            shader2D.release();
            shader2D = null;
        }
        
        if (this.vertsID != 0) {
            glDeleteBuffers(vertsID);
            vertsID = 0;
        }
        if (this.texcoordsID != 0) {
            glDeleteBuffers(texcoordsID);
            texcoordsID = 0;
        }
    }

//    private int nextPowerOfTwo(int value) {
//        if (value > 1 && value <= 2) {
//            value = 2;
//        } else if (value > 2 && value <= 4) {
//            value = 4;
//        } else if (value > 4 && value <= 8) {
//            value = 8;
//        } else if (value > 8 && value <= 16) {
//            value = 16;
//        } else if (value > 16 && value <= 32) {
//            value = 32;
//        } else if (value > 32 && value <= 64) {
//            value = 64;
//        } else if (value > 64 && value <= 128) {
//            value = 128;
//        } else if (value > 128 && value <= 256) {
//            value = 256;
//        } else if (value > 256 && value <= 512) {
//            value = 512;
//        } else if (value > 512 && value <= 1024) {
//            value = 1024;
//        }
//        return value;
//    }

    private void buildLutTexture() {
        if (lutTextureName == 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(4);
            IntBuffer texName = buf.asIntBuffer();

            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenTextures(texName);
            lutTextureName = texName.get(0);
        }
        
        glBindTexture(GL_TEXTURE_1D, lutTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
        if (TransferFunction != null) {
            ByteBuffer tmp = TransferFunction.getByteBuffer();
            //tmp.order(ByteOrder.BIG_ENDIAN);
            FloatBuffer pixelBuf = (tmp.asFloatBuffer());
            //glTexImage1D(GL_TEXTURE_1D, 0, org.lwjgl.opengl.GL11.GL_INTENSITY16, TransferFunction.getDimension(0).getSize(), 0, GL_LUMINANCE, GL_FLOAT, pixelBuf);
            glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA8, TransferFunction.getDimension(0).getSize(), 0, GL_RGBA, GL_FLOAT, pixelBuf);
        }
        
        glBindTexture(GL_TEXTURE_1D, 0);
                    
    }
    
    private float sigmoid(float t, float m) {
            return (1f / (1f + (float)Math.exp(-((t-0.5f)*m))));
    }
    
    private void setupLutTexture(int textureUnit) {

        if (lutTextureName == 0) {
            return;
        }

        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_1D);
        
        glBindTexture(GL_TEXTURE_1D, lutTextureName);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    }
    
    private void setupVRSlices() {
        if (this.vertsID != 0) {
            glDeleteBuffers(vertsID);
            vertsID = 0;
        }
        if (this.texcoordsID != 0) {
            glDeleteBuffers(texcoordsID);
            texcoordsID = 0;
        }
        
            // Need a set physical size for the image canvas
            float canvasSize = 300f; // 30cm transducer size?
            
//            float sliceThickness = 0.5f;
            
            float sliceExtent = 1200f;            
            int startSlice = 500;
            int endSlice = 600; //fgVolSlices;
            
//            float sliceExtent = 600f;            
//            int startSlice = 250;
//            int endSlice = 300; //fgVolSlices;
            
            int nslices = startSlice + endSlice + 1;
            
            FloatBuffer vertsBuffer, texCoordsBuffer;
            vertsBuffer = BufferUtils.createFloatBuffer(3 * 4 * nslices);
            texCoordsBuffer = BufferUtils.createFloatBuffer(3 * 4 * nslices);
                       
            for (int s=-startSlice; s<endSlice; s++) {

                    float zTextureOffset = -s/sliceExtent;
                    float zSliceOffset = -s/sliceExtent*(float)canvasSize;
//System.out.println("zSliceOffset   = " + zSliceOffset);
//System.out.println("zTextureOffset = " + zTextureOffset);
                    // TODO: would probably be nice to issue texture coordinates in world mm space
                    //          rather then normalized 0-1 texture space coordinates. Could handle this
                    //          in the texture matrix setup
                    
//                    glMultiTexCoord3d(GL_TEXTURE0, -0.5, 0.5, zTextureOffset); // 0.75 factor to expand the rendering area to fill the transducer volume approx
//                glVertex3d(-canvasSize / 2d, canvasSize / 2d, zSliceOffset); // 1.5 factor to expand the rendering area to fill the transducer volume approx

                texCoordsBuffer.put(-0.5f);
                texCoordsBuffer.put(0.5f);
                texCoordsBuffer.put(zTextureOffset);
                
                vertsBuffer.put(-canvasSize / 2f);
                vertsBuffer.put(canvasSize / 2f);
                vertsBuffer.put(zSliceOffset);
                
//                    glMultiTexCoord3d(GL_TEXTURE0, 0.5, 0.5, zTextureOffset);
//                glVertex3d(canvasSize / 2d, canvasSize / 2d, zSliceOffset);
                
                texCoordsBuffer.put(0.5f);
                texCoordsBuffer.put(0.5f);
                texCoordsBuffer.put(zTextureOffset);
                
                vertsBuffer.put(canvasSize / 2f);
                vertsBuffer.put(canvasSize / 2f);
                vertsBuffer.put(zSliceOffset);

//                    glMultiTexCoord3d(GL_TEXTURE0, 0.5, -0.5, zTextureOffset);
//                glVertex3d(canvasSize / 2d, -canvasSize / 2d, zSliceOffset);

                texCoordsBuffer.put(0.5f);
                texCoordsBuffer.put(-0.5f);
                texCoordsBuffer.put(zTextureOffset);
                
                vertsBuffer.put(canvasSize / 2f);
                vertsBuffer.put(-canvasSize / 2f);
                vertsBuffer.put(zSliceOffset);
                
//                    glMultiTexCoord3d(GL_TEXTURE0, -0.5, -0.5, zTextureOffset);
//                glVertex3d(-canvasSize / 2d, -canvasSize / 2d, zSliceOffset);
                
                texCoordsBuffer.put(-0.5f);
                texCoordsBuffer.put(-0.5f);
                texCoordsBuffer.put(zTextureOffset);
                
                vertsBuffer.put(-canvasSize / 2f);
                vertsBuffer.put(-canvasSize / 2f);
                vertsBuffer.put(zSliceOffset);                

            }
            vertsBuffer.flip();
            texCoordsBuffer.flip();

            vertsID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vertsID);
            glBufferData(GL_ARRAY_BUFFER, vertsBuffer, GL_STATIC_DRAW);

            texcoordsID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, texcoordsID);
            glBufferData(GL_ARRAY_BUFFER, texCoordsBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    private void setupImageTexture(ImageVolume image, int textureUnit) {
        if (image == null) return;
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return;
        
        
        glEnable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        
        int textureName = tn.intValue();
        glBindTexture(GL_TEXTURE_3D, textureName);
        
        FloatBuffer color = BufferUtils.createFloatBuffer(4);
        color.put(0f).put(0f).put(0f).put(0f).flip();
        glTexParameter(GL_TEXTURE_3D, GL_TEXTURE_BORDER_COLOR, color);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
        
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;
            
            if (texDepth == 1) {
//                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);                
            }

            // Build transformation of Texture matrix
            ///////////////////////////////////////////
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();

            //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
            //fix voxel scaling
            float xres = image.getDimension(0).getSampleWidth(0);
            float yres = image.getDimension(1).getSampleWidth(1);
            float zres = image.getDimension(2).getSampleWidth(2);
            
            // Translation to the texture volume center (convert 0 -> 1 value range to -0.5 -> 0.5)
            ///////////////////////////////////////////////////////////
            glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);

//            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
            
            float canvasSize = 300f;
            
            float xzoom = canvasSize/(xres*texWidth);
            float yzoom = canvasSize/(yres*texHeight);
            float zzoom = -canvasSize/(zres*texDepth);
            
            // Scale for in-plane/out-of-plane ratio
            ///////////////////////////////////////////////////////////
            //glScaled(1.0f, 1.0f, -zscaleFactor); // this assumes in-plane pixel dimesions are the same! //HACK
            glScaled(xzoom, yzoom, zzoom); 


            Quaternion imageOrientation = (Quaternion)image.getAttribute("ImageOrientationQ");
            if (imageOrientation == null) return;
            
            FloatBuffer orientBuffer = BufferUtils.createFloatBuffer(16);
		Trackball.toMatrix4f(imageOrientation).store(orientBuffer);
		orientBuffer.flip();
                         
            // Rotation for image orientation
            ////////////////////////////////////////////////////////////
		glMultMatrix(orientBuffer);
            
            // Translation of center of rotation to origin (mm to texture coord values (0 -> 1))
            ////////////////////////////////////////////////////////////
            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            if (imageTranslation == null) {
                imageTranslation = new Vector3f();
            }
            
            float[] imagePosition = (float[])image.getAttribute("ImagePosition");
            
            if (imagePosition == null) {
                imagePosition = new float[3];
            }
            glTranslatef((centerOfRotation.x + imageTranslation.x /* + imagePosition[0] */)/ (canvasSize),
                    (centerOfRotation.y + imageTranslation.y /* + imagePosition[1] */) / (canvasSize),
                    (centerOfRotation.z + imageTranslation.z /* + imagePosition[2] */) / (canvasSize));

            // Rotation for camera view
            if (trackball != null) {
                trackball.renderOpposite();
            }
            
            // save the transformation from the image canvas to the texture volume
            // so we can later transform mouse clicks into the texture volume and 
            // hence the image volume
            
            if (image == CTimage) { // HACK
                matrixBuf.rewind();
                glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                ctTexMatrix.load(matrixBuf);
                
                // setup gradient texture
                glActiveTexture(GL_TEXTURE0 + 3);
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
                tn = (Integer) image.getAttribute("gradientTexName");
        
                if (tn != null) {       
                    textureName = tn.intValue();
                    glBindTexture(GL_TEXTURE_3D, textureName);
                }

            }
            else if (image == MRimage) {
                matrixBuf.rewind();
                glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                mrTexMatrix.load(matrixBuf);                
                // setup gradient texture
                glActiveTexture(GL_TEXTURE0 + 4);
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        
                tn = (Integer) image.getAttribute("gradientTexName");
        
                if (tn != null) {       
                    textureName = tn.intValue();
                    glBindTexture(GL_TEXTURE_3D, textureName);
                }
            }
            
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
            
}
    
private Vector3f setupLightPosition(Vector4f lightPosIn, ImageVolume image) {
        if (image == null) return null;
        
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return null;
        
       
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;

            // Build transformation of Texture matrix
            ///////////////////////////////////////////
 
            //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
            //fix voxel scaling
            float xres = image.getDimension(0).getSampleWidth(0);
            float yres = image.getDimension(1).getSampleWidth(1);
            float zres = image.getDimension(2).getSampleWidth(2);
            
            // Translation to the texture volume center (convert 0 -> 1 value range to -0.5 -> 0.5)
            ///////////////////////////////////////////////////////////
//            glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);
//
//            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
//            
//            // Scale for in-plane/out-of-plane ratio
//            ///////////////////////////////////////////////////////////
////            glScaled(1.0f, 1.0f, -zscaleFactor); // this assumes in-plane pixel dimesions are the same! //HACK
//            glScaled(1f /(texWidth * xres), 1f/(texWidth * xres), 1f/(texDepth * zres));
//
//
//            // Translation of center of rotation to origin (mm to texture coord values (0 -> 1))
//            ////////////////////////////////////////////////////////////
            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            if (imageTranslation == null) {
                imageTranslation = new Vector3f();
            }
//            glTranslatef(
////                    (imageTranslation.x)/ (xres * iWidth),
////                    (imageTranslation.y) / (yres * iHeight),
////                    (imageTranslation.z) / (zres * idepth * zscaleFactor));
//                    (-imageTranslation.x),
//                    (-imageTranslation.y),
//                    (-imageTranslation.z) - 150f);

            Quaternion imageOrientation = (Quaternion)image.getAttribute("ImageOrientationQ");
            if (imageOrientation == null) return null;
            
            FloatBuffer texOrientBuffer = BufferUtils.createFloatBuffer(16);
		Trackball.toMatrix4f(imageOrientation).store(texOrientBuffer);
		texOrientBuffer.flip();
                
            FloatBuffer camOrientBuffer = BufferUtils.createFloatBuffer(16);
		Trackball.toMatrix4f(trackball.getCurrent().negate(null)).store(camOrientBuffer);
		camOrientBuffer.flip();
                
                         
            // Rotation for image orientation
            ////////////////////////////////////////////////////////////
		//glMultMatrix(orientBuffer);
            
            // Rotation for camera view
//            if (trackball != null) {
//                trackball.renderOpposite();
//            }

//            matrixBuf.rewind();
//            glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
//            ctTexMatrix.load(matrixBuf);
            
            Matrix4f lightMat = new Matrix4f();
            lightMat.setIdentity();
            
            // Final translation to put origin in the center of texture space (0.5, 0.5, 0.5)
            Matrix4f.translate(new Vector3f(0.5f, 0.5f, 0.5f), lightMat, lightMat);
                       
            // Scale for image resolution (normalize to texture coordinates 0-1
            
            //TODO: fix scaling for real world canvas size
            Matrix4f.scale(new Vector3f(600.0f/(texWidth*xres), 600.0f/(texHeight*yres), -600.0f/(texDepth*zres)), lightMat, lightMat);

            Matrix4f rotMat = new Matrix4f();
             
             // Rotation
            rotMat.load(texOrientBuffer);
            Matrix4f.mul(lightMat, rotMat, lightMat);
            
            rotMat.load(camOrientBuffer);
            Matrix4f.mul(lightMat, rotMat, lightMat);
                      
            
            // Translation
            Matrix4f.translate(new Vector3f(
                        (centerOfRotation.x + -imageTranslation.x),
                        (centerOfRotation.y + -imageTranslation.y),
                        (centerOfRotation.z + -imageTranslation.z)
                        ),
                    lightMat, lightMat);
            
            Matrix4f.rotate((float)Math.PI, new Vector3f(1f, 0f, 0f), lightMat, lightMat);
            //Matrix4f.rotate((float)Math.PI, new Vector3f(0f, 1f, 0f), ctTexMatrix, ctTexMatrix);
           
            // add in transducer tilt
//            Matrix4f.rotate(transducerTilt/180f*(float)Math.PI, new Vector3f(1f, 0f, 0f), lightMat, lightMat);
            
            // Translate transducer origin to the transducer face
            Matrix4f.translate(new Vector3f(0f, 0f, -150f), lightMat, lightMat);
            

            Vector4f tmp = new Vector4f();
            Matrix4f.transform(lightMat, lightPosIn, tmp);

            return new Vector3f(tmp.x, tmp.y, tmp.z);            

    }

    public void display() {

        init();
        
        
        //glPushAttrib(0xffffffff);//GL_ENABLE_BIT);
//**************************************************************
        Main.glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);

        if (CTimage == null) {
            Main.glPopAttrib();
            return;
        } //HACK
        
        Integer tn = (Integer) CTimage.getAttribute("textureName");
        if (tn == null) {
            Main.glPopAttrib();
            return;
        }
        
        int textureName = tn.intValue();
        
        if (textureName <= 0) {
            Main.glPopAttrib();
            return;
        }

        glColor3d(0.7, 0.7, 0.9);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        if (CTimage != null) {
            setupImageTexture(CTimage, 0);
        }
        
        if (MRimage != null) {
            setupImageTexture(MRimage, 1);
        }
        
        if (OverlayImage != null) {
            setupImageTexture(OverlayImage, 5);
        }
        
        buildLutTexture();
        setupLutTexture(2);                

        double texsize = 0.5;
        double canvasSize = 1.0;
        double yvertsize = 1.0;
/*
        double aspect = 1.0f;
*/
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
        
        if (CTimage != null) {

            int iWidth = CTimage.getDimension(0).getSize();
            int iHeight = CTimage.getDimension(1).getSize();
            int idepth = CTimage.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;

            float xSampleSpacing = CTimage.getDimension(0).getSampleWidth(0) * iWidth;
            float ySampleSpacing = CTimage.getDimension(1).getSampleWidth(0) * iHeight;

            canvasSize = xSampleSpacing;
            yvertsize = ySampleSpacing;
            
            // Need a set physical size for the image canvas
            canvasSize = 300f; // 30cm transducer size?
            yvertsize = 300f;

            float xres = CTimage.getDimension(0).getSampleWidth(0);

            ShaderProgram shaderToUse = null;
            
            if (doVolumeRender) {
                if (showThermometry && (this.OverlayImage != null)) {
                    shaderToUse = thermometryShader;
                }
                else if (showPressure) {
                    shaderToUse = pressureShader;
                }
                else {
                    shaderToUse = shader;                
                }
            }
            else {
                shaderToUse = shader2D;
            }
            
        int sliceUniform = 0;
        int lastSliceUniform = 0;
        
        if (shaderToUse != null) {
            shaderToUse.start();
            
                int shaderProgID = shaderToUse.getShaderProgramID();
                
                centerUniform = glGetUniformLocation(shaderProgID, "center");
                windowUniform = glGetUniformLocation(shaderProgID, "window");
                sliceUniform = glGetUniformLocation(shaderProgID, "slice");
                lastSliceUniform = glGetUniformLocation(shaderProgID, "last_slice");
                int mrcenterUniform = glGetUniformLocation(shaderProgID, "mr_center");
                int mrwindowUniform = glGetUniformLocation(shaderProgID, "mr_window");
                ct_thresholdUniform = glGetUniformLocation(shaderProgID, "ct_threshold");
                mr_thresholdUniform = glGetUniformLocation(shaderProgID, "mr_threshold");
                int showMRUniform = glGetUniformLocation(shaderProgID, "showMR");
                
                int texLoc = glGetUniformLocation(shaderProgID, "ct_tex");
                glUniform1i(texLoc, 0);
                texLoc = glGetUniformLocation(shaderProgID, "mr_tex");
                glUniform1i(texLoc, 1);
                texLoc = glGetUniformLocation(shaderProgID, "lut_tex");
                glUniform1i(texLoc, 2);
                texLoc = glGetUniformLocation(shaderProgID, "ct_grad_tex");
                glUniform1i(texLoc, 3);
                texLoc = glGetUniformLocation(shaderProgID, "mr_grad_tex");
                glUniform1i(texLoc, 4);
                texLoc = glGetUniformLocation(shaderProgID, "ovly_tex");
                glUniform1i(texLoc, 5);
                
                texLoc = glGetUniformLocation(shaderProgID, "ct_rescale_slope");
                glUniform1f(texLoc, ct_rescale_slope);
                texLoc = glGetUniformLocation(shaderProgID, "ct_rescale_intercept");
                glUniform1f(texLoc, ct_rescale_intercept);

                texLoc = glGetUniformLocation(shaderProgID, "mr_rescale_slope");
                glUniform1f(texLoc, mr_rescale_slope);
                texLoc = glGetUniformLocation(shaderProgID, "mr_rescale_intercept");
                glUniform1f(texLoc, mr_rescale_intercept);
                
                glUniform1f(centerUniform, (float) ct_center);
                glUniform1f(windowUniform, (float) ct_window);
                glUniform1f(mrcenterUniform, (float) mr_center);
                glUniform1f(mrwindowUniform, (float) mr_window);
                glUniform1f(ct_thresholdUniform, (float) ct_threshold);
                glUniform1f(mr_thresholdUniform, (float) mr_threshold);
                glUniform1i(showMRUniform, showMR ? 1:0);
                
                Vector3f newLightPos;
                               
                newLightPos = this.setupLightPosition(new Vector4f(150f, 150f, 450f, 1f), this.CTimage);
                 
                int lightLoc = glGetUniformLocation(shaderProgID, "light_position");
                glUniform3f(lightLoc, newLightPos.x, newLightPos.y, newLightPos.z);
                
                Vector3f newEyePos;
                               
                newEyePos = this.setupLightPosition(new Vector4f(0f, 0f, 600f, 1f), this.CTimage);
                 
                int eyeLoc = glGetUniformLocation(shaderProgID, "eye_position");
                glUniform3f(eyeLoc, newEyePos.x, newEyePos.y, newEyePos.z);
            }

            //Keep the image plane perpendicular to the camera
            if (trackball != null) {
                glMatrixMode(GL_MODELVIEW);
                trackball.renderOpposite();
            }
            
            if (doVolumeRender) {
                glEnable(GL_BLEND);
                //glBlendFuncSeparate( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
                
                // This blends src and dst alpha
                glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            }
            
             int startSlice = 500;
            // int startSlice = 250;
            int endSlice = fgVolSlices;
            
            if (!doVolumeRender) {
                startSlice = 0;
                endSlice = 0;
            }
            
            glNormal3d(0.0, 0.0, 1.0);
                if (shaderToUse != null) {
                    glUniform1i(sliceUniform, endSlice);
                    glUniform1i(lastSliceUniform, -1000); // so shader knows to treat clipped slice differently
                }
            

            glBindBuffer(GL_ARRAY_BUFFER, vertsID);
            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(3, GL_FLOAT, 0, 0);
            
            glBindBuffer(GL_ARRAY_BUFFER, texcoordsID);
            glClientActiveTexture(GL_TEXTURE0);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            glTexCoordPointer(3, GL_FLOAT, 0, 0);
            
            if (doVolumeRender) {
                 // back to front slices except front most slice
                glDrawArrays(GL_QUADS, 0, (startSlice + endSlice) * 4);
                
                // front most slice
                glUniform1i(sliceUniform, endSlice);
                glUniform1i(lastSliceUniform, endSlice); // so shader knows to treat clipped slice differently
                glDrawArrays(GL_QUADS, (startSlice + endSlice + 1)*4, 4);
            }
            else {
                // central slice for 2D
                glDrawArrays(GL_QUADS, (500)*4, 4);   // TODO: fix magic number             
            }
                        
            glDisableClientState(GL_VERTEX_ARRAY);
            glClientActiveTexture(GL_TEXTURE0);
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            
            glDisable(GL_BLEND);

            glUseProgram(0);
        }

        glActiveTexture(GL_TEXTURE0 + 2);
        glDisable(GL_TEXTURE_1D);
        
        glActiveTexture(GL_TEXTURE0 + 5);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 4);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 3);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 1);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);
        
        // Draw outline of the slice for debugging
//**********************************************************
        Main.glPushAttrib(GL_POLYGON_BIT | GL_ENABLE_BIT);
                
            glDisable(GL_LIGHTING);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glEnable(GL_COLOR_MATERIAL);    // enables opengl to use glColor3f to define material color               
            glEnable(GL_BLEND);
            glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glColor4d(0.3, 0.5, 1f, 0.2);

            glBegin(GL_QUADS);

                glNormal3d(0.0, 0.0, -1.0);

                glVertex3d(-canvasSize  / 2d, yvertsize  / 2d, 0.0);
                glVertex3d(-canvasSize  / 2d, -yvertsize  / 2d, 0.0);
                glVertex3d(canvasSize  / 2d, -yvertsize  / 2d, 0.0);
                glVertex3d(canvasSize  / 2d, yvertsize  / 2d, 0.0);

            glEnd();
        


            // Draw a frame around the canvas/////////
//            glMatrixMode(GL_PROJECTION);
//            glPushMatrix();
//            glLoadIdentity();

            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
            glLoadIdentity();

            glColor4d(0.1, 0.1, 0.3, 1.0);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    //        glLineWidth(2.0f);

            glBegin(GL_QUADS);

                glNormal3d(0.0, 0.0, 1.0);

                glVertex3d(-1.0, 1.0, 0.0);

                glVertex3d(1.0, 1.0, 0.0);

                glVertex3d(1.0, -1.0, 0.0);

                glVertex3d(-1.0, -1.0, 0.0);

            glEnd();

            glMatrixMode(GL_MODELVIEW);
            Main.glPopMatrix();

//            glMatrixMode(GL_PROJECTION);
//            glPopMatrix();
            ////////////////////////////////////////////
        
        Main.glPopAttrib();        
        
        glMatrixMode(GL_MODELVIEW);
//        if (trackball != null) {
                Main.glPopMatrix();
//        }
        
        Main.glPushMatrix();
//        glTranslatef(-centerOfRotation.x , -centerOfRotation.y , -centerOfRotation.z );

        // draw image volume cube proxy
        this.drawVolumeProxyCube(CTimage);
        this.drawVolumeProxyCube(MRimage);
        
        Main.glPopMatrix();
                
        Main.glPopAttrib();
    }
    
    private void drawVolumeProxyCube(ImageVolume image) {
        if (image != null) {
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();
            
            Quaternion imageOrientationQ = (Quaternion)image.getAttribute("ImageOrientationQ");
            float[]    imagePosition = (float[])image.getAttribute("ImagePosition");
            Vector3f    imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            if (imageTranslation == null) {
                imageTranslation = new Vector3f();
            }
            
            if (imagePosition != null) {
//                imageTranslation.x += imagePosition[0];
//                imageTranslation.y += imagePosition[1];
//                imageTranslation.z += imagePosition[2];
            }
            
            Matrix4f imageOrientationM = Trackball.toMatrix4f(imageOrientationQ.negate(null));
            
            Vector4f ImageOrientationX4 = Matrix4f.transform(imageOrientationM, new Vector4f(1f, 0f, 0f, 1f), null);//Vector3f)image.getAttribute("ImageOrientationX");
            Vector4f ImageOrientationY4 = Matrix4f.transform(imageOrientationM, new Vector4f(0f, 1f, 0f, 1f), null);//(Vector3f)image.getAttribute("ImageOrientationY");
            Vector4f ImageOrientationZ4 = Matrix4f.transform(imageOrientationM, new Vector4f(0f, 0f, 1f, 1f), null);//(Vector3f)image.getAttribute("ImageOrientationZ");
            
            Vector3f ImageOrientationX = new Vector3f(ImageOrientationX4);
            Vector3f ImageOrientationY = new Vector3f(ImageOrientationY4);
            Vector3f ImageOrientationZ = new Vector3f(ImageOrientationZ4);
            
            if (    ImageOrientationX == null ||
                    ImageOrientationY == null ||
                    ImageOrientationZ == null) {
                return;
            }
            
            float xSize = image.getDimension(0).getSampleWidth(0) * iWidth;
            float ySize = image.getDimension(1).getSampleWidth(0) * iHeight;
            float zSize = image.getDimension(2).getSampleWidth(0) * idepth;
//            Vector3f zdir = Vector3f.cross(ImageOrientationX, ImageOrientationY, null);
//            zdir.x *= zSize;
//            zdir.y *= zSize;
//            zdir.z *= zSize;
            
            Main.glPushAttrib(GL_ENABLE_BIT | GL_POLYGON_BIT );
//*************************************************************
            glDisable(GL_LIGHTING);
            glEnable(GL_BLEND);
            //glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
             glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            
            // mark image volume origin position
            glPointSize(5f);
            glBegin(GL_POINTS);
                glColor4f(1f, 0f, 0f, 0.5f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z-imageTranslation.z - ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                glColor4f(0f, 1f, 0f, 0.5f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z-imageTranslation.z + ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
            glEnd();
            
            //box begin and end slice planes
            glBegin(GL_QUADS);

                glNormal3d(0.0, 0.0, 1.0);

                glColor4f(1f, 0f, 0f, 0.4f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);

                glColor4f(0.5f, 0.5f, 0.5f, 0.3f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);

            glEnd();
            
            glBegin(GL_LINES);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f - ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f - ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f - ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x + ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y + ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z + ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f - ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f - ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f - ImageOrientationZ.z*zSize/2f);
                 glVertex3f(-centerOfRotation.x -imageTranslation.x - ImageOrientationX.x*xSize/2f + ImageOrientationY.x*ySize/2f + ImageOrientationZ.x*zSize/2f,
                            -centerOfRotation.y -imageTranslation.y - ImageOrientationX.y*xSize/2f + ImageOrientationY.y*ySize/2f + ImageOrientationZ.y*zSize/2f,
                            -centerOfRotation.z -imageTranslation.z - ImageOrientationX.z*xSize/2f + ImageOrientationY.z*ySize/2f + ImageOrientationZ.z*zSize/2f);
            glEnd();
            Main.glPopAttrib();
        }        
    }


    public void renderDemographics() {
        // Overlay demographics
        if (overlay != null) {
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT);
//****************************************************************************
            
            byte buf[] = (byte[]) overlay.getRaster().getDataElements(0, 0, overlay.getWidth(), overlay.getHeight(), null);

            glMatrixMode(GL_PROJECTION);
            Main.glPushMatrix();
            glLoadIdentity();

            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
            glLoadIdentity();

//            glEnable(GL_BLEND);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glPixelZoom(1.0f, -1.0f);
            glRasterPos2d(-1.0, 1.0);

            glDisable(GL_CLIP_PLANE0);
            glDisable(GL_CLIP_PLANE1);
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

            ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
            bbuf.put(buf, 0, buf.length);
            bbuf.flip();
            glDrawPixels(overlay.getWidth(), overlay.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

            //      }
            //     glPopMatrix();
            //  }
            //  glMatrixMode(GL_PROJECTION);
            //   glPopMatrix();
            glDisable(GL_BLEND);
            
            glMatrixMode(GL_MODELVIEW);
            Main.glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            Main.glPopMatrix();
                 
            Main.glPopAttrib();
            
            glMatrixMode(GL_MODELVIEW);
        }        
    }


    /**
     * Invoked when a key has been pressed. See the class description for
     * {@link KeyEvent} for a definition of a key pressed event.
     *
     */
    public void keyPressed(KeyEvent e) {
        /*
        if (drawMode == GL_FILL) {
            drawMode = GL_LINE;
        }
        else {
            drawMode = GL_FILL;
        }
        this.repaint();
         **/
    }

    /**
     * Invoked when a key has been released. See the class description for
     * {@link KeyEvent} for a definition of a key released event.
     *
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * Invoked when a key has been typed. See the class description for
     * {@link KeyEvent} for a definition of a key typed event.
     *
     */
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Invoked when a mouse button is pressed on a component and then dragged.
     * <code>MOUSE_DRAGGED</code> events will continue to be delivered to the
     * component where the drag originated until the mouse button is released
     * (regardless of whether the mouse position is within the bounds of the
     * component).
     * <p>
     * Due to platform-dependent Drag&Drop implementations,
     * <code>MOUSE_DRAGGED</code> events may not be delivered during a native
     * Drag&Drop operation.
     *
     */
    public void setZoom(float value) {
        if (zoom != value) {
            setIsDirty(true);
        }
        System.out.println("zoom = " + value);
        zoom = value;
    }

    public void setCenterWindow(int c, int w) {
        if (ct_center != c || ct_window != w) {
            setIsDirty(true);
        }
        ct_center = c;
        ct_window = w;
    }
    
    public void setMRCenterWindow(int c, int w) {
        if (mr_center != c || mr_window != w) {
            setIsDirty(true);
        }
        mr_center = c;
        mr_window = w;    
    }
    
    public void setMRrescale(float slope, float intercept) {
        if (mr_rescale_slope != slope || mr_rescale_intercept != intercept) {
            setIsDirty(true);
        }
        mr_rescale_slope = slope;
        mr_rescale_intercept = intercept;    
    }
    
    public void setCTrescale(float slope, float intercept) {
        if (ct_rescale_slope != slope || ct_rescale_intercept != intercept) {
            setIsDirty(true);
        }
        ct_rescale_slope = slope;
        ct_rescale_intercept = intercept;    
    }
    
    public void setPan(float px, float py) {
        if (panx != px || pany != py) {
            setIsDirty(true);
        }
        panx = px;
        pany = py;
    }

    public void setRotation(float r) {
        if (rotation != r) {
            setIsDirty(true);
        }
        rotation = r;
    }

    public void init() {

        if (shader == null && shader2D == null) {
            initShaders();
        }
        
        if (this.vertsID == 0) {
            setupVRSlices();
        }
    }

    public void initShaders() {

        if (shader != null && shader2D != null) {
            return;
        } // already compiled the shader

        String vsrc = "#version 120\n"
                + "varying vec3 frag_position; // in object space\n"
                + "varying vec3 light_position;\n"
                + "void main(void)\n"
                + "{\n"
                + "   gl_TexCoord[0]  = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n"
                + "   gl_TexCoord[1]  = gl_TextureMatrix[1] * gl_MultiTexCoord1;\n"
                + "   gl_TexCoord[3]  = gl_TextureMatrix[3] * gl_MultiTexCoord3;\n"
                + "   gl_TexCoord[4]  = gl_TextureMatrix[4] * gl_MultiTexCoord4;\n"
                + "   gl_TexCoord[5]  = gl_TextureMatrix[5] * gl_MultiTexCoord5;\n"
                + "   gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
                + "   light_position = (gl_TextureMatrix[0] * vec4(150, -150, -400, 1)).xyz;\n"
                + "   frag_position = gl_Vertex.xyz;\n"
                + "}";

        String fsrc2D = "#version 120\n"
                + "uniform int slice;\n"
                + "uniform int showMR;\n"
                + "uniform float center;\n"
                + "uniform float window;\n"
                + "uniform float mr_center;\n"
                + "uniform float mr_window;\n"
                + "uniform float ct_threshold;\n"
                + "uniform float mr_threshold;\n"
                + "uniform sampler3D ct_tex;\n"
                + "uniform sampler3D mr_tex;\n"
                + "uniform sampler3D ovly_tex;\n"
                + "void main(void)\n"
                + "{\n"
                + "       vec4 color;\n"
                + "        float ctsample = texture3D(ct_tex, gl_TexCoord[0].stp).r * 65536.0;\n"
                + "        float mrsample = texture3D(mr_tex, gl_TexCoord[1].stp).r * 65536.0;\n"
                + "        float ovlyTexVal = texture3D(ovly_tex, gl_TexCoord[5].stp).r; // * 32767.0;\n"
                + "        if (ctsample < ct_threshold-150 && (showMR!=1 || mrsample < mr_threshold))\n"
                + "           discard;\n"
                + "        vec4 ovlyColor = vec4(1.0);\n"
                + "        if (ovlyTexVal > 699.0) {\n"
                + "            ovlyColor = vec4(0.3, 1.0, 0.3, 1.0);\n"
                + "        }\n"
                + "        else if (ovlyTexVal > 499.0) {\n"
                + "            ovlyColor = vec4(1, 1, 0.3, 1.0);\n"
                + "        }\n"
                + "        else if (ovlyTexVal > 399.0) {\n"
                + "            ovlyColor = vec4(1.0, 0.7, 0.7, 1.0);\n"
                + "        }\n"
                + "        float ctval = (ctsample - center)/window;\n"
                + "        float mrval = (mrsample - mr_center)/mr_window;\n"
                + "        color.rgb = vec3(ctval*0.6, ctval, ctval*0.6);\n"
                + "        if (showMR==1 && ctsample < ct_threshold) \n"
                + "             color.rgb = vec3(mrval);\n"
                + "        color.a = 1;\n"
                + "        gl_FragColor = color * ovlyColor;\n"
                + "}";
        
        Shader vertexShader = new Shader();
        vertexShader.addShaderSource(GL_VERTEX_SHADER, "/org/fusfoundation/kranion/shaders/ImageCanvasVolRend.vs.glsl");

        Shader fragShader = new Shader();
        fragShader.addShaderSource(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/ImageCanvasVolRend.fs.glsl");
        
        Shader fragShaderPressure = new Shader();
        fragShaderPressure.addShaderSource(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/ImageCanvasVolRendWPressure.fs.glsl");
        
        Shader fragShaderThermometry = new Shader();
        fragShaderThermometry.addShaderSource(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/ImageCanvasVolRendThermometry.fs.glsl");
        
        Shader fragShader2D = new Shader();
        fragShader2D.addShaderSourceString(GL_FRAGMENT_SHADER, fsrc2D);
        
        shader = new ShaderProgram();
        shader.addShader(vertexShader);
        shader.addShader(fragShader);
        shader.compileShaderProgram();
        
        pressureShader = new ShaderProgram();
        pressureShader.addShader(vertexShader);
        pressureShader.addShader(fragShaderPressure);
        pressureShader.compileShaderProgram();

        thermometryShader = new ShaderProgram();
        thermometryShader.addShader(vertexShader);
        thermometryShader.addShader(fragShaderThermometry);
        thermometryShader.compileShaderProgram();
        
        shader2D = new ShaderProgram();
        shader2D.addShader(vertexShader);
        shader2D.addShader(fragShader2D);
        shader2D.compileShaderProgram();
        
    }
    
    @Override
    public void update(Object newValue) {
        try {
            Vector3f target = (Vector3f)newValue;
            centerOfRotation.set(target);
        }
        catch(Exception e) {
            System.out.println(this + ": Wrong or NULL new value.");
        }
    }
    
}
