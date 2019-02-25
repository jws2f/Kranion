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
package org.fusfoundation.kranion.view;

import org.fusfoundation.kranion.model.image.io.Loader;
import org.fusfoundation.kranion.Landmark;
import org.fusfoundation.kranion.FloatParameter;
import org.fusfoundation.kranion.Main;
import org.fusfoundation.kranion.Slider;
import org.fusfoundation.kranion.Trackball;
import org.fusfoundation.kranion.InsightecTxdrGeomReader;
import org.fusfoundation.kranion.HistogramChartControl;
import org.fusfoundation.kranion.Ring;
import org.fusfoundation.kranion.Canvas2DLayoutManager;
import org.fusfoundation.kranion.Scene;
import org.fusfoundation.kranion.ImageCanvas2D;
import org.fusfoundation.kranion.DirtyFollower;
import org.fusfoundation.kranion.TextBox;
import org.fusfoundation.kranion.RenderLayer;
import org.fusfoundation.kranion.ProgressBar;
import org.fusfoundation.kranion.XYChartControl;
import org.fusfoundation.kranion.Transducer;
import org.fusfoundation.kranion.TransformationAdapter;
import org.fusfoundation.kranion.OrientationAnimator;
import org.fusfoundation.kranion.FloatAnimator;
import org.fusfoundation.kranion.ShaderProgram;
import org.fusfoundation.kranion.ScreenTransition;
import org.fusfoundation.kranion.ImageHistogram;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.Cylinder;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.CoordinateWidget;
import org.fusfoundation.kranion.ImageCanvas3D;
import org.fusfoundation.kranion.TransferFunctionDisplay;
import org.fusfoundation.kranion.CrossHair;
import org.fusfoundation.kranion.RenderableAdapter;
import org.fusfoundation.kranion.RenderList;
import org.fusfoundation.kranion.PullDownSelection;
import org.fusfoundation.kranion.PlyFileReader;
import org.fusfoundation.kranion.TransducerRayTracer;
import org.fusfoundation.kranion.ImageLabel;
import org.fusfoundation.kranion.RadioButtonGroup;
import java.awt.Desktop;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.*;

import org.lwjgl.BufferUtils;
import java.nio.*;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;
import java.util.regex.Pattern;

import org.lwjgl.util.vector.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import java.lang.reflect.Constructor;



import java.io.File;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.Observable;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fusfoundation.kranion.Framebuffer;
import org.fusfoundation.kranion.GUIControl;
import org.fusfoundation.kranion.MRCTRegister;
import org.fusfoundation.kranion.RegionGrow;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.TargetRenderer;

import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.model.Sonication;
import org.fusfoundation.kranion.model.image.*;
import org.fusfoundation.kranion.controller.Controller;


import org.knowm.xchart.Histogram;

import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;

import java.util.prefs.Preferences;
import org.fusfoundation.kranion.FileDialog;
import org.fusfoundation.kranion.FlyoutDialog;
import org.fusfoundation.kranion.GUIControlModelBinding;
import org.fusfoundation.kranion.MessageBoxDialog;




public class DefaultView extends View {

    @Override
    public void percentDone(String msg, int percent) {
        System.out.println(msg + " - " + percent + "%");
        statusBar.setValue(percent);
        
        if (Thread.currentThread() == this.myThread) {
                    Main.update();
        }
    }
    
    private Preferences prefs;

    public static final int DISPLAY_HEIGHT = 1024;
    public static final int DISPLAY_WIDTH = 1680;
    public static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    private final org.fusfoundation.kranion.UpdateEventQueue updateEventQueue = new org.fusfoundation.kranion.UpdateEventQueue();

    private float cameraZ = -600f;
    private Trackball trackball = new Trackball(DISPLAY_HEIGHT / 2, DISPLAY_WIDTH / 2, DISPLAY_HEIGHT / 2f);
    private Trackball registerBallCT = new Trackball(DISPLAY_HEIGHT / 2, DISPLAY_WIDTH / 2, DISPLAY_HEIGHT / 2f);
    
//    private Vector3f startMrImageTranslate = new Vector3f();
    private int mouseStartX, mouseStartY;

    private int selTrans, drawingStyle;
    private boolean doClip = false;
    private boolean doFrame = false;
    private boolean doMRI = false;
    private boolean showScanner = false;
    private FloatParameter dolly = new FloatParameter(-100f);
    private float transducerTilt = 0f;
    private float viewportAspect = 1f;
    
    private boolean attenuation_term_on = false ;
    private boolean transmissionLoss_term_on = false ;

    private Loader ctloader, mrloader;
//    private ImageVolume4D ctImage, mrImage;
    
    private Transducer transducerModel;
    private PlyFileReader stereotacticFrame = new PlyFileReader("/org/fusfoundation/kranion/meshes/Frame1v3.ply");
    private PlyFileReader mrBore2 = new PlyFileReader("/org/fusfoundation/kranion/meshes/Bore.ply");
    private PlyFileReader mrHousing = new PlyFileReader("/org/fusfoundation/kranion/meshes/Housing.ply");
    private PlyFileReader mrPedestal = new PlyFileReader("/org/fusfoundation/kranion/meshes/Pedestal.ply");
    private Landmark currentTarget = new Landmark();
    private Landmark currentSteering = new Landmark();
    private Cylinder mrBore, mrBoreOuter;
    private Ring mrBoreFront, mrBoreBack;
    private TransformationAdapter mrBoreTransform, frameTransform, frameOffsetTransform, steeringTransform;
    
    private RenderList mrBoreGroup = new RenderList();
    
    private int center = 150, window = 160, ct_threshold = 900;
    private int mr_center = 150, mr_window = 160, mr_threshold = 125;
    private ImageCanvas3D canvas = new ImageCanvas3D();
    private ImageCanvas2D canvas1 = new ImageCanvas2D();
    private ImageCanvas2D canvas2 = new ImageCanvas2D();
    private ImageCanvas2D canvas3 = new ImageCanvas2D();
        
    private Canvas2DLayoutManager mprLayout = new Canvas2DLayoutManager(canvas1, canvas2, canvas3);
        
    private TransducerRayTracer transRayTracer = new TransducerRayTracer();
    private boolean showRayTracer = false;
    
    private ProgressBar statusBar = new ProgressBar();
    private ProgressBar activeElementsBar = new ProgressBar();
    private ProgressBar sdrBar = new ProgressBar();
    private ProgressBar beamBar = new ProgressBar();
    private TextBox tempPredictionIndicator = new TextBox();
    private TransferFunctionDisplay transFuncDisplay = new TransferFunctionDisplay();
    private DirtyFollower transFuncDisplayProxy = new DirtyFollower(transFuncDisplay);
    private ImageHistogram ctHistogram = new ImageHistogram();
    
    private XYChartControl thermometryChart;
    private HistogramChartControl incidentAngleChart, sdrChart;
    
    private XYChartControl skullProfileChart;
    
//    private Framebuffer overlay = new Framebuffer();
//    private Framebuffer overlayMSAA = new Framebuffer();
//    private Framebuffer mainLayerMSAA = new Framebuffer();
//    private Framebuffer mainLayer = new Framebuffer();
    
    private RenderLayer background = new RenderLayer(1);
    private RenderLayer mainLayer = new RenderLayer(8);
    private RenderLayer overlay = new RenderLayer(8);
    
    private Framebuffer pickLayer = new Framebuffer();
    
    private FlyoutPanel flyout1 = new FlyoutPanel();
    private FlyoutPanel flyout2 = new FlyoutPanel();
    private FlyoutPanel flyout3 = new FlyoutPanel();
    
    private FileDialog fileDialog = new FileDialog();
    private MessageBoxDialog messageDialog = new MessageBoxDialog("Quit Kranion?");
    
    private PullDownSelection sonicationSelector;
    private PullDownSelection mrSeriesSelector;
    private PullDownSelection transducerPatternSelector;
    
    
    private Scene scene = new Scene();
    private ScreenTransition transition = new ScreenTransition();
    private boolean doTransition = false;
    private float transitionTime = 0.5f;
    
    private MRCTRegister imageregistration;

    // So plugins have access to implemented file choosing mechanism
    @Override
    public File chooseFile(String title, FileDialog.fileChooseMode mode, String[] fileFilters) {
            fileDialog.setDialogTitle(title);
            fileDialog.setFileChooseMode(mode);
            return fileDialog.open(fileFilters);
    }

    @Override
    public boolean doOkCancelMessageBox(String title, String message) {
        messageDialog.setDialogTitle(title);
        messageDialog.setMessageText(message);
        return this.messageDialog.open();
    }
    
    private enum mouseMode {
        SCENE_ROTATE,
        HEAD_ROTATE,
        HEAD_TRANSLATE,
        SKULL_ROTATE,
        SKULL_TRANSLATE,
        FRAME_ROTATE,
        FRAME_TRANSLATE,
        MRI_ROTATE,
        MRI_TRANSLATE
    }
    
    private mouseMode currentMouseMode = mouseMode.SCENE_ROTATE;
    
    private OrientationAnimator orientAnimator = new OrientationAnimator();
    private FloatAnimator zoomAnimator = new FloatAnimator();
        
    // Game controller components
    private static net.java.games.input.ControllerEnvironment controllerEnvironement;
    private static net.java.games.input.Controller gameController=null;

    public DefaultView() {
        selTrans = 0;
        drawingStyle = 0;
        
        this.setAcceptsKeyboardFocus(true);
        Renderable.setDefaultKeyboardFocus(this);
        this.acquireKeyboardFocus();
        
        // For storing any persistent preference values
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }
    
    @Override
    public boolean okToExit() {
        messageDialog.setDialogTitle("Are you sure you want to exit?");
        messageDialog.setMessageText("Exit Kranion");
        return this.messageDialog.open();
    }

    @Override
    public void create() throws LWJGLException {
        
        initController();
        
        this.imageregistration = new MRCTRegister(model);
        
        mprLayout.setTag("mprLayout");
        
        canvas1.setOrientation(0);
        canvas2.setOrientation(1);
        canvas3.setOrientation(2);
        
        FlyoutPanel.setGuiScale(Display.getWidth()/1980f);
        
//        fileDialog.setBounds(200, 0, 800, 600);
//        fileDialog.setFlyDirection(FlyoutPanel.direction.SOUTH);
        
        
        
        
        flyout1.setBounds(0, 350, 400, 600);
        flyout1.setFlyDirection(FlyoutPanel.direction.EAST);

        TextBox textbox = (TextBox)new TextBox(225, 400, 100, 25, "", controller).setTitle("Acoustic Power").setCommand("sonicationPower");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTag("tbSonicationAcousticPower");
        textbox.setTextEditable(true);
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(100, 505, 265, 25, "", controller).setTitle("Description").setCommand("sonicationDescription");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTextEditable(true);
        textbox.setTag("tbSonicationDescription");
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(100, 475, 150, 25, "", controller).setTitle("Timestamp").setCommand("sonicationTimestamp");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTextEditable(true);
        textbox.setTag("tbSonicationTimestamp");
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        flyout1.addChild(new Button(Button.ButtonType.TOGGLE_BUTTON, 275, 475, 50, 25, this).setDrawBackground(false).setTitle("Visible").setPropertyPrefix("Model.Attribute").setCommand("targetVisible"));
        
        textbox = (TextBox)new TextBox(225, 370, 100, 25, "", controller).setTitle("Duration").setCommand("sonicationDuration");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTextEditable(true);
        textbox.setTag("tbSonicationDuration");
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(225, 340, 100, 25, "", controller).setTitle("Frequency").setCommand("sonicationFrequency");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTag("tbSonicationFrequency");
        textbox.setTextEditable(true);
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(10, 340, 100, 25, "ABORTED", controller);
        textbox.setTag("tbSonicationAborted");
        textbox.setColor(0.3f, 0.15f, 0.15f, 1f);
        textbox.setTextColor(1f, 0.1f, 0.1f, 1f);
        textbox.setTextEditable(false);
        textbox.setVisible(false);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(100, 300, 70, 25, "", controller).setTitle("Max Temp").setCommand("sonicationMaxTemp");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTextEditable(false);
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(260, 300, 125, 25, "", controller).setTitle("Max Dose").setCommand("sonicationMaxDose");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        textbox.setTextEditable(false);
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(100, 445, 60, 25, "", controller).setTitle("R").setCommand("sonicationRLoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(200, 445, 60, 25, "", controller).setTitle("A").setCommand("sonicationALoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        textbox = (TextBox)new TextBox(300, 445, 60, 25, "", controller).setTitle("S").setCommand("sonicationSLoc");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout1.addChild(textbox);
        
        thermometryChart = new XYChartControl(10, 100, 380, 200);
        thermometryChart.setCommand("currentOverlayFrame");
        thermometryChart.addActionListener(this);
        flyout1.addChild(thermometryChart);
        
        flyout1.addChild(new Button(Button.ButtonType.TOGGLE_BUTTON, 200, 70, 180, 25, this).setTitle("Show Thermometry").setPropertyPrefix("Model.Attribute").setCommand("showThermometry"));
        flyout1.addChild(new Button(Button.ButtonType.TOGGLE_BUTTON, 215, 40, 165, 25, this).setDrawBackground(false).setTitle("as Dose").setPropertyPrefix("Model.Attribute").setCommand("showDose"));
        flyout1.addChild(new Button(Button.ButtonType.TOGGLE_BUTTON, 200, 10, 180, 25, controller).setTitle("Show Targets").setPropertyPrefix("Model.Attribute").setCommand("showTargets"));
        
        flyout1.addChild(new Button(Button.ButtonType.BUTTON, 10, 10, 100, 25, this).setTitle("Update").setCommand("updateSonication"));
        flyout1.addChild(new Button(Button.ButtonType.BUTTON,10, 50, 100, 25, this).setTitle("Add").setCommand("addSonication"));
        
        flyout1.addChild(sonicationSelector = (PullDownSelection)new PullDownSelection(10, 550, 380, 25, controller).setTitle("Sonication").setCommand("currentSonication"));
        sonicationSelector.setPropertyPrefix("Model.Attribute");
        model.addObserver(sonicationSelector);
        
        flyout2.addTab("File"); // Just to make sure this is the first tab
        
        flyout2.setBounds(0, Display.getHeight()-300, Display.getWidth(), 300);
        flyout2.setAutoExpand(true);
        flyout2.setFlyDirection(FlyoutPanel.direction.SOUTH);
        flyout2.setTag("MainFlyout");
                
        Button button = new Button(Button.ButtonType.TOGGLE_BUTTON, 10, 125, 120, 25, controller);
        button.setTitle("Raytracer");
        button.setCommand("showRayTracer");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("View", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 170, 125, 120, 25, controller);
        button.setTitle("Clip");
        button.setCommand("doClip");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("View", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 170, 160, 120, 25, controller);
        button.setTitle("Frame");
        button.setCommand("doFrame");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("View", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 10, 160, 120, 25, controller);
        button.setTitle("Imaging");
        button.setCommand("doMRI");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("View", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 750, 115, 410, 25, controller);
        button.setTitle("Show skull floor strikes");
        button.setCommand("showSkullFloorStrikes");
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        button.setDrawBackground(false);
        model.addObserver(button);
        flyout2.addChild("View", button);

//        // new added part
//         button = new Button(Button.ButtonType.TOGGLE_BUTTON, 400, 205, 240, 25, controller);
//        button.setTitle("Attenuation Term");
//        button.setCommand("AttenuationTerm");
//        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        model.addObserver(button);
//        flyout2.addChild(button);
//        
//        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 400, 170, 240, 25, controller);
//        button.setTitle("Transmission Loss Term");
//        button.setCommand("TransmissionLossTerm");
//        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        model.addObserver(button);
//        flyout2.addChild(button);
//            
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 400, 250, 240, 25, controller).setTitle("Pressure Calc").setCommand("PressureCompute"));
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 720, 160, 100, 25, controller).setTitle("Ellipse +").setCommand("EllipsePlus"));
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 830, 160, 100, 25, controller).setTitle("Ellipse -").setCommand("EllipseMinus"));
//             

        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON, 10, 240, 200, 25, this).setTitle("Open...").setCommand("openKranionFile"));
        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON, 10, 205, 200, 25, this).setTitle("Save...").setCommand("saveKranionFile"));
      
        
        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON, 230, 240, 200, 25, this).setTitle("Import DICOM CT...").setCommand("loadCT"));
        
        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON,230, 205, 200, 25, this).setTitle("Import DICOM MR...").setCommand("loadMR"));
        
        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON, 445, 240, 150, 25, this).setTitle("Filter CT").setCommand("filterCT"));

        Button regButton = new Button(Button.ButtonType.TOGGLE_BUTTON, 10, 205, 125, 25, this);
        regButton.setTitle("Registration").setCommand("registerMRCT").setTag("registerMRCT");
        regButton.setPropertyPrefix("Model.Attribute");
        flyout2.addChild("View", regButton);
        
        flyout2.addChild("File", new Button(Button.ButtonType.BUTTON, 10, 170, 200, 25, this).setTitle("Close").setCommand("close"));
        
        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 10, 10, 200, 25, this).setTitle("Exit").setCommand("exit"));
        
        Button verButton = new Button(Button.ButtonType.BUTTON, 1450, 0, 200, 25, this);
        verButton.setTitle("Build " + Main.getRbTok("app.version") + ":" + Main.getRbTok("app.build"));
        verButton.setDrawBackground(false);
        verButton.setTextColor(0.5f, 0.5f, 0.5f, 1f);
        flyout2.addChild(verButton);
                
//        flyout2.addChild(new Button(Button.ButtonType.BUTTON, 350, 250, 220, 25, controller).setTitle("Find Fiducials").setCommand("findFiducials"));
        flyout2.addChild("File",new Button(Button.ButtonType.BUTTON, 445, 115, 220, 25, this).setTitle("Save Skull Params...").setCommand("saveSkullParams"));
        flyout2.addChild("File",new Button(Button.ButtonType.BUTTON, 445, 80, 220, 25, this).setTitle("Save CPC ACT file...").setCommand("saveACTfile"));
        flyout2.addChild("File",new Button(Button.ButtonType.BUTTON, 445, 45, 220, 25, this).setTitle("Save Workstation ACT file...").setCommand("saveACTfileWS"));
        
        Slider slider1 = new Slider(750, 100, 410, 25, controller);
        slider1.setTitle("Average bone speed");
        slider1.setCommand("boneSOS"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(1482, 3500);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f m/s");
        slider1.setCurrentValue(2652);
        flyout2.addChild("Transducer", slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(750, 50, 410, 25, controller);
        slider1.setTitle("Cortical bone speed");
        slider1.setCommand("boneRefractionSOS"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(1482, 3500);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f m/s");
        slider1.setCurrentValue(2900);
        flyout2.addChild("Transducer", slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(750, 175, 410, 25, controller);
        slider1.setTitle("Transducer tilt");
        slider1.setCommand("transducerXTilt"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(-45, 45);
        slider1.setLabelWidth(180);
        slider1.setFormatString("%4.0f degrees");
        slider1.setCurrentValue(0);
        flyout2.addChild("Transducer", slider1);
        model.addObserver(slider1);
        
        flyout2.addChild("Transducer", transducerPatternSelector = (PullDownSelection)new PullDownSelection(750, 225, 410, 25, this).setTitle("TransducerPattern").setCommand("transducerPattern"));
        int transducerCount = Transducer.getTransducerDefCount();
        for (int i=0; i<transducerCount; i++) {
            transducerPatternSelector.addItem(Transducer.getTransducerDef(i).getName());
        }
        
               // for the slider of the ellipse function
//        slider1 = new Slider(720, 200, 450, 25, controller);
//        slider1.setTitle("Ellipse Ratio:");
//        slider1.setCommand("EllipseShapechange"); // controller will set command name as propery on model
//        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
//        slider1.setMinMax(75, 100);
//        slider1.setLabelWidth(120);
//        slider1.setFormatString("%3.0f ");
//        slider1.setCurrentValue(0);
//        flyout2.addChild(slider1);
//        model.addObserver(slider1);
        
        slider1 = new Slider(750, 155, 410, 25, controller);
        slider1.setTitle("VR Slicing");
        slider1.setCommand("foregroundVolumeSlices"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 600);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%3.0f");
        slider1.setCurrentValue(0);
        flyout2.addChild("View", slider1);
        model.addObserver(slider1);
        
        Button envelopeCalcButton = new Button(Button.ButtonType.BUTTON, 1250, 175, 150, 25, this);
        envelopeCalcButton.setTitle("Calc Envelope");
        envelopeCalcButton.setCommand("calcEnvelope");
        flyout2.addChild("Transducer", envelopeCalcButton);
        
        
        skullProfileChart = new XYChartControl(250, 10, 500, 250);
        flyout2.addChild("Transducer", skullProfileChart);
        
        textbox = (TextBox)new TextBox(150, 225, 60, 25, "", this).setTitle("Channel").setCommand("currentTransducerChannelNum");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        textbox = (TextBox)new TextBox(150, 190, 60, 25, "", this).setTitle("SDR").setCommand("currentTransducerChannelSDR");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        textbox = (TextBox)new TextBox(150, 155, 60, 25, "", this).setTitle("SDR 5x5 Avg").setCommand("currentTransducerChannelSDRavg");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        textbox = (TextBox)new TextBox(150, 120, 60, 25, "", this).setTitle("Incident Angle").setCommand("currentTransducerChannelOuterAngle");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        textbox = (TextBox)new TextBox(150, 85, 60, 25, "", this).setTitle("Skull Path Length").setCommand("currentTransducerChannelSkullThickness");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        textbox = (TextBox)new TextBox(150, 50, 60, 25, "", this).setTitle("Skull thickness").setCommand("currentTransducerChannelNormSkullThickness");
        textbox.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(textbox);
        flyout2.addChild("Transducer", textbox);
        
        
        flyout3.addTab("CT"); // Make sure this is the first tab
        flyout3.setBounds(Display.getWidth() - 400, 200, 400, 275);
        flyout3.setFlyDirection(FlyoutPanel.direction.WEST);
        
        flyout3.addChild("CT", new Button(Button.ButtonType.BUTTON, 10, 30, 150, 25, this).setTitle("Filter CT").setCommand("filterCT"));
        
        slider1 = new Slider(10, 150, 380, 25, controller);
        slider1.setTitle("MR Center");
        slider1.setCommand("MRcenter"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild("MR", slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 90, 380, 25, controller);
        slider1.setTitle("MR Window");
        slider1.setCommand("MRwindow"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild("MR", slider1);
        model.addObserver(slider1);

        slider1 = new Slider(10, 30, 380, 25, controller);
        slider1.setTitle("MR Thresh");
        slider1.setCommand("MRthresh"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 1024);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild("MR", slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 150, 380, 25, controller);
        slider1.setTitle("CT Center");
        slider1.setCommand("CTcenter"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild("CT", slider1);
        model.addObserver(slider1);
        
        slider1 = new Slider(10, 90, 380, 25, controller);
        slider1.setTitle("CT Window");
        slider1.setCommand("CTwindow"); // controller will set command name as propery on model
        slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        slider1.setMinMax(0, 4095);
        slider1.setLabelWidth(120);
        slider1.setFormatString("%4.0f");
        slider1.setCurrentValue(400);
        flyout3.addChild("CT", slider1);
        model.addObserver(slider1);
        
//                flyout2.addChild(new ImageLabel("images/3dmoveicon.png", 350, 50, 200, 200));
//                flyout2.addChild(new ImageLabel("images/3drotateicon.png", 425, 50, 200, 200));
                
                RadioButtonGroup buttonGrp1 = new RadioButtonGroup();
                
                Button radio1 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 125, 150, 25, this);
                radio1.setTitle("Rotate Skull");
                radio1.setCommand("rotateSkull");
                
                Button radio2 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 160, 150, 25, this);
                radio2.setTitle("Rotate Head");
                radio2.setCommand("rotateHead");
                
//                Button radio3 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 95, 150, 25, this);
//                radio3.setTitle("Rotate Frame");
//                radio3.setCommand("rotateFrame");
                
                Button radio4 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 195/*130*/, 150, 25, this);
                radio4.setTitle("Rotate Scene");
                radio4.setCommand("rotateScene");
                radio4.setIndicator(true);
            
                
                Button radio5 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 125, 150, 25, this);
                radio5.setTitle("Translate Skull");
                radio5.setCommand("translateSkull");
                
                Button radio6 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 160, 150, 25, this);
                radio6.setTitle("Translate Head");
                radio6.setCommand("translateHead");
                
                Button radio7 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 195, 150, 25, this);
                radio7.setTitle("Translate Frame");
                radio7.setCommand("translateFrame");
                
                Button radio8 = new Button(Button.ButtonType.RADIO_BUTTON, 350, 90, 150, 25, this);
                radio8.setTitle("Rotate MRI");
                radio8.setCommand("rotateMRI");
                
                Button radio9 = new Button(Button.ButtonType.RADIO_BUTTON, 500, 90, 150, 25, this);
                radio9.setTitle("Translate MRI");
                radio9.setCommand("translateMRI");
                


                radio1.setDrawBackground(false);
                radio2.setDrawBackground(false);
//                radio3.setDrawBackground(false);
                radio4.setDrawBackground(false);
                radio5.setDrawBackground(false);
                radio6.setDrawBackground(false);
                radio7.setDrawBackground(false);
                radio8.setDrawBackground(false);
                radio9.setDrawBackground(false);
                
                buttonGrp1.addChild(radio1);
                buttonGrp1.addChild(radio2);
//                buttonGrp1.addChild(radio3);
                buttonGrp1.addChild(radio4);
                buttonGrp1.addChild(radio5);
                buttonGrp1.addChild(radio6);
                buttonGrp1.addChild(radio7);
                buttonGrp1.addChild(radio8);
                buttonGrp1.addChild(radio9);
                
                flyout2.addChild("View", buttonGrp1);

        mrSeriesSelector = (PullDownSelection)new PullDownSelection(10, 210, 380, 25, controller).setTitle("MR Series").setCommand("currentMRSeries");
        mrSeriesSelector.setPropertyPrefix("Model.Attribute");
        model.addObserver(mrSeriesSelector);
        flyout3.addChild("MR", mrSeriesSelector);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 50, 160, 240, 25, this);
        button.setTitle("CT Filter GPU Acceleration");
        button.setCommand("CTFilterGpuAcceleration");
        button.setIndicator(this.prefs.getBoolean(button.getCommand(), true));
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("Preferences", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 50, 130, 240, 25, this);
        button.setTitle("Show demographics");
        button.setCommand("prefShowDemographics");
        button.setIndicator(this.prefs.getBoolean(button.getCommand(), true));
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("Preferences", button);
        
        button = new Button(Button.ButtonType.TOGGLE_BUTTON, 50, 100, 240, 25, this);
        button.setTitle("Show logo");
        button.setCommand("prefShowLogo");
        button.setIndicator(this.prefs.getBoolean(button.getCommand(), true));
        button.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(button);
        flyout2.addChild("Preferences", button);
        
        mrBore = new Cylinder(300.0f, 50.0f, -25.0f, -1f);
        mrBoreOuter = new Cylinder(310.0f, 50.0f, -25.0f, 1f);
        mrBoreFront = new Ring(310.0f, 10.0f, -25.0f, -1f);
        mrBoreBack = new Ring(310.0f, 10.0f, 25.0f, 1f);

        mrBore.setColor(0.7f, 0.7f, 0.8f);
        mrBoreOuter.setColor(0.7f, 0.7f, 0.8f);
        mrBoreFront.setColor(0.7f, 0.7f, 0.8f);
        mrBoreBack.setColor(0.7f, 0.7f, 0.8f);
        
        mrBoreGroup.add(mrBore);
        mrBoreGroup.add(mrBoreOuter);
        mrBoreGroup.add(mrBoreFront);
        mrBoreGroup.add(mrBoreBack);
        mrBoreGroup.setClipColor(0.7f, 0.7f, 0.8f, 1f);

        
        try {
            System.out.println(Transducer.getTransducerDefCount() + " transdcuer definitions found.");
            transducerModel = new Transducer(0);

//            transducer220.setTrackball(trackball);

            stereotacticFrame.readObject();
            stereotacticFrame.setColor(0.65f, 0.65f, 0.65f, 1f);
            
            mrBore2.readObject();
            mrBore2.setColor(0.80f, 0.80f, 0.80f, 1f);
            mrHousing.readObject();
            mrHousing.setColor(0.55f, 0.55f, 0.55f, 1f);
            mrPedestal.readObject();
            mrPedestal.setColor(0.25f, 0.25f, 0.25f, 1f);
                               
            ShaderProgram shader = new ShaderProgram();
            shader.addShader(GL_VERTEX_SHADER, "/org/fusfoundation/kranion/shaders/Collision.vs.glsl");
            shader.addShader(GL_FRAGMENT_SHADER, "/org/fusfoundation/kranion/shaders/Collision.fs.glsl");
            shader.compileShaderProgram();
            stereotacticFrame.setShader(shader);
        

        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        
        transRayTracer.init(transducerModel);
        transRayTracer.addObserver(this);
        
        statusBar.setMinMax(0, 100);
        statusBar.setBounds(550, 500, 600, 30);
        statusBar.setFormatString("Status: %3.0f");
        statusBar.setFontSize(18);
        statusBar.setGreenLevel(0.0f);
        statusBar.setYellowLevel(0.0f);
        statusBar.setTag("statusBar");
        
        activeElementsBar.setMinMax(0, 1024);
        activeElementsBar.setBounds(300, 201, 500, 20);
        activeElementsBar.setFormatString("Elements: %3.0f");
        activeElementsBar.setFontSize(14);
        activeElementsBar.setGreenLevel(0.7f);
        activeElementsBar.setYellowLevel(0.5f);
        
        sdrBar.setMinMax(0f, 1f);
        sdrBar.setBounds(850, 201, 500, 20);
        sdrBar.setFormatString("SDR: %1.2f");
        
        sdrBar.setFontSize(14);
        sdrBar.setGreenLevel(0.45f);
        sdrBar.setYellowLevel(0.4f);
        
        beamBar.setMinMax(0f, 100f);
        beamBar.setBounds(300, 225, 500, 20);
        beamBar.setFormatString("BEAM: %1.2f");
        
        beamBar.setFontSize(14);
        beamBar.setGreenLevel(0.5f);
        beamBar.setYellowLevel(0.4f);
        
        tempPredictionIndicator.setBounds(300, 250, 140, 20);
        tempPredictionIndicator.setVisible(false);
        
       incidentAngleChart = new HistogramChartControl("Incident Angle", "Element Count", 300, 50, 500, 150);
       incidentAngleChart.setXAxisFormat("##");
       incidentAngleChart.setVisible(false);
       
       sdrChart = new HistogramChartControl("SDR Value", "Element Count", 850, 50, 500, 150);
       sdrChart.setXAxisFormat(".##");
       sdrChart.setVisible(false);

        transFuncDisplay.setBounds(550, 2, 600, 38);
        
        RenderList steeringTransformGroup = new RenderList();
        steeringTransform = new TransformationAdapter(steeringTransformGroup);
        steeringTransform.rotate(new Vector3f(0f, 0f, 1f), 180);
        steeringTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
        
        RenderList boreTransformGroup = new RenderList();
        boreTransformGroup.setTag("BoreTransformGroup");
        
        mrBoreTransform = new TransformationAdapter(boreTransformGroup);
//        mrBoreTransform.rotate(new Vector3f(1f, 0f, 0f), 180);
//        mrBoreTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
                
        frameOffsetTransform = new TransformationAdapter(stereotacticFrame);
        frameOffsetTransform.rotate(new Vector3f(0, 0, 1), 180);
        frameOffsetTransform.translate(new Vector3f(0f, -40f, -75f)); // Plausible, if not accurate frame position
        
        frameTransform = new TransformationAdapter(frameOffsetTransform);
        
        
        RenderList globalTransformList = new RenderList();        
        TransformationAdapter globalTransform = new TransformationAdapter(globalTransformList);
        globalTransform.rotate(new Vector3f(0f, 0f, 1f), 180);
        globalTransform.rotate(new Vector3f(0f, 1f, 0f), 180);
//        globalTransform.translate(new Vector3f(0f, 0f, 150f));
        
//                glTranslatef(0f, 0f, 300f);
//                glRotatef(180f, 0f, 0f, 1f);
//                //glRotatef(3f, 0f, 0f, 1f);
//                //glRotatef(-5f, 1f, 0f, 0f);
//                glTranslated(0.0, -10f, -220.00);  /////HACK   manual registration for specific frame model
//
//                // final translation for targetting
//                glTranslatef(skull.getXpos(), skull.getYpos(), skull.getZpos());
        
//                glTranslatef(0f, 0f, 300f);
//                glRotatef(180f, 0f, 0f, 1f);
//                //glRotatef(3f, 0f, 0f, 1f);
//                //glRotatef(-5f, 1f, 0f, 0f);
//                glTranslated(0.0, -10f, -220.00);  /////HACK   manual registration for specific frame model
        
        boreTransformGroup.add(mrBore2);
        boreTransformGroup.add(mrHousing);
        boreTransformGroup.add(mrPedestal);
        boreTransformGroup.add(mrBoreGroup);
        
//        globalTransformList.add(transducerModel);
        steeringTransformGroup.add(transducerModel);
        steeringTransformGroup.add(mrBoreTransform);
        
        
        globalTransformList.add(transRayTracer);
        
        background.setClearColor(0.22f, 0.25f, 0.30f, 1f);
        background.setIs2d(true);

//        background.addChild(new DirtyFollower(mainLayer));
        
//        mainLayer.setClearColor(0.22f, 0.25f, 0.30f, 1f);
        mainLayer.setClearColor(0f, 0f, 0f, 0f);
         
        mainLayer.addChild(trackball);
        mainLayer.addChild(frameTransform);
//        mainLayer.addChild(mrBoreTransform);
        mainLayer.addChild(globalTransform);
        mainLayer.addChild(steeringTransform);
        mainLayer.addChild(canvas);
        mainLayer.addChild(transFuncDisplayProxy); // this will trigger updates to mainLayer when the overlay widget is dirty
        mainLayer.addChild(new RenderableAdapter(transducerModel, "renderFocalSpot"));
        mainLayer.addChild(new CrossHair(this.trackball));
        
        
        CrossHair steeringCrossHair = new CrossHair(this.trackball);
        steeringCrossHair.setOffset(this.currentSteering.getLocation());
        steeringCrossHair.setStyle(1);
        
        mainLayer.addChild(steeringCrossHair);
        
        mainLayer.addChild(new TargetRenderer(canvas).setTag("targetRenderer").setVisible(false));
        
        // add animation renderables
        mainLayer.addChild(zoomAnimator);
        mainLayer.addChild(orientAnimator);
                
        
        // The RenderableAdapter allows one Renderable object to render into a different layer
        // with a different method name that we can specify at runtime. The canvas object renders
        // the volume rendering into the main layer with the standard render() method and the
        // demographics into the overlay layer.
        overlay.setIs2d(true);
        overlay.addChild(new RenderableAdapter(canvas, "renderDemographics").setTag("demographics").setVisible(prefs.getBoolean("prefShowDemographics", true)));
        
        overlay.addChild(mprLayout);
        overlay.addChild(canvas1);
        overlay.addChild(canvas2);
        overlay.addChild(canvas3);
        overlay.addChild(flyout1);
        overlay.addChild(flyout2);
        overlay.addChild(flyout3);
        overlay.addChild(fileDialog);
        overlay.addChild(messageDialog);
        overlay.addChild(activeElementsBar);
        overlay.addChild(sdrBar);
        overlay.addChild(beamBar);
        overlay.addChild(statusBar);
        overlay.addChild(transFuncDisplay);
        overlay.addChild(tempPredictionIndicator);
        
        overlay.addChild(incidentAngleChart);
        overlay.addChild(sdrChart);
        
        
        currentTarget.setCommand("currentTargetPoint");
        currentTarget.setPropertyPrefix("Model.Attribute");
        model.addObserver(currentTarget);
        
        currentSteering.setCommand("currentTargetSteering");
        currentSteering.setPropertyPrefix("Model.Attribute");
        model.addObserver(currentSteering);
        
        // Send events to the controller
        canvas.addActionListener(controller);
        canvas.setCommand("currentTargetPoint");
        canvas.setTrackball(trackball);
        canvas.setTransferFunction(transFuncDisplay.getTransferFunction());
        
        canvas1.addActionListener(controller);
        canvas2.addActionListener(controller);
        canvas3.addActionListener(controller);
        canvas1.setCommand("currentTargetPoint");
        canvas2.setCommand("currentTargetPoint");
        canvas3.setCommand("currentTargetPoint");
        
        // Bind to property change update notifications
        canvas.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas2.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        canvas3.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
        model.addObserver(canvas);
        model.addObserver(canvas1);
        model.addObserver(canvas2);
        model.addObserver(canvas3);
        
        ImageLabel logoLabel = new ImageLabel("images/KranionMed.png", 10, 0, 500, 200);
        logoLabel.setTag("logo");
        logoLabel.setVisible(prefs.getBoolean("prefShowLogo", true));
        //ImageLabel logoLabel = new ImageLabel("images/KranionSm.png", 110, 0, 500, 200);
        //logoLabel.setBounds(0, 0, logoLabel.getBounds().width/1.8f, logoLabel.getBounds().height/1.8f);
        //logoLabel.setVisible(false);
        overlay.addChild(logoLabel);
        
        // little oriented human figure in lower left corner
        CoordinateWidget widget = new CoordinateWidget();
        widget.setTrackball(trackball);
        overlay.addChild(widget);
              
        background.setSize(Display.getWidth(), Display.getHeight());
        overlay.setSize(Display.getWidth(), Display.getHeight());
        mainLayer.setSize(Display.getWidth(), Display.getHeight());
        
        scene.addChild(background);
        scene.addChild(mainLayer);
        scene.addChild(overlay);
        
        scene.addChild(transition); // must be last to work properly
        
        
        this.transition.doFadeFromBlack(0.75f);
    }
    
    public int doPick(int mouseX, int mouseY) {
        int result = 0;
        

        pickLayer.bind();
        pickLayer.clear();
        
        preRenderSetup();
        scene.renderPickable();
        
        pickLayer.unbind();
        
        result = pickLayer.getPixelRGBasInt(mouseX, mouseY);
                
        int index = result-5;
        
        transRayTracer.setSelectedElement(index);
        
        updateSelectedChannelData();
                
        return result;
    }
    
    private void updateSelectedChannelData() {
        
        int index = transRayTracer.getSelectedElement();
        
        if (index >= 0 && index < 1024) { // transducer channel
            
            transRayTracer.setSelectedElement(index);
            
            double angle = transRayTracer.getIncidentAnglesFull().get(index);
            double sdr = transRayTracer.getSDRsFull().get(index);
            double sdrAvg = transRayTracer.getSDR2sFull().get(index);
            double skullThickness = transRayTracer.getSkullThicknesses().get(index);
            double normSkullThickness = transRayTracer.getNormSkullThicknesses().get(index);
            float samples[] = transRayTracer.getChannelSkullSamples(index);
            double yData[] = new double[60];
            double xData[] = new double[60];
            
            for (int i=0; i<60; i++) {
                xData[i] = i * 0.5f - 10f;
                yData[i] = (double)samples[i];
            }
            
            model.setAttribute("currentTransducerChannelNum", String.format("%d", index));
            model.setAttribute("currentTransducerChannelOuterAngle", String.format("%3.1f", angle));
            model.setAttribute("currentTransducerChannelSDRavg", String.format("%3.2f", sdrAvg));
            model.setAttribute("currentTransducerChannelSDR", String.format("%3.2f", sdr));
            model.setAttribute("currentTransducerChannelSkullThickness", String.format("%3.1f", skullThickness));
            model.setAttribute("currentTransducerChannelNormSkullThickness", String.format("%3.1f", normSkullThickness));
            
            skullProfileChart.newChart("Depth (mm)", "HU", 7);
            skullProfileChart.addSeries("HU", xData, yData, new Vector4f(0.7f, 0.7f, 0.7f, 1f), false);
            
            xData = new double[1];
            yData = new double[1];
            
            if (samples[60] >= 0) {
                xData[0] = samples[60] * 0.5 - 10d;
                yData[0] = samples[(int)samples[60]];
                skullProfileChart.addSeries("L Peak", xData, yData, new Vector4f(0.2f, 0.9f, 0.2f, 1f));
                
                System.out.println("L Peak: " + yData[0]);
            }
            
            if (samples[61] >= 0) {
                xData[0] = samples[61] * 0.5 - 10d;
                yData[0] = samples[(int)samples[61]];
                skullProfileChart.addSeries("R Peak", xData, yData, new Vector4f(0.3f, 0.3f, 0.9f, 1f));
            }
            
            if (samples[62] >= 0) {
                xData[0] = samples[62] * 0.5 - 10d;
                yData[0] = samples[(int)samples[62]];
                skullProfileChart.addSeries("Mid", xData, yData, new Vector4f(0.9f, 0.2f, 0.2f, 1f));
                System.out.println("Mid minima: " + yData[0]);
            }
            
            skullProfileChart.generateChart();
        }
        else {
            transRayTracer.setSelectedElement(-1);
        }
    }
        
    // sets flag to do an animated blend transition at next frame update
    public void setDoTransition(boolean doTransition) {
        setDoTransition(doTransition, 0.5f);
    }
    
    public void setDoTransition(boolean doTransition, float time) {
        this.doTransition = doTransition;
        this.transitionTime = time;
    }
    
    public void setDisplayCTimage(ImageVolume image) {
        if (image == null) {
            canvas.setCTImage(image);
            canvas1.setCTImage(image);
            canvas2.setCTImage(image);
            canvas3.setCTImage(image);

            return;
        }
        
        float windowWidth;
        float windowCenter;
        
        try {
            windowWidth = (Float) image.getAttribute("WindowWidth");
            windowCenter = (Float) image.getAttribute("WindowCenter");
        }
        catch(NullPointerException e) {
            windowWidth = 3000f;
            windowCenter = 1000f;
        }

        float rescaleSlope = (Float) image.getAttribute("RescaleSlope");
        float rescaleIntercept = (Float) image.getAttribute("RescaleIntercept");

        this.center = (int)windowCenter;
        this.window = (int)windowWidth;
        this.ct_threshold = 500;//(int) ((1800f - rescaleIntercept) / rescaleSlope);

        if (model != null) {
            model.setAttribute("CTwindow", (float)window);
            model.setAttribute("CTcenter", (float)center);
            model.setAttribute("CTthresh", (float)ct_threshold);
       }
        
        canvas.setCTImage(image);
        canvas.setCenterWindow(center, window);
        canvas.setCTrescale(rescaleSlope, rescaleIntercept);
        canvas.setOrientation(0);
        canvas.setCTThreshold(ct_threshold);

        System.out.println("MAIN: setImage #2");
        canvas1.setCTImage(image);
        canvas1.setCenterWindow((int) center, (int) window);
        canvas1.setCTrescale(rescaleSlope, rescaleIntercept);
        canvas1.setOrientation(0);
        canvas1.setCTThreshold(ct_threshold);

        canvas2.setCTImage(image);
        canvas2.setCenterWindow((int) center, (int) window);
        canvas2.setCTrescale(rescaleSlope, rescaleIntercept);
        canvas2.setOrientation(1);
        canvas2.setCTThreshold(ct_threshold);

        canvas3.setCTImage(image);
        canvas3.setCenterWindow((int) center, (int) window);
        canvas3.setCTrescale(rescaleSlope, rescaleIntercept);
        canvas3.setOrientation(2);
        canvas3.setCTThreshold(ct_threshold);

        transRayTracer.setImage(image);
        ctHistogram.setImage(image);
        
    }
    
    public void setDisplayMRimage(ImageVolume image) {
        
        if (image == null) {
            canvas.setMRImage(image);
            canvas1.setMRImage(image);
            canvas2.setMRImage(image);
            canvas3.setMRImage(image);

            return;
        }
        
        float windowWidth = 4095;
        float windowCenter = 1024;
        try {
            windowWidth = (Float) image.getAttribute("WindowWidth");
            windowCenter = (Float) image.getAttribute("WindowCenter");
        }
        catch(Exception e) {
              e.printStackTrace();
        }
        
        Float mrThreshold = (Float)image.getAttribute("Threshold");
        if (mrThreshold == null) {
            mrThreshold = 125f;
        }
        
        //            float rescaleSlope = (Float)mrImage.getAttribute("RescaleSlope");
        //            float rescaleIntercept = (Float)mrImage.getAttribute("RescaleIntercept");
        float rescaleSlope = (Float) image.getAttribute("RescaleSlope");
        float rescaleIntercept = (Float) image.getAttribute("RescaleIntercept");

        this.mr_center = (int)windowCenter;
        this.mr_window = (int)windowWidth;
        
        if (model != null) {
            model.setAttribute("MRwindow", (float)this.mr_window);
            model.setAttribute("MRcenter", (float)this.mr_center);
            model.setAttribute("MRthresh", mrThreshold);
        }
        

        canvas.setMRImage(image);
        canvas.setMRCenterWindow((int) this.mr_center, (int) this.mr_window);
        canvas.setMRrescale(rescaleSlope, rescaleIntercept);
        canvas.setOrientation(0);
        canvas.setMRThreshold(mrThreshold);

        canvas1.setMRImage(image);
        canvas1.setMRCenterWindow((int) this.mr_center, (int) this.mr_window);
        canvas1.setMRrescale(rescaleSlope, rescaleIntercept);
        canvas1.setOrientation(0);
        canvas1.setMRThreshold(mrThreshold);

        canvas2.setMRImage(image);
        canvas2.setMRCenterWindow((int) this.mr_center, (int) this.mr_window);
        canvas2.setMRrescale(rescaleSlope, rescaleIntercept);
        canvas2.setOrientation(1);
        canvas2.setMRThreshold(mrThreshold);

        canvas3.setMRImage(image);
        canvas3.setMRCenterWindow((int) this.mr_center, (int) this.mr_window);
        canvas3.setMRrescale(rescaleSlope, rescaleIntercept);
        canvas3.setOrientation(2);
        canvas3.setMRThreshold(mrThreshold);
    }

    private void saveScene() {
        try {
            File selectedFile;
            
//            JFileChooser fileChooser = new JFileChooser() {
//                @Override
//                public void approveSelection(){
//                    File f = getSelectedFile();
//                    String fname = f.getAbsolutePath();
//                    if(!fname.endsWith(".kranion") && !fname.endsWith(".krn")) {
//                        f = new File(fname + ".krn");
//                    }
//                    this.setSelectedFile(f);
//                    if(f.exists() && getDialogType() == SAVE_DIALOG){
//                        int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_CANCEL_OPTION);
//                        switch(result){
//                            case JOptionPane.YES_OPTION:
//                                super.approveSelection();
//                                return;
//                            case JOptionPane.CANCEL_OPTION:
//                                cancelSelection();
//                                return;
//                            default:
//                                return;
//                        }
//                    }
//                    super.approveSelection();
//                }                        
//            };
//            fileChooser.setDialogTitle(new String("Save Kranion file..."));
////            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("Kranion file", "kranion", "krn");
//            fileChooser.addChoosableFileFilter(filter);
//            fileChooser.setFileFilter(filter);
////            fileChooser.setAcceptAllFileFilterUsed(false);    
//            
//            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//                selectedFile = fileChooser.getSelectedFile();
//            }
//            else {
//                return;
//            }
            
            fileDialog.setDialogTitle("Save to a Kranion scene file");
            fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
            String filters[] = {".krn", ".kranion"};
            selectedFile = fileDialog.open(filters);
            if (selectedFile == null) {
                return;
            }
            
            String selectedFileName = selectedFile.getName();
            if (!selectedFileName.endsWith(".krn") && !selectedFileName.endsWith(".kranion")) {
                selectedFile = new File(selectedFile.getPath() + ".krn");
            }
            
            //check for existing file that would be overwritten
            if (selectedFile.exists()) {
                messageDialog.setDialogTitle("Existing file");
                messageDialog.setMessageText("The file exists, overwrite?");
                if (messageDialog.open() == false) {
                    return;
                }
            }
                                    
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(selectedFile));
            os.writeObject(model);
            os.close();
            
            return;
            
//            FileWriter saveFile = new FileWriter(selectedFile);
//            PrintWriter writer = new PrintWriter(saveFile);
//            
//            Quaternion orient = ((Quaternion)model.getCtImage().getAttribute("ImageOrientationQ"));
//            Vector3f trans = (Vector3f)model.getCtImage().getAttribute("ImageTranslation");
//            
//            writer.println(orient.x);
//            writer.println(orient.y);
//            writer.println(orient.z);
//            writer.println(orient.w);
//            
//            writer.println(trans.x);
//            writer.println(trans.y);
//            writer.println(trans.z);
//            
//            try {
//                orient = ((Quaternion)model.getMrImage(0).getAttribute("ImageOrientationQ"));
//            }
//            catch(NullPointerException e) {
//                orient = new Quaternion().setIdentity();
//            }
//            
//            try {
//                trans = (Vector3f)model.getMrImage(0).getAttribute("ImageTranslation");
//            }
//            catch(NullPointerException e) {
//                trans = new Vector3f();
//            }
//            
//            writer.println(orient.x);
//            writer.println(orient.y);
//            writer.println(orient.z);
//            writer.println(orient.w);
//            
//            writer.println(trans.x);
//            writer.println(trans.y);
//            writer.println(trans.z);
//            
//            writer.println(this.center);
//            writer.println(this.window);
//            writer.println(this.mr_center);
//            writer.println(this.mr_window);
//
//            //Close writer
//            writer.close();
//            saveFile.close();
        }
        catch (Exception e) {
            System.out.println("Error writing scene file.");
            e.printStackTrace();
        }
        
    }
    
    private void enumSavedClasses(Model model) {
        Iterator<String> keys = model.getAttributeKeys();
        while (keys != null && keys.hasNext()) {
            String key = keys.next();
            Object obj = model.getAttribute(key);
            System.out.println("Model[" + key + "] = " + obj.getClass().toString());
        }
        
        try {
            keys = model.getCtImage().getAttributeKeys();
            while (keys != null && keys.hasNext()) {
                String key = keys.next();
                Object obj = model.getCtImage().getAttribute(key);
                System.out.println("Model.CTImage[" + key + "] = " + obj.getClass().toString());
            }
        } catch (NullPointerException e) {
        }

        try {
            keys = model.getMrImage(0).getAttributeKeys();
            while (keys != null && keys.hasNext()) {
                String key = keys.next();
                Object obj = model.getMrImage(0).getAttribute(key);
                System.out.println("Model.MRImage[0][" + key + "] = " + obj.getClass().toString());
            }
        } catch (NullPointerException e) {
        }

        try {
            keys = model.getSonication(0).getAttributeKeys();
            while (keys != null && keys.hasNext()) {
                String key = keys.next();
                Object obj = model.getSonication(0).getAttribute(key);
                System.out.println("Model.Sonication[0][" + key + "] = " + obj.getClass().toString());
            }
        } catch (NullPointerException e) {
        }
    }
    
    private void loadScene() { // TODO: FIX THIS AFTER TESTING        
        try {
            
            File selectedFile;
            
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(new String("Load Kranion file..."));
//            FileNameExtensionFilter filter = new FileNameExtensionFilter("Kranion file", "kranion", "krn");
//            fileChooser.addChoosableFileFilter(filter);
//            fileChooser.setFileFilter(filter);
////            fileChooser.setAcceptAllFileFilterUsed(false);    
////            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                selectedFile = fileChooser.getSelectedFile();
//            }
//            else {
//                return;
//            }

            fileDialog.setDialogTitle("Open a Kranion scene file");
            fileDialog.setFileChooseMode(FileDialog.fileChooseMode.EXISTING_FILES);
            String[] filters = {".krn", ".kranion"};
            selectedFile = fileDialog.open(filters);
            if (selectedFile == null) {
                return;
            }
                                    
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(selectedFile));
            Model newModel = (Model)is.readObject();
            is.close();
            
            enumSavedClasses(newModel);
            
            // re-plumb everything for the new model...
            
            Main.setModel(newModel); // should update M,V,C
                        
            ImageVolumeUtil.releaseTextures(model.getCtImage());
            
            // make sure any texture name attributes that exist are removed
            // this is a holdover from before we had transient attributes
            for (int i=0; i<model.getMrImageCount(); i++) {
                ImageVolumeUtil.releaseTextures(model.getMrImage(i));
            }
            
            this.setDisplayCTimage(model.getCtImage());
            this.setDisplayMRimage(model.getMrImage(0));
            
            try {
                System.out.println("CT registration stored:");
                System.out.println(model.getCtImage().getAttribute("ImageOrientationQ"));
                System.out.println(model.getCtImage().getAttribute("ImageTranslation"));
                System.out.println("MR registration stored:");
                System.out.println(model.getMrImage(0).getAttribute("ImageOrientationQ"));
                System.out.println(model.getMrImage(0).getAttribute("ImageTranslation"));
            }
            catch(Exception e) {}
            
//                    model.setAttribute("doMRI", true);
                    model.updateAllAttributes();
                    canvas.setVolumeRender(true);
                    canvas.setIsDirty(true);
                    
                    ctHistogram.calculate();
                    transFuncDisplay.setHistogram(ctHistogram.getData());
                    ctHistogram.release();
            
//            this.updateFromModel();

            this.imageregistration.setModel(newModel);
            
            this.updateMRlist();
            this.updateSonicationList();
            model.setAttribute("currentSonication", 0);
            
            setDoTransition(true);
            
            return;

//            String sCurrentLine;
//            BufferedReader bufferedReader = new BufferedReader(new FileReader(selectedFile));
//            
//            Quaternion orient = new Quaternion();
//            Vector3f trans = new Vector3f();
//            
//            orient.x = Float.parseFloat(bufferedReader.readLine());
//            orient.y = Float.parseFloat(bufferedReader.readLine());
//            orient.z = Float.parseFloat(bufferedReader.readLine());
//            orient.w = Float.parseFloat(bufferedReader.readLine());
//            
//            trans.x = Float.parseFloat(bufferedReader.readLine());
//            trans.y = Float.parseFloat(bufferedReader.readLine());
//            trans.z = Float.parseFloat(bufferedReader.readLine());
//            
//            model.getCtImage().setAttribute("ImageOrientationQ", orient);
//            model.getCtImage().setAttribute("ImageTranslation", trans);
//            
//            orient = new Quaternion();
//            trans = new Vector3f();
//            
//            orient.x = Float.parseFloat(bufferedReader.readLine());
//            orient.y = Float.parseFloat(bufferedReader.readLine());
//            orient.z = Float.parseFloat(bufferedReader.readLine());
//            orient.w = Float.parseFloat(bufferedReader.readLine());
//            
//            trans.x = Float.parseFloat(bufferedReader.readLine());
//            trans.y = Float.parseFloat(bufferedReader.readLine());
//            trans.z = Float.parseFloat(bufferedReader.readLine());
//            
//            try {
//                model.getMrImage(0).setAttribute("ImageOrientationQ", orient);
//                model.getMrImage(0).setAttribute("ImageTranslation", trans);
//            }
//            catch(NullPointerException e) {
//                
//            }
//            
//            this.center = Integer.parseInt(bufferedReader.readLine());
//            this.window = Integer.parseInt(bufferedReader.readLine());
//            this.mr_center = Integer.parseInt(bufferedReader.readLine());
//            this.mr_window = Integer.parseInt(bufferedReader.readLine());
//            
//            if (model != null) {
//                model.setAttribute("MRcenter", (float)mr_center);
//                model.setAttribute("MRwindow", (float)mr_window);
//                model.setAttribute("CTcenter", (float)center);
//                model.setAttribute("CTwindow", (float)window);
//            }
//            
//            canvas.setCenterWindow(center, window);
//            canvas.setMRCenterWindow(mr_center, mr_window);
//            canvas1.setCenterWindow(center, window);
//            canvas1.setMRCenterWindow(mr_center, mr_window);
//            canvas2.setCenterWindow(center, window);
//            canvas2.setMRCenterWindow(mr_center, mr_window);
//            canvas3.setCenterWindow(center, window);
//            canvas3.setMRCenterWindow(mr_center, mr_window);
//
//            //Close reader
//            bufferedReader.close();
        }
        catch (Exception e) {
            System.out.println("Error reading scene file.");
            e.printStackTrace();
        }
        
    }

    private void setShowTargets(boolean bShow) {
        Renderable targetRenderer = Renderable.lookupByTag("targetRenderer");
        targetRenderer.setVisible(bShow);
    }
    
    private void showThermometry(int sonicationIndex) {
        
        if (sonicationIndex == -1) {
            canvas.setShowThermometry(false);
//            canvas.setOverlayImage(null);
            return;
        }
        
        Vector3f steering = this.currentSteering.getLocation();//(Vector3f)model.getAttribute("currentTargetSteering");
        if (steering == null) {
            steering = new Vector3f(0, 0, 0);
        }
        Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
        Vector3f spotPosition = Vector3f.add(naturalFocusPosition, steering, null);
        
        ImageVolume image = model.getSonication(sonicationIndex).getThermometryPhase();
        Integer currentFrame = (Integer)model.getSonication(sonicationIndex).getAttribute("currentFrame");
        if (currentFrame == null) {
            currentFrame = new Integer(0);
        }
        
        canvas.setShowPressure(false);
        
        if (model.getAttribute("showThermometry") != null) {
            boolean bShow = (Boolean)model.getAttribute("showThermometry");
            canvas.setShowThermometry(bShow, /*as dose?*/false);

            if (bShow && model.getAttribute("showDose") != null) {
                Boolean bShowDose = (Boolean) model.getAttribute("showDose");
                if (bShowDose) {
                    image = this.calcThermalDoseImage(sonicationIndex);
                    currentFrame = 0;
                }
                canvas.setShowThermometry(bShow, /*as dose?*/(bShowDose==null)?false:bShowDose);
            }
            else {
                canvas.setShowThermometry(false);            
            }
        }
        else {
            canvas.setShowThermometry(false);            
        }

        System.out.println("Spot position " + spotPosition);
        
        float[] imageLoc = (float[])image.getAttribute("ImagePosition");
        System.out.println("Image position = " + imageLoc[0] + ", " + imageLoc[1] + ", " + imageLoc[2]);
                
        
        canvas.setCurrentOverlayFrame(currentFrame);
        ImageVolumeUtil.releaseTexture(image);
        canvas.setOverlayImage(image);
        
        model.setAttribute("currentSceneOrienation", (Quaternion)image.getAttribute("ImageOrientationQ"));

    }
    
    private void zeroImageTranslations() {
        try {
            model.getCtImage().setAttribute("ImageTranslation", new Vector3f());
        }
        catch(Exception e) {}
        
        for (int i=0; i<model.getMrImageCount(); i++) {
            try {
                model.getMrImage(i).setAttribute("ImageTranslation", new Vector3f());
            }
            catch(Exception e) {}
        }
    }
    
    private void updateTargetAndSteering() {
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f(0, 0, 0);
            }
            this.transRayTracer.setTargetSteering(steering.x, -steering.y, -steering.z);
            
            Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
            Vector3f spotPosition = Vector3f.add(naturalFocusPosition, steering, null);
            
            canvas.setTextureRotatation(spotPosition, trackball);
            
            this.transRayTracer.setTextureRotatation(naturalFocusPosition, trackball);
            
            //zeroImageTranslations();
    }
    
    private void updatePressureCalc() {
        if (canvas.getShowPressure()) {
                       
            this.transRayTracer.calcPressureEnvelope(new Quaternion(this.trackball.getCurrent()));

            canvas.setShowPressure(true);
            canvas.setOverlayImage(transRayTracer.getEnvelopeImage());

            canvas1.setOverlayImage(null);
            canvas2.setOverlayImage(null);
            canvas3.setOverlayImage(null);
        }
    }
    
    private void updateFromModel() {

        
        this.showRayTracer = (Boolean) model.getAttribute("showRayTracer", false);

        if (!this.showRayTracer) {
            activeElementsBar.setValue(-1f);
            sdrBar.setValue(-1f);
            beamBar.setValue(-1f);
            incidentAngleChart.setVisible(false);
            sdrChart.setVisible(false);
        } else {
            incidentAngleChart.setVisible(true);
            sdrChart.setVisible(true);
            boolean showSkullFloor = (Boolean) model.getAttribute("showSkullFloorStrikes", false);
            this.transRayTracer.setShowSkullFloorStrikes(showSkullFloor);
        }
        
        try {
            this.doClip = (Boolean)model.getAttribute("doClip");
        }
        catch(NullPointerException e) {
            this.doClip = false;
            model.setAttribute("doClip", doClip);
        }
        
        try {
            this.doMRI = (Boolean)model.getAttribute("doMRI");
        }
        catch(NullPointerException e) {
            this.doMRI = false;
            model.setAttribute("doMRI", doMRI);
        }

        try {
            this.doFrame = (Boolean)model.getAttribute("doFrame");
        }
        catch(NullPointerException e) {
            this.doFrame = false;
            model.setAttribute("doFrame", doFrame);
        }
        
        try {
            this.transRayTracer.setBoneSpeed((Float)model.getAttribute("boneSOS"));
        }
        catch(NullPointerException e) {
            model.setAttribute("boneSOS", transRayTracer.getBoneSpeed());
        }  
        
        try {
            this.transRayTracer.setBoneRefractionSpeed((Float)model.getAttribute("boneRefractionSOS"));
        }
        catch(NullPointerException e) {
            model.setAttribute("boneRefractionSOS", transRayTracer.getBoneRefractionSpeed());
        }
        
        try {
            mr_center = ((Float)model.getAttribute("MRcenter")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            boolean old = canvas.getShowThermometry();
            boolean oldShowDose = canvas.getShowDose();
            
            Boolean bShow = (Boolean)model.getAttribute("showThermometry");
            Boolean bShowDose = (Boolean)model.getAttribute("showDose");
            
            if (bShow == null) bShow = new Boolean(false);
            if (bShowDose == null) bShowDose = new Boolean(false);
            
            canvas.setShowThermometry(bShow, bShowDose);
                        
            if (old != bShow || oldShowDose != bShowDose) {
                this.setDoTransition(true);
            }
        }
        catch(NullPointerException e) {
            model.setAttribute("showThermometry", false);
        }
        
        try {
            mr_window = ((Float)model.getAttribute("MRwindow")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            mr_threshold = ((Float)model.getAttribute("MRthresh")).intValue();
        }
        catch(NullPointerException e) {
        }
        
        try {
            center = ((Float)model.getAttribute("CTcenter")).intValue();
        }
            catch(NullPointerException e) {
        }
        
        try {
            window = ((Float)model.getAttribute("CTwindow")).intValue();
        }
        catch(NullPointerException e) {
        }
       
        try {
            this.canvas.setForegroundVolumeSlices(((Float)model.getAttribute("foregroundVolumeSlices")).intValue());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        catch(NullPointerException e) {
            model.setAttribute("foregroundVolumeSlices", 0f);
        }
        
        try {
            Vector3f trans = ((Vector3f)model.getAttribute("frameOffsetTranslation"));
            this.frameOffsetTransform.setTranslation(trans.x, trans.y, trans.z);
        }
        catch(NullPointerException e) {
            model.setAttribute("frameOffsetTranslation", frameOffsetTransform.getTranslation());
        }     
    }
    
    protected void preRenderSetup() {
        updateFromModel();
        
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Looking up the positive Z axis, Y is down, compliant with DICOM scanner sensibilities
        gluLookAt(0.0f, 0.0f, cameraZ, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f);

        // camera dolly in/out
        glTranslatef(0.0f, 0.0f, dolly.getValue());

        // clip plan through the focal point normal to camera view
        DoubleBuffer eqnBuf = BufferUtils.createDoubleBuffer(4);
        eqnBuf.put(0.0f).put(0.0f).put(1.0f).put(8f);
        eqnBuf.flip();
        glClipPlane(GL_CLIP_PLANE0, eqnBuf);

        DoubleBuffer eqnBuf2 = BufferUtils.createDoubleBuffer(4);
        eqnBuf2.put(0.0f).put(0.0f).put(-1.0f).put(8f);
        eqnBuf2.flip();
        glClipPlane(GL_CLIP_PLANE1, eqnBuf2);

//        trackball.render();

        mrBoreTransform.setTranslation(-currentTarget.getXpos(), currentTarget.getYpos(), 0f);
        
        frameTransform.setTranslation(
                -currentTarget.getXpos()-currentSteering.getXpos(),
                -currentTarget.getYpos()-currentSteering.getYpos(),
                -currentTarget.getZpos()-currentSteering.getYpos());
        
        steeringTransform.setTranslation( -currentSteering.getXpos(), -currentSteering.getYpos(), -currentSteering.getZpos());

        Vector3f naturalFocusPosition = new Vector3f(currentTarget.getXpos(),
                                                     currentTarget.getYpos(),
                                                     currentTarget.getZpos());
        
        Vector3f spotPosition = new Vector3f(currentTarget.getXpos()+currentSteering.getXpos(),
                                             currentTarget.getYpos()+currentSteering.getYpos(),
                                             currentTarget.getZpos()+currentSteering.getZpos());
        
        // render oblique MR slice
        if (doMRI) {

            canvas.setVisible(true);
            
            // These could all eventuall be done as property notifications
            canvas.setCenterWindow(center, window);
            canvas1.setCenterWindow(center, window);
            canvas2.setCenterWindow(center, window);
            canvas3.setCenterWindow(center, window);

            canvas.setMRCenterWindow(mr_center, mr_window);
            canvas1.setMRCenterWindow(mr_center, mr_window);
            canvas2.setMRCenterWindow(mr_center, mr_window);
            canvas3.setMRCenterWindow(mr_center, mr_window);

            canvas.setCTThreshold(ct_threshold);
            canvas.setMRThreshold(mr_threshold);

            // don't need this now. canvas.update() will get changes from model
//            canvas.setTextureRotatation(CofR, trackball);
        } else {
            canvas.setVisible(false);
        }

        // render frame
        if (doFrame) {
            stereotacticFrame.setVisible(true);

            stereotacticFrame.getShader().start();

            Vector3f loc = Vector3f.sub(currentTarget.getLocation(), this.frameOffsetTransform.getTranslation(), null);
            
            int uniformLoc = glGetUniformLocation(stereotacticFrame.getShader().getShaderProgramID(), "offset");
            glUniform3f(uniformLoc, loc.x, loc.y, loc.z); // 300 - 220 = 80

            uniformLoc = glGetUniformLocation(stereotacticFrame.getShader().getShaderProgramID(), "transducerAngle");
            glUniform1f(uniformLoc, (float) (transducerTilt / 180f * Math.PI));

            stereotacticFrame.getShader().stop();

            if (doClip) {
                stereotacticFrame.setClipped(false);
            } else {
                stereotacticFrame.setClipped(false);
            }
        } else {
            stereotacticFrame.setVisible(false);
        }

        if (showScanner) {
            if (doClip) {
                mrBore2.setClipped(true);

                mrBore2.setVisible(true);
                mrHousing.setVisible(false);
                mrPedestal.setVisible(false);

                mrBore2.setClipColor(0.6f, 0.6f, 0.6f, 1f);
                mrHousing.setClipColor(0.4f, 0.4f, 0.4f, 1f);

                mrBore2.setTrackball(trackball);
                mrBore2.setDolly(cameraZ, dolly.getValue());
            } else {
                mrBore2.setClipped(false);

                mrBore2.setVisible(true);
                mrHousing.setVisible(true);
                mrPedestal.setVisible(true);
            }
        } else {
            mrBore2.setVisible(false);
            mrHousing.setVisible(false);
            mrPedestal.setVisible(false);
        }

        // select shaded or wireframe rendering for the transducer
//        if (drawingStyle == 0) {
//            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//        } else {
//            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
//        }

        transducerModel.setVisible(true);
        if (doClip) {
            transducerModel.setClipped(true);
            transducerModel.setTrackball(trackball);
            transducerModel.setDolly(cameraZ, dolly.getValue());
        } else {
            transducerModel.setClipped(false);
        }

        transducerModel.setTransducerXTilt(transducerTilt);

        if (showRayTracer) {
            transRayTracer.setVisible(true);
            transRayTracer.setTextureRotatation(naturalFocusPosition, trackball);
            transRayTracer.setTransducerTilt(-transducerTilt);

            activeElementsBar.setValue(transRayTracer.getActiveElementCount());
            sdrBar.setValue(transRayTracer.getSDR());
            
            // Ohio State temp rise prediction display
            try {
                int sonicationIndex = (Integer) model.getAttribute("currentSonication");
                if (sonicationIndex >= 0 && model.getSonication(sonicationIndex) != null) {
                    float sonicationEnergy = model.getSonication(sonicationIndex).getPower() * model.getSonication(sonicationIndex).getDuration();
                    float predictedTemp = 37f + 17.2638f - 2.5318f + 0.0000073749f * transRayTracer.getBEAMvalue() * sonicationEnergy;

                    if (sonicationEnergy < 500f) {
                        predictedTemp = 37f;
                    }

                    if (predictedTemp > 62.920f) {
                        tempPredictionIndicator.setText(">=55C (97%tile)");
                        tempPredictionIndicator.setColor(0.1f, 0.6f, 0.1f, 1f);
                        tempPredictionIndicator.setVisible(true);
                    } else if (predictedTemp > 58.780f) {
                        tempPredictionIndicator.setText(">=55C (75%tile)");
                        tempPredictionIndicator.setColor(0.75f, 0.75f, 0.1f, 1f);
                        tempPredictionIndicator.setVisible(true);
                    } else if (predictedTemp > 55.72f) {
                        tempPredictionIndicator.setText(">=50C (97%tile)");
                        tempPredictionIndicator.setColor(0.1f, 0.6f, 0.1f, 1f);
                        tempPredictionIndicator.setVisible(true);
                    } else if (predictedTemp > 52.730f) {
                        tempPredictionIndicator.setText(">=50C (75%tile)");
                        tempPredictionIndicator.setColor(0.75f, 0.75f, 0.1f, 1f);
                        tempPredictionIndicator.setVisible(true);
                    } else {
                        tempPredictionIndicator.setVisible(false);
                    }

                    beamBar.setValue(transRayTracer.getBEAMvalue());
                } else {
                    beamBar.setValue(-1);
                    tempPredictionIndicator.setVisible(false);
                }
            } catch (Exception e) {
                beamBar.setValue(-1);
                tempPredictionIndicator.setVisible(false);
            }

        } else {
            transRayTracer.setVisible(false);
            tempPredictionIndicator.setVisible(false);
        }

        if (!showScanner) {
            if (doClip) {
                mrBoreGroup.setClipped(true);
                mrBoreGroup.setTrackball(trackball);
                mrBoreGroup.setDolly(cameraZ, dolly.getValue());
            } else {
                mrBoreGroup.setClipped(false);
            }
        }

        if (doMRI) {
            canvas.setVisible(true);
        }

//        canvas1.setTextureRotatation(spotPosition, trackball);
//        canvas2.setTextureRotatation(spotPosition, trackball);
//        canvas3.setTextureRotatation(spotPosition, trackball);

        if (canvas.getVolumeRender() == true) {
            transFuncDisplay.setVisible(true);
        } else {
            transFuncDisplay.setVisible(false);
        }
        
        if (this.canvas.getShowPressure() && this.trackball.getIsDirty()) {
            this.updatePressureCalc();
            System.out.println("update pressure calc");
        }
        
        if (doTransition) {
            System.out.println("Do transition");
            transition.doTransition(transitionTime);
            doTransition = false;
        }        
    }
    
    @Override
    public void render() {
        
//        setIsDirty(false);

        preRenderSetup();

        scene.render();
    }

    private static boolean mouseButton1Drag = false;
    private boolean OnMouse(int x, int y, boolean button1down, boolean button2down, int dwheel) {
                
        if (button2down) {
            int pickVal = doPick(x, y);
            System.out.println("*** Picked value: " + pickVal);
        }
        
        if (!scene.OnMouse(x, y, button1down, button2down, dwheel)) {
            
            if (dwheel != 0) {
                scene.setIsDirty(true);
                dolly.incrementValue(-dwheel/5f); // zoom with mouse wheel
            }

            if (button1down) {
                this.acquireKeyboardFocus();
            }
            
            // Mouse dragged
            if (mouseButton1Drag == true && Mouse.isButtonDown(0)) {
//                System.out.println("*** Mouse dragged");
                if (orientAnimator != null) {
                    orientAnimator.cancelAnimation();
                }

                if (currentMouseMode == mouseMode.SCENE_ROTATE) {
                    trackball.mouseDragged(x, y);
                    return true;
                }
                
                if (currentMouseMode == mouseMode.FRAME_TRANSLATE) {
                    // image translation in the plane of the screen
                    Quaternion orient = trackball.getCurrent().negate(null);
                    Matrix4f mat4 = Trackball.toMatrix4f(orient);
                    Vector4f offset = new Vector4f((x - mouseStartX) / 5f, -(y - mouseStartY) / 5f, 0f, 0f);
                    System.out.println("offset = " + offset);
                    Matrix4f.transform(mat4, offset, offset);
                    Vector3f translate = new Vector3f(offset);
                    
                    Vector3f translateStart = (Vector3f)model.getAttribute("frameTranslateStart");
                    
                    frameOffsetTransform.setTranslation(translateStart.x + translate.x, translateStart.y + translate.y, translateStart.z + translate.z);
                    
                    model.setAttribute("frameOffsetTranslation", new Vector3f(frameOffsetTransform.getTranslation()));
                    
                }
                
                if (currentMouseMode == mouseMode.SKULL_ROTATE || currentMouseMode == mouseMode.HEAD_ROTATE) {
                    
                    if (model.getCtImage() == null) return false;
                    
                    // Rotate the CT volume
                    registerBallCT.mouseDragged(x, y);
                    
                    Quaternion mrQnow = new Quaternion(registerBallCT.getCurrent());
                    Quaternion.mul(trackball.getCurrent().negate(null), mrQnow, mrQnow);
                    model.getCtImage().setAttribute("ImageOrientationQ", mrQnow.negate(null));
                    this.setIsDirty(true);

                    if (currentMouseMode == mouseMode.HEAD_ROTATE) {
                        // Rotating the CT origin about the MR origin
                        ///////////////////////////////////////////////
                        
                        // Rotate each MR volume
                        try {
                            mrQnow = new Quaternion().setIdentity();

                            for (int i = 0; i < model.getMrImageCount(); i++) {
                                Trackball registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                                if (registerBall == null) {
                                    registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                                    model.getMrImage(i).setAttribute("registerBall", registerBall, true);
                                }
                                registerBall.mouseDragged(x, y);
                                mrQnow = new Quaternion(registerBall.getCurrent());
                                Quaternion.mul(trackball.getCurrent().negate(null), mrQnow, mrQnow);
                                model.getMrImage(i).setAttribute("ImageOrientationQ", mrQnow.negate(null));

                            }
                            
                            for (int i=1; i<model.getMrImageCount(); i++) {
                                fixTranslationToRotateAboutImage(model.getMrImage(i), model.getMrImage(0));
                            }
                                        
                            fixTranslationToRotateAboutImage(model.getCtImage(), model.getMrImage(0));

                            this.setIsDirty(true);

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    return true;
                }                
                else if (currentMouseMode == mouseMode.SKULL_TRANSLATE || currentMouseMode == mouseMode.HEAD_TRANSLATE) {
                    System.out.println("*** Skull translate");
                    
                    if (model.getCtImage() == null) return false;
                    
                    // image translation in the plane of the screen
                    Quaternion orient = trackball.getCurrent().negate(null);
                    Matrix4f mat4 = Trackball.toMatrix4f(orient);
                    Vector4f offset = new Vector4f((x - mouseStartX) / 5f, -(y - mouseStartY) / 5f, 0f, 0f);
                    System.out.println("offset = " + offset);
                    Matrix4f.transform(mat4, offset, offset);
                    Vector3f translate = new Vector3f(offset);

                    Vector3f startCtImageTranslate = (Vector3f)model.getCtImage().getAttribute("startTranslation");
                    model.getCtImage().setAttribute("ImageTranslation", Vector3f.add(startCtImageTranslate, translate.negate(null), null));
                    this.setIsDirty(true);

                    if (currentMouseMode == mouseMode.HEAD_TRANSLATE) {
                        System.out.println("*** Head translate");
                        try {
                            for (int i = 0; i < model.getMrImageCount(); i++) {
                                Vector3f startMrImageTranslate = (Vector3f) model.getMrImage(i).getAttribute("startTranslation");
                                if (startMrImageTranslate == null) {
                                    startMrImageTranslate = new Vector3f();
                                }
                                System.out.println("startMRImageTranslate = " + startMrImageTranslate);
                                model.getMrImage(i).setAttribute("ImageTranslation", Vector3f.add(startMrImageTranslate, translate.negate(null), null)); //TEMP CHANGE
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        this.setIsDirty(true);
                    }
                    return true;
                }
                else if (currentMouseMode == mouseMode.MRI_ROTATE) {
                        // Rotate current MR volume
                        try {
                            Quaternion mrQnow = new Quaternion().setIdentity();

                            int i = model.getSelectedMR();
                            
                            if (i==-1 && model.getMrImageCount() > 0) {
                                i = 0;
                            }
                            
                                Trackball registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                                if (registerBall == null) {
                                    registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                                    model.getMrImage(i).setAttribute("registerBall", registerBall, true);
                                }
                                registerBall.mouseDragged(x, y);
                                mrQnow = new Quaternion(registerBall.getCurrent());
                                Quaternion.mul(trackball.getCurrent().negate(null), mrQnow, mrQnow);
                                model.getMrImage(i).setAttribute("ImageOrientationQ", mrQnow.negate(null));

                            
                                fixTranslationToRotateAboutImage(model.getMrImage(i), model.getMrImage(0));
                                        
                            fixTranslationToRotateAboutImage(model.getCtImage(), model.getMrImage(0));

                            this.setIsDirty(true);

                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                }
                else if (currentMouseMode == mouseMode.MRI_TRANSLATE) {
                        System.out.println("*** MRI translate");
                        try {
                            int i = model.getSelectedMR();
                            
                            if (i==-1 && model.getMrImageCount() > 0) {
                                i = 0;
                            }
                            
                                // image translation in the plane of the screen
                                Quaternion orient = trackball.getCurrent().negate(null);
                                Matrix4f mat4 = Trackball.toMatrix4f(orient);
                                Vector4f offset = new Vector4f((x - mouseStartX) / 5f, -(y - mouseStartY) / 5f, 0f, 0f);
                                System.out.println("offset = " + offset);
                                Matrix4f.transform(mat4, offset, offset);
                                Vector3f translate = new Vector3f(offset);
                    
                                Vector3f startMrImageTranslate = (Vector3f) model.getMrImage(i).getAttribute("startTranslation");
                                if (startMrImageTranslate == null) {
                                    startMrImageTranslate = new Vector3f();
                                }
                                System.out.println("startMRImageTranslate = " + startMrImageTranslate);
                                model.getMrImage(i).setAttribute("ImageTranslation", Vector3f.add(startMrImageTranslate, translate.negate(null), null)); //TEMP CHANGE
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        this.setIsDirty(true);                    
                }
            }
            // Mouse pressed, init drag
            else if (button1down && !mouseButton1Drag) {
//                System.out.println("*** Mouse pressed, drag init");
                mouseButton1Drag = true;
                
                this.mouseStartX = x;
                this.mouseStartY = y;
                
                if (currentMouseMode == mouseMode.FRAME_TRANSLATE) {
                    model.setAttribute("frameTranslateStart", new Vector3f(this.frameOffsetTransform.getTranslation()));
                    return true;
                }
                
                Vector3f startCtImageTranslate = new Vector3f();
                try {
                        if (model.getCtImage() != null) {
                        Vector3f ctrans = (Vector3f)model.getCtImage().getAttribute("ImageTranslation");
                        if (ctrans != null) {
                            startCtImageTranslate = new Vector3f(ctrans);
                        }
                    }
                }
                catch(NullPointerException e) {
                    e.printStackTrace();
                }
                
                if (model.getCtImage() != null) {
                    model.getCtImage().setAttribute("startTranslation", new Vector3f(startCtImageTranslate), true);
                }
                
                for (int i = 0; i < model.getMrImageCount(); i++) {
                    if (model.getMrImage(i) != null) {
                        try {
                            Vector3f startMrImageTranslate = (Vector3f) model.getMrImage(i).getAttribute("ImageTranslation"); //TEMP CHANGE
                            if (startMrImageTranslate == null) {
                                startMrImageTranslate = new Vector3f();
                            }
                            model.getMrImage(i).setAttribute("startTranslation", new Vector3f(startMrImageTranslate), true);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            model.getMrImage(i).setAttribute("startTranslation", new Vector3f(), true);
                        }
                        
                    }
                }
                

                if (currentMouseMode == mouseMode.SCENE_ROTATE) {
                    trackball.mousePressed(x, y);
                    return true;
                }
                else if (this.currentMouseMode == mouseMode.HEAD_ROTATE
                        || this.currentMouseMode == mouseMode.SKULL_ROTATE
                        || this.currentMouseMode == mouseMode.MRI_ROTATE) {
                    
                    
                    if (model.getCtImage() == null) return false;
                                        
                    Quaternion ctQstart = new Quaternion((Quaternion) model.getCtImage().getAttribute("ImageOrientationQ")).negate(null);
                    model.getCtImage().setAttribute("startOrientationQ", new Quaternion(ctQstart), true);
                    
                    Quaternion.mul(trackball.getCurrent(), ctQstart, ctQstart);
                    registerBallCT.setCurrent(ctQstart);
                    registerBallCT.mousePressed(x, y);

                    if (this.currentMouseMode == mouseMode.HEAD_ROTATE || this.currentMouseMode == mouseMode.MRI_ROTATE) {
                        for (int i = 0; i < model.getMrImageCount(); i++) {
                            Quaternion mrQstartTmp = ((Quaternion) model.getMrImage(i).getAttribute("ImageOrientationQ")).negate(null);

                            Trackball registerBall = null;
                            try {
                                registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                                if (registerBall == null) {
                                    registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                                    model.getMrImage(i).setAttribute("registerBall", registerBall, true);                                    
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }

                            Quaternion.mul(trackball.getCurrent(), mrQstartTmp, mrQstartTmp);
                            registerBall.setCurrent(new Quaternion(mrQstartTmp));
                            registerBall.mousePressed(x, y);
                            
                            Quaternion mrQstart = (Quaternion) model.getMrImage(i).getAttribute("ImageOrientationQ");
                            model.getMrImage(i).setAttribute("startOrientationQ", new Quaternion(mrQstart), true);
                        }
                    }
                }
                return true;
            }
            // Mouse released, end drag
            else if (!button1down && mouseButton1Drag) {
//                System.out.println("*** Mouse released, drag end");
                mouseButton1Drag = false;
                trackball.mouseReleased(x, y);
                registerBallCT.mouseReleased(x, y);

                //for each MR image in the model
                for (int i = 0; i < model.getMrImageCount(); i++) {
                    if (model.getMrImage(i) != null) {
                        Trackball registerBall = null;
                        
                        try {
                            registerBall = (Trackball) model.getMrImage(i).getAttribute("registerBall");
                        }
                        catch(ClassCastException e) {}
                        
                        if (registerBall == null) {
                            registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                            model.getMrImage(i).setAttribute("registerBall", registerBall);
                        }
                        registerBall.mouseReleased(x, y);
                    }
                }
                
                return true;
            }
        }
        
        return false;
    }

    // Adjust the translation of an target image volume so that it correctly orbits around
    // the origin of the specified center image volume. Assumes both images have been rotated
    // already by the same amount in the World coordinate system.
    private void fixTranslationToRotateAboutImage(ImageVolume target, ImageVolume center) {
        // Need to fix the translation of the CT image since these rotations are about the MR volume center
        Vector3f centerTrans = (Vector3f) center.getAttribute("startTranslation");
        if (centerTrans == null) {
            centerTrans = new Vector3f();
        }
        Vector4f centerTrans4f = new Vector4f(centerTrans.x, centerTrans.y, centerTrans.z, 1f);

        Vector3f targetTrans = (Vector3f) target.getAttribute("startTranslation");
        if (targetTrans == null) {
            targetTrans = new Vector3f();
        }
        Vector4f targetTrans4f = new Vector4f(targetTrans.x, targetTrans.y, targetTrans.z, 1f);

        // compute offset vector between MR and CT translated origins
        Vector4f targetRotVec = Vector4f.sub(targetTrans4f, centerTrans4f, null);
        targetRotVec.w = 1f;

        // World to MR-local transform
        Quaternion centerQstart = new Quaternion((Quaternion) center.getAttribute("startOrientationQ"));

        Matrix4f worldToCenterLocal = Trackball.toMatrix4f(centerQstart);
        Matrix4f.transform(worldToCenterLocal, targetRotVec, targetRotVec);

        Quaternion centerQNow = new Quaternion((Quaternion) center.getAttribute("ImageOrientationQ"));

        Quaternion mrQdiff = Quaternion.mul(centerQstart, centerQNow.negate(null), null); // effectively the rotational difference between start and drag Qs
        Matrix4f translateMat4 = Trackball.toMatrix4f(mrQdiff);
//                           System.out.println("mrQstart = " + mrQstart);
//                           System.out.println("mrQNow = " + mrQNow);
//                           System.out.println("Q diff = " + Quaternion.mul(mrQstart, mrQNow.negate(null), null));

        // MR-local rotate the offset vector
        Matrix4f.transform(translateMat4, targetRotVec, targetRotVec);

        // MR-local to World transform
        Matrix4f centerLocalToWorld = Trackball.toMatrix4f(centerQstart.negate(null));
        Matrix4f.transform(centerLocalToWorld, targetRotVec, targetRotVec);

        // add MR orgin back
        Vector4f.add(targetRotVec, centerTrans4f, targetRotVec);

        Vector3f translate = new Vector3f(targetRotVec.x, targetRotVec.y, targetRotVec.z);

        // update CT translation
        target.setAttribute("ImageTranslation", translate);
    }
    
    @Override
    public void doLayout() {
        trackball.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
        registerBallCT.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
        
        try {
            for (int i=0; i<model.getMrImageCount(); i++) {
                Trackball registerBall;
                try {
                    registerBall = (Trackball)model.getMrImage(i).getAttribute("registerBall"); //TEMP CHANGE
                }
                catch(ClassCastException e) {
                    registerBall = null;
                }
                
                if (registerBall == null) {
                   registerBall = new Trackball(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f); 
                }
                registerBall.set(Display.getWidth() / 2, Display.getHeight() / 2, Display.getHeight() / 2f);
                model.getMrImage(i).setAttribute("registerBall", registerBall, true);
            }
        }
        catch(NullPointerException e) {
        }
        
        this.statusBar.setBounds(Display.getWidth() / 2 - 300, Display.getHeight() / 2 - 15, 600, 30);

        pickLayer.doLayout();
        
        scene.doLayout();
        
    }

    @Override
    public void release() {
        scene.release();
    }
    
    @Override
    public boolean getIsDirty() {
        // empty updateEventQueue
        ////////////////////////////
        updateEventQueue.handleEvents(this);

        return scene.getIsDirty();
    }
    
    @Override
    public Renderable setIsDirty(boolean dirty) {
        scene.setIsDirty(dirty);
        return this;
    }
    
    @Override
    public void update(Observable o, Object arg) {
        
        // If updates are called from a different thread than the
        // main thread, queue them for later processing on the main thread.
        if (myThread != Thread.currentThread()) {
            updateEventQueue.push(o, arg);
            return;
        }
// To display property change notifications for debugging        
//        if (o != null) {
//            System.out.print("----DefaultView update: " + o.toString());
//        }
        
        if (arg != null && arg instanceof PropertyChangeEvent) {
            PropertyChangeEvent event = (PropertyChangeEvent)arg;
//            System.out.print(" Property Change: " + ((PropertyChangeEvent)arg).getPropertyName());
//            System.out.println();
            
            switch(this.getFilteredPropertyName(event)) {
                case "registerMRCT":
                    Button registerButton = (Button)Renderable.lookupByTag("registerMRCT");
                    registerButton.setIndicator((Boolean)event.getNewValue());
                    break;
                case "currentSceneOrienation":
                    Quaternion orient = (Quaternion)event.getNewValue();
                    orientAnimator.set(trackball.getCurrent(), orient, 1.5f);
                    orientAnimator.setTrackball(trackball);
                    return;
                case "currentTargetPoint":
                case "currentTargetSteering":
                case "boneSOS":
                case "boneRefractionSOS":
                    updateTargetAndSteering();
                    updatePressureCalc();
                    this.setIsDirty(true);
                    return;
                case "doFrame":
                case "doMRI":
                case "doClip":
                case "showRayTracer":
                case "showSkullFloorStrikes":
                    setDoTransition(true);
                    this.setIsDirty(true);
                    return;
                case "currentMRSeries":
                    int currentMRseries = ((Integer)model.getAttribute("currentMRSeries")).intValue();
                    model.setSelectedMR(currentMRseries);
                    this.setDisplayMRimage(model.getMrImage(currentMRseries));
                    setDoTransition(true);
                    break;
                case "currentSonication":
                    int sonicationIndex = (Integer)model.getAttribute("currentSonication");
                    if (sonicationIndex>=0 && model.getSonication(sonicationIndex) != null) {
                        model.setAttribute("currentTargetPoint", new Vector3f(model.getSonication(sonicationIndex).getNaturalFocusLocation()));
                        model.setAttribute("currentTargetSteering", new Vector3f(model.getSonication(sonicationIndex).getFocusSteering()));
                        model.setAttribute("sonicationPower", String.format("%4.1f W", model.getSonication(sonicationIndex).getPower()));
                        model.setAttribute("sonicationDuration", String.format("%4.1f s", model.getSonication(sonicationIndex).getDuration()));
                        model.setAttribute("sonicationFrequency", String.format("%4.1f kHz", model.getSonication(sonicationIndex).getFrequency()/1000f));
                        model.setAttribute("sonicationTimestamp", model.getSonication(sonicationIndex).getAttribute("timestamp"));
                        model.setAttribute("targetVisible", model.getSonication(sonicationIndex).getAttribute("targetVisible"));
                        
                        String desc = (String)model.getSonication(sonicationIndex).getAttribute("Description");
                        if (desc == null) {
                            desc = "";
                        }
                        model.setAttribute("sonicationDescription", String.format("%s", desc));
                        
                        Vector3f t = Vector3f.add(model.getSonication(sonicationIndex).getFocusSteering(), model.getSonication(sonicationIndex).getNaturalFocusLocation(), null);
                        model.setAttribute("sonicationRLoc", String.format("%4.1f", -t.x));
                        model.setAttribute("sonicationALoc", String.format("%4.1f", -t.y));
                        model.setAttribute("sonicationSLoc", String.format("%4.1f", t.z));
                        updateThermometryDisplay(sonicationIndex, true);
                        updateTransducerModel(sonicationIndex);
                        setDoTransition(true);
                    }
                    else {
                        model.setAttribute("sonicationDescription", "");
                        model.setAttribute("sonicationRLoc", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationALoc", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationSLoc", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationPower", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationDuration", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationFrequency", String.format("%4.1f", 0f));
                        model.setAttribute("sonicationTimestamp", "");
                        model.setAttribute("targetVisible", false);
                    }
                    break;
                case "transducerXTilt":
                    this.transducerModel.setTransducerXTilt((float)event.getNewValue());
                    this.transducerTilt = (float)event.getNewValue();
                    break;
                case "showTargets":
                    boolean bShow = (Boolean)model.getAttribute("showTargets");
                    Renderable.lookupByTag("targetRenderer").setVisible(bShow);
                    this.setDoTransition(true);
                    break;
            }
            
            switch(event.getPropertyName()) {
                case "Model.CtImage":
                    setDisplayCTimage(model.getCtImage());
                                
                    model.setAttribute("doMRI", true);
                    canvas.setVolumeRender(true);
                    canvas.setIsDirty(true);
                    ctHistogram.calculate();
                    transFuncDisplay.setHistogram(ctHistogram.getData());
                    ctHistogram.release();
                    setDoTransition(true);
                    return;
                case "Model.MrImage[0]":
                    setDisplayMRimage(model.getMrImage(0));
                    return;
                case "rayCalc":
                    if (transRayTracer.getVisible()) {

                        Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                        barColor.x *= 2f;
                        barColor.y *= 2f;
                        barColor.z *= 2f;
                        barColor.w = 1f;

                        incidentAngleChart.newChart();
                        incidentAngleChart.addSeries("Frequency", transRayTracer.getIncidentAngles(), barColor, 35, 0f, 36f);
                        
                        updateSelectedChannelData();
                    }
                    return;
                case "sdrCalc":
                    if (transRayTracer.getVisible()) {

                        Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                        barColor.x *= 2f;
                        barColor.y *= 2f;
                        barColor.z *= 2f;
                        barColor.w = 1f;

                        sdrChart.newChart();
                        sdrChart.addSeries("Frequency", transRayTracer.getSDR2s(), barColor, 22, 0.0f, 1f);
                    }
                    return;
            }
            
            if (event.getPropertyName().startsWith("Model.MrImage[")) {
                if (Pattern.matches("Model\\.MrImage\\[\\d{1,2}\\]", event.getPropertyName())){                   
                    updateMRlist();
                }
            }
            else if (event.getPropertyName().startsWith("Model.Sonication[")) {
                if (Pattern.matches("Model\\.Sonication\\[\\d{1,2}\\]", event.getPropertyName())){                   
                    updateSonicationList();
                }                
            }
            
        }
        else if (arg != null && arg instanceof TransducerRayTracer) {
            if (transRayTracer.getVisible()) {
                
                Vector4f barColor = new Vector4f(0.22f, 0.25f, 0.30f, 1f); // background layer color
                barColor.x *= 2f;
                barColor.y *= 2f;
                barColor.z *= 2f;
                barColor.w = 1f;
                
                incidentAngleChart.newChart();
                incidentAngleChart.addSeries("Frequency", transRayTracer.getIncidentAngles(), barColor, 35, 0f, 36f);
                incidentAngleChart.generateChart();
                
                sdrChart.newChart();
                sdrChart.addSeries("Frequency", transRayTracer.getSDRs(), barColor, 22, 0.0f, 1f);
                sdrChart.generateChart();
            }                    
        }
    }
    
    private void updateTransducerModel(int sonicationIndex) {
        try {
            String txdrGeomFileName = (String) model.getSonication(sonicationIndex).getAttribute("txdrGeomFileName");
            
            if (txdrGeomFileName == null) return; // TODO: do some error handling and notify user that something is missing
            
            Vector3f tdXdir = (Vector3f) model.getSonication(sonicationIndex).getAttribute("txdrTiltXdir");
            Vector3f tdYdir = (Vector3f) model.getSonication(sonicationIndex).getAttribute("txdrTiltYdir");
            Vector3f tdZdir = Vector3f.cross(tdXdir, tdYdir, null);

            this.transducerModel.buildElements(new InsightecTxdrGeomReader(new File(txdrGeomFileName)));
            this.transducerModel.setTransducerTilt(tdXdir, tdYdir);
            this.transRayTracer.init(transducerModel);
            
            float tiltXAngleDeg = Vector3f.angle(new Vector3f(0, tdZdir.y, tdZdir.z), new Vector3f(0, 0, -1)) / ((float) Math.PI * 2f) * 360f;
            model.setAttribute("transducerXTilt", tiltXAngleDeg);
                    
    } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
    }
    
    private void updateThermometryDisplay(int sonicationIndex, boolean setToMax) {
        System.out.println("DefaultView::updateThermometryDisplay");
        Sonication sonication = model.getSonication(sonicationIndex);
        ImageVolume thermometry = null;
        Boolean sonicationWasAborted = null;
        if (sonication != null) {
            thermometry = model.getSonication(sonicationIndex).getThermometryPhase();
            sonicationWasAborted = (Boolean)(model.getSonication(sonicationIndex).getAttribute("aborted"));
        }
        
        if (thermometry != null) {
            float data[] = (float[])thermometry.getData();

            int cols = thermometry.getDimension(0).getSize();
            int rows = thermometry.getDimension(1).getSize();
            int timepoints = thermometry.getDimension(3).getSize();

            float phaseTime = thermometry.getDimension(3).getSampleSpacing();

            double maxVals[] = new double[timepoints];
            double avgVals[] = new double[timepoints];
            double times[] = new double[timepoints];
            double overallMax = 0.0;
            int maxTimePoint = 0;
            for (int i=0; i<timepoints; i++) {
                maxVals[i] = data[thermometry.getVoxelOffset(cols/2, rows/2, i)];

                double maxVal = 0;
                double valueSum = 0;
                int counter = 0;
                for (int x=-1; x<=0; x++) {
                    for (int y=-1; y<=0; y++) {
                        counter++;
                        double val = data[thermometry.getVoxelOffset(cols/2+x, rows/2+y, i)];
                        valueSum += val;
//                        if (x==0 || x==1 || y==0 || y==1) {
//                            counter++;
//                            valueSum += val;        
//                        }
                        if (val > maxVal) {
                            maxVal = val;
                        }      
                    }
                }

                maxVals[i] = maxVal;
                avgVals[i] = valueSum/(float)counter;
                times[i] = i*phaseTime;

                if (maxVal > overallMax) {
                    overallMax = maxVal;
                    maxTimePoint = i;
                }

                System.out.println("Thermometry " + times[i] + " s, " + maxVals[i] + ", " + avgVals[i]);
            }

            model.getSonication(sonicationIndex).setAttribute("maxFrame", maxTimePoint);
            if (setToMax) {
                model.getSonication(sonicationIndex).setAttribute("currentFrame", maxTimePoint);
            }
            
            Renderable abortText = Renderable.lookupByTag("tbSonicationAborted");
            if (sonicationWasAborted != null && sonicationWasAborted == true) {
                abortText.setVisible(true);
            }
            else {
                abortText.setVisible(false);
            }

            thermometryChart.newChart();
            thermometryChart.addSeries("Max", times, maxVals, new Vector4f(0.8f, 0.2f, 0.2f, 1f));
            thermometryChart.addSeries("Avg", times, avgVals, new Vector4f(0.2f, 0.8f, 0.2f, 1f));
            thermometryChart.generateChart();
            
            Vector2f maxT = findMaxTemperature(sonicationIndex);
            System.out.println("Max temperature found = " + maxT.x);
            model.setAttribute("sonicationMaxTemp", String.format("%4.1f C", maxT.x));
            model.setAttribute("sonicationMaxDose", String.format("%4.2f kCEM", maxT.y/1000f));

            showThermometry(sonicationIndex);
        }
        else {
            thermometryChart.newChart();
            thermometryChart.generateChart();
            showThermometry(-1);
        }
    }
    
    // returns max temp in X and max dose in Y
    private Vector2f findMaxTemperature(int sonicationIndex) {
        Vector2f maxTemp = new Vector2f(-1f, 0f);
        Vector2f maxPoint = new Vector2f();
        
        ImageVolume thermometry = model.getSonication(sonicationIndex).getThermometryPhase();
        
        if (thermometry != null) {
            float data[] = (float[])thermometry.getData();

            int cols = thermometry.getDimension(0).getSize();
            int rows = thermometry.getDimension(1).getSize();
            int timepoints = thermometry.getDimension(3).getSize();
            
            float deltaT = thermometry.getDimension(3).getSampleWidth(0);

            // searching in 16x16 neighborhood around the target
            // for the maximum temperature achieved
            int xspan = Math.min(8, cols/2);
            int yspan = Math.min(8, rows/2);
            
            for (int i=0; i<timepoints; i++) {                
                for (int x=-xspan; x<=xspan; x++) {
                    for (int y=-yspan; y<=yspan; y++) {
                        float tempVal = data[thermometry.getVoxelOffset(cols/2+x, rows/2+y, i)];
                        if (tempVal > maxTemp.x) {
                            maxTemp.x = tempVal;
                            maxPoint.set(cols/2+x, rows/2+y);
                        }
                    }
                }
            }
            
            System.out.println("Max temperature index: " + maxPoint);
            System.out.println("Sonication deltaT = " + deltaT);

            /*
                dt = x(2)-x(1);

                for i = 1:length(x)
                    if y(i) <43
                        Dose(i) = .25^(43-y(i))*dt/60;
                    else
                        Dose(i) = .5^(43-y(i))*dt/60;
                    end

                    if i ~= 1
                        Dose(i) = Dose(i) + Dose(i-1);
                    end
                end
            */
            float dose = 0f;
            for (int i=1; i<timepoints; i++) {
                // trapezoidal rule integration
                float tempVal = data[thermometry.getVoxelOffset((int)(maxPoint.x), (int)(maxPoint.y), i)];
                tempVal += data[thermometry.getVoxelOffset((int)(maxPoint.x), (int)(maxPoint.y), i-1)];
                tempVal /= 2f;
                
                if (tempVal < 43f) {
                    dose += Math.pow(0.25f, (43f - tempVal))*(deltaT/60f);
                }
                else {
                    dose += Math.pow(0.5f, (43f - tempVal))*(deltaT/60f);
                }
            }
            maxTemp.y = dose;
        }
        
        return maxTemp;
    }
    
    private ImageVolume4D calcThermalDoseImage(int sonicationIndex) {
        Vector2f maxTemp = new Vector2f(-1f, 0f);
        Vector2f maxPoint = new Vector2f();
        
        ImageVolume4D result = null;
        
        ImageVolume thermometry = model.getSonication(sonicationIndex).getThermometryPhase();
        
        if (thermometry != null) {
            float data[] = (float[])thermometry.getData();

            int cols = thermometry.getDimension(0).getSize();
            int rows = thermometry.getDimension(1).getSize();
            int timepoints = thermometry.getDimension(3).getSize();
            
            // Create a one slice volume of same size and resolution to hold dose
            result = new ImageVolume4D(ImageVolume.FLOAT_VOXEL, cols, rows, 1, 1);
            for (int d=0; d<4; d++) {
                result.getDimension(d).setSampleWidth(thermometry.getDimension(d).getSampleWidth());
            }
            
            result.setAttribute("ImageOrientationQ", (Quaternion)thermometry.getAttribute("ImageOrientationQ"));
            result.setAttribute("ImageTranslation", (Vector3f)thermometry.getAttribute("ImageTranslation"));
            result.setAttribute("ImagePosition", (float[])thermometry.getAttribute("ImagePosition"));
            
            float deltaT = thermometry.getDimension(3).getSampleWidth();


            float[] resultData = (float[])(result.getData());
            for (int y=0; y<rows; y++) {
                for (int x=0; x<cols; x++) {
                    float dose = 0f;
                    for (int i=1; i<timepoints; i++) {
                        // trapezoidal rule integration
                        float tempVal = data[thermometry.getVoxelOffset(x, y, i)];
                        tempVal += data[thermometry.getVoxelOffset(x, y, i-1)];
                        tempVal /= 2f;

                        if (tempVal < 43f) {
                            dose += Math.pow(0.25f, (43f - tempVal))*(deltaT/60f);
                        }
                        else {
                            dose += Math.pow(0.5f, (43f - tempVal))*(deltaT/60f);
                        }
                        
                    }                    
                    resultData[result.getVoxelOffset(x, y, 0)] = dose;
                }
            }
        }
        
        return result;
    }
    
    private void updateMRlist() {
        System.out.println("updateMRList");
        
        this.mrSeriesSelector.clear();
        mrSeriesSelector.setTitle("MR Series");
        for (int i=0; i<model.getMrImageCount(); i++) {
            try {
                mrSeriesSelector.addItem(i, model.getMrImage(i).getAttribute("ProtocolName").toString());
            }
            catch(Exception e) {
                mrSeriesSelector.addItem(i, "Unspecified MR protocol");                
            }
        }
//        if (mrSeriesSelector.getSelectionIndex() != 0) {
            mrSeriesSelector.setSelectionIndex(0);
//        }
    }
    
    private void updateSonicationList() {
        System.out.println("updateSonicationList");
        
        this.sonicationSelector.clear();
        if (model.getSonicationCount() <= 0) {
            sonicationSelector.setTitle("Sonications");
        }
        else {
            sonicationSelector.setTitle("Sonication 1");
        }
        
        for (int i=0; i<model.getSonicationCount(); i++) {
            String desc = (String)model.getSonication(i).getAttribute("Description");
            if (desc == null) desc = "";
            
            try {
                sonicationSelector.addItem(i, "Sonication " + (i + 1) + " (" + Math.round(model.getSonication(i).getPower() * 10f) / 10f + "W) " + desc);
            }
            catch(Exception e) {
            }
        }
        sonicationSelector.setSelectionIndex(0);
    }

    @Override
    public void processInput() {
        
        while (Mouse.next()) {
                OnMouse(Mouse.getEventX(), Mouse.getEventY(), Mouse.isButtonDown(0), Mouse.isButtonDown(1), Mouse.getEventDWheel());
        }
        
        if (!hasKeyboardFocus()) {
            while (Keyboard.next()) {
                ProcessKeyboard(Keyboard.getEventKey(), Keyboard.getEventCharacter(), Keyboard.getEventKeyState());
            }
            return;
        }
        
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x-0.1f, steering.y, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
           // model.setAttribute("currentTargetSteering", steering.translate(-0.1f, 0f, 0f));
            
           // updateTargetAndSteering();
           // updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_center -= 2;
//                model.setAttribute("MRcenter", (float)mr_center);
//                System.out.println("MR Center = " + mr_center);
//            } else {
//                center -= 10;
//                model.setAttribute("CTcenter", (float)center);
////                System.out.println("Center = " + center);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x+0.1f, steering.y, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0.1f, 0f, 0f));
            
//            System.out.println("************** Steering: " + steering);
            
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_center += 2;
//                model.setAttribute("MRcenter", (float)mr_center);
//                System.out.println("MR Center = " + mr_center);
//            } else {
//                center += 10;
//                model.setAttribute("CTcenter", (float)center);
////                System.out.println("Center = " + center);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x, steering.y+0.1f, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0f, 0.1f, 0f));
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_window += 10;
//                model.setAttribute("MRwindow", (float)mr_window);
////                System.out.println("MR Window = " + mr_window);
//            } else {
//                window += 10;
//                model.setAttribute("CTwindow", (float)window);
//                System.out.println("Window = " + window);
//            }
// //           needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
//            Vector3f steering = transRayTracer.getTargetSteering();
//            transRayTracer.setTargetSteering(steering.x, steering.y-0.1f, steering.z);
            Vector3f steering = (Vector3f)model.getAttribute("currentTargetSteering");
            if (steering == null) {
                steering = new Vector3f();
            }
            //model.setAttribute("currentTargetSteering", steering.translate(0f, -0.1f, 0f));
            //updateTargetAndSteering();
            //updatePressureCalc();
//            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
//                mr_window -= 10;
//                model.setAttribute("MRwindow", (float)mr_window);
//                System.out.println("MR Window = " + mr_window);
//            } else {
//                window -= 10;
//                model.setAttribute("CTwindow", (float)window);
////                System.out.println("Window = " + window);
//            }
////            needsRendering = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_7)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                mr_threshold += 5;
                model.setAttribute("MRthresh", (float)mr_threshold);
            } else {
                ct_threshold += 5;
                model.setAttribute("CTthresh", (float)mr_threshold);
            }
//            needsRendering = true;
            System.out.println(" CT Thresh = " + ct_threshold);
            System.out.println(" MR Thresh = " + mr_threshold);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_8)) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                mr_threshold -= 5;
                model.setAttribute("MRthresh", (float)mr_threshold);
            } else {
                ct_threshold -= 5;
                model.setAttribute("CTthresh", (float)mr_threshold);
            }
//            needsRendering = true;
            System.out.println("Thresh = " + ct_threshold);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_9)) {
//            needsRendering = true;
            canvas.setForegroundVolumeSlices(Math.min(600, canvas.getForegroundVolumeSlices() + 10));
            model.setAttribute("foregroundVolumeSlices", (float)canvas.getForegroundVolumeSlices());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
//            needsRendering = true;
            canvas.setForegroundVolumeSlices(Math.max(0, canvas.getForegroundVolumeSlices() - 10));
            model.setAttribute("foregroundVolumeSlices", (float)canvas.getForegroundVolumeSlices());
            if (canvas.getForegroundVolumeSlices() <= 50) {
                transRayTracer.setClipRays(true);
            } else {
                transRayTracer.setClipRays(false);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_3)) {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                transRayTracer.setBoneSpeed((float)Math.round(transRayTracer.getBoneSpeed()/5f)*5f - 5f);
                model.setAttribute("boneSOS", transRayTracer.getBoneSpeed());
    //            needsRendering = true;
                System.out.println("Bone speed: " + transRayTracer.getBoneSpeed());
            }
            else {
                transRayTracer.setBoneRefractionSpeed((float)Math.round(transRayTracer.getBoneRefractionSpeed()/5f)*5f - 5f);
                model.setAttribute("boneRefractionSOS", transRayTracer.getBoneRefractionSpeed());
    //            needsRendering = true;
                System.out.println("Bone refraction speed: " + transRayTracer.getBoneRefractionSpeed());
                
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_4)) {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                transRayTracer.setBoneSpeed((float)Math.round(transRayTracer.getBoneSpeed()/5f)*5f + 5f);
                model.setAttribute("boneSOS", transRayTracer.getBoneSpeed());
    //            needsRendering = true;
                System.out.println("Bone speed: " + transRayTracer.getBoneSpeed());
            }
            else {
                transRayTracer.setBoneRefractionSpeed((float)Math.round(transRayTracer.getBoneRefractionSpeed()/5f)*5f + 5f);
                model.setAttribute("boneRefractionSOS", transRayTracer.getBoneRefractionSpeed());
    //            needsRendering = true;
                System.out.println("Bone refraction speed: " + transRayTracer.getBoneRefractionSpeed());
                
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_T)) {
//            needsRendering = true;
            transducerTilt += 0.5f;
            transducerModel.setTransducerXTilt(transducerTilt);
            model.setAttribute("transducerXTilt", transducerTilt);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_G)) {
//            needsRendering = true;
            transducerTilt -= 0.5f;
            transducerModel.setTransducerXTilt(transducerTilt);
            model.setAttribute("transducerXTilt", transducerTilt);
        }

        while (Keyboard.next()) {
            
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_D) {
                    transducerModel.setShowSteeringVolume(!this.transducerModel.getShowSteeringVolume());
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_F) {
                    transducerModel.setShowFocalVolume(!this.transducerModel.getShowFocalVolume());
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_S) {
                    //System.out.println("1 Key Pressed");
                    saveScene();
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_A) {
                    //System.out.println("1 Key Pressed");
                    loadScene();
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_Z) {
                    //System.out.println("1 Key Pressed");
                    zoomAnimator.set(dolly, dolly.getValue(), -35f, 4f);
//                    needsRendering = true;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_1) {
                    //System.out.println("1 Key Pressed");
//                    needsRendering = true;
                    selTrans = ++selTrans % Transducer.getTransducerDefCount();

                    transRayTracer.release();
                    setDoTransition(true);
//                    if (selTrans == 0) {
//                        transRayTracer.init(transducer220);
//                    } else {
//                        transRayTracer.init(transducer650);
//                    }
                    
//                    this.transducerModel.setTransducerTilt(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0));
                    transRayTracer.init(transducerModel.buildElements(Transducer.getTransducerDef(selTrans)));
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_H) {
                    //System.out.println("1 Key Pressed");
//                    needsRendering = true;
                    canvas.setShowMR(!canvas.getShowMR());
                    setDoTransition(true);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_2) {
                    //System.out.println("2 Key Pressed");
//                    needsRendering = true;
                    drawingStyle = 1 - drawingStyle;
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_N) {
                    //System.out.println("2 Key Pressed");
//                    needsRendering = true;
                    canvas.setVolumeRender(!canvas.getVolumeRender());
                    ctHistogram.calculate();
                    transFuncDisplay.setHistogram(ctHistogram.getData());
                    ctHistogram.release();
                    setDoTransition(true);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_5) {
                    //System.out.println("5 Key Pressed");
//                    needsRendering = true;
                    transducerModel.setShowRays(!transducerModel.getShowRays());
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_6) {
                    //System.out.println("5 Key Pressed");
//                    needsRendering = true;
                    
                    try {
                        showRayTracer = (Boolean)model.getAttribute("showRayTracer");
                    }
                    catch(NullPointerException e) {
                        showRayTracer = false;
                    }
                    showRayTracer = !showRayTracer;
                    model.setAttribute("showRayTracer", showRayTracer);
//                    if (!showRayTracer) {
//                        activeElementsBar.setValue(-1f);
//                        sdrBar.setValue(-1);
//                    }
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_C) {
                    //System.out.println("C Key Pressed");
//                    needsRendering = true;
                    try {
                        doClip = (Boolean)model.getAttribute("doClip");
                    }
                    catch(NullPointerException e) {
                        doClip = false;
                    }
                    doClip = !doClip;
                    model.setAttribute("doClip", doClip);
                    
                    
                    transducerModel.setClipRays(doClip);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_X) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    try {
                        doFrame = (Boolean)model.getAttribute("doFrame");
                    }
                    catch(NullPointerException e) {
                        doFrame = false;
                    }
                    doFrame = !doFrame;
                    model.setAttribute("doFrame", doFrame);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_V) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    showScanner = !showScanner;
                    scene.setIsDirty(true);
                    setDoTransition(true);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_M) {
                    //System.out.println("S Key Pressed");
//                    needsRendering = true;
                    try {
                        doMRI = (Boolean)model.getAttribute("doMRI");
                    }
                    catch(NullPointerException e) {
                        doMRI = false;
                    }
                    doMRI = !doMRI;
                    model.setAttribute("doMRI", doMRI);
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_Q) {
                    initController();
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_P) {
                    calcTreatmentEnvelope();
                }
            }
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_L) {
                    boolean bDoPressure = canvas.getShowPressure();
                    if (bDoPressure == false) {
                        transRayTracer.setShowEnvelope(false); // TODO: this is probably overtaken by events now. remove?
                        canvas.setShowPressure(true);
                        canvas.setShowThermometry(false);
                        updateTargetAndSteering();
                        updatePressureCalc();
                        setDoTransition(true);
                    }
                    else {
                        transRayTracer.setShowEnvelope(false);
                        canvas.setShowPressure(false);
                        canvas.setShowThermometry(false);
                        setDoTransition(true);
                    }
                }
            }
//            if (Keyboard.getEventKeyState()) {
//                if (Keyboard.getEventKey() == Keyboard.KEY_K) {
//                    controller.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "loadExport"));
//                }
//
//            }
//            if (Keyboard.getEventKeyState()) {
//                if (Keyboard.getEventKey() == Keyboard.KEY_I) {
//                    JFileChooser fileChooser = new JFileChooser();
//                    fileChooser.setDialogTitle(new String("Choose CT file..."));
//                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//                    File ctfile = null;
//                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                        ctfile = fileChooser.getSelectedFile();
//                        System.out.println("Selected file: " + ctfile.getAbsolutePath());
//
//                        ctloader = new Loader();
//                        ctloader.load(ctfile, "CT_IMAGE_LOADED", controller);
//                    }
//                }
//            }
//            if (Keyboard.getEventKeyState()) {
//                if (Keyboard.getEventKey() == Keyboard.KEY_O) {
//                    JFileChooser fileChooser = new JFileChooser();
//                    fileChooser.setDialogTitle(new String("Choose MR file..."));
//                    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//                    File mrfile = null;
//                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
//                        mrfile = fileChooser.getSelectedFile();
//                        System.out.println("Selected file: " + mrfile.getAbsolutePath());
//
//                        mrloader = new Loader();
//                        mrloader.load(mrfile, "MR_IMAGE_0_LOADED", controller);
//                    }
//                }
//            }
        }            
        
        processController();
    }
    
    public void pressureCalCalledByButton(){
        transRayTracer.setShowEnvelope(false); // TODO: this is probably overtaken by events now. remove?
        canvas.setShowPressure(true);
        // updatePressureCalc();
        
        if (canvas.getShowPressure()) {
            Vector3f CofR = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());
            canvas.setTextureRotatation(CofR.translate(currentSteering.getXpos(), currentSteering.getYpos(), currentSteering.getZpos()), trackball);
            this.transRayTracer.setTextureRotatation(CofR, trackball);
//TODO: put back later
//            this.transRayTracer.attenuation_term_on = attenuation_term_on;
//            this.transRayTracer.transmissionLoss_term_on = transmissionLoss_term_on;
//            
//            this.transRayTracer.calcPressureEnvelope3D();
            //  this.transRayTracer.calcPressureEnvelope();
            canvas.setOverlayImage(transRayTracer.getEnvelopeImage());

            canvas1.setOverlayImage(transRayTracer.getEnvelopeImage());
            canvas2.setOverlayImage(transRayTracer.getEnvelopeImage());
            canvas3.setOverlayImage(transRayTracer.getEnvelopeImage());
        }
        
        System.out.println("pressure button pressed");
    }
 
    private float processAxisInput(net.java.games.input.Controller c, net.java.games.input.Component.Identifier id) {
        float data = c.getComponent(id).getPollData();
        if (Math.abs(data) < 0.1d) return 0f;
        return data;
    }
    
    private float processTriggerInput(net.java.games.input.Controller c, net.java.games.input.Component.Identifier id) {
        float data;
        try {
            data = (c.getComponent(id).getPollData() + 1f)/2f;
        }
        catch(NullPointerException e) {
//            e.printStackTrace();
            data = 0;
        }
        if (data < 0.1d || Math.abs(data-0.5d) < 1E-4) return 0f;
        return data;
    }
    
    public void saveSkullParams() {
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle(new String("Choose file for skull params..."));
//        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//        fileChooser.setSelectedFile(new File("skullParams.txt"));
//        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File outFile = null;
            fileDialog.setDialogTitle("Save skull measures file");
            fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
            outFile = fileDialog.open();
            if (outFile == null) {
                return;
            }
//        if (fileChooser.showSaveDialog(Display.getParent()) == JFileChooser.APPROVE_OPTION) {
//            outFile = fileChooser.getSelectedFile();
            
            model.setAttribute("showRayTracer", true); // turn raytracer on
            Main.update(); // TODO: kind of a hack to make sure the raytracer is active and initialized, forces one rendered frame
            
            this.transRayTracer.writeSkullMeasuresFile(outFile);


            try {
                java.awt.Desktop desktop = Desktop.getDesktop();
                if (desktop != null) {                
                    try {
                        desktop.open(new File(outFile.getAbsolutePath()));
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
//        }
    }
    
    public void saveACTFileForCPC() {
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle(new String("Save CPC ACT..."));
//        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileChooser.setSelectedFile(new File("ACT.ini"));
//        File outFile = null;
        File outFile = null;
            fileDialog.setDialogTitle("Save CPC compatible ACT file");
            fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
            outFile = fileDialog.open();
            if (outFile == null) {
                return;
            }
//        if (fileChooser.showSaveDialog(Display.getParent()) == JFileChooser.APPROVE_OPTION) {
//            outFile = fileChooser.getSelectedFile();

            model.setAttribute("showRayTracer", true); // turn raytracer on
            Main.update(); // TODO: kind of a hack to make sure the raytracer is active and initialized, forces one rendered frame

            this.transRayTracer.writeACTFile(outFile);


            try {
                java.awt.Desktop desktop = Desktop.getDesktop();
                if (desktop != null) {                
                    try {
                        desktop.open(new File(outFile.getAbsolutePath()));
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
 //       }
    }
    public void saveACTFileForWorkstation() {
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setDialogTitle(new String("Save Workstation ACT..."));
//        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileChooser.setSelectedFile(new File("ACT.ini"));
        File outFile = null;
            fileDialog.setDialogTitle("Save Workstation compatible ACT file");
            fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
            outFile = fileDialog.open();
            if (outFile == null) {
                return;
            }
//        if (fileChooser.showSaveDialog(Display.getParent()) == JFileChooser.APPROVE_OPTION) {
//            outFile = fileChooser.getSelectedFile();

            model.setAttribute("showRayTracer", true); // turn raytracer on
            Main.update(); // TODO: kind of a hack to make sure the raytracer is active and initialized, forces one rendered frame

            this.transRayTracer.writeACTFileForWorkstation(outFile);


            try {
                java.awt.Desktop desktop = Desktop.getDesktop();
                if (desktop != null) {                
                    try {
                        desktop.open(new File(outFile.getAbsolutePath()));
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
//        }
    }
    
    private void processController() {
        if (gameController != null) {
            try {
                if (!gameController.poll()) {
                    gameController = null;
                    return;
                }
            }
            catch(Exception e) {
                gameController = null;
                return;
            }
            
            float xrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.Y);
            float yrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.X);
            float zrot = processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.RX);
            float zoom =  processAxisInput(gameController, net.java.games.input.Component.Identifier.Axis.RY);
//            float zoom =  processTriggerInput(gameController, net.java.games.input.Component.Identifier.Axis.Z);
            if (zoom == 0f) {
                zoom =  -processTriggerInput(gameController, net.java.games.input.Component.Identifier.Axis.RZ);   
            }
                        
            if (xrot != 0f || yrot != 0f || zrot != 0f || zoom != 0f) {
                //System.out.println(xrot);
                
                Quaternion q = this.trackball.getCurrent();
                Matrix4f mat = Trackball.toMatrix4f(q);
                Matrix4f.transpose(mat, mat);
                
                //mat = Matrix4f.setIdentity(mat);
                
                mat = Matrix4f.rotate(-xrot/20f, new Vector3f(1f, 0f, 0f), mat, null);
                mat = Matrix4f.rotate(yrot/20f, new Vector3f(0f, 1f, 0f), mat, null);
                mat = Matrix4f.rotate(-zrot/20f, new Vector3f(0f, 0f, 1f), mat, null);
                Quaternion.setFromMatrix(mat, q);
                q = q.normalise(null);
                
                trackball.setCurrent(q);
                
                dolly.incrementValue(zoom*10f);
                
                mainLayer.setIsDirty(true);

//                needsRendering = true;
            }
            
            float pov = gameController.getComponent(net.java.games.input.Component.Identifier.Axis.POV).getPollData();
            if (pov == net.java.games.input.Component.POV.DOWN) {
                canvas.setForegroundVolumeSlices(Math.min(600, canvas.getForegroundVolumeSlices()+10));
                if (canvas.getForegroundVolumeSlices()<=50) {
                    transRayTracer.setClipRays(true);
                }
                else {
                   transRayTracer.setClipRays(false);
                }
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.UP) {
                canvas.setForegroundVolumeSlices(Math.max(0, canvas.getForegroundVolumeSlices()-10));
                if (canvas.getForegroundVolumeSlices()<=50) {
                    transRayTracer.setClipRays(true);
                }
                else {
                   transRayTracer.setClipRays(false);
                }
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.LEFT) {
                int ot = transFuncDisplay.getOpacityThreshold() - 1;
                transFuncDisplay.setOpacityThreshold(ot);
                transFuncDisplay.setMaterialThreshold(ot-20);
//                needsRendering = true;
            }
            else if (pov == net.java.games.input.Component.POV.RIGHT) {
                int ot = transFuncDisplay.getOpacityThreshold() + 1;
                transFuncDisplay.setOpacityThreshold(ot);
                transFuncDisplay.setMaterialThreshold(ot-20);
//                needsRendering = true;                
//                needsRendering = true;
            }
            
            net.java.games.input.EventQueue queue = gameController.getEventQueue();
            net.java.games.input.Event event = new net.java.games.input.Event();
            while (queue.getNextEvent(event)) {
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._0) {
                    if (event.getValue() == 1f) {
                        try {
                            doClip = (Boolean)model.getAttribute("doClip");
                        }
                        catch(NullPointerException e) {
                            doClip = false;
                        }
                        doClip = !doClip;
                        model.setAttribute("doClip", doClip);


                        transducerModel.setClipRays(doClip);
                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._1) {
                    if (event.getValue() == 1f) {
                        try {
                            showRayTracer = (Boolean)model.getAttribute("showRayTracer");
                        }
                        catch(NullPointerException e) {
                            showRayTracer = false;
                        }
                        showRayTracer = !showRayTracer;
                        model.setAttribute("showRayTracer", showRayTracer);
                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._2) {
                    if (event.getValue() == 1f) {
                        try {
                            doFrame = (Boolean)model.getAttribute("doFrame");
                        }
                        catch(NullPointerException e) {
                            doFrame = false;
                        }
                        doFrame = !doFrame;
                        model.setAttribute("doFrame", doFrame);

                    }
                }
                if (event.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Button._3) {
                    if (event.getValue() == 1f) {
//                        needsRendering = true;
                        canvas.setShowMR(!canvas.getShowMR());
                    }
                }
            }
        }
    }
    
    private static net.java.games.input.ControllerEnvironment createDefaultEnvironment() throws ReflectiveOperationException {

        // Find constructor (class is package private, so we can't access it directly)
        Constructor<net.java.games.input.ControllerEnvironment> constructor = (Constructor<net.java.games.input.ControllerEnvironment>)
            (Class.forName("net.java.games.input.DefaultControllerEnvironment").getDeclaredConstructors()[0]);

        // Constructor is package private, so we have to deactivate access control checks
        constructor.setAccessible(true);

        // Create object with default constructor
        return constructor.newInstance();
    }
    
    // Init game controller UI if present
    private static void initController() {
        net.java.games.input.Controller[] controls;
        try {
            
                try {
                    controllerEnvironement = createDefaultEnvironment();
                    controls = controllerEnvironement.getControllers();
                }
                catch(Exception e) {
                    e.printStackTrace();
                    return;
                }
//            }

            
            String preferredControllerName = null;
            for (int i=0; i<controls.length; i++) {
                System.out.println(controls[i].getName());
                if (preferredControllerName == null && controls[i].getName().startsWith("NVIDIA Shield")) {
                    preferredControllerName = new String(controls[i].getName());
                }
                else if (controls[i].getName().startsWith("Controller")) {
                    preferredControllerName = new String(controls[i].getName());
                }
            }
            
            for (int i=0; i<controls.length; i++) {
                System.out.println(controls[i].getName());
                if (preferredControllerName != null && controls[i].getName().equalsIgnoreCase(preferredControllerName)) {
                    gameController = controls[i];
                    break;
                }
                gameController = null;
            }
            
            if (gameController != null) {
                net.java.games.input.Component comps[] = gameController.getComponents();
                for (int i=0; i<comps.length; i++) {
                    System.out.println(comps[i].getName() + " - " + comps[i].getIdentifier());
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            gameController = null;
        }
    }        

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "openKranionFile":
                this.loadScene();
                break;
            case "saveKranionFile":
                this.saveScene();
                break;
            case "saveSkullParams":
                saveSkullParams();
                break;
            case "saveACTfile":
                saveACTFileForCPC();
                break;
            case "saveACTfileWS":
                saveACTFileForWorkstation();
                break;
            case "currentOverlayFrame":
                Integer sonicationIndex = (Integer) model.getAttribute("currentSonication");
                if (sonicationIndex == null) {
                    sonicationIndex = 0;
                }

                int nFrames = 1;
                try {
                    nFrames = model.getSonication(sonicationIndex).getThermometryPhase().getDimension(3).getSize();
                    model.getSonication(sonicationIndex).setAttribute("currentFrame", (int) (this.thermometryChart.getSelectedXValue() * (nFrames - 1)));
                    updateThermometryDisplay(sonicationIndex, false);
                } catch (Exception ex) {
                }
                break;
            case "rotateScene":
                this.currentMouseMode = mouseMode.SCENE_ROTATE;
                break;
            case "rotateHead":
                this.currentMouseMode = mouseMode.HEAD_ROTATE;
                break;
            case "rotateSkull":
                this.currentMouseMode = mouseMode.SKULL_ROTATE;
                break;
            case "rotateFrame":
                this.currentMouseMode = mouseMode.FRAME_ROTATE;
                break;
            case "translateHead":
                this.currentMouseMode = mouseMode.HEAD_TRANSLATE;
                break;
            case "translateSkull":
                this.currentMouseMode = mouseMode.SKULL_TRANSLATE;
                break;
            case "translateFrame":
                this.currentMouseMode = mouseMode.FRAME_TRANSLATE;
                break;
            case "rotateMRI":
                this.currentMouseMode = mouseMode.MRI_ROTATE;
                break;
            case "translateMRI":
                this.currentMouseMode = mouseMode.MRI_TRANSLATE;
                break;
            case "exit":
                //int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", "Exit Kranion", JOptionPane.YES_NO_OPTION);
                if (okToExit()) {
                    this.release();
                    System.exit(0);
                }
                break;
            case "close":
//                int result = JOptionPane.showConfirmDialog(null, "Close the current plan a lose changes?", "Close Plan", JOptionPane.YES_NO_OPTION);
                messageDialog.setDialogTitle("Close the current plan a lose changes?");
                messageDialog.setMessageText("Close Plan");
//                if (result == JOptionPane.YES_OPTION) {
                if (messageDialog.open()) {
                    if (this.model != null) {
                        this.model.clear();
                        this.setDisplayCTimage(null);
                        this.setDisplayMRimage(null);
                        model.setSelectedSonication(-1); // TODO: SelectedSonication and currentSonication need to be unified
                        model.setAttribute("currentTargetPoint", new Vector3f());
                        model.setAttribute("currentTargetSteering", new Vector3f());
                        model.setAttribute("currentSonication", -1);
                        updateSonicationList();
                    }
                }
                break;
            case "CTFilterGpuAcceleration": // This is a preference, update preference key
                boolean checked = true;
                try {
                    checked = ((Button) e.getSource()).getIndicator();
                } catch (Exception ex) {
                }
                prefs.putBoolean("CTFilterGpuAcceleration", checked);
                break;
            case "prefShowLogo": // This is a preference, update preference key
                checked = true;
                try {
                    checked = ((Button) e.getSource()).getIndicator();
                } catch (Exception ex) {
                }
                prefs.putBoolean("prefShowLogo", checked);
                Renderable.lookupByTag("logo").setVisible(checked);
                setDoTransition(true);
                break;
            case "prefShowDemographics": // This is a preference, update preference key
                checked = true;
                try {
                    checked = ((Button) e.getSource()).getIndicator();
                } catch (Exception ex) {
                }
                prefs.putBoolean("prefShowDemographics", checked);
                Renderable.lookupByTag("demographics").setVisible(checked);
                setDoTransition(true);
                break;
            case "filterCT":
                ImageVolume4D image = (ImageVolume4D) model.getCtImage();
                if (image != null) {
                    RegionGrow rg = new RegionGrow(image);

                    rg.setUseGPUAcceleration(prefs.getBoolean("CTFilterGpuAcceleration", true));

                    rg.grow(image.getDimension(0).getSize() / 2, image.getDimension(1).getSize() / 2, image.getDimension(2).getSize() / 2);
                    this.setDisplayCTimage(image);
                    this.mainLayer.setIsDirty(true);
                    this.setDoTransition(true);
                }
                break;
            case "registerMRCT":
                Button registerButton = (Button) Renderable.lookupByTag("registerMRCT");
                if (registerButton.getIndicator()) {
                    Main.startBackgroundWorker("MRCTRegister");
                } else {
                    Main.stopBackgroundWorker("MRCTRegister");
                }
                break;
            case "addSonication":

                Sonication newSonication = new Sonication();
                newSonication.setNaturalFocusLocation(new Vector3f(this.currentTarget.getLocation()));
                newSonication.setFocusSteering(new Vector3f(this.currentSteering.getLocation()));
                newSonication.setAttribute("Description", ((TextBox) Renderable.lookupByTag("tbSonicationDescription")).getText());
                newSonication.setPower(parseTextBoxFloat("tbSonicationAcousticPower"));
                newSonication.setDuration(parseTextBoxFloat("tbSonicationDuration"));
                newSonication.setFrequency(parseTextBoxFloat("tbSonicationFrequency"));

                model.addSonication(newSonication);

                this.updateSonicationList();
                sonicationSelector.setSelectionIndex(model.getSonicationCount() - 1);
                model.setAttribute("currentSonication", model.getSonicationCount() - 1);

                this.mainLayer.setIsDirty(true);

                break;
            case "updateSonication":
                int sIndex = this.sonicationSelector.getSelectionIndex();
                Sonication selSonication = model.getSonication(sIndex);
                if (selSonication != null) {
                    selSonication.setNaturalFocusLocation(new Vector3f(this.currentTarget.getLocation()));
                    selSonication.setFocusSteering(new Vector3f(this.currentSteering.getLocation()));
                    selSonication.setDuration(parseTextBoxFloat("tbSonicationDuration"));
                    selSonication.setPower(parseTextBoxFloat("tbSonicationAcousticPower"));
                    selSonication.setFrequency(parseTextBoxFloat("tbSonicationFrequency"));

                    selSonication.setAttribute("Description", ((TextBox) Renderable.lookupByTag("tbSonicationDescription")).getText());

                    this.updateSonicationList();
                    sonicationSelector.setSelectionIndex(sIndex);

                    this.mainLayer.setIsDirty(true);
                }
                break;
            case "targetVisible":
                sIndex = this.sonicationSelector.getSelectionIndex();
                selSonication = model.getSonication(sIndex);
                if (selSonication != null) {
                    selSonication.setAttribute("targetVisible", ((Button) (e.getSource())).getIndicator());
                    this.mainLayer.setIsDirty(true);
                }
                break;
            case "transducerPattern":
                selTrans = this.transducerPatternSelector.getSelectionIndex();
                transRayTracer.release();
                setDoTransition(true);
                transRayTracer.init(transducerModel.buildElements(Transducer.getTransducerDef(selTrans)));
                break;
            case "calcEnvelope":
                calcTreatmentEnvelope();
                break;
            case "showThermometry":
            case "showDose":
                //TODO: need to decide if model binding should always be done before event handling
                // we need this here to make sure the model is updated before the following code runs
                if (e.getSource() instanceof GUIControlModelBinding) {
                    ((GUIControlModelBinding) e.getSource()).doBinding(model);
                }
                sonicationIndex = (Integer) model.getAttribute("currentSonication");
                if (sonicationIndex == null) {
                    sonicationIndex = 0;
                }
                updateThermometryDisplay(sonicationIndex, false);
                doTransition = true;

                break;
            case "loadCT":
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(new String("Choose CT file..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                File ctfile = null;
                fileDialog.setDialogTitle("Choose a CT DICOM file from desired series");
                fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
                ctfile = fileDialog.open();
                if (ctfile != null) {
                    System.out.println("Selected file: " + ctfile.getAbsolutePath());

                    Loader ctloader = new Loader();
                    ctloader.load(ctfile, "CT_IMAGE_LOADED", getController());
                }
                break;
            case "loadMR":
//            JFileChooser fileChooser = new JFileChooser();
//            fileChooser.setDialogTitle(new String("Choose MR file..."));
//            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
//            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                File mrfile = null;
                fileDialog.setDialogTitle("Choose a MR DICOM file from desired series");
                fileDialog.setFileChooseMode(FileDialog.fileChooseMode.FILES);
                mrfile = fileDialog.open();
                if (mrfile != null) {
                    System.out.println("Selected file: " + mrfile.getAbsolutePath());

                    Loader mrloader = new Loader();
                    mrloader.load(mrfile, "MR_IMAGE_0_LOADED", getController());
                }
                break;
        }
    }
    
    private void calcTreatmentEnvelope() {
        transRayTracer.setShowEnvelope(false);

        Vector3f CofR = new Vector3f(currentTarget.getXpos(), currentTarget.getYpos(), currentTarget.getZpos());

        canvas.setShowPressure(false);
        canvas.setTextureRotatation(CofR.translate(currentSteering.getXpos(), currentSteering.getYpos(), currentSteering.getZpos()), trackball);

        transRayTracer.setTextureRotatation(CofR, trackball);
        transRayTracer.calcEnvelope(this.controller);

        canvas.setOverlayImage(transRayTracer.getEnvelopeImage());
        canvas1.setOverlayImage(transRayTracer.getEnvelopeImage());
        canvas2.setOverlayImage(transRayTracer.getEnvelopeImage());
        canvas3.setOverlayImage(transRayTracer.getEnvelopeImage());

        setDoTransition(true);
    }
    
    private float parseTextBoxFloat(String textboxTagName) {
        TextBox tb = (TextBox)Renderable.lookupByTag(textboxTagName);
        String text = tb.getText().trim();
        int trailWhiteSpace = text.indexOf(" ");
        if (trailWhiteSpace != -1) {
            text = text.substring(0, trailWhiteSpace).trim();
        }
        float retVal = 0f;
        try {
            retVal = Float.parseFloat(text);
        }
        catch(NumberFormatException e) {
            retVal = 0f;
        }
        
        return retVal;       
    }
}
