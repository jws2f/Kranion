/*
 * The MIT License
 *
 * Copyright 2018 Focused Ultrasound Foundation.
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
package org.fusfoundation.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;
import javax.swing.JFileChooser;

/**
 *
 * @author john
 */
public class ActFileFilter {

public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(new String("Choose ACT..."));
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("ACT.ini"));
        File inFile = null;
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            inFile = fileChooser.getSelectedFile();
            
            try {
                FileInputStream fis = new FileInputStream(inFile);

                //Construct BufferedReader from InputStreamReader
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                
                FileWriter bw = new FileWriter(inFile+".mod");
                PrintWriter pw = new PrintWriter(bw);
                
                pw.println("[AMPLITUDE_AND_PHASE_CORRECTIONS]");
                pw.println("NumberOfChannels = 1024");
                pw.println();
                                

                String line = null;
                int count = 0;
                while ((line = br.readLine()) != null) {
                    
                    if (line.startsWith("Element") || line.startsWith("CH")) {
                        Scanner scanner = new Scanner(line);
                        scanner.useDelimiter("=");
//                        pw.print(scanner.next());
                       scanner.next();
                        scanner.useDelimiter("\t");
                        scanner.skip("=");
//                        pw.print("=");
                        float amplitude = Float.parseFloat(scanner.next());
//////                        if (amplitude > 0f) amplitude = 1f; // if you want the amplitude thresheld ///////
 //                       pw.printf("%3.1f", amplitude);
                        
                        float phase = Float.parseFloat(scanner.next());
                        
                        pw.printf("CH%d\t=\t%3.3f\t%3.3f", count++, amplitude, phase);
                        pw.println();
                        
                    }
                }

                br.close();
                pw.close();
            } catch (FileNotFoundException  e) {
            } catch (IOException e) {
            }

    }
    
}    
}
