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
//import static org.lwjgl.opengl.GL14.*;
//import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.BufferUtils;

import org.fusfoundation.kranion.model.image.*;
import org.lwjgl.input.Keyboard;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

/**
 *
 * @author jsnell
 */
public class ImageCanvas2D extends GUIControl {

    private ImageVolume CTimage, MRimage, OverlayImage=null;
    private int imageChannel = 0;
    private BufferedImage overlay;
    private float rotx = 0.0f;
    private float roty = 0.0f;
    private float zoom = 1.0f;
    private int textureName = -1;
//    private int[] lut = new int[256 * 256];
//    private Object pixels;
    
    private float canvasX=0f, canvasY=0f, canvasSize=300f, canvasFOV=200f;
    
    private int ct_center = 1024, ct_window = 1024;
    private int mr_center = 1024, mr_window = 1024;
    private float ct_rescale_slope = 1f;
    private float ct_rescale_intercept = 0f;
    private float mr_rescale_slope = 1f;
    private float mr_rescale_intercept = 0f;
    private int slice = 0, imageTime = 0;
    private double panx = 0.0, pany = 0.0;
    private double rotation = 0.0;
    private float ct_threshold = 0.0f, mr_threshold = 0.0f;
//    private int iWidth, iHeight, texWidth, texHeight, texDepth;
    private int width, height;
//    private boolean needsTexture = true;
//    private boolean needsRendering = true;
//    private boolean needsLUT = true;
//    private float[] target = new float[3];
    
    public FloatBuffer matrixBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f ctTexMatrix = new Matrix4f();
    public Matrix4f mrTexMatrix = new Matrix4f();
    public Matrix4f ovlyTexMatrix = new Matrix4f();
    
    public Vector3f ctImageOrientationX = new Vector3f();
    public Vector3f ctImageOrientationY = new Vector3f();
    public Vector3f ctImageOrientationZ = new Vector3f();    
    public Vector3f ctImageOriginPosition = new Vector3f();    
    private Quaternion ctImageOrientation = new Quaternion();

    public Vector3f mrImageOrientationX = new Vector3f();
    public Vector3f mrImageOrientationY = new Vector3f();
    public Vector3f mrImageOrientationZ = new Vector3f();    
    public Vector3f mrImageOriginPosition = new Vector3f();    
    private Quaternion mrImageOrientation = new Quaternion();
    private Vector3f imagePlaneX = new Vector3f(1f, 0f, 0f);
    private Vector3f imagePlaneY = new Vector3f(0f, 1f, 0f);

    // we use this to rotate our imaging plane in the 3D texture space
    private Trackball trackball = null;
    private Vector3f centerOfRotation = new Vector3f();

    private static int centerUniform = 0, windowUniform = 0, thresholdUniform = 0;
//    private static int shaderprogram = 0;
    private ShaderProgram shader, shaderGray;
    
    private boolean showMR = true;

    private int orientation = 0; // temp hack for now, playing with tex matrix ops
    
    private boolean mouseGrabbed = false;
    private boolean rightMouseButtonDown = false;
    
    private boolean displayPosition = false;
    private boolean targetingEnabled = true;
    private boolean useGrayScale = false; // default is "green" scale
    
    /**
     * Creates a new instance of ImageCanvasGL
     */
    public ImageCanvas2D() {

        computeLUT();

        renderOverlay();
    }
    
    public ImageCanvas2D(float x, float y, float size, float fov) {
        setCanvasPosition(x, y, size, fov);
        
        computeLUT();
        
        renderOverlay();
    }

    public void setCanvasPosition(float x, float y, float size, float fov) {
        
        if (canvasX != x || canvasY != y || canvasSize != size || canvasFOV != fov) {
            setIsDirty(true);
        }
        
        canvasX = x;
        canvasY = y;
        canvasSize = size;
        canvasFOV = fov;
        
        super.setBounds((int)x, (int)y, (int)size, (int)size);
    }
    
    public void setCanvasSize(float size) {
        if (canvasSize != size) {
            setIsDirty(true);
        }
        canvasSize = size;
    }
    
    public void setTargetingEnabled(boolean flag) {
        this.targetingEnabled = flag;
    }
    
    public void setUseGrayScale(boolean flag) {
        useGrayScale = flag;
    }
    
    public Vector2f getCanvasPosition() { return new Vector2f(canvasX, canvasY); }
    public float getCanvasSize() { return canvasSize; }
    public float getCanvasFOV() { return canvasFOV; }

    public Vector3f getSelectedPoint() { return new Vector3f(centerOfRotation.x, centerOfRotation.y, centerOfRotation.z); }
    
    public void setOrientation(int orient) {
        if (orientation != orient) {
            setIsDirty(true);
        }
        orientation = orient;
    }
    
    public void setCTThreshold(float value) {
        if (ct_threshold != value) {
            setIsDirty(true);
        }
        ct_threshold = value;
    }
    
    public void setMRThreshold(float value) {
        if (mr_threshold != value) {
            setIsDirty(true);
        }
        mr_threshold = value;
    }
    public void setTextureRotatation(Vector3f rotationOffset, Trackball tb) {
        if (trackball==null || !centerOfRotation.equals(rotationOffset) || trackball.getCurrent() != tb.getCurrent()) {
            setIsDirty(true);
        }
//        centerOfRotation.set(rotationOffset);
        trackball = tb;
    }

//    public void setTarget(float x, float y, float z) {
//        if (target[0] != x || target[1] != y || target[2] != z) {
//            setIsDirty(true);
//            
//            target[0] = x;
//            target[1] = y;
//            target[2] = z;
//            
//            if (this.CTimage != null) {
//                CTimage.setAttribute("volumeTarget", target);
//            }
//        }
//    }
//
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
        
    public void setOverlayImage(ImageVolume image) {
        
//        if (OverlayImage != null) {
//            releaseTexture(OverlayImage);
//        }
        
        System.out.println("ImageCanvasGL::setOverlayImage()");
        
        if (OverlayImage != image) {
            setIsDirty(true);
            
            if (OverlayImage != null) {
                ImageVolumeUtil.releaseTexture(OverlayImage);
            }
        }
        OverlayImage = image;
        
        if (OverlayImage == null) {
            return;
        }
        
//        theImage.setAttribute("textureName", null); ///////HACK
        Integer tn = (Integer) OverlayImage.getAttribute("textureName");
        System.out.println("texname = " + tn);
        
        OverlayImage.setAttribute("ImageOrientationQ", new Quaternion().setIdentity());
//        OverlayImage.setAttribute("ImageTranslation", new Vector3f(0f,0f,0f));
             
        ImageVolumeUtil.buildTexture(OverlayImage, false);
//        setupImageTexture(OverlayImage, 2);

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
        //renderOverlay();
    }
        
    public void setCTImage(ImageVolume image) {
        System.out.println("ImageCanvasGL::setImage()");
        
        if (image != CTimage) {
            setIsDirty(true);
            setOverlayImage(null);
        }
        
        CTimage = image;
        

//        theImage.setAttribute("textureName", null); ///////HACK
//        Integer tn = (Integer) CTimage.getAttribute("textureName");
//        System.out.println("texname = " + tn);
        
        
        ImageVolumeUtil.setupImageOrientationInfo(image);
       
//        Float v = (Float)MRimage.getAttribute("WindowCenter");
//        if (v != null) {
//            this.ct_center = v.intValue();
//        }
//        v = (Float)MRimage.getAttribute("WindowWidth");
//        if (v != null) {
//            this.ct_window = v.intValue();
//        }
//        System.out.println("CT center = " + ct_center + ", window = " + ct_window);
//        needsRendering = true;
//        needsLUT = true;

//        if (needsTexture) {
//            buildTexture();
//        }
        // Really just a test for now of combining Java2D
        // graphics with GL via glDisplayPixels()
//        renderOverlay();
    }
    
    public void setMRImage(ImageVolume image) {
        System.out.println("ImageCanvasGL::setImage()");

        if (image != MRimage) setIsDirty(true);

        MRimage = image;
        
        if (MRimage == null) {
            release();
        }


//        theImage.setAttribute("textureName", null); ///////HACK
//        Integer tn = (Integer) MRimage.getAttribute("textureName");
//        System.out.println("texname = " + tn);
                
        ImageVolumeUtil.setupImageOrientationInfo(image);
        
//        Float v = (Float)MRimage.getAttribute("WindowCenter");
//        if (v != null) {
//            this.mr_center = v.intValue();
//        }
//        v = (Float)MRimage.getAttribute("WindowWidth");
//        if (v != null) {
//            this.mr_window = v.intValue();
//        }
        
//        System.out.println("MR center = " + mr_center + ", window = " + mr_window);
        
//        needsRendering = true;
//        needsLUT = true;

//        if (needsTexture) {
//            buildTexture();
//        }
        // Really just a test for now of combining Java2D
        // graphics with GL via glDisplayPixels()
//        renderOverlay();
    }
    
    public void setImageChannel(int channel) {
        imageChannel = channel;
//        needsRendering = true;
    }

    private void renderOverlay() {

        overlay = new BufferedImage(256, 64, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) overlay.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gc.setColor(new Color(0.2f, 0.2f, 0.5f, 0.5f));
        gc.fillRect(0, 0, overlay.getWidth() - 1, overlay.getHeight() - 1);
        gc.setColor(new Color(0.2f, 0.2f, 0.5f, 0.8f));
        gc.drawRect(0, 0, overlay.getWidth() - 1, overlay.getHeight() - 1);

        gc.setFont(new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, 15));

        if (CTimage != null) {
            int ypos = 16;

            try {

                String value = CTimage.getAttribute("PatientsName").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("PatientID").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);

                value = CTimage.getAttribute("PatientsSex").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 86, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 85, ypos);
                ypos += 15;

                value = "DOB: " + CTimage.getAttribute("PatientsBirthDate").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("InstitutionName").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("AcquisitionDate").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
                gc.drawString(value, 7, ypos + 1);
                gc.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
                gc.drawString(value, 6, ypos);
                ypos += 15;

                value = CTimage.getAttribute("AcquisitionTime").toString();
                gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
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

    public int getSlice() {
        return slice;
    }

    public void setSlice(int value) {
        if (value != slice) {
            setIsDirty(true);
            slice = value;
        }
    }

    public int getTime() {
        return imageTime;
    }

    public void setTime(int value) {
        if (value != imageTime) {
            if (CTimage != null && CTimage.getDimension(3).getSize() > 1) {
                imageTime = value;
            } else {
                imageTime = 0;
            }
        }
    }

    @Override
    public void render() {

        if (!getVisible()) return;

            if (CTimage != null)
                ImageVolumeUtil.buildTexture(CTimage); ///HACK
            if (MRimage != null)
                ImageVolumeUtil.buildTexture(MRimage); ///HACK

            
        
        Main.glPushAttrib(GL_ENABLE_BIT | GL_TRANSFORM_BIT | GL_LINE_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT);
        
        glDisable(GL_DEPTH_TEST);
        displayFlat();
        glEnable(GL_DEPTH_TEST);
        
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
            glLoadIdentity();

            org.lwjgl.opengl.GL11.glOrtho(0.0f, Display.getWidth(), 0.0f, Display.getHeight(), -1000, 2000); // TODO: move this elsewhere, to overlay managment

                glEnable(GL_BLEND);
                glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

            glMatrixMode(GL_MODELVIEW);
            Main.glPushMatrix();
                glLoadIdentity();
                    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
                    lightPosition.put(Display.getWidth() / 2).put(Display.getHeight() / 2).put(10000.0f).put(1f).flip();
                    glLight(GL_LIGHT0, GL_POSITION, lightPosition);	// sets light position
                    
                    renderChildren();
                    
             glMatrixMode(GL_MODELVIEW);
             Main.glPopMatrix();
            
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
        
        glMatrixMode(GL_MODELVIEW);
        
        Main.glPopAttrib();
                
        setIsDirty(false);

    }

    public ImageVolume getImage() {
        return CTimage;
    }

    public void computeLUT() {
        /*
        for (int i=0; i<256*256; i++) {
            //int value = (int)Math.max(0, (Math.min((float)(i-center)/(window/2.0)*128 + 127, 255)));
            int value = (int)Math.max(0, (Math.min((float)(i-center)/(window/2.0)*32768 + 32767, 65535)));
            lut[i] = value & 0xffff;
        }
         */
//        needsLUT = false;
    }

    public void release() {
        ImageVolumeUtil.releaseTextures(CTimage);
        ImageVolumeUtil.releaseTextures(MRimage);
        ImageVolumeUtil.releaseTextures(OverlayImage);
        
        if (shader != null)
        {
            shader.release();
            shader = null;
        }
        
        if (shaderGray != null)
        {
            shaderGray.release();
            shaderGray = null;
        }
    }

    private int nextPowerOfTwo(int value) {
        if (value > 1 && value <= 2) {
            value = 2;
        } else if (value > 2 && value <= 4) {
            value = 4;
        } else if (value > 4 && value <= 8) {
            value = 8;
        } else if (value > 8 && value <= 16) {
            value = 16;
        } else if (value > 16 && value <= 32) {
            value = 32;
        } else if (value > 32 && value <= 64) {
            value = 64;
        } else if (value > 64 && value <= 128) {
            value = 128;
        } else if (value > 128 && value <= 256) {
            value = 256;
        } else if (value > 256 && value <= 512) {
            value = 512;
        } else if (value > 512 && value <= 1024) {
            value = 1024;
        }
        return value;
    }
    
    private void setupImageTexture(ImageVolume image, int textureUnit) {
        if (image == null) {
           
            // if there is no image for this texture unit, zero it out
            glEnable(GL_TEXTURE_3D);
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
            
            glBindTexture(GL_TEXTURE_3D, 0);
            
            glActiveTexture(GL_TEXTURE0 + 0);
            glDisable(GL_TEXTURE_3D);

            return;
        }
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return;
        
        
        glEnable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        
        int textureName = tn.intValue();
        glBindTexture(GL_TEXTURE_3D, textureName);

        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        
        
        
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int idepth = image.getDimension(2).getSize();

            int texWidth = iWidth;
            int texHeight = iHeight;
            int texDepth = idepth;


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
            
            //canvasSize = 200f;
            
            float xzoom = canvasFOV/(xres*texWidth);
            float yzoom = canvasFOV/(yres*texHeight);
            float zzoom = -canvasFOV/(zres*texDepth);
            
            // Scale for in-plane/out-of-plane ratio
            ///////////////////////////////////////////////////////////
            //glScaled(1.0f, 1.0f, -zscaleFactor); // this assumes in-plane pixel dimesions are the same! //HACK
            // Canvas is 250mm wide
            //glScaled(1f/(xres*texWidth/200f), 1f/(yres*texHeight/200f), -zscaleFactor * 1f/(xres*texWidth/200f)); 
            glScaled(xzoom, yzoom, zzoom); 


            Quaternion imageOrientation = (Quaternion)image.getAttribute("ImageOrientationQ");
            if (imageOrientation == null) return;
            
            if (orientation==-1) {
                imageOrientation = new Quaternion().setIdentity();
            }
            
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
            glTranslatef((/*centerOfRotation.x + */ imageTranslation.x /* + imagePosition[0]*/)/ (canvasFOV),
                    (/*centerOfRotation.y + */ imageTranslation.y /* + imagePosition[1]*/) / (canvasFOV),
                    (/*centerOfRotation.z + */ imageTranslation.z /* + imagePosition[2]*/) / (canvasFOV));
            
           // Compute normal vectors for axial,coronal,sagittal planes
           //    - this puts the image in standard axial orientation no matter
           //       what the acquistion orientation was
            //   - we dont want to add the rotation from the image orientation quaternion here
            //     Once we have the imagePlaneNorm vector we pop the matrix stack
            //     to revert to using the image orientation rotation.
            //     Could do this without GL matrix functions I guess.
            //////
            Main.glPushMatrix();
            
                glLoadIdentity();
                // Assuming canonical axial plane, normal vector is in z-direction
                Vector4f tnorm = new Vector4f(0f, 0f, 1f, 0f);
                Vector4f xdir = new Vector4f(1f, 0f, 0f, 0f);
                Vector4f ydir = new Vector4f(0f, 1f, 0f, 0f);
                switch(orientation) {
                    case 0:
                        break;
                    case 1:
                        glRotated(-90.0, 0.0, 1.0, 0.0);
                        glRotated(90.0, 0.0, 0.0, 1.0);
                       break;
                    case 2:
                        glRotated(-90.0, 1.0, 0.0, 0.0);
                        //glRotated(180.0, 0.0, 0.0, 1.0);
                        break;                    
                }
                matrixBuf.rewind();
                
                if (image == CTimage) {
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    ctTexMatrix.load(matrixBuf);
                }
                else if (image == MRimage) {
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    mrTexMatrix.load(matrixBuf);
                }
                else if (image == OverlayImage) {
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    ovlyTexMatrix.load(matrixBuf);
                }
                
                Vector3f imagePlaneNorm = null;
                if (image == CTimage) {
                    imagePlaneNorm = new Vector3f(Matrix4f.transform(ctTexMatrix, tnorm, null)).normalise(null);
                    imagePlaneX = new Vector3f(Matrix4f.transform(ctTexMatrix, xdir, null)).normalise(null);
                    imagePlaneY = new Vector3f(Matrix4f.transform(ctTexMatrix, ydir, null)).normalise(null);
                }
                else if (image == MRimage) {
                    imagePlaneNorm = new Vector3f(Matrix4f.transform(mrTexMatrix, tnorm, null)).normalise(null);
                    imagePlaneX = new Vector3f(Matrix4f.transform(mrTexMatrix, xdir, null)).normalise(null);
                    imagePlaneY = new Vector3f(Matrix4f.transform(mrTexMatrix, ydir, null)).normalise(null);
                }
                else if (image == OverlayImage) {
                    imagePlaneNorm = new Vector3f(Matrix4f.transform(ovlyTexMatrix, tnorm, null)).normalise(null);
                    imagePlaneX = new Vector3f(Matrix4f.transform(ovlyTexMatrix, xdir, null)).normalise(null);
                    imagePlaneY = new Vector3f(Matrix4f.transform(ovlyTexMatrix, ydir, null)).normalise(null);
                }

            Main.glPopMatrix();
            ////////
            //

            //
            // Now we do this again to setup the transform that we need in order
            // map mouse actions on the image to dicom coordinate system.
            //////////////
            Main.glPushMatrix();
                glLoadIdentity();

                //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
                //fix voxel scaling
                glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);

                // huge assumption that in-plane pixels are square //HACK
                //glScaled(1.0f, 1.0f, -zscaleFactor);
                glScaled(xzoom, yzoom, zzoom); 
                
                switch(orientation) {
                    case 0:
                        break;
                    case 1:
                        glRotated(-90.0, 0.0, 1.0, 0.0);
                        glRotated(90.0, 0.0, 0.0, 1.0);
                       break;
                    case 2:
                        glRotated(-90.0, 1.0, 0.0, 0.0);
                        //glRotated(180.0, 0.0, 0.0, 1.0);
                        break;                    
                }
                
                float offset = Vector3f.dot(imagePlaneNorm, centerOfRotation);
                glTranslatef(0f, 0f, -offset/(canvasFOV));

                if (image == CTimage) {
                    matrixBuf.rewind();
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    ctTexMatrix.load(matrixBuf);
                }
                else if (image == MRimage) {
                    matrixBuf.rewind();
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    mrTexMatrix.load(matrixBuf);
                }
                else if (image == OverlayImage) {
                    matrixBuf.rewind();
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    ovlyTexMatrix.load(matrixBuf);
                }
                
            Main.glPopMatrix();
            /////////////////////
            //
            //

            switch(orientation) {
                case 0:
                    break;
                case 1:
                    glRotated(-90.0, 0.0, 1.0, 0.0);
                    glRotated(90.0, 0.0, 0.0, 1.0);
                   break;
                case 2:
                    glRotated(-90.0, 1.0, 0.0, 0.0);
                    //glRotated(180.0, 0.0, 0.0, 1.0);
                    break;                    
            }

            // slice position is the dot product of the current focal spot/target spot
            // with the image plane normal
            offset = Vector3f.dot(imagePlaneNorm, centerOfRotation);
            glTranslatef(0f, 0f, offset/(canvasFOV));
            
    }
    
    public Quaternion getPlaneQuaternion() {
            switch(orientation) {
                case 0:
                    return new Quaternion().setIdentity();
                case 1:
                    return new Quaternion(0.5f, 0.5f, -0.5f, 0.5f).normalise(null);
                case 2:
                    return new Quaternion(0.707f, 0f, 0f, 0.707f).normalise(null);
                default:
                    return new Quaternion();
             }        
    }
    
//    public void display() {
//
//        init();
//
//        if (pixels != null && needsTexture) {
//            //System.out.println("building texture...");
//            buildTexture();
//            //System.out.println("building texture done");
//        }
//
//        glColor3d(0.7, 0.7, 0.9);
//        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//
//        glPolygonMode(GL_FRONT, GL_FILL);
//
//        glEnable(GL_TEXTURE_3D);
//        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
//        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
//        glBindTexture(GL_TEXTURE_3D, textureName);
//        //System.out.println("textureName="+textureName);
//        //System.out.println(glu.gluErrorString(glGetError()));
//
//        double texsize = 0.5;
//        double xvertsize = 1.0;
//        double yvertsize = 1.0;
//        double slicepos = 0.0;
//
//        double aspect = 1.0f;
//        int idepth = 0;
//
//        glMatrixMode(GL_MODELVIEW);
//        glPushMatrix();
////        if (trackball != null) {
////                trackball.renderOpposite();
////        }
//        
//        if (CTimage != null) {
//
//            iWidth = CTimage.getDimension(0).getSize();
//            iHeight = CTimage.getDimension(1).getSize();
//            idepth = CTimage.getDimension(2).getSize();
//
//            //texWidth = nextPowerOfTwo(iWidth);
//            //texHeight = nextPowerOfTwo(iHeight);
//            //texDepth = nextPowerOfTwo(idepth);
//            texWidth = iWidth;
//            texHeight = iHeight;
//            texDepth = idepth;
//
//            float xSampleSpacing = CTimage.getDimension(0).getSampleWidth(0) * iWidth;
//            float ySampleSpacing = CTimage.getDimension(1).getSampleWidth(0) * iHeight;
//
//            aspect = (double) (xSampleSpacing) / (ySampleSpacing);
//            //System.out.println(iWidth + " x " + iHeight);
//            //System.out.println(texWidth + " x " + texHeight);
//            //System.out.println(xSampleSpacing + " x " + ySampleSpacing + " aspect: " + aspect);
//
//            xvertsize = xSampleSpacing;
//            yvertsize = ySampleSpacing;
//
//            slicepos = ((double) slice / idepth) - 0.5;
//
//            // Transformationso on Texture matrix
//            ///////////////////////////////////////////
//            glMatrixMode(GL_TEXTURE);
//            glLoadIdentity();
//
//            //System.out.println("mid z = " + (double)idepth/texDepth/2.0);
//            //fix voxel scaling
//            float xres = CTimage.getDimension(0).getSampleWidth(0);
//            float yres = CTimage.getDimension(1).getSampleWidth(1);
//            float zres = CTimage.getDimension(2).getSampleWidth(2);
//            glTranslated(0.5, 0.5, (double) idepth / texDepth / 2.0);
//
//            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
//            glScaled(1.0f, 1.0f, -zscaleFactor);
//
//            //glTranslated(-0.05, -0.06, 0.00);  /////HACK   manual registration for specific test dataset
//
//            // Flip from saggital to axial
////            glRotated(90.0, 0.0, 1.0, 0.0);
////            glRotated(90.0, 1.0, 0.0, 0.0);
//            FloatBuffer trackballBuffer = BufferUtils.createFloatBuffer(16);
//		Trackball.toMatrix4f(ctImageOrientation).store(trackballBuffer);
//		trackballBuffer.flip();
//		glMultMatrix(trackballBuffer);
//            
//
//            glTranslatef(centerOfRotation.x / (xres * iWidth), centerOfRotation.y / (yres * iHeight), centerOfRotation.z / (zres * idepth * zscaleFactor));
//
//            if (trackball != null) {
//                trackball.renderOpposite();
//            }
//            
//            // save the transformation from the image canvas to the texture volume
//            // so we can later transform mouse clicks into the texture volume and 
//            // hence the image volume
//            
//            matrixBuf.rewind();
//            glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
//            
//            //buf.flip();
//                        
//            texMatrix.load(matrixBuf);
//
//
//            if (shaderprogram != 0) {
//                glUseProgram(shaderprogram);
//                glUniform1f(centerUniform, (float) ct_center);
//                glUniform1f(windowUniform, (float) ct_window);
//                glUniform1f(thresholdUniform, (float) ct_threshold);
//            }
//
//            //Keep the image plane perpendicular to the camera
//            if (trackball != null) {
//                glMatrixMode(GL_MODELVIEW);
//                trackball.renderOpposite();
//            }
//            
//            glBegin(GL_QUADS);
//
//            glNormal3d(0.0, 0.0, 1.0);
//
//                glTexCoord3d(-0.5, 0.5, 0);
////                glVertex3d(x, y, 0.0);
//            glVertex3d(-xvertsize / 2d, yvertsize / 2d, 0.0);
//
//                glTexCoord3d(0.5, 0.5, 0);
// //               glVertex3d(x+width, y, 0.0);
//            glVertex3d(xvertsize / 2d, yvertsize / 2d, 0.0);
//
//                glTexCoord3d(0.5, -0.5, 0);
// //               glVertex3d(x+width, y+height, 0.0);
//            glVertex3d(xvertsize / 2d, -yvertsize / 2d, 0.0);
//
//                glTexCoord3d(-0.5, -0.5, 0);
////                glVertex3d(x, y+height, 0.0);
//            glVertex3d(-xvertsize / 2d, -yvertsize / 2d, 0.0);
//
////              The following works in terms of trackball rotations            
////            glTexCoord3d(0.5, -0.5, 0);
////            glVertex3d(-xvertsize / 2d, yvertsize / 2d, 0.0);
////
////            glTexCoord3d(0.5, 0.5, 0);
////            glVertex3d(-xvertsize / 2d, -yvertsize / 2d, 0.0);
////
////            glTexCoord3d(-0.5, 0.5, 0);
////            glVertex3d(xvertsize / 2d, -yvertsize / 2d, 0.0);
////
////            glTexCoord3d(-0.5, -0.5, 0);
////            glVertex3d(xvertsize / 2d, yvertsize / 2d, 0.0);
//
//            glEnd();
//
//            glUseProgram(0);
//
////            // Draw outline of the slice for debugging
////            glPushAttrib(GL_POLYGON_BIT | GL_ENABLE_BIT);
////                glDisable(GL_LIGHTING);
////                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
////                glEnable(GL_COLOR_MATERIAL);    // enables opengl to use glColor3f to define material color               
////                glColor4d(0.9, 0.9, 0.9, 1.0);
////                
////                glBegin(GL_QUADS);
////
////                    glNormal3d(0.0, 0.0, -1.0);
////
////                    glVertex3d(-xvertsize/2d, yvertsize/2d, 0.0);
////                    glVertex3d(-xvertsize/2d, -yvertsize/2d, 0.0);
////                    glVertex3d(xvertsize/2d, -yvertsize/2d, 0.0);
////                    glVertex3d(xvertsize/2d, yvertsize/2d, 0.0);
////
////                glEnd();
////
////            glPopAttrib();
//            //glLoadIdentity();
//
//            //glPopMatrix();
//        }
//
//        glDisable(GL_TEXTURE_3D);
//
//        // Draw outline of the slice for debugging
//        glPushAttrib(GL_POLYGON_BIT | GL_ENABLE_BIT);
//        
//            glDisable(GL_LIGHTING);
//            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
//            glEnable(GL_COLOR_MATERIAL);    // enables opengl to use glColor3f to define material color               
//            glEnable(GL_BLEND);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//            glColor4d(0.3, 0.5, 1f, 0.2);
//
//            glBegin(GL_QUADS);
//
//                glNormal3d(0.0, 0.0, -1.0);
//
//                glVertex3d(-xvertsize / 2d, yvertsize / 2d, 0.0);
//                glVertex3d(-xvertsize / 2d, -yvertsize / 2d, 0.0);
//                glVertex3d(xvertsize / 2d, -yvertsize / 2d, 0.0);
//                glVertex3d(xvertsize / 2d, yvertsize / 2d, 0.0);
//
//            glEnd();
//        
//        glPopAttrib();
//
//        // Overlay demographics
//        if (overlay != null) {
//            glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT);
//            
//            byte buf[] = (byte[]) overlay.getRaster().getDataElements(0, 0, overlay.getWidth(), overlay.getHeight(), null);
//
//            glMatrixMode(GL_PROJECTION);
//            glPushMatrix();
//            glLoadIdentity();
//
//            glMatrixMode(GL_MODELVIEW);
//            glPushMatrix();
//            glLoadIdentity();
//
//            glEnable(GL_BLEND);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//            glPixelZoom(1.0f, -1.0f);
//            glRasterPos2d(-1.0, 1.0);
//
//            glDisable(GL_CLIP_PLANE0);
//            glDisable(GL_CLIP_PLANE1);
//            glDisable(GL_DEPTH_TEST);
//
//            ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
//            bbuf.put(buf, 0, buf.length);
//            bbuf.flip();
//            glDrawPixels(overlay.getWidth(), overlay.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
//
//            //      }
//            //     glPopMatrix();
//            //  }
//            //  glMatrixMode(GL_PROJECTION);
//            //   glPopMatrix();
//            glDisable(GL_BLEND);
//            
//            glMatrixMode(GL_MODELVIEW);
//            glPopMatrix();
//            glMatrixMode(GL_PROJECTION);
//            glPopMatrix();
//                 
//            glPopAttrib();
//        }
//        // Draw a frame around the canvas/////////
//        glMatrixMode(GL_PROJECTION);
//        glPushMatrix();
//        glLoadIdentity();
//
//        glMatrixMode(GL_MODELVIEW);
//        glPushMatrix();
//        glLoadIdentity();
//
//        glColor4d(0.1, 0.1, 0.3, 1.0);
//        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
////        glLineWidth(2.0f);
//        
//        glBegin(GL_QUADS);
//
//            glNormal3d(0.0, 0.0, 1.0);
//
//            glVertex3d(-1.0, 1.0, 0.0);
//
//            glVertex3d(1.0, 1.0, 0.0);
//
//            glVertex3d(1.0, -1.0, 0.0);
//
//            glVertex3d(-1.0, -1.0, 0.0);
//
//        glEnd();
//        
//        glMatrixMode(GL_MODELVIEW);
//        glPopMatrix();
//
//        glMatrixMode(GL_PROJECTION);
//        glPopMatrix();
//        ////////////////////////////////////////////
//        
//        
//        glMatrixMode(GL_MODELVIEW);
////        if (trackball != null) {
//                glPopMatrix();
////        }
//        
//        glPushMatrix();
////        glTranslatef(-centerOfRotation.x , -centerOfRotation.y , -centerOfRotation.z );
//
//        // draw image volume cube proxy
//        if (CTimage != null) {
//            float xSize = CTimage.getDimension(0).getSampleWidth(0) * iWidth;
//            float ySize = CTimage.getDimension(1).getSampleWidth(0) * iHeight;
//            float zSize = CTimage.getDimension(2).getSampleWidth(0) * idepth;
//            Vector3f zdir = Vector3f.cross(ctImageOrientationX, ctImageOrientationY, null);
//            zdir.x *= zSize;
//            zdir.y *= zSize;
//            zdir.z *= zSize;
//            
//            glPushAttrib(GL_ENABLE_BIT);
//            glDisable(GL_LIGHTING);
//            glDisable(GL_CLIP_PLANE0);
//            glDisable(GL_CLIP_PLANE1);
//            
//            // mark image volume origin position
//            glBegin(GL_POINTS);
//                glColor4f(1f, 0f, 0f, 0.5f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                glColor4f(0f, 1f, 0f, 0.5f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//        glEnd();
//            
//            //box begin and end slice planes
//            glBegin(GL_QUADS);
//
//                glNormal3d(0.0, 0.0, 1.0);
//
//                glColor4f(1f, 0f, 0f, 0.5f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//
//                glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//
//            glEnd();
//            
//            glBegin(GL_LINES);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f - ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f - ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f - ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x + ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y + ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z + ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f - ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f - ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f - ctImageOrientationZ.z*zSize/2f);
//                 glVertex3f(-centerOfRotation.x - ctImageOrientationX.x*xSize/2f + ctImageOrientationY.x*ySize/2f + ctImageOrientationZ.x*zSize/2f,
//                            -centerOfRotation.y - ctImageOrientationX.y*xSize/2f + ctImageOrientationY.y*ySize/2f + ctImageOrientationZ.y*zSize/2f,
//                            -centerOfRotation.z - ctImageOrientationX.z*xSize/2f + ctImageOrientationY.z*ySize/2f + ctImageOrientationZ.z*zSize/2f);
//            glEnd();
//            glPopAttrib();
//        }
//        
//        glPopMatrix();
//
//    }

    public void displayFlat() {

        init();
        
        Main.glPushAttrib(GL_ENABLE_BIT);

            if (CTimage != null)
                ImageVolumeUtil.buildTexture(CTimage); ///HACK
            if (MRimage != null)
                ImageVolumeUtil.buildTexture(MRimage); ///HACK
            if (OverlayImage != null)
                ImageVolumeUtil.buildTexture(OverlayImage); ///HACK

        // Go into ORTHO projection, but save any 
        glMatrixMode(GL_PROJECTION);
        Main.glPushMatrix();
        glLoadIdentity();
        
        org.lwjgl.util.glu.GLU.gluOrtho2D(0.0f, Display.getWidth(), 0.0f, Display.getHeight());

        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
//        glLoadIdentity();
        
        glColor4d(0.7, 0.7, 0.9, 1.0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        //glPolygonMode(GL_FRONT, GL_FILL);
        
        if (CTimage != null) {
            setupImageTexture(CTimage, 0);
        }
        
        if (MRimage != null) {
            setupImageTexture(MRimage, 1);
        }
        if (OverlayImage != null) {
            setupImageTexture(OverlayImage, 2);
        }
       

        float xres=1f, yres=1f, zres=1f;
        
        double aspect = 1.0f;
        int iWidth = 0;
        int iHeight = 0;
        int iDepth = 0;
        
        int texWidth = iWidth;
        int texHeight = iHeight;
        int texDepth = iDepth;

        if (CTimage != null) {
            iWidth = CTimage.getDimension(0).getSize();
            iHeight = CTimage.getDimension(1).getSize();
            iDepth = CTimage.getDimension(2).getSize();

            texWidth = iWidth;
            texHeight = iHeight;
            texDepth = iDepth;


            //fix voxel scaling
            xres = CTimage.getDimension(0).getSampleWidth(0);
            yres = CTimage.getDimension(1).getSampleWidth(1);
            zres = CTimage.getDimension(2).getSampleWidth(2);
           
            int shaderprogram = 0;
            if (shader != null && shaderGray != null) {
                if (useGrayScale) {
                    shaderprogram = shaderGray.getShaderProgramID();
                }
                else {
                    shaderprogram = shader.getShaderProgramID();
                }
            }
            
            if (shaderprogram != 0) {
                glUseProgram(shaderprogram);
                glUniform1f(centerUniform, (float) ct_center);
                glUniform1f(windowUniform, (float) ct_window);
                glUniform1f(thresholdUniform, (float) ct_threshold);
                int mrcenterUniform = glGetUniformLocation(shaderprogram, "mr_center");
                int mrwindowUniform = glGetUniformLocation(shaderprogram, "mr_window");
                
                int texLoc = glGetUniformLocation(shaderprogram, "ct_tex");
                glUniform1i(texLoc, 0);
                texLoc = glGetUniformLocation(shaderprogram, "mr_tex");
                glUniform1i(texLoc, 1);
                texLoc = glGetUniformLocation(shaderprogram, "ovly_tex");
                glUniform1i(texLoc, 2);
                
                texLoc = glGetUniformLocation(shaderprogram, "ct_rescale_slope");
                glUniform1f(texLoc, ct_rescale_slope);
                texLoc = glGetUniformLocation(shaderprogram, "ct_rescale_intercept");
                glUniform1f(texLoc, ct_rescale_intercept);

                texLoc = glGetUniformLocation(shaderprogram, "mr_rescale_slope");
                glUniform1f(texLoc, mr_rescale_slope);
                texLoc = glGetUniformLocation(shaderprogram, "mr_rescale_intercept");
                glUniform1f(texLoc, mr_rescale_intercept);
                
                glUniform1f(mrcenterUniform, (float) mr_center);
                glUniform1f(mrwindowUniform, (float) mr_window);
                int showMRUniform = glGetUniformLocation(shaderprogram, "showMR");
                glUniform1i(showMRUniform, showMR ? 1:0);
            }

            glDisable(GL_BLEND);
        glColor4d(1, 1, 1, 1);
        glBegin(GL_QUADS);

                glNormal3d(0.0, 0.0, 1.0);

                glTexCoord3d(-0.5, 0.5, 0);
                glVertex3d(canvasX, canvasY, 0.0);

                glTexCoord3d(0.5, 0.5, 0);
                glVertex3d(canvasX+canvasSize, canvasY, 0.0);

                glTexCoord3d(0.5, -0.5, 0);
                glVertex3d(canvasX+canvasSize, canvasY+canvasSize, 0.0);

                glTexCoord3d(-0.5, -0.5, 0);
                glVertex3d(canvasX, canvasY+canvasSize, 0.0);

            glEnd();

            glUseProgram(0);

            glLoadIdentity();

        }

        glActiveTexture(GL_TEXTURE0 + 2);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 1);
        glDisable(GL_TEXTURE_3D);
        glActiveTexture(GL_TEXTURE0 + 0);
        glDisable(GL_TEXTURE_3D);        
        

        // Draw outline of the slice for debugging
        Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT);
        
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CLIP_PLANE0);
        glDisable(GL_CLIP_PLANE1);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glEnable(GL_COLOR_MATERIAL);    // enables opengl to use glColor3f to define material color               
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glColor4d(0.15, 0.15, 0.3f, 1f);
        glLineWidth(2f);

        glBegin(GL_LINE_STRIP);

                glVertex3d(canvasX, canvasY+canvasSize, 0.0);
                glVertex3d(canvasX, canvasY, 0.0);
                glVertex3d(canvasX+canvasSize, canvasY, 0.0);
                glVertex3d(canvasX+canvasSize, canvasY+canvasSize, 0.0);
                glVertex3d(canvasX, canvasY+canvasSize, 0.0);

        glEnd();
        
        //canvasSize = 200f;
        
        float xoff = Vector3f.dot(imagePlaneX, centerOfRotation);
        float xpos = canvasSize/2.0f + xoff*(canvasSize/canvasFOV);
        
        float yoff = Vector3f.dot(imagePlaneY, centerOfRotation);
        float ypos = canvasSize/2.0f - yoff*(canvasSize/canvasFOV);
        
        if (targetingEnabled) {
            glBegin(GL_LINES);
                    glColor4f(0.3f, 0.8f, 0.3f, 0.3f);

                    glVertex3d(xpos+canvasX, canvasY+canvasSize, 0.0);
                    glVertex3d(xpos+canvasX, canvasY, 0.0);

                    glVertex3d(canvasX+canvasSize, ypos+canvasY, 0.0);
                    glVertex3d(canvasX,       ypos+canvasY, 0.0);
           glEnd();
        }
        
        Main.glPopAttrib();

        // Restore matrices
        glMatrixMode(GL_MODELVIEW);
        Main.glPopMatrix();
        
        glMatrixMode(GL_PROJECTION);
        Main.glPopMatrix();
        
        glMatrixMode(GL_MODELVIEW);

        if (CTimage != null && displayPosition && targetingEnabled) {
            glEnable(GL_BLEND);
            glBlendFuncSeparate (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            Rectangle textRect = new Rectangle(bounds);
            textRect.x += 25;
            textRect.y += 2;
            String positionText = String.format("R %3.1f  A %3.1f  S %3.1f", -centerOfRotation.x, -centerOfRotation.y, centerOfRotation.z);
            this.renderText(positionText, textRect, stdfont, Color.white, true, VPosFormat.VPOSITION_BOTTOM, HPosFormat.HPOSITION_LEFT);
        }
        
        Main.glPopAttrib();
        
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
        //System.out.println("zoom = " + value);
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

        if (shader == null) {
            initShaders();
        }
    }

    public void initShaders() {

        if (shader != null && shaderGray != null) {
            return;
        } // already compiled the shader
        
        shader = new ShaderProgram();
        shaderGray = new ShaderProgram();

        String vsrc = "void main(void)\n"
                + "{\n"
                + "   gl_TexCoord[0]  = gl_TextureMatrix[0] * gl_MultiTexCoord0;\n"
                + "   gl_TexCoord[1]  = gl_TextureMatrix[1] * gl_MultiTexCoord0;\n"
                + "   gl_TexCoord[2]  = gl_TextureMatrix[2] * gl_MultiTexCoord0;\n"
                + "   gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
                + "}";

        String fsrc = "#version 120 \n"
                + "uniform float center;\n"
                + "uniform float window;\n"
                + "uniform float mr_center;\n"
                + "uniform float mr_window;\n"
                + "uniform float mr_rescale_intercept;\n"
                + "uniform float mr_rescale_slope;\n"
                + "uniform float ct_rescale_intercept;\n"
                + "uniform float ct_rescale_slope;\n"
                + "uniform float threshold;\n"
                + "uniform int showMR;\n"
                + "uniform sampler3D ct_tex;\n"
                + "uniform sampler3D mr_tex;\n"
                + "uniform sampler3D ovly_tex;\n"
                + "void main(void)\n"
                + "{\n"
                + "       vec4 color;\n"
                + "        float ctsample = texture3D(ct_tex, gl_TexCoord[0].stp).r * 65535.0 * ct_rescale_slope + ct_rescale_intercept;\n"
                + "        float mrsample = texture3D(mr_tex, gl_TexCoord[1].stp).r * 65535.0 * mr_rescale_slope + mr_rescale_intercept;\n"
                + "//        if (ctsample < threshold-150 && (showMR!=1 || mrsample < threshold))\n"
                + "//           discard;\n"
                + "        float ovlyTexVal = texture3D(ovly_tex, gl_TexCoord[2].stp).r; // * 32767.0;\n"
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
                + "        float ctval = (ctsample - center)/(window) + 0.5;\n"
                + "        float mrval = (mrsample - mr_center)/(mr_window) + 0.5;\n"
                + "        color.rgb = vec3(ctval*0.6, ctval, ctval*0.6);\n"
                + "        if (showMR==1 && ctsample < threshold) \n"
                + "             color.rgb = vec3(mrval);\n"
                + "        color.a = 1;\n"
                + "        gl_FragColor = color * ovlyColor;\n"
                + "}";
        
        String fsrcGray = "#version 120 \n"
                + "uniform float center;\n"
                + "uniform float window;\n"
                + "uniform float mr_center;\n"
                + "uniform float mr_window;\n"
                + "uniform float mr_rescale_intercept;\n"
                + "uniform float mr_rescale_slope;\n"
                + "uniform float ct_rescale_intercept;\n"
                + "uniform float ct_rescale_slope;\n"
                + "uniform float threshold;\n"
                + "uniform int showMR;\n"
                + "uniform sampler3D ct_tex;\n"
                + "uniform sampler3D mr_tex;\n"
                + "uniform sampler3D ovly_tex;\n"
                + "void main(void)\n"
                + "{\n"
                + "       vec4 color;\n"
                + "        float ctsample = texture3D(ct_tex, gl_TexCoord[0].stp).r * 65535.0 * ct_rescale_slope + ct_rescale_intercept;\n"
                + "        float mrsample = texture3D(mr_tex, gl_TexCoord[1].stp).r * 65535.0 * mr_rescale_slope + mr_rescale_intercept;\n"
                + "//        if (ctsample < threshold-150 && (showMR!=1 || mrsample < threshold))\n"
                + "//           discard;\n"
                + "        float ovlyTexVal = texture3D(ovly_tex, gl_TexCoord[2].stp).r; // * 32767.0;\n"
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
                + "        float ctval = (ctsample - center)/(window) + 0.5;\n"
                + "        float mrval = (mrsample - mr_center)/(mr_window) + 0.5;\n"
                + "        color.rgb = vec3(ctval, ctval, ctval);\n"
                + "        if (showMR==1 && ctsample < threshold) \n"
                + "             color.rgb = vec3(mrval);\n"
                + "        color.a = 1;\n"
                + "        gl_FragColor = color * ovlyColor;\n"
                + "}";
        
        shader.addShaderSourceString(GL_VERTEX_SHADER, vsrc);

        shader.addShaderSourceString(GL_FRAGMENT_SHADER, fsrc);
        
        shader.compileShaderProgram();
        
        
        shaderGray.addShaderSourceString(GL_VERTEX_SHADER, vsrc);
        
        shaderGray.addShaderSourceString(GL_FRAGMENT_SHADER, fsrcGray);
        
        shaderGray.compileShaderProgram();

        shader.start();
        
            int shaderProgramID = shader.getShaderProgramID();

            centerUniform = glGetUniformLocation(shaderProgramID, "center");
            windowUniform = glGetUniformLocation(shaderProgramID, "window");
            thresholdUniform = glGetUniformLocation(shaderProgramID, "threshold");

        shader.stop();

    }

    @Override
    public boolean MouseIsInside(float mouseX, float mouseY) {
        return (mouseX >= canvasX &&
                        mouseX < canvasX + canvasSize &&
                        mouseY >= canvasY &&
                        mouseY < canvasY + canvasSize);
    }
    
    @Override
    public boolean OnMouse(float mouseX, float mouseY, boolean button1down, boolean button2down, int dwheel) {
            if (super.OnMouse(mouseX, mouseY, button1down, button2down, dwheel)) {
                return true;
            }
            
            if (MouseIsInside(mouseX, mouseY)) {
                if (!displayPosition) {
                    displayPosition = true;
                    setIsDirty(true);
                }
                
                if (button1down && !this.hasGrabbed()) {
                    this.grabMouse(mouseX, mouseY);
                }
            }
            else if (displayPosition) {
                displayPosition = false;
                setIsDirty(false);
            }
                        
            if (MouseIsInside(mouseX, mouseY) && dwheel != 0) {

                Vector4f pos = new Vector4f();
                Vector4f texpos = new Vector4f();
                
                float xoff = Vector3f.dot(imagePlaneX, centerOfRotation)/canvasSize;
                float yoff = Vector3f.dot(imagePlaneY, centerOfRotation);
                
                Vector3f imagePlaneZ = new Vector3f();
                Vector3f.cross(imagePlaneX, imagePlaneY, imagePlaneZ);
                
                float wheelIncr = dwheel > 0f ? 1f : -1f;
                float scaleWheel = 0.1f;  // 0.1mm
                
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    scaleWheel *= 10f;
                }
                
                Vector3f zoffset = (Vector3f)imagePlaneZ.scale(wheelIncr * scaleWheel);
                
                Vector3f.add(centerOfRotation, zoffset, centerOfRotation);
                
                fireActionEvent();

                setIsDirty(true);
 
                return true;
            }
                       
            if (!mouseGrabbed) {
                if (mouseX < canvasX ||
                        mouseX > canvasX + canvasSize ||
                        mouseY < canvasY ||
                        mouseY > canvasY + canvasSize) {
                    return false;
                }
                
                if (button1down && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                    fireActionEvent("toggleCanvasZoom");
                    return true;
                }
                
                if (button2down && !rightMouseButtonDown) {
                    rightMouseButtonDown = true;
                    fireActionEvent("rightClick");
                }
                else if (!button2down && rightMouseButtonDown == true) {
                    rightMouseButtonDown = false;
                }
            }
            
            if (button1down != true) {
                mouseGrabbed = false;
                this.ungrabMouse();
                return false;
            }
            
            if (this.hasGrabbed()) {

                float mx = Math.max(0f, Math.min(canvasSize, (float) mouseX - canvasX));
                float my = Math.max(0f, Math.min(canvasSize, (float) mouseY - canvasY));

                Vector4f pos = new Vector4f();
                Vector4f texpos = new Vector4f();
                pos.x = -(mx / canvasSize - 0.5f);
                pos.y = (my / canvasSize - 0.5f);
                pos.z = (dwheel * 0.0001f);//0f;
                pos.w = 1f;
                Matrix4f.transform(ctTexMatrix, pos, texpos);
                //            System.out.println("texpos 1: " + texpos);

                try {
                    float iwidth = CTimage.getDimension(0).getSize();
                    float iheight = CTimage.getDimension(1).getSize();
                    float idepth = CTimage.getDimension(2).getSize();

                    float xres = CTimage.getDimension(0).getSampleWidth(0);
                    float yres = CTimage.getDimension(1).getSampleWidth(0);
                    float zres = CTimage.getDimension(2).getSampleWidth(0);

                    texpos.x = -(texpos.x - 0.5f) * (xres * iwidth); /// TODO
                    texpos.y = -(texpos.y - 0.5f) * (yres * iheight);
                    texpos.z = -(texpos.z - 0.5f) * (zres * idepth);

                    //skull.setPos(texpos.x, texpos.y, texpos.z);
                    centerOfRotation.set(texpos.x, texpos.y, -texpos.z);
                    fireActionEvent();

                    setIsDirty(true);
                } catch (NullPointerException e) {
                    return true;
                }
            }
            
            return true;
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
