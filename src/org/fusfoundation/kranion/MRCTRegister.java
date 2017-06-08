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
import  org.fusfoundation.kranion.model.image.*;
import org.itk.simple.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.*;

/**
 *
 * @author john
 */


public class MRCTRegister {
        
    public void register(ImageVolume ct_image, ImageVolume mr_image, Renderable renderable) {
        Runnable r = new registerWorker(ct_image, mr_image, renderable);
        Thread thread = new Thread(r);
        thread.start();
    }


    private org.itk.simple.Image copyToITKimage(ImageVolume image) {
        
            int iWidth = image.getDimension(0).getSize();
            int iHeight = image.getDimension(1).getSize();
            int iDepth = image.getDimension(2).getSize();
            
            float xres = image.getDimension(0).getSampleWidth(0);
            float yres = image.getDimension(1).getSampleWidth(1);
            float zres = image.getDimension(2).getSampleWidth(2);

            org.itk.simple.Image itkImage = new org.itk.simple.Image(iWidth, iHeight, iDepth, PixelIDValueEnum.sitkFloat32);
            
            org.itk.simple.VectorDouble res = new org.itk.simple.VectorDouble(3);
            res.set(0, xres);
            res.set(1, yres);
            res.set(2, zres);
                    
            itkImage.setSpacing(res);
            
            Quaternion orientation = (Quaternion)image.getAttribute("ImageOrientationQ");
            Matrix4f mat = Trackball.toMatrix4f(orientation);
            
            VectorDouble dir = new VectorDouble(9);
            dir.set(0, mat.m00);
            dir.set(1, mat.m01);
            dir.set(2, mat.m02);
            dir.set(3, mat.m10);
            dir.set(4, mat.m11);
            dir.set(5, mat.m12);
            dir.set(6, mat.m20);
            dir.set(7, mat.m21);
            dir.set(8, mat.m22);
 
            itkImage.setDirection(dir);
            
            float[] imagePosition = (float[])image.getAttribute("ImagePosition");
            Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
            if (imageTranslation == null) imageTranslation = new Vector3f();
            
            VectorDouble origin = new VectorDouble(3);
                origin.set(0, -iWidth*xres/2f + imagePosition[0] + imageTranslation.x);
                origin.set(1, -iHeight*yres/2f + imagePosition[1] + imageTranslation.y);
                origin.set(2, -iDepth*zres/2f + imagePosition[2] + imageTranslation.z);
                
            itkImage.setOrigin(origin);
                        
            short[] voxels = (short[])image.getData();
            
            org.itk.simple.VectorUInt32 pos = new org.itk.simple.VectorUInt32(3);
            
            int min=32768;
            int max = 0;
            
            int framesize = iHeight * iWidth;
            
            for (int i = 0; i < iWidth; i++) {
                int percentDone = (int)((float)i/iWidth*100f);
                if (i % 25 == 0) {
                    System.out.println(percentDone + "% Copied.");
                }
                pos.set(0, i);
                for (int j = 0; j < iHeight; j++) {
                    pos.set(1, j);
                    for (int k = 0; k < iDepth; k++) {
                        pos.set(2, k);
                        int value = voxels[i + j*iWidth + k*framesize] & 0x0fff;
                        if (value<min) min = value;
                        if (value>max) max = value;
                        itkImage.setPixelAsFloat(pos, (float)value);
                    }
                }
            }
            
            System.out.println("min=" + min + ", max=" + max);
            
            return itkImage;
    }
    
    private class registerWorker implements Runnable {

        private ImageVolume srcctImage = null;
        private ImageVolume srcmrImage = null;
        private org.itk.simple.Image ctImage;
        private org.itk.simple.Image mrImage;
        private Renderable renderableToUpdate = null;
        private org.itk.simple.Transform transform = null;
        private float[] translation = new float[3];
        private float[] orientation = new float[6];

        public float[] getTranslation() {
            return translation;
        }

        public float[] getOrientation() {
            return orientation;
        }

        public org.itk.simple.Transform getTransform() {
            return transform;
        }

        public registerWorker(ImageVolume ct_image, ImageVolume mr_image, Renderable renderable) {
            srcctImage = ct_image;
            srcmrImage = mr_image;
            renderableToUpdate = renderable;
        }

        @Override
        public void run() {
            register(srcctImage, srcmrImage, renderableToUpdate);
        }

        public void register(ImageVolume ct_image, ImageVolume mr_image, Renderable renderable) {
            renderableToUpdate = renderable;

            System.out.println("Copy CT volume...");
            ctImage = copyToITKimage(ct_image);
            System.out.println("Copy MR volume...");
            mrImage = copyToITKimage(mr_image);

//        System.out.println("Gaussian filter...");
//        org.itk.simple.DiscreteGaussianImageFilter filter = new org.itk.simple.DiscreteGaussianImageFilter();
//        filter.setVariance(5d);
//        mrImage = filter.execute(ctImage);
            System.out.println("Center transform");

            transform = SimpleITK.centeredTransformInitializer(mrImage, ctImage, new Euler3DTransform(), CenteredTransformInitializerFilter.OperationModeType.GEOMETRY);

            System.out.println(transform);

            System.out.println("Setup registration");
            org.itk.simple.ImageRegistrationMethod registration_method = new org.itk.simple.ImageRegistrationMethod();
            registration_method.setMetricAsMattesMutualInformation(50);
//        registration_method.setMetricAsJointHistogramMutualInformation(50);
            registration_method.setMetricSamplingStrategy(ImageRegistrationMethod.MetricSamplingStrategyType.RANDOM);
            registration_method.setMetricSamplingPercentage(0.01);
            registration_method.setInterpolator(InterpolatorEnum.sitkLinear);
            registration_method.setOptimizerAsGradientDescent(1d, 100, 1e-6, 10);

            registration_method.setOptimizerScalesFromPhysicalShift();
//        registration_method.setOptimizerScalesFromIndexShift();

            VectorUInt32 shrinkFactors = new VectorUInt32(3);
            shrinkFactors.set(0, 4);
            shrinkFactors.set(1, 2);
            shrinkFactors.set(2, 1);
            registration_method.setShrinkFactorsPerLevel(shrinkFactors);
            VectorDouble smoothingSigmas = new VectorDouble(3);
            smoothingSigmas.set(0, 2);
            smoothingSigmas.set(1, 1);
            smoothingSigmas.set(2, 0);
            registration_method.setSmoothingSigmasPerLevel(smoothingSigmas);
            registration_method.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
            registration_method.setInitialTransform(transform);

            MyCommand cmd = new MyCommand(registration_method, transform, ct_image, mr_image, renderable);
            registration_method.addCommand(EventEnum.sitkAnyEvent, cmd);

            System.out.println("Registering...");
            registration_method.execute(mrImage, ctImage);

        }
        
        public class MyCommand extends Command {
            private ProcessObject processObject;
            private Transform transform;
            private ImageVolume moving_image, ref_image;
            private Renderable renderableToUpdate = null;

            public MyCommand(ProcessObject po, Transform trfm, ImageVolume moving_image, ImageVolume ref_image, Renderable renderable) {
                super();
                processObject = po;
                transform = trfm;
                this.moving_image = moving_image;
                this.ref_image = ref_image;
                renderableToUpdate = renderable;
            }

            public void execute() {
                double progress = processObject.getProgress();
                System.out.format("%s Progress: %f\n", processObject.getName(), progress);
                System.out.println(transform);

                Quaternion result = new Quaternion();

                org.itk.simple.Euler3DTransform etransform = new org.itk.simple.Euler3DTransform(transform);
                VectorDouble vec = etransform.getMatrix();

                System.out.println(etransform);
                System.out.println("Done.");

                FloatBuffer matBuffer = BufferUtils.createFloatBuffer(9);
                for (int i = 0; i < 9; i++) {
                    matBuffer.put((float) vec.get(i));
                }
                matBuffer.flip();

                Matrix3f mat = new Matrix3f();
                mat.load(matBuffer);

                result.setFromMatrix(mat);
                
//                Quaternion mrQ = (Quaternion)ref_image.getAttribute("ImageOrientationQ");
//
//                Quaternion.mul(result, mrQ, result);

                VectorDouble trans = etransform.getTranslation();
                translation[0] = (float) trans.get(0);
                translation[1] = (float) trans.get(1);
                translation[2] = (float) trans.get(2);

                for (int i = 0; i < 6; i++) {
                    orientation[i] = (float) vec.get(i);
                }
                
                if (renderableToUpdate != null) {
                    moving_image.setAttribute("ImageOrientationQ", result);
                    renderableToUpdate.setIsDirty(true);
                }
            }
        }
    }

//    public static void main(String[] args) {
//        MRCTRegister register = new MRCTRegister();
//        
//        
//        register.ctImage = new org.itk.simple.Image(512, 512, 128, PixelIDValueEnum.sitkFloat32);
//        register.mrImage = new org.itk.simple.Image(512, 512, 128, PixelIDValueEnum.sitkFloat32);
//        
//        int pixelCount = 512*512*128;
//        
//        org.itk.simple.VectorUInt32 pos = new org.itk.simple.VectorUInt32(3);
//        for (int i = 0; i < 512; i++) {
//            System.out.println("i=" + i);
//            pos.set(0, i);
//            for (int j = 0; j < 512; j++) {
//                pos.set(1, j);
//                for (int k = 0; k < 128; k++) {
//                    pos.set(2, k);
//                    int value = 0;
//                    if (i>64 && i<128 && j>64 && j<128 && k>64 && k<80) value = 256;
//                    register.ctImage.setPixelAsFloat(pos, (float)value);
//                }
//            }
//        }
//        
//        org.itk.simple.DiscreteGaussianImageFilter filter = new org.itk.simple.DiscreteGaussianImageFilter();
//        filter.setVariance(5d);
//        register.mrImage = filter.execute(register.ctImage);
//        
//        System.out.println("Center transform");
//        org.itk.simple.Transform transform = null;
//        transform = org.itk.simple.SimpleITK.centeredTransformInitializer(register.mrImage, register.ctImage, new Euler3DTransform());
//        
//        System.out.println("Setup registration");        
//        org.itk.simple.ImageRegistrationMethod registration_method = new org.itk.simple.ImageRegistrationMethod();
//        registration_method.setMetricAsMattesMutualInformation(50);
//        registration_method.setMetricSamplingStrategy(ImageRegistrationMethod.MetricSamplingStrategyType.RANDOM);
//        registration_method.setMetricSamplingPercentage(0.01);
//        registration_method.setInterpolator(InterpolatorEnum.sitkLinear);
//        registration_method.setOptimizerAsGradientDescent(1d, 100, 1e-6, 10);
//        registration_method.setOptimizerScalesFromPhysicalShift();
//        VectorUInt32 shrinkFactors = new VectorUInt32(3);
//        shrinkFactors.set(0, 4);
//        shrinkFactors.set(1, 2);
//        shrinkFactors.set(2, 1);
//        registration_method.setShrinkFactorsPerLevel(shrinkFactors);
//        VectorDouble smoothingSigmas = new VectorDouble(3);
//        smoothingSigmas.set(0, 2);
//        smoothingSigmas.set(1, 1);
//        smoothingSigmas.set(2, 0);
//        registration_method.setSmoothingSigmasPerLevel(smoothingSigmas);
//        registration_method.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
//        registration_method.setInitialTransform(transform);
//        
//        MyCommand cmd = new MyCommand(registration_method, transform);
//        registration_method.addCommand(EventEnum.sitkAnyEvent, cmd);
//     
//        
//        System.out.println("Registering...");
//        registration_method.execute(register.mrImage, register.ctImage);
//        
//        
//        System.out.println(transform);
//        
////        org.itk.simple.SimpleITK.show(register.mrImage, "Hellow World");
//              
//    }
}
