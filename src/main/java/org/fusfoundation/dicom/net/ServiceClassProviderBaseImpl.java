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
package org.fusfoundation.dicom.net;

import java.io.IOException;

import org.fusfoundation.dicom.DicomObject;
import org.fusfoundation.dicom.VrReader;
import org.fusfoundation.dicom.UID;
import org.fusfoundation.dicom.net.Association;

import org.apache.logging.log4j.Logger;

/**
 *
 * @author  jsnell
 */
public abstract class ServiceClassProviderBaseImpl implements Runnable, ServiceClassProvider {
    
    private boolean done = false;
    private Thread worker = null;

    // These hold the values passed to ProcessCommand() for the
    // background thread's use
    private Association association;
    private DicomObject command;
    private VrReader inputVRStream;
    private int pcID; // Presentation Context ID
    
   static final Logger logger = org.apache.logging.log4j.LogManager.getLogger();

    /** Creates a new instance of ServiceClassProviderBaseImpl */
    public ServiceClassProviderBaseImpl() {
    }
    
    public Thread ProcessCommand(Association assoc, int PresentationContextID, DicomObject cmd, VrReader messageStream) {
        if (logger.isDebugEnabled()) logger.debug("Received command");
        inputVRStream = messageStream;
        command = cmd;
        association = assoc;
        pcID = PresentationContextID;

        if (worker != null && worker.isAlive()) {
            if (logger.isDebugEnabled()) logger.debug("Waiting for worker to finish.");
            try {
                worker.join();
            }
            catch(java.lang.InterruptedException e) {
                System.out.println(e);
            }
            if (logger.isDebugEnabled()) logger.debug("Worker is finished.");
        }

        worker = new Thread(this);
        done = false;
        worker.start();
        
        return worker;
    }
    
    public void run() {
        try
        {
            handleCommand(association, pcID, command, inputVRStream);
        }
        catch(Exception e)
        {
            // Something we didn't plan for happend
            // so notify the caller that we are aborting
            // this Association (probably a little rude)
            logger.warn("Unexpected exception thrown: " + e);
            logger.warn("Sending A-ABORT");
            try
            {
               association.sendAbort(true, 0);
               //association.getOutputStream().close();
            }
            catch(IOException e2)
            {
               logger.error("Failed to abort: " + e2);
            }
        }
        finally
        {
            if (logger.isDebugEnabled()) logger.debug("Command completed (or aborted), shutting down thread.");
        }
    }
    
    abstract public void handleCommand(Association assoc, int PresentationContextID, DicomObject cmd, VrReader messageStream);
}
