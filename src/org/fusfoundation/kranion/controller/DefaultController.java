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

import org.fusfoundation.kranion.Button;
import java.util.Observable;
import java.util.Observer;
import java.beans.PropertyChangeEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JFileChooser;

import org.lwjgl.util.vector.*;

import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.ImageCanvas2D;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.Canvas2DLayoutManager;
import org.fusfoundation.kranion.ProgressBar;

import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.fusfoundation.kranion.model.image.io.Loader;
import org.fusfoundation.kranion.view.DefaultView;
import org.fusfoundation.kranion.RegionGrow;
import org.lwjgl.opengl.Display;

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
            System.out.print(" Property Change: " + ((PropertyChangeEvent)arg).getPropertyName());
        }
        System.out.println();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(e);
        
        // propagate to any other registerd listeners (plugins, etc)
        super.actionPerformed(e);
        
        if (e.getActionCommand().equals("currentSonication")) {
                org.fusfoundation.kranion.PullDownSelection pulldown = (org.fusfoundation.kranion.PullDownSelection)e.getSource();
                model.setAttribute("currentSonication", pulldown.getSelectionIndex());
        }
        else if (e.getActionCommand().equals("registerMRCT")) {
                Main.startBackgroundWorker("MRCTRegister");
        }
        else if (e.getActionCommand().equals("transducerXTilt")) {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                model.setAttribute("transducerXTilt", slider.getCurrentValue());
        }
        else if (e.getActionCommand().equals("selectMRSeries")) {
                org.fusfoundation.kranion.PullDownSelection pulldown = (org.fusfoundation.kranion.PullDownSelection)e.getSource();
                model.setAttribute("currentMRSeries", pulldown.getSelectionIndex());
        }
        else if (e.getActionCommand().equals("showRayTracer")) {
            if (model != null) {
                org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
                model.setAttribute("showRayTracer", button.getIndicator());
            }
        }
        else if (e.getActionCommand().equals("showThermometry")) {
            if (model != null) {
                org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
                model.setAttribute("showThermometry", button.getIndicator());
            }
        }
        else if (e.getActionCommand().equals("doClip")) {
            if (model != null) {
                org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
                model.setAttribute("doClip", button.getIndicator());
            }
        }
        else if (e.getActionCommand().equals("doMRI")) {
            if (model != null) {
                org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
                model.setAttribute("doMRI", button.getIndicator());
            }
        }
        else if (e.getActionCommand().equals("doFrame")) {
            if (model != null) {
                org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
                model.setAttribute("doFrame", button.getIndicator());
            }
        }
        else if (e.getActionCommand().equals("boneSOS")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("boneSOS", (float)Math.round(slider.getCurrentValue()));
        }
        else if (e.getActionCommand().equals("boneRefractionSOS")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("boneRefractionSOS", (float)Math.round(slider.getCurrentValue()));
        }
        else if (e.getActionCommand().equals("foregroundVolumeSlices")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("foregroundVolumeSlices", slider.getCurrentValue());
        }        
        else if (e.getActionCommand().equals("MRwindow")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                model.setAttribute("MRwindow", slider.getCurrentValue());
                int currentMRindex = 0;
                try {
                    currentMRindex = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                }
                catch(Exception e1) {}
                model.getMrImage(currentMRindex).setAttribute("WindowWidth", slider.getCurrentValue());
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("MRcenter")) {
            try {
                org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
                model.setAttribute("MRcenter", slider.getCurrentValue());
                int currentMRindex = 0;
                try {
                    currentMRindex = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                }
                catch(Exception e1) {}
                model.getMrImage(currentMRindex).setAttribute("WindowCenter", slider.getCurrentValue());
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
            }
        }        
        else if (e.getActionCommand().equals("MRthresh")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("MRthresh", slider.getCurrentValue());
        }        
        else if (e.getActionCommand().equals("CTwindow")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("CTwindow", slider.getCurrentValue());
        }        
        else if (e.getActionCommand().equals("CTcenter")) {
            org.fusfoundation.kranion.Slider slider = (org.fusfoundation.kranion.Slider)e.getSource();
            model.setAttribute("CTcenter", slider.getCurrentValue());
        }        
        else if (e.getActionCommand().equals("loadCT")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(new String("Choose CT file..."));
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            File ctfile = null;
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                ctfile = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + ctfile.getAbsolutePath());

                Loader ctloader = new Loader();
                ctloader.load(ctfile, "CT_IMAGE_LOADED", this);
            }
        }
        else if (e.getActionCommand().equals("loadMR")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(new String("Choose MR file..."));
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            File mrfile = null;
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                mrfile = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + mrfile.getAbsolutePath());

                Loader mrloader = new Loader();
                mrloader.load(mrfile, "MR_IMAGE_0_LOADED", this);
            }
        }
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

            model.clearMrImages();
            model.setMrImage(0, image);
        }
        else if (e.getActionCommand().equals("currentTargetPoint")) {
            Object source = e.getSource();
            if (source instanceof ImageCanvas2D) {
                ImageCanvas2D canvas = (ImageCanvas2D)source;
                Vector3f target = canvas.getSelectedPoint();
                model.setAttribute("currentTargetPoint", target);
                model.setAttribute("sonicationRLoc", String.format("%4.1f", -target.x));
                model.setAttribute("sonicationALoc", String.format("%4.1f", -target.y));
                model.setAttribute("sonicationSLoc", String.format("%4.1f", target.z));
            }
        }
        else if (e.getActionCommand().equals("rightClick")) {
            Object source = e.getSource();
            if (source instanceof ImageCanvas2D) {
                ImageCanvas2D canvas = (ImageCanvas2D)source;
                Quaternion orient = canvas.getPlaneQuaternion();
                model.setAttribute("currentSceneOrienation", orient);
            }
        }

        
        // new added C.J         
        else if (e.getActionCommand().equals("AttenuationTerm")) {
            org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
            model.setAttribute("AttenuationTerm", button.getIndicator());
      
//              Renderable r = Renderable.lookupByTag("Raytracer");
//              if (r != null && r instanceof TransducerRayTracer) {
//                  ((TransducerRayTracer)r).calcEnvelope();
//              }

        }
        
        else if (e.getActionCommand().equals("TransmissionLossTerm")) {
 
            org.fusfoundation.kranion.Button button = (org.fusfoundation.kranion.Button)e.getSource();
            model.setAttribute("TransmissionLossTerm", button.getIndicator());
         
        }
       
        else if (e.getActionCommand().equals("PressureCompute")) {
            
            System.out.println("** check here");
            
             Object r = e.getSource();
              if (r != null && r instanceof Button) {
                  ((DefaultView)view).pressureCalCalledByButton();
                  System.out.println("** pressure button pressed");
              }
//                System.out.println("pressure button pressed");
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
