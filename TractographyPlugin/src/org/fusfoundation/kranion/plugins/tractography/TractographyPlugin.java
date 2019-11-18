package org.fusfoundation.kranion.plugins.tractography;

import com.sun.scenario.effect.impl.BufferUtil;
import java.util.Observer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.FibImageLoader;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.GUIControlModelBinding;
import org.fusfoundation.kranion.RenderLayer;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.Slider;
import org.fusfoundation.kranion.controller.Controller;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.model.image.ImageVolume;
import org.fusfoundation.kranion.model.image.ImageVolume4D;
import org.fusfoundation.kranion.model.image.io.niftii.LEDataInputStream;
import org.fusfoundation.kranion.model.image.io.niftii.NiftiHeader;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Vector3f;

import org.fusfoundation.kranion.plugin.Plugin;
import org.fusfoundation.kranion.view.View;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jsnell
 */
public class TractographyPlugin  implements Plugin, Observer, ActionListener  {
    private Model model;
    private View view;
    private Controller controller;
    
    private boolean guiInitialized = false;
    private FibImageLoader fibloader = new FibImageLoader();
    
    private final String propertyPrefix = "Model.Attribute";
    
    private Vector3f currentTargetPoint = new Vector3f();
    
    @Override
    public void init(Controller controller) {
        System.out.println("******* Hello from TractographyPlugin !! ****************");
                
        this.controller = controller;
        model = controller.getModel();
        view = controller.getView();
                
        // listen to app level controller actions
        controller.addActionListener(this);
        
        // listen to model updates
        model.addObserver(this);
        
        setupGUI();
    }
    
    private void setupGUI() {
        if (!guiInitialized) {
            FlyoutPanel flyout = null;

            Renderable mainPanel = Renderable.lookupByTag("MainFlyout");
            if (mainPanel != null && mainPanel instanceof FlyoutPanel) {
                flyout = (FlyoutPanel) mainPanel;
            } else {
                System.out.println("*** Tractography Plugin failed to initialize.");
                return;
            }

            // AC-PC registration and target calculator
            flyout.addTab("Tractography");
            Button loadNiftiMrButton = new Button(Button.ButtonType.BUTTON, 10, 205, 125, 25, this);
            loadNiftiMrButton.setTitle("Load NIFTI MR").setCommand("loadNiftiMR");
            loadNiftiMrButton.setPropertyPrefix("Model.Attribute");
            loadNiftiMrButton.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Tractography", loadNiftiMrButton);
            
            Button showFibers = new Button(Button.ButtonType.RADIO_BUTTON, 150, 205, 125, 25, this);
            showFibers.setTitle("Fibers").setCommand("showFibers");
            showFibers.setPropertyPrefix("Model.Attribute");
            showFibers.setColor(0.35f, 0.35f, 0.55f, 1f);
            flyout.addChild("Tractography", showFibers);
            
            Button clipFibers = new Button(Button.ButtonType.RADIO_BUTTON, 150, 165, 125, 25, this);
            clipFibers.setTitle("Clip Fibers").setCommand("clipFibers");
            clipFibers.setPropertyPrefix("Model.Attribute");
            clipFibers.setColor(0.35f, 0.35f, 0.55f, 1f);
            flyout.addChild("Tractography", clipFibers);
            
                    
            Slider slider1 = new Slider(300, 165, 350, 25, controller);
            slider1.setTitle("Clip Thickness");
            slider1.setTag("TractographyPlugin.clipThickness");
            slider1.setCommand("TractographyPlugin.clipThickness"); // controller will set command name as propery on model
            slider1.setPropertyPrefix("Model.Attribute"); // model will report propery updates with this prefix
            slider1.setMinMax(0, 50);
            slider1.setLabelWidth(130);
            slider1.setFormatString("%2.1f mm");
            slider1.setCurrentValue(2);
            flyout.addChild("Tractography", slider1);
            model.addObserver(slider1);

            Button addStartPoint = new Button(Button.ButtonType.BUTTON, 750, 205, 125, 25, this);
            addStartPoint.setTitle("Add Start").setCommand("addStartPoint");
            addStartPoint.setColor(0.35f, 0.55f, 0.35f, 1f);
            flyout.addChild("Tractography", addStartPoint);
            
            Button addEndPoint = new Button(Button.ButtonType.BUTTON, 750, 165, 125, 25, this);
            addEndPoint.setTitle("Add End").setCommand("addEndPoint");
            addEndPoint.setColor(0.55f, 0.35f, 0.35f, 1f);
            flyout.addChild("Tractography", addEndPoint);
            
            Button clearROIs = new Button(Button.ButtonType.BUTTON, 750, 125, 125, 25, this);
            clearROIs.setTitle("Clear Points").setCommand("clearPoints");
            clearROIs.setColor(0.35f, 0.35f, 0.35f, 1f);
            flyout.addChild("Tractography", clearROIs);
            
            Button filterFibers = new Button(Button.ButtonType.BUTTON, 900, 205, 125, 25, this);
            filterFibers.setTitle("Filter Fibers").setCommand("filterFibers");
            filterFibers.setColor(0.35f, 0.35f, 0.35f, 1f);
            flyout.addChild("Tractography", filterFibers);
            
            guiInitialized = true;
        }
    }

    @Override
    public void release() {
    }

    @Override
    public String getName() {
        return "TractographyPlugin";
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
                case "TractographyPlugin.clipThickness":
                    if (fibloader != null) {
                        fibloader.setClipThickness((Float)event.getNewValue());
                    }
                    break;
                case "currentTargetPoint":
                    System.out.println(this.getName() + "[currentTargetPoint]: "+ event.getNewValue());
                    currentTargetPoint.set((Vector3f)event.getNewValue());
                    break;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        Object source = e.getSource();

// TODO: not sure if we should have default model binding come first always by default
        if (e.getSource() instanceof GUIControlModelBinding) {
            ((GUIControlModelBinding)e.getSource()).doBinding(model);
        }

        System.out.println("TractographyPlugin got command: " + command);        
        switch (command) {
            case "clearPoints":
                fibloader.clearAllROIPoints();
                break;
            case "filterFibers":
                fibloader.filterFibers();
                break;
            case "addStartPoint":
                fibloader.addStartPoint(new Vector4f(currentTargetPoint.x, currentTargetPoint.y, currentTargetPoint.z, 2.5f));
                break;
            case "addEndPoint":
                fibloader.addEndPoint(new Vector4f(currentTargetPoint.x, currentTargetPoint.y, currentTargetPoint.z, 2.5f));
                break;
            case "clearAllROIPoints":
                fibloader.clearAllROIPoints();
                break;
            case "showFibers":
                if (fibloader != null) {
                    fibloader.setVisible(((Button)source).getIndicator());
                    view.doTransition(500);
                }
                break;
            case "clipFibers":
                if (fibloader != null) {
                    fibloader.setClipped(((Button)source).getIndicator());
                    view.doTransition(500);
                }
                break;
            case "loadNiftiMR":
                try {
                    NiftiHeader hd = NiftiHeader.read("D:/Downloads/OhioDTIcase/kranion/nifti/anat.nii.gz");
                           System.out.println(hd.magic.toString());
                            System.out.println(NiftiHeader.decodeDatatype(hd.datatype));
                            for (int i=0; i<hd.dim[0]; i++) {
                                System.out.println(i + ") " + hd.dim[i+1] + " (" + hd.pixdim[i+1] + " " + hd.decodeUnits(hd.xyz_unit_code)+ ")");
                            }
                            System.out.println(hd.info());
                            
                            Quaternion q = new Quaternion();
                            Vector3f t = new Vector3f();
                            
                            if (hd.sform_code > 0) {
                                double mat44[][] = hd.sform_to_mat44();
                                Matrix3f m = new Matrix3f();
                                m.m00 = (float)mat44[0][0];
                                m.m01 = (float)mat44[0][1];
                                m.m02 = (float)mat44[0][2];
                                m.m10 = (float)mat44[1][0];
                                m.m11 = (float)mat44[1][1];
                                m.m12 = (float)mat44[1][2];
                                m.m20 = (float)mat44[2][0];
                                m.m21 = (float)mat44[2][1];
                                m.m22 = (float)mat44[2][2];
                                
                                t.x = 0;//(float)mat44[0][3];
                                t.y = 0;//(float)mat44[1][3];
                                t.z = 0;//(float)mat44[2][3];
                                
                                System.out.println(m);
                                System.out.println(t);
                                Quaternion.setFromMatrix(m, q).normalise();
                                System.out.println(q);
                            }
                            
                            ImageVolume4D mrImage = new ImageVolume4D(ImageVolume.USHORT_VOXEL, hd.dim[1], hd.dim[2], hd.dim[3], 1);
                            
                            mrImage.getDimension(0).setSampleSpacing(hd.pixdim[0]);
                            mrImage.getDimension(1).setSampleSpacing(hd.pixdim[1]);
                            mrImage.getDimension(2).setSampleSpacing(hd.pixdim[2]);

                            mrImage.getDimension(0).setSampleWidth(hd.pixdim[0]);
                            mrImage.getDimension(1).setSampleWidth(hd.pixdim[1]);
                            mrImage.getDimension(2).setSampleWidth(hd.pixdim[2]);
                            
                            mrImage.setAttribute("ImageOrientationQ", q);
                            mrImage.setAttribute("ImageTranslation", t);
                            mrImage.setAttribute("ProtocolName", "NIFTI MR");
                            
                            InputStream fis = new GZIPInputStream(new FileInputStream(new File("D:/Downloads/OhioDTIcase/kranion/nifti/anat.nii.gz")));
                            BufferedInputStream bufferedStream = new BufferedInputStream(fis);

                            DataInput di = hd.little_endian ? new LEDataInputStream(bufferedStream)
                                    : new DataInputStream(bufferedStream);
                            
                            byte header[] = new byte[348];
                            di.readFully(header);
                            
                            int voxCount = hd.dim[1]*hd.dim[2]*hd.dim[3];
                            short voxels[] = (short[])mrImage.getData();
                            for (int i=0; i<voxCount; i++) {
                                voxels[voxCount-i-1] = di.readShort();
                            }
                            
                            model.addMrImage(mrImage);
                            
                            // try loading the fa0 volume from the FIB file
                    File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/dti_ecc.src.gz.odf8.f8.csfc.012fy.rdi.gqi.1.25.fib");
//                 File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/data.src.gz.odf8.f8.qsdr.1.25.R52.fib");
                    fibloader.setFile(file);
                    fibloader.setPropertyPrefix("Model.Attribute");
                    
                    fibloader.parse();
                    
                    // load odf, index and fa data to textures
                    fibloader.loadTextures();
                    
                    fibloader.initShader();
                    
//                    Iterator<String> iter = fibloader.keys.keySet().iterator();
//                    while (iter.hasNext()) {
//                        String keyname = iter.next();
//                        FibImageLoader.mat4header hd = fibloader.getEntryHeader(keyname);
//                        System.out.println(keyname + ": " + hd.rows + " x " + hd.cols + " : m=" + hd.m + " p=" + hd.p + " t=" + hd.t);
//
//                    }
                    
                    FibImageLoader.mat4header entry = fibloader.getEntryHeader("dimension");
                    System.out.println("Dimensions = " + entry.rows + " x " + entry.cols);
                    ShortBuffer sbuf = fibloader.getEntryData("dimension").asShortBuffer();
                    int nx, ny, nz;
                    System.out.println(nx = sbuf.get());
                    System.out.println(ny = sbuf.get());
                    System.out.println(nz = sbuf.get());
                    
                    entry = fibloader.getEntryHeader("voxel_size");
                    System.out.println("Voxel size = " + entry.rows + " x " + entry.cols);
                    FloatBuffer fbuf = fibloader.getEntryData("voxel_size").asFloatBuffer();
                    float rx, ry, rz;
                    System.out.println(rx = fbuf.get());
                    System.out.println(ry = fbuf.get());
                    System.out.println(rz = fbuf.get());
                    
                    ImageVolume4D fa0Image = new ImageVolume4D(ImageVolume.USHORT_VOXEL, nx, ny, nz, 1);

                    fa0Image.getDimension(0).setSampleSpacing(rx);
                    fa0Image.getDimension(1).setSampleSpacing(ry);
                    fa0Image.getDimension(2).setSampleSpacing(rz);

                    fa0Image.getDimension(0).setSampleWidth(rx);
                    fa0Image.getDimension(1).setSampleWidth(ry);
                    fa0Image.getDimension(2).setSampleWidth(rz);

                    Matrix4f mat = new Matrix4f();
                    mat.setIdentity();
//                    mat.rotate(3.14159f, new Vector3f(0, 1, 0));
                    fa0Image.setAttribute("ImageOrientationQ", new Quaternion().setFromMatrix(mat));
                    
                    fa0Image.setAttribute("ImageTranslation", new Vector3f());
                    fa0Image.setAttribute("ProtocolName", "fa0");
                    
//                    entry = fibloader.getEntryHeader("fa0");
//                    System.out.println("fa0 = " + entry.rows + " x " + entry.cols);
//                    System.out.println("datasize = " + entry.dataSize);
//                    fbuf = fibloader.getEntryData("fa0").asFloatBuffer();
                    
                    entry = fibloader.getEntryHeader("fa0");
                    System.out.println("fa0 = " + entry.rows + " x " + entry.cols);
                    System.out.println("datasize = " + entry.dataSize);
                    fbuf = fibloader.getEntryData("fa0").asFloatBuffer();
                    
                    voxCount = nx*ny*nz;
                    voxels = (short[])fa0Image.getData();
//                    for (int i=0; i<voxCount; i++) {
//                        voxels[i] = (short)(fbuf.get() *4095);
//                    }
//                    for (int i=0; i<voxCount; i++) {
//                        voxels[i] = (short)(sbuf.get() & 0xfff);
//                    }
                    
int seedCounter = 0;
                    for (int z = 0; z < nz; z++) {
                        for (int y = 0; y < ny; y++) {
                            for (int x = 0; x < nx; x++) {
                                int index = x + (y * nx) + (nz - z - 1) * nx * ny; // flip S/I
                                float faVal = fbuf.get();
                                short val = (short)(faVal * 2048);
                                voxels[index] = val;
                                if (faVal>0.2f) {
                                    seedCounter++;
                                }
                            }
                        }
                    }
                    
                    System.out.println("Seed count for fa>0.2 = " + seedCounter);

                    model.addMrImage(fa0Image);
                    model.addObserver(fibloader);

                    fibloader.doFiberTracking();
                    fibloader.trackWithImage(fa0Image);
                    
                    Renderable parent = Renderable.lookupByTag("DefaultView.main_layer");
                    if (parent != null && parent instanceof RenderLayer) {
                        ((RenderLayer) parent).addChild(fibloader);
                    }

                    view.doTransition(500);

                } catch (IOException ex) {
                    Logger.getLogger(TractographyPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
            
        }
    }
    
}
