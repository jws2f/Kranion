package org.fusfoundation.kranion.plugins.tractography;

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
import java.util.Iterator;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.FibImageLoader;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.GUIControlModelBinding;
import org.fusfoundation.kranion.Renderable;
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
    private boolean guiInitialized = false;

    @Override
    public void init(Controller controller) {
        System.out.println("******* Hello from TractographyPlugin !! ****************");
                
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

    @Override
    public void update(Observable o, Object arg) {
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
            // ACPC Planning commands
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
                 File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/data.src.gz.odf8.f8.qsdr.1.25.R52.fib");
                    FibImageLoader fibloader = new FibImageLoader(file);
                    fibloader.parse();
                    
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

                    fa0Image.setAttribute("ImageOrientationQ", new Quaternion());
                    fa0Image.setAttribute("ImageTranslation", new Vector3f());
                    fa0Image.setAttribute("ProtocolName", "fa0");
                    
                    entry = fibloader.getEntryHeader("fa0");
                    System.out.println("fa0 = " + entry.rows + " x " + entry.cols);
                    System.out.println("datasize = " + entry.dataSize);
                    fbuf = fibloader.getEntryData("fa0").asFloatBuffer();
                    
                    voxCount = nx*ny*nz;
                    voxels = (short[])fa0Image.getData();
                    for (int i=0; i<voxCount; i++) {
                        voxels[i] = (short)(fbuf.get() *4095);
                    }

                    model.addMrImage(fa0Image);

                } catch (IOException ex) {
                    Logger.getLogger(TractographyPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
            
        }
    }
    
}
