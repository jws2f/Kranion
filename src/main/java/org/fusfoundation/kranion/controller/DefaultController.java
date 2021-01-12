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
package org.fusfoundation.kranion.controller;

import java.util.Observable;
import java.beans.PropertyChangeEvent;
import java.awt.event.ActionEvent;

import org.lwjgl.util.vector.*;

import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.ImageCanvas2D;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.GUIControlModelBinding;
//import org.fusfoundation.kranion.N4BiasCorrector;
import org.fusfoundation.kranion.ProgressBar;


import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.fusfoundation.kranion.model.image.io.Loader;

/**
 *
 * @author John Snell
 */
public class DefaultController extends Controller {
        
    public DefaultController() {
        
    }
    
    @Override
    public void update(Observable o, Object arg) {
        System.out.print("----DefaultController update: " + o.toString());
        if (arg != null && arg instanceof PropertyChangeEvent) {
            System.out.print(" Property Change: " + ((PropertyChangeEvent)arg).getPropertyName() + " = " + ((PropertyChangeEvent)arg).getNewValue());
            
            switch (((PropertyChangeEvent)arg).getPropertyName()) {
                case "Model.Attribute[currentTargetPoint]":
                    Vector3f target = (Vector3f)((PropertyChangeEvent)arg).getNewValue();
                    model.setAttribute("sonicationRLoc", String.format("%4.1f", -target.x)); // target is LPS, display RAS
                    model.setAttribute("sonicationALoc", String.format("%4.1f", -target.y));
                    model.setAttribute("sonicationSLoc", String.format("%4.1f", target.z));
                break;
                    
            }
        }
        System.out.println();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e);
        
        // propagate to any other registerd listeners (plugins, etc)
        super.actionPerformed(e);
        
// TODO: not sure if we should have default model binding come first always by default
//        if (e.getSource() instanceof GUIControlModelBinding) {
//            ((GUIControlModelBinding)e.getSource()).doBinding(model);
//        }
              
        if (e.getActionCommand().equals("MRwindow")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                slider.doBinding(model);
                int currentMRindex = 0;
                try {
                    currentMRindex = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                }
                catch(Exception e1) {}
                if (model.getMrImageCount() > currentMRindex) {
                    model.getMrImage(currentMRindex).setAttribute("WindowWidth", slider.getCurrentValue());
                }
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("MRcenter")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                slider.doBinding(model);
                
                int currentMRindex = 0;
                try {
                    currentMRindex = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                }
                catch(Exception e1) {}
                if (model.getMrImageCount() > currentMRindex) {
                    model.getMrImage(currentMRindex).setAttribute("WindowCenter", slider.getCurrentValue());
                }
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("MRthresh")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                slider.doBinding(model);
                
                int currentMRindex = 0;
                try {
                    currentMRindex = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                }
                catch(Exception e1) {}
                if (model.getMrImageCount() > currentMRindex) {
                    model.getMrImage(currentMRindex).setAttribute("Threshold", slider.getCurrentValue());
                }
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("CTcenter")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                slider.doBinding(model);
                
                if (model.getCtImage() != null) {
                    model.getCtImage().setAttribute("WindowCenter", slider.getCurrentValue());
                }
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("CTwindow")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                slider.doBinding(model);
                
                if (model.getCtImage() != null) {
                    model.getCtImage().setAttribute("WindowWidth", slider.getCurrentValue());
                }
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }
//        else if (e.getActionCommand().equals("loadCT")) {
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(new String("Choose CT file..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//            File ctfile = null;
//            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                ctfile = fileChooser.getSelectedFile();
//                System.out.println("Selected file: " + ctfile.getAbsolutePath());
//
//                Loader ctloader = new Loader();
//                ctloader.load(ctfile, "CT_IMAGE_LOADED", this);
//            }
//        }
//        else if (e.getActionCommand().equals("loadMR")) {
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(new String("Choose MR file..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//            File mrfile = null;
//            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                mrfile = fileChooser.getSelectedFile();
//                System.out.println("Selected file: " + mrfile.getAbsolutePath());
//
//                Loader mrloader = new Loader();
//                mrloader.load(mrfile, "MR_IMAGE_0_LOADED", this);
//            }
//        }
        else if (e.getActionCommand().equals("CT_IMAGE_LOADED")) {
            Loader loader = (Loader) e.getSource();
            ImageVolume4D image = (ImageVolume4D) loader.getLoadedImage();
            System.out.println("MAIN: setImage #1 CT");
            
            model.setCtImage(image);
        }
        else if (e.getActionCommand().equals("MR_IMAGE_0_LOADED")) {
            Loader loader = (Loader) e.getSource();

            ImageVolume4D image = (ImageVolume4D) loader.getLoadedImage();
            System.out.println("MAIN: setImage #1 MR");

// TODO: Looks like only isometric (or some aspect ratio) voxels will work here
//          Only the Windows Java bin is available for SimpleITK for now
//            N4BiasCorrector n4 = new N4BiasCorrector(image);
//            n4.execute();

//            model.clearMrImages();
            int mrImageCount = model.getMrImageCount();
            model.setMrImage(mrImageCount, image);
            model.setAttribute("currentMRSeries", mrImageCount);

        }
        else if (e.getActionCommand().equals("currentTargetPoint")) {
            Object source = e.getSource();
            if (source instanceof ImageCanvas2D) {
                ImageCanvas2D canvas = (ImageCanvas2D)source;
                Vector3f target = canvas.getSelectedPoint();
                model.setAttribute("currentTargetPoint", target);
//                model.setAttribute("sonicationRLoc", String.format("%4.1f", -target.x));
//                model.setAttribute("sonicationALoc", String.format("%4.1f", -target.y));
//                model.setAttribute("sonicationSLoc", String.format("%4.1f", target.z));
            }
        }
        else if (e.getActionCommand().equals("rightClick")) {
            Object source = e.getSource();
            if (source instanceof ImageCanvas2D) {
                ImageCanvas2D canvas = (ImageCanvas2D)source;
                Quaternion orient = canvas.getPlaneQuaternion();
                model.setAttribute("currentSceneOrientation", orient);
            }
        }        
        // Else call the model binding code of the control if it supports it
        else if (e.getSource() instanceof GUIControlModelBinding) {
            ((GUIControlModelBinding)e.getSource()).doBinding(model);
            return;
        }
                
                         
            
    }
    
    
    @Override
    public void percentDone(String msg, int percent) {
        System.out.println(msg + " " + percent);
                Renderable r = Renderable.lookupByTag("statusBar");
                if (r != null && r instanceof ProgressBar) {
                    ((ProgressBar)r).setFormatString(msg + ": %3.0f%%");
                    ((ProgressBar)r).setValue(percent);
                    Main.update();
                }
    }
    
}
