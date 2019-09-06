/* 
 * The MIT License
 *
 * Copyright 2019 Focused Ultrasound Foundation.
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
package org.fusfoundation.kranion.plugins.stxtools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import org.fusfoundation.kranion.plugin.Plugin;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.GUIControlModelBinding;
import org.fusfoundation.kranion.Landmark;
import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.RadioButtonGroup;
import org.fusfoundation.kranion.RenderLayer;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.Slider;
import org.fusfoundation.kranion.TextBox;

import org.fusfoundation.kranion.view.View;
import org.fusfoundation.kranion.model.*;
import org.fusfoundation.kranion.controller.Controller;
import org.fusfoundation.kranion.model.image.ImageVolumeUtil;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

public class ACPCPlanPlugin implements Plugin, Observer, ActionListener  {

    private Model model;
    private View view;
    private ACPCdisplay acpcDisplay;
    
    private Button goacButton, gopcButton, goacpcSupButton;
    private Button acpcOffsetLeftButton, acpcOffsetRightButton;
    private boolean doRightOffset = false;
    private Button showACPCbut;
    
    private final String propertyPrefix = "Model.Attribute";
    
    @Override
    public void init(Controller controller) {
        System.out.println("******* Hello from ACPCPlanPlugin !! ****************");
                
        model = controller.getModel();
        view = controller.getView();
                
        // listen to app level controller actions
        controller.addActionListener(this);
        
        // listen to model updates
        model.addObserver(this);
        
        setupGUI();
        
    }

    private void setupGUI() {
        
        
        if (acpcDisplay == null) {
            acpcDisplay = new ACPCdisplay();
            acpcDisplay.setPropertyPrefix("Model.Attribute");

            // have the display main layer draw the ac pc graphic
            Renderable parent = Renderable.lookupByTag("DefaultView.main_layer");
            if (parent != null && parent instanceof RenderLayer) {
                System.out.println("ACPCdisplay bering added to main layer for rendering");
                ((RenderLayer) parent).addChild(acpcDisplay);
            }
            // listen to model changes
            model.addObserver(acpcDisplay);
        }
        
        if (goacButton != null && gopcButton != null && goacpcSupButton != null) {
            // We should only create GUI elements once.
            // init() gets called everytime a new model is loaded.
            goacButton.setIsEnabled(false);
            gopcButton.setIsEnabled(false);
            goacpcSupButton.setIsEnabled(false);
            return;
        }
        else {

            FlyoutPanel flyout = null;

            Renderable mainPanel = Renderable.lookupByTag("MainFlyout");
            if (mainPanel != null && mainPanel instanceof FlyoutPanel) {
                flyout = (FlyoutPanel) mainPanel;
            } else {
                System.out.println("*** ACPCPlanPlugin failed to initialize.");
                return;
            }

            // AC-PC registration and target calculator
            flyout.addTab("Planning");
            Button acButton = new Button(Button.ButtonType.BUTTON, 10, 205, 125, 25, this);
            acButton.setTitle("Set AC").setCommand("setAC").setTag("setAC");
            acButton.setPropertyPrefix("Model.Attribute");
            acButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", acButton);

            goacButton = new Button(Button.ButtonType.BUTTON, 150, 205, 125, 25, this);
            goacButton.setTitle("Go AC").setCommand("goAC").setTag("goAC");
            goacButton.setPropertyPrefix("Model.Attribute");
            goacButton.setIsEnabled(false);
            flyout.addChild("Planning", goacButton);

            Button pcButton = new Button(Button.ButtonType.BUTTON, 10, 175, 125, 25, this);
            pcButton.setTitle("Set PC").setCommand("setPC").setTag("setPC");
            pcButton.setPropertyPrefix("Model.Attribute");
            pcButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", pcButton);

            gopcButton = new Button(Button.ButtonType.BUTTON, 150, 175, 125, 25, this);
            gopcButton.setTitle("Go PC").setCommand("goPC").setTag("goPC");
            gopcButton.setPropertyPrefix("Model.Attribute");
            gopcButton.setIsEnabled(false);
            flyout.addChild("Planning", gopcButton);

            Button acpcSupButton = new Button(Button.ButtonType.BUTTON, 10, 145, 125, 25, this);
            acpcSupButton.setTitle("Set Superior").setCommand("setSuperior").setTag("setSuperior");
            acpcSupButton.setPropertyPrefix("Model.Attribute");
            acpcSupButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", acpcSupButton);

            TextBox acpcLength = (TextBox) new TextBox(150, 100, 125, 25, "", this).setTitle("AC-PC Length").setCommand("acpcLength");
            acpcLength.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            model.addObserver(acpcLength);
            flyout.addChild("Planning", acpcLength);

            goacpcSupButton = new Button(Button.ButtonType.BUTTON, 150, 145, 125, 25, this);
            goacpcSupButton.setTitle("Go Superior").setCommand("goSuperior").setTag("goSuperior");
            goacpcSupButton.setPropertyPrefix("Model.Attribute");
            goacpcSupButton.setIsEnabled(false);
            flyout.addChild("Planning", goacpcSupButton);

            Button goTarget = new Button(Button.ButtonType.BUTTON, 300, 175, 125, 55, this);
            goTarget.setTitle("Go Target").setCommand("goTarget").setTag("goTarget");
            goTarget.setColor(0.35f, 0.4f, 0.55f, 1f);
            goTarget.setIsEnabled(false);
            flyout.addChild("Planning", goTarget);

            Button goACPCPlane = new Button(Button.ButtonType.BUTTON, 300, 145, 125, 25, this);
            goACPCPlane.setTitle("AC-PC Plane").setCommand("goACPCplane").setTag("goACPCplane");
            goACPCPlane.setColor(0.5f, 0.5f, 0.25f, 1f);
            goACPCPlane.setIsEnabled(false);
            flyout.addChild("Planning", goACPCPlane);

            RadioButtonGroup acpcOffsetButtonGrp = new RadioButtonGroup();

            acpcOffsetLeftButton = new Button(Button.ButtonType.RADIO_BUTTON, 475, 205, 150, 25, this);
            acpcOffsetLeftButton.setTitle("Left Offset");
            acpcOffsetLeftButton.setCommand("acpcLeftOffset");
            acpcOffsetLeftButton.setDrawBackground(false);
            acpcOffsetLeftButton.setIndicator(true);

            acpcOffsetRightButton = new Button(Button.ButtonType.RADIO_BUTTON, 475, 175, 150, 25, this);
            acpcOffsetRightButton.setTitle("Right Offset");
            acpcOffsetRightButton.setCommand("acpcRightOffset");
            acpcOffsetRightButton.setTag("acpcRightOffset");
            acpcOffsetRightButton.setDrawBackground(false);

            acpcOffsetButtonGrp.addChild(acpcOffsetLeftButton);
            acpcOffsetButtonGrp.addChild(acpcOffsetRightButton);

            flyout.addChild("Planning", acpcOffsetButtonGrp);

            Slider slider1 = new Slider(650, 220, 380, 25, this);
            model.addObserver(slider1);
            slider1.setTitle("Lateral Offset");
            slider1.setCommand("acpcOffsetValue"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(0, 30);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%3.1f");
            slider1.setUnitsString(" mm");
            slider1.setCurrentValue(0);
            flyout.addChild("Planning", slider1);

            Button zeroButton = new Button(Button.ButtonType.BUTTON, 1030, 220, 45, 25, this);
            zeroButton.setTitle("Zero").setCommand("zeroLateralOffset");
            zeroButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", zeroButton);

            slider1 = new Slider(1085, 220, 380, 25, this);
            model.addObserver(slider1);
            slider1.setTitle("3rd Vent Offset");
            slider1.setCommand("acpc3rdVentricleOffsetValue"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(0, 15);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%3.1f");
            slider1.setUnitsString(" mm");
            slider1.setCurrentValue(0);
            flyout.addChild("Planning", slider1);

            zeroButton = new Button(Button.ButtonType.BUTTON, 1465, 220, 45, 25, this);
            zeroButton.setTitle("Zero").setCommand("zeroVentLateralOffset");
            zeroButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", zeroButton);

            slider1 = new Slider(650, 75, 380, 25, this);
            model.addObserver(slider1);
            slider1.setTitle("AC/PC ratio");
            slider1.setCommand("acpcRatioValue"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(0, 100);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%3.1f");
            slider1.setUnitsString("%");
            slider1.setCurrentValue(50);
            flyout.addChild("Planning", slider1);

            zeroButton = new Button(Button.ButtonType.BUTTON, 1030, 75, 45, 25, this);
            zeroButton.setTitle("MCP").setCommand("zeroACPCratio");
            zeroButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", zeroButton);

            slider1 = new Slider(650, 175, 380, 25, this);
            model.addObserver(slider1);
            slider1.setTitle("Superior Offset");
            slider1.setCommand("acpcSuperiorOffsetValue"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(-30, 30);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%3.1f");
            slider1.setUnitsString(" mm");
            slider1.setCurrentValue(0);
            flyout.addChild("Planning", slider1);

            zeroButton = new Button(Button.ButtonType.BUTTON, 1030, 175, 45, 25, this);
            zeroButton.setTitle("Zero").setCommand("zeroSuperiorOffset");
            zeroButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", zeroButton);

            slider1 = new Slider(650, 130, 380, 25, this);
            model.addObserver(slider1);
            slider1.setTitle("Anterior Offset");
            slider1.setCommand("acpcAnteriorOffsetValue"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(-30, 30);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%3.1f");
            slider1.setUnitsString(" mm");
            slider1.setCurrentValue(0);
            flyout.addChild("Planning", slider1);

            zeroButton = new Button(Button.ButtonType.BUTTON, 1030, 130, 45, 25, this);
            zeroButton.setTitle("Zero").setCommand("zeroAnteriorOffset");
            zeroButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Planning", zeroButton);

            TextBox textbox = (TextBox) new TextBox(60, 55, 60, 25, "", this).setTitle("R").setCommand("sonicationRLoc");
            textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            model.addObserver(textbox);
            flyout.addChild("Planning", textbox);

            textbox = (TextBox) new TextBox(160, 55, 60, 25, "", this).setTitle("A").setCommand("sonicationALoc");
            textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            model.addObserver(textbox);
            flyout.addChild("Planning", textbox);

            textbox = (TextBox) new TextBox(260, 55, 60, 25, "", this).setTitle("S").setCommand("sonicationSLoc");
            textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            model.addObserver(textbox);
            flyout.addChild("Planning", textbox);

            showACPCbut = new Button(Button.ButtonType.TOGGLE_BUTTON, 400, 55, 150, 25, this);
            showACPCbut.setTitle("Show AC-PC").setCommand("showACPCgraphic");
            showACPCbut.setTag("showACPCgraphic");
            showACPCbut.setIndicator(true);
            
            flyout.addChild("Planning", showACPCbut);
            
            // End AC PC planning controls
        }      
    }
    
    @Override
    public void release() {
        Renderable parent = Renderable.lookupByTag("DefaultView.main_layer");
        if (parent != null && parent instanceof RenderLayer) {
            ((RenderLayer)parent).removeChild(this.acpcDisplay);
        }
        if (model != null) {
            model.deleteObserver(this);
            model.deleteObserver(this.acpcDisplay);
        }
        
        view.getController().removeActionListener(this);
        
        acpcDisplay = null;
    }

    @Override
    public String getName() {
        return "ACPCPlanPlugin";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        Object source = e.getSource();

// TODO: not sure if we should have default model binding come first always by default
        if (e.getSource() instanceof GUIControlModelBinding) {
            ((GUIControlModelBinding)e.getSource()).doBinding(model);
        }

        Vector3f currentTarget = (Vector3f)model.getAttribute("currentTargetPoint");
System.out.println("ACPCPlanPlugin got command: " + command);        
        switch (command) {
            // ACPC Planning commands
            case "setAC":
            {
                Vector3f p = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), currentTarget);
                model.setAttribute("AC", p);
                goacButton.setIsEnabled(true);
                calcACPCLength();
                updateTargetGeometry();
            }
                break;
            case "goAC":
            {
                Vector3f p = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f)model.getAttribute("AC"));
                model.setAttribute("currentTargetPoint", p);
                view.doTransition(250);
            }
                break;
            case "setPC":
            {
                Vector3f p = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), currentTarget);
                model.setAttribute("PC", p);
                gopcButton.setIsEnabled(true);
                calcACPCLength();
                updateTargetGeometry();
            }
                break;
            case "goPC":
            {
                Vector3f p = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f)model.getAttribute("PC"));
                model.setAttribute("currentTargetPoint", p);
            }
                view.doTransition(250);
                break;
            case "setSuperior":
            {
                Vector3f p = ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), currentTarget);
                model.setAttribute("ACPCSup", p);
                goacpcSupButton.setIsEnabled(true);
                updateTargetGeometry();
            }
                break;
            case "goSuperior":
            {
                Vector3f p = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f)model.getAttribute("ACPCSup"));
                model.setAttribute("currentTargetPoint", p);
            }
                view.doTransition(250);
                break;
            case "goTarget":
            {
                    updateTargetGeometry();
                    
                    Vector3f acpcTarget = (Vector3f)model.getAttribute("acpcTarget");
                    if (acpcTarget != null) {
                        model.setAttribute("currentTargetPoint", ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), acpcTarget));
                    }
                    view.doTransition(250);
                                    
            }
                break;
            case "goACPCplane":
                calcACPCplane();
                model.setAttribute("foregroundVolumeSlices", 0f);
                break;
            case "acpcOffsetValue":
            case "acpc3rdVentricleOffsetValue":
            case "acpcRatioValue":
            case "acpcSuperiorOffsetValue":
            case "acpcAnteriorOffsetValue":
                updateTargetGeometry();
                break;
            case "acpcLeftOffset":
                    doRightOffset = false;
                    updateTargetGeometry();
                    this.acpcDisplay.setIsDirty(true);
                break;
            case "acpcRightOffset":
                    doRightOffset = true;
                    updateTargetGeometry();
                    this.acpcDisplay.setIsDirty(true);
                break;
            case "zeroLateralOffset":
                model.setAttribute("acpcOffsetValue", 0f);
                this.acpcDisplay.setIsDirty(true);
                break;
            case "zeroVentLateralOffset":
                model.setAttribute("acpc3rdVentricleOffsetValue", 0f);
                this.acpcDisplay.setIsDirty(true);
                break;
            case "zeroSuperiorOffset":
                model.setAttribute("acpcSuperiorOffsetValue", 0f);
                this.acpcDisplay.setIsDirty(true);
                break;
            case "zeroAnteriorOffset":
                model.setAttribute("acpcAnteriorOffsetValue", 0f);
                this.acpcDisplay.setIsDirty(true);
                break;
            case "zeroACPCratio":
                model.setAttribute("acpcRatioValue", 50f);
                this.acpcDisplay.setIsDirty(true);
                break;
            case "showACPCgraphic":
                boolean showGraphic = showACPCbut.getIndicator();
                this.acpcDisplay.setVisible(showGraphic);
                this.view.doTransition(500);
                break;
        }
    }
    
    private void updateTargetGeometry() {
        Vector3f ac = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f) model.getAttribute("AC"));
        Vector3f pc = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f) model.getAttribute("PC"));
        Vector3f sup = ImageVolumeUtil.pointFromImageToWorld(model.getCtImage(), (Vector3f) model.getAttribute("ACPCSup"));

        try {

            if (ac != null
                    && pc != null
                    && sup != null) {

                // all three ref landmarks defined so enable targeting buttons
                Renderable button = Renderable.lookupByTag("goTarget");
                if (button != null && button instanceof Button) {
                    ((Button) button).setIsEnabled(true);
                }
                button = Renderable.lookupByTag("goACPCplane");
                if (button != null && button instanceof Button) {
                    ((Button) button).setIsEnabled(true);
                }

                float anteriorOffsetVal = 0f;
                float lateralOffsetVal = 0f;
                float ventricleOffsetVal = 0f;
                float superiorOffsetVal = 0f;
                float ratioVal = 50; // attribute value is in percent

                try {
                    anteriorOffsetVal = (Float) model.getAttribute("acpcAnteriorOffsetValue");
                } catch (NullPointerException e) {
                }

                try {
                    lateralOffsetVal = (Float) model.getAttribute("acpcOffsetValue");
                } catch (NullPointerException e) {
                }

                try {
                    ventricleOffsetVal = (Float) model.getAttribute("acpc3rdVentricleOffsetValue");
                } catch (NullPointerException e) {
                }

                try {
                    superiorOffsetVal = (Float) model.getAttribute("acpcSuperiorOffsetValue"); // attribute value is in percent
                } catch (NullPointerException e) {
                }

                try {
                    ratioVal = (Float) model.getAttribute("acpcRatioValue") / 100f; // attribute value is in percent
                } catch (NullPointerException e) {
                }

                if (!doRightOffset) {
                    lateralOffsetVal = -lateralOffsetVal;
                    ventricleOffsetVal = -ventricleOffsetVal;
                }

                Vector3f target = new Vector3f();

                Vector3f up = Vector3f.sub(sup, ac, null);
                Vector3f posterior = Vector3f.sub(pc, ac, null);

                Vector3f right = Vector3f.cross(up, posterior, null).normalise(null);
                Vector3f.cross(posterior, right, up);
                up.normalise();
                posterior.normalise();

                model.setAttribute("acpcCoordSup", new Vector3f(up));
                model.setAttribute("acpcCoordRight", new Vector3f(right));
                model.setAttribute("acpcCoordPost", new Vector3f(posterior));

                Vector3f.add(ac, (Vector3f) Vector3f.sub(pc, ac, null).scale(ratioVal), target);

                model.setAttribute("acpcRefPoint", ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), target));

                Vector3f.add(target, (Vector3f) up.scale(superiorOffsetVal), target);
                Vector3f.add(target, (Vector3f) new Vector3f(right).scale(lateralOffsetVal + ventricleOffsetVal), target);
                Vector3f.add(target, (Vector3f) posterior.scale(-anteriorOffsetVal), target);

                // Calculate the perpendicular intercept of between
                // the target point and the ac-pc line
                Vector3f u = new Vector3f();
                Vector3f q = new Vector3f(target);
                Vector3f pq = new Vector3f();
                Vector3f w2 = new Vector3f();
                Vector3f refPt = new Vector3f();

                Vector3f.sub(ac, pc, u);
                Vector3f.sub(q, ac, pq);
                Vector3f.sub(pq, (Vector3f) u.scale(Vector3f.dot(pq, u) / u.lengthSquared()), w2);
                Vector3f.sub(q, w2, refPt);

                Vector3f.add(refPt, (Vector3f) right.scale(ventricleOffsetVal), refPt);

                model.setAttribute("acpcTarget", ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), target));
                model.setAttribute("acpcPerpendicularPoint", ImageVolumeUtil.pointFromWorldToImage(model.getCtImage(), refPt));
                
                acpcDisplay.setIsDirty(true);
            }
        } catch (NullPointerException e) {

        }
    }
    
    private void calcACPCLength() {
        if (model != null) {
            Vector3f ac = (Vector3f) model.getAttribute("AC");
            Vector3f pc = (Vector3f) model.getAttribute("PC");
            if (ac != null && pc != null) {
                model.setAttribute("acpcLength", String.format("%3.1f mm", Vector3f.sub(ac, pc, null).length()));
            }
        }
    }

    private void calcACPCplane() {
        
        Vector3f x, y, z;
        
        try {
            x = new Vector3f((Vector3f)model.getAttribute("acpcCoordRight"));
            y = new Vector3f((Vector3f)model.getAttribute("acpcCoordPost"));
            z = new Vector3f((Vector3f)model.getAttribute("acpcCoordSup"));
        }
        catch (NullPointerException e) {
            return;
        }
        
        if (x!=null && y!=null && z!=null) {
            
            x.negate(x);
            Vector3f.cross(x, y, z);
        
            Matrix3f mat = new Matrix3f();
            mat.m00 = x.x;
            mat.m01 = x.y;
            mat.m02 = x.z;
            mat.m10 = y.x;
            mat.m11 = y.y;
            mat.m12 = y.z;
            mat.m20 = z.x;
            mat.m21 = z.y;
            mat.m22 = z.z;

            Quaternion q = matToQuaternion(mat);
            
            model.setAttribute("currentSceneOrienation", q);
            
        }
    }
    
    private Quaternion matToQuaternion(Matrix3f mat) {
        Quaternion result = new Quaternion();
        float tr = mat.m00 + mat.m11 + mat.m22;

        if (tr > 0) {
            float S = (float) Math.sqrt(tr + 1.0) * 2f; // S=4*qw 
            result.w = 0.25f * S;
            result.x = (mat.m21 - mat.m12) / S;
            result.y = (mat.m02 - mat.m20) / S;
            result.z = (mat.m10 - mat.m01) / S;
        } else if ((mat.m00 > mat.m11) & (mat.m00 > mat.m22)) {
            float S = (float) Math.sqrt(1.0 + mat.m00 - mat.m11 - mat.m22) * 2f; // S=4*qx 
            result.w = (mat.m21 - mat.m12) / S;
            result.x = 0.25f * S;
            result.y = (mat.m01 + mat.m10) / S;
            result.z = (mat.m02 + mat.m20) / S;
        } else if (mat.m11 > mat.m22) {
            float S = (float) Math.sqrt(1.0 + mat.m11 - mat.m00 - mat.m22) * 2f; // S=4*qy
            result.w = (mat.m02 - mat.m20) / S;
            result.x = (mat.m01 + mat.m10) / S;
            result.y = 0.25f * S;
            result.z = (mat.m12 + mat.m21) / S;
        } else {
            float S = (float) Math.sqrt(1.0 + mat.m22 - mat.m00 - mat.m11) * 2f; // S=4*qz
            result.w = (mat.m10 - mat.m01) / S;
            result.x = (mat.m02 + mat.m20) / S;
            result.y = (mat.m12 + mat.m21) / S;
            result.z = 0.25f * S;
        }
        
        return result;
    }
    
    protected String getFilteredPropertyName(PropertyChangeEvent arg) {
        String propName = "";
        String nameString = arg.getPropertyName();
        
        if (nameString.startsWith(propertyPrefix + "[")) {
            int last = nameString.indexOf("]", propertyPrefix.length()+1);
            propName = nameString.substring(propertyPrefix.length()+1, last);            
        }
        
        return propName;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg != null && arg instanceof PropertyChangeEvent) {
            PropertyChangeEvent event = (PropertyChangeEvent)arg;
//            System.out.print(" Property Change: " + ((PropertyChangeEvent)arg).getPropertyName());
//            System.out.println();
            
            switch(this.getFilteredPropertyName(event)) {
                case "AC":
                    if (event.getNewValue() != null) {
                        goacButton.setIsEnabled(true);
                    }
                    break;
                case "PC":
                    if (event.getNewValue() != null) {
                        gopcButton.setIsEnabled(true);
                    }
                    break;
                case "ACPCSup":
                    if (event.getNewValue() != null) {
                        goacpcSupButton.setIsEnabled(true);
                    }
                    break;
            }
        }       
    }
}

