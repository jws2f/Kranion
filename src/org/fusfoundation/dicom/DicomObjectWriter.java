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
package org.fusfoundation.dicom;

import java.io.*;
import java.util.*;

/**
 *
 * @author  jsnell
 */
public class DicomObjectWriter {

    private VrOutputStream vos;
    
    /** Creates a new instance of DicomObjectOutputStream */
    public DicomObjectWriter(OutputStream os) {
        vos = new VrOutputStream(os);
    }
    
    public DicomObjectWriter(OutputStream os, boolean littleEndian, boolean implicitVr) {
        vos = new VrOutputStream(os, littleEndian, implicitVr);
    }
    
    public DicomObjectWriter(VrOutputStream os) {
        vos = os;
    }
    
    public void write(DicomObject obj) throws IOException {
        Iterator i = obj.iterator();
        while (i.hasNext()) {
            vos.writeVR((VR)i.next());
        }
    }

}
