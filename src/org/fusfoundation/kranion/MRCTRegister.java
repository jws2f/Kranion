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
import org.fusfoundation.kranion.model.Model;
import  org.fusfoundation.kranion.model.image.*;
import org.itk.simple.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.*;

/**
 *
 * @author john
 */


public class MRCTRegister implements BackgroundWorker {
    
    private Model model = null;
    
    private boolean done = true;
    
    private MutualInformation mutualInformation;
    private float mi = Float.NEGATIVE_INFINITY;
    private float bestMI = Float.NEGATIVE_INFINITY;
    private int miStep = 0;
    private int failedSteps = 0;
    private int stepPhase = 0;

    private float tranNoiseScale = 1f;
    private float rotNoiseScale = 1.0f / 25.0f;
    private float percentSample = 0.5f;
    private boolean doBlur = true;
    
    private Quaternion rotx, roty, rotz, rot, origRot;
    private Vector3f tranX, tranY, tranZ, tran;
    private Vector3f origTran;
    
    public MRCTRegister(Model model) {
        this.model = model;
        mutualInformation = new MutualInformation(null, null);
        Main.addBackgroundWorker(this);
    }
    
    public void start() {
        
        if (this.model.getCtImage() == null || this.model.getMrImageCount() == 0 || this.model.getMrImage(0) == null) {
            return;
        }
        
        done = false;
        
        bestMI = Float.NEGATIVE_INFINITY;
        miStep = 0;
        failedSteps = 0;
        stepPhase = 0;

        tranNoiseScale = 1f;
        rotNoiseScale = 1.0f / 25.0f;
        percentSample = 0.5f;
        doBlur = true;

        mutualInformation.setImageVolumes(this.model.getCtImage(), this.model.getMrImage(0));
        
        origRot = new Quaternion((Quaternion) this.model.getCtImage().getAttribute("ImageOrientationQ"));
        Vector3f tranVal = (Vector3f) this.model.getCtImage().getAttribute("ImageTranslation");
        if (tranVal == null) {
            tranVal = new Vector3f();
            this.model.getCtImage().setAttribute("ImageTranslation", tranVal);
        }
        origTran = new Vector3f(tranVal);

        rotx = MutualInformation.toQuaternion((float) (Math.PI / 180.0), 0, 0);
        roty = MutualInformation.toQuaternion(0, (float) (Math.PI / 180.0), 0);
        rotz = MutualInformation.toQuaternion(0, 0, (float) (Math.PI / 180.0));
        rot = null;

        tranX = new Vector3f(2f, 0, 0);
        tranY = new Vector3f(0, 2f, 0);
        tranZ = new Vector3f(0, 0, 2f);
        tran = null;
    }
    
    public void stop() {
       done = true; 
    }
    
    public void doWorkStep() {
        if (done) return;
                  
//                  mutualInformation.updateTestImage(joinHistogram);
//                  histoCanvas.setCTImage(joinHistogram);
//                  histoCanvas.setMRImage(joinHistogram);
//                  histoCanvas.setIsDirty(true);
//                  miText.setText(String.format("%1.3f", mi));
                  
//                  Quaternion origRot = new Quaternion((Quaternion)this.model.getCtImage().getAttribute("ImageOrientationQ"));
//                  Vector3f tranVal = (Vector3f)this.model.getCtImage().getAttribute("ImageTranslation");
//                  if (tranVal == null) {
//                      tranVal = new Vector3f();
//                      this.model.getCtImage().setAttribute("ImageTranslation", tranVal);
//                  }
//                  Vector3f origTran = new Vector3f(tranVal);
//                  
//                  Quaternion rotx = MutualInformation.toQuaternion((float)(Math.PI/180.0), 0,  0);
//                  Quaternion roty = MutualInformation.toQuaternion(0, (float)(Math.PI/180.0),  0);
//                  Quaternion rotz = MutualInformation.toQuaternion(0,  0, (float)(Math.PI/180.0));
//                  Quaternion rot = null;
//                  
//                  Vector3f tranX = new Vector3f(2f, 0, 0);
//                  Vector3f tranY = new Vector3f(0, 2f, 0);
//                  Vector3f tranZ = new Vector3f(0, 0, 2f);
//                  Vector3f tran = null;
                  
                 

                      Quaternion testRot1 = new Quaternion();
                      Quaternion testRot2 = new Quaternion();
                      Vector3f testTran1 = new Vector3f();
                      Vector3f testTran2 = new Vector3f();
                      
//                  origRot = new Quaternion((Quaternion)this.model.getCtImage().getAttribute("ImageOrientationQ"));
//                  origTran = new Vector3f((Vector3f)this.model.getCtImage().getAttribute("ImageTranslation"));
                      
Vector3f noise = new Vector3f((float)((Math.random()-0.5)/10.0), (float)((Math.random()-0.5)/10.0), (float)((Math.random()-0.5)/10.0));


                      if (failedSteps > 12 && stepPhase==0) {
                            failedSteps = 0;
                            stepPhase++;
                            tranX = new Vector3f(0.75f, 0, 0);
                            tranY = new Vector3f(0, 0.75f, 0);
                            tranZ = new Vector3f(0, 0, 0.75f);                          
                            rotx = MutualInformation.toQuaternion((float)(Math.PI/720.0), 0,  0);
                            roty = MutualInformation.toQuaternion(0, (float)(Math.PI/720.0),  0);
                            rotz = MutualInformation.toQuaternion(0,  0, (float)(Math.PI/720.0));
                            percentSample = 0.75f;
                            rotNoiseScale *= 0.5f;
                            tranNoiseScale *= 0.4;
                      }
                      else if (failedSteps > 24 && stepPhase==1) {
                            failedSteps = 0;
                            stepPhase++;
                            tranX = new Vector3f(0.2f, 0, 0);
                            tranY = new Vector3f(0, 0.2f, 0);
                            tranZ = new Vector3f(0, 0, 0.2f);                          
                            rotx = MutualInformation.toQuaternion((float)(Math.PI/1440.0), 0,  0);
                            roty = MutualInformation.toQuaternion(0, (float)(Math.PI/1440.0),  0);
                            rotz = MutualInformation.toQuaternion(0,  0, (float)(Math.PI/1440.0));
                            //rotNoiseScale = 0.5f;
                           percentSample = 2.0f;
                           rotNoiseScale *= 0.5f;
                           tranNoiseScale *= 0.25;
                           doBlur = false;
                      }
                      else if (failedSteps > 24 && stepPhase==2) {
                          done = true;
                          Main.getView().setIsDirty(true);
                          return;
                      }
                      
                      int n = miStep;//(int)Math.round(Math.random() * 36.0);
                      
                      switch(n%6) {
                          case 0:
                              rot = rotx;
                              break;
                          case 1:
                              rot = roty;
                              break;
                          case 2:
                              rot = rotz;
                              break;
                          case 3:
                              tran = tranX;
                              break;
                          case 4:
                              tran = tranY;
                              break;
                          case 5:
                              tran = tranZ;
                              break;
                          default:
                              break;
                      }
                      
                      if (n % 6 < 3) {
                          Quaternion.mul(origRot, rot, testRot1);
                          Quaternion.mul(origRot, rot.negate(null), testRot2);

                          model.getCtImage().setAttribute("ImageOrientationQ", testRot1);

                          float testResult1 = mutualInformation.calcMI(percentSample, doBlur);

                          model.getCtImage().setAttribute("ImageOrientationQ", testRot2);

                          float testResult2 = mutualInformation.calcMI(percentSample, doBlur);

                          if (testResult1 > bestMI && testResult1 > testResult2) {
                              origRot.set(testRot1.x+noise.x*rotNoiseScale, testRot1.y+noise.y*rotNoiseScale, testRot1.z+noise.z*rotNoiseScale, testRot1.w);
                              origRot = origRot.normalise(null);
                              mi = testResult1;
//                              miText.setText(String.format("%1.3f", mi));
                          }
                          if (testResult2 > bestMI && testResult2 > testResult1) {
                              origRot.set(testRot2.x+noise.x*rotNoiseScale, testRot2.y+noise.y*rotNoiseScale, testRot2.z+noise.z*rotNoiseScale, testRot2.w);
                              origRot = origRot.normalise(null);
                              mi = testResult2;
//                              miText.setText(String.format("%1.3f", mi));
                          }
                      }
                      else {
                          Vector3f.add(origTran, tran, testTran1);
                          Vector3f.add(origTran, tran.negate(null), testTran2);

                          model.getCtImage().setAttribute("ImageTranslation", testTran1);

                          float testResult1 = mutualInformation.calcMI(percentSample, doBlur);

                          model.getCtImage().setAttribute("ImageTranslation", testTran2);

                          float testResult2 = mutualInformation.calcMI(percentSample, doBlur);

                          if (testResult1 > bestMI && testResult1 > testResult2) {
                              origTran.set(testTran1.x+noise.x*tranNoiseScale, testTran1.y+noise.y*tranNoiseScale, testTran1.z+noise.z*tranNoiseScale);
                              mi = testResult1;
//                              miText.setText(String.format("%1.3f", mi));
                          }
                          if (testResult2 > bestMI && testResult2 > testResult1) {
                              origTran.set(testTran2.x+noise.x, testTran2.y+noise.y, testTran2.z+noise.z);
                              mi = testResult2;
//                              miText.setText(String.format("%1.3f", mi));
                          }
                      }
                      
                      if (mi > bestMI) {
                            failedSteps = 0;
                            bestMI = mi;
                            model.getCtImage().setAttribute("ImageTranslation", origTran);
                            model.getCtImage().setAttribute("ImageOrientationQ", origRot);
                            
//                            mutualInformation.updateTestImage(joinHistogram);
//                            histoCanvas.setCTImage(joinHistogram);
//                            histoCanvas.setMRImage(joinHistogram);
//                            histoCanvas.setIsDirty(true);
//                            miText.setText(String.format("%1.4f", mi));
//                            
//                            this.setIsDirty(true);
//                            Main.update();

                              Main.getView().setIsDirty(true);
                      }
                      else {
                          failedSteps++;
                      }
                      
                      miStep++;
                      

//                  mutualInformation.updateTestImage(joinHistogram);
//                  histoCanvas.setCTImage(joinHistogram);
//                  histoCanvas.setMRImage(joinHistogram);
//                  histoCanvas.setIsDirty(true);
//                  miText.setText(String.format("%1.3f", mi));

                  
    }

    @Override
    public String getName() {
        return "MRCTRegister";
    }


}
