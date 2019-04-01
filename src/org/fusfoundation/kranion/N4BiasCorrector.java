///*
// * The MIT License
// *
// * Copyright 2019 Focused Ultrasound Foundation.
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//package org.fusfoundation.kranion;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import org.fusfoundation.kranion.model.image.ImageVolume;
//import org.itk.simple.Command;
//import org.itk.simple.EventEnum;
//import org.itk.simple.PixelIDValueEnum;
//import org.itk.simple.VectorDouble;
//import org.itk.simple.OtsuThresholdImageFilter;
//import org.itk.simple.N4BiasFieldCorrectionImageFilter;
//import org.itk.simple.ProcessObject;
//import org.itk.simple.VectorUInt32;
//import org.lwjgl.util.vector.Matrix4f;
//import org.lwjgl.util.vector.Quaternion;
//import org.lwjgl.util.vector.Vector3f;
//
///**
// *
// * @author jsnell
// */
//public class N4BiasCorrector {
//    private ImageVolume source;
//    private org.itk.simple.Image workingImage, maskImage;
//    
//    public N4BiasCorrector(ImageVolume img) {
//        source = img;
//    }
//    
//    class MyCommand extends Command {
//
//        private ProcessObject m_ProcessObject;
//
//        public MyCommand(ProcessObject po) {
//            super();
//            m_ProcessObject = po;
//        }
//
//        public void execute() {
//            double progress = m_ProcessObject.getProgress();
//            System.out.format("%s Progress: %f\n", m_ProcessObject.getName(), progress);
//        }
//    }
//    
//    public void execute() {
//        workingImage = copyToITKimage(source);
//        
//        //TODO: looks like we need to always resample the volume into isometic voxels (1x1x1mm) for N4 to work
//                
//        System.out.println("Otsu");
//        OtsuThresholdImageFilter otsuThresh = new OtsuThresholdImageFilter();
//        otsuThresh.setDebug(true);
//        maskImage = otsuThresh.execute(workingImage, (short)0, (short)1, 200, false, (short)1 );
//        
//        System.out.println("Working Image:\n" + workingImage);
//        System.out.println("Mask:\n" + maskImage);
//        
//        
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
//        
//        System.out.println("N4");
//        LocalDateTime now = LocalDateTime.now();  
//        System.out.println(dtf.format(now));
//        
//        N4BiasFieldCorrectionImageFilter biasFilter = new N4BiasFieldCorrectionImageFilter();
//        
//        biasFilter.setDebug(true);
//        
//        MyCommand cmd = new MyCommand(biasFilter);
//        biasFilter.addCommand(EventEnum.sitkAnyEvent, cmd);
//                
//        org.itk.simple.VectorUInt32 iterations = new org.itk.simple.VectorUInt32();
//        iterations.add(50);
//        iterations.add(50);
//        iterations.add(50);
//        iterations.add(50);
//        
//        org.itk.simple.VectorUInt32 controlPoints = new org.itk.simple.VectorUInt32();
//        controlPoints.add(4);
//        controlPoints.add(4);
//        controlPoints.add(4);
//        
//        workingImage = biasFilter.execute(workingImage, maskImage, 0.001, iterations, 0.15, 0.01, (long)200, controlPoints, 3, true, (short)1);
//        
//        
//        System.out.println("Done.");
//        now = LocalDateTime.now();  
//        System.out.println(dtf.format(now));
//        
//        copyFromITKimage(workingImage, source);
//        
//    }
//    
//    private void copyFromITKimage(org.itk.simple.Image src, ImageVolume dst) {
//        if (src == null || dst == null) return;
//        
//            int iWidth = dst.getDimension(0).getSize();
//            int iHeight = dst.getDimension(1).getSize();
//            int iDepth = dst.getDimension(2).getSize();
//            
//            short[] voxels = (short[])dst.getData();
//            
//            org.itk.simple.VectorUInt32 pos = new org.itk.simple.VectorUInt32(3);
//            int framesize = iHeight * iWidth;
//            
//            for (int i = 0; i < iWidth; i++) {
//                int percentDone = (int)((float)i/iWidth*100f);
//                if (i % 25 == 0) {
//                    System.out.println(percentDone + "% Copied.");
//                }
//                pos.set(0, i);
//                for (int j = 0; j < iHeight; j++) {
//                    pos.set(1, j);
//                    for (int k = 0; k < iDepth; k++) {
//                        pos.set(2, k);
//                        voxels[i + j*iWidth + k*framesize] = (short)(src.getPixelAsFloat(pos));
//                    }
//                }
//            }
//        
//    }
//    
//    private org.itk.simple.Image copyToITKimage(ImageVolume image) {
//        
//            int iWidth = image.getDimension(0).getSize();
//            int iHeight = image.getDimension(1).getSize();
//            int iDepth = image.getDimension(2).getSize();
//            
//            float xres = image.getDimension(0).getSampleWidth(0);
//            float yres = image.getDimension(1).getSampleWidth(1);
//            float zres = image.getDimension(2).getSampleWidth(2);
//
//            org.itk.simple.Image itkImage = new org.itk.simple.Image(iWidth, iHeight, iDepth, PixelIDValueEnum.sitkFloat32);
//            
//            //TEST
//            xres = yres = zres = 1f;
//            
//            org.itk.simple.VectorDouble res = new org.itk.simple.VectorDouble(3);
//            res.set(0, xres);
//            res.set(1, yres);
//            res.set(2, zres);
//                    
//            itkImage.setSpacing(res);
//                        
//            Quaternion orientation = (Quaternion)image.getAttribute("ImageOrientationQ");
//            if (orientation != null) {
//                Matrix4f mat = Trackball.toMatrix4f(orientation);
//
//                VectorDouble dir = new VectorDouble(9);
//                dir.set(0, mat.m00);
//                dir.set(1, mat.m01);
//                dir.set(2, mat.m02);
//                dir.set(3, mat.m10);
//                dir.set(4, mat.m11);
//                dir.set(5, mat.m12);
//                dir.set(6, mat.m20);
//                dir.set(7, mat.m21);
//                dir.set(8, mat.m22);
//
//                itkImage.setDirection(dir);
//            }
//            
//            float[] imagePosition = (float[])image.getAttribute("ImagePosition");
//            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
//            if (imageTranslation == null) imageTranslation = new Vector3f();
//            
//            VectorDouble origin = new VectorDouble(3);
//                origin.set(0, -iWidth*xres/2f + imageTranslation.x);
//                origin.set(1, -iHeight*yres/2f + imageTranslation.y);
//                origin.set(2, -iDepth*zres/2f + imageTranslation.z);
//                
//            itkImage.setOrigin(origin);
//                        
//            short[] voxels = (short[])image.getData();
//            
//            org.itk.simple.VectorUInt32 pos = new org.itk.simple.VectorUInt32(3);
//            
//            int min=32768;
//            int max = 0;
//            
//            int framesize = iHeight * iWidth;
//            
//            for (int i = 0; i < iWidth; i++) {
//                int percentDone = (int)((float)i/iWidth*100f);
//                if (i % 25 == 0) {
//                    System.out.println(percentDone + "% Copied.");
//                }
//                pos.set(0, i);
//                for (int j = 0; j < iHeight; j++) {
//                    pos.set(1, j);
//                    for (int k = 0; k < iDepth; k++) {
//                        pos.set(2, k);
//                        int value = voxels[i + j*iWidth + k*framesize] & 0x0fff;
//                        if (value<min) min = value;
//                        if (value>max) max = value;
//                        itkImage.setPixelAsFloat(pos, (float)value);
//                    }
//                }
//            }
//            
//            System.out.println("min=" + min + ", max=" + max);
//            
//            return itkImage;
//    }    
//}
