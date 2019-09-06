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
package org.fusfoundation.kranion.model.image.io.niftii;

import java.io.File;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Quaternion;

/**
 *
 * @author jsnell
 */
public class NiftiTest {
    public static void main(String[] args) {
        
        //        JFileChooser chooser = new JFileChooser();
        //        chooser.setDialogTitle("Choose FIB file");
        //        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        //        chooser.setMultiSelectionEnabled(false);
        //        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        {
        //                File file = chooser.getSelectedFile();
        //                File file = new File("D:/Downloads/OhioDTIcase/kranion/nifti/anat.nii.gz");
                        try {
                            NiftiHeader hd = NiftiHeader.read("D:/Downloads/OhioDTIcase/kranion/nifti/anat.nii.gz");

                            System.out.println(hd.magic.toString());
                            System.out.println(NiftiHeader.decodeDatatype(hd.datatype));
                            for (int i=0; i<hd.dim[0]; i++) {
                                System.out.println(i + ") " + hd.dim[i+1] + " (" + hd.pixdim[i+1] + " " + hd.decodeUnits(hd.xyz_unit_code)+ ")");
                            }
                            System.out.println(hd.info());
                            
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
                                System.out.println(m);
                                Quaternion q = new Quaternion();
                                Quaternion.setFromMatrix(m, q);
                                System.out.println(q);
                            }
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

        }
    }
}
