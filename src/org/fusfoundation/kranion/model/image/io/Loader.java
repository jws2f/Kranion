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
package org.fusfoundation.kranion.model.image.io;

import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

import org.fusfoundation.kranion.ProgressListener;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageLoader;

/**
 *
 * @author  jsnell
 */
public class Loader implements Runnable {
    
    /** Creates a new instance of Loader */
    public Loader() {
        loaders.add(new DicomImageLoader());
    }
    
    private static List<ImageLoader> loaders = new ArrayList<ImageLoader>();
    
    private File theFile=null;
    private List<File> theFileList=null;
    private ProgressListener progressListener;
    private ImageVolume image = null;
    private ActionListener actionListener = null;
    private String command = new String();
    
    public static void addImageLoader(ImageLoader loader) {
        loaders.add(loader);
    }
    
    public static void removeImageLoader(ImageLoader loader) {
        loaders.remove(loader);
    }
    
    public void load(File file, String command, ProgressListener pl) {
        theFile = file;
        progressListener = pl;
        this.command = new String(command);
        if (pl != null && (pl instanceof ActionListener))
            actionListener = (ActionListener)pl;
        
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    
    public void load(List<File> files, String command, ProgressListener pl) {
        theFileList = files;
        progressListener = pl;
        this.command = new String(command);
        if (pl != null && (pl instanceof ActionListener))
            actionListener = (ActionListener)pl;
        
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    
    public ImageVolume getLoadedImage() { return image; }
    
    public void run() {
        if (theFile != null || theFileList != null) {
            ImageVolume stuntImage = null;

            if (theFile != null) {
                for (int i=0; i<loaders.size(); i++) {
                    if (loaders.get(i).probe(theFile) == 100) {
                        stuntImage = loaders.get(i).load(theFile, progressListener);
                        System.out.println("Loaded DICOM series");
                        break;
                    }
                }
            }
            else if (theFileList != null) {
                for (int i=0; i<loaders.size(); i++) {
                    if (loaders.get(i).probe(theFileList.get(0)) == 100) {
                        stuntImage = loaders.get(i).load(theFileList, progressListener);
                        System.out.println("Loaded DICOM series");
                        break;
                    }
                }
            }
            else {
                return;
            }
            
            
            image = stuntImage;
            if (image != null && progressListener != null) {
                
          
                // This is all so we can asynchronously notify Main when we have finished loading
                class LoaderFinished implements Runnable {
                    private Loader parent = null;
                    
                    public LoaderFinished(Loader p) {
                        parent = p;
                    }
                    
                    public void run() {
                        ActionEvent event = new ActionEvent(parent, 0, command);
                        actionListener.actionPerformed(event);                        
                    }
                }
                
                SwingUtilities.invokeLater(new LoaderFinished(this));
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        ActionEvent event = new ActionEvent(thisOne, 0, "Image.Loaded");
//                        actionListener.actionPerformed(event);
//                    }
//                });
            }
            
            
            
            
        }
    }
    
    
}
