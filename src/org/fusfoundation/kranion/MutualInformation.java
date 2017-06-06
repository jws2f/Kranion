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

import java.nio.FloatBuffer;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import org.lwjgl.util.vector.*;

/**
 *
 * @author john
 */
public class MutualInformation {
    private ImageVolume CTimage, MRimage;
    public FloatBuffer matrixBuf = BufferUtils.createFloatBuffer(16);
    public Matrix4f ctTexMatrix = new Matrix4f();
    public Matrix4f mrTexMatrix = new Matrix4f();
    private Vector3f imagePlaneX = new Vector3f(1f, 0f, 0f);
    private Vector3f imagePlaneY = new Vector3f(0f, 1f, 0f);
    private Vector3f centerOfRotation = new Vector3f();
    
    private void setupImageTexture(ImageVolume image, int textureUnit) {
        if (image == null) return;
        
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glEnable(GL_TEXTURE_3D);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        
        Integer tn = (Integer) image.getAttribute("textureName");
        
        if (tn == null) return;
        
        int textureName = tn.intValue();
        glBindTexture(GL_TEXTURE_3D, textureName);  
        
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

            float zscaleFactor = ((float) texWidth * xres) / ((float) texDepth * zres);
            
            float  canvasFOV = 200f; //TODO: what value?
            
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
            
            FloatBuffer orientBuffer = BufferUtils.createFloatBuffer(16);
		Trackball.toMatrix4f(imageOrientation).store(orientBuffer);
		orientBuffer.flip();
                         
            // Rotation for image orientation
            ////////////////////////////////////////////////////////////
		glMultMatrix(orientBuffer);
            
            // Translation of center of rotation to origin (mm to texture coord values (0 -> 1))
            ////////////////////////////////////////////////////////////
            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            glTranslatef((/*centerOfRotation.x + */ imageTranslation.x)/ (canvasFOV),
                    (/*centerOfRotation.y + */ imageTranslation.y) / (canvasFOV),
                    (/*centerOfRotation.z + */ imageTranslation.z) / (canvasFOV));
            
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
                
//                switch(orientation) {
//                    case 0:
//                        break;
//                    case 1:
//                        glRotated(-90.0, 0.0, 1.0, 0.0);
//                        glRotated(90.0, 0.0, 0.0, 1.0);
//                       break;
//                    case 2:
//                        glRotated(-90.0, 1.0, 0.0, 0.0);
//                        //glRotated(180.0, 0.0, 0.0, 1.0);
//                        break;                    
//                }

                matrixBuf.rewind();
                
                if (image == CTimage) {
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    ctTexMatrix.load(matrixBuf);
                }
                else if (image == MRimage) {
                    glGetFloat(GL_TEXTURE_MATRIX, matrixBuf);
                    mrTexMatrix.load(matrixBuf);
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
                
//                switch(orientation) {
//                    case 0:
//                        break;
//                    case 1:
//                        glRotated(-90.0, 0.0, 1.0, 0.0);
//                        glRotated(90.0, 0.0, 0.0, 1.0);
//                       break;
//                    case 2:
//                        glRotated(-90.0, 1.0, 0.0, 0.0);
//                        //glRotated(180.0, 0.0, 0.0, 1.0);
//                        break;                    
//                }
                
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
                
            Main.glPopMatrix();
            /////////////////////
            //
            //

//            switch(orientation) {
//                case 0:
//                    break;
//                case 1:
//                    glRotated(-90.0, 0.0, 1.0, 0.0);
//                    glRotated(90.0, 0.0, 0.0, 1.0);
//                   break;
//                case 2:
//                    glRotated(-90.0, 1.0, 0.0, 0.0);
//                    //glRotated(180.0, 0.0, 0.0, 1.0);
//                    break;                    
//            }

            // slice position is the dot product of the current focal spot/target spot
            // with the image plane normal
            offset = Vector3f.dot(imagePlaneNorm, centerOfRotation);
            glTranslatef(0f, 0f, offset/(canvasFOV));
            
    }    
}
