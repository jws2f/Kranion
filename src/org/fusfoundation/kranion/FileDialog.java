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
package org.fusfoundation.kranion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 *
 * @author jsnell
 */
public class FileDialog extends FlyoutDialog {
        
    ListControl dirList = new ListControl(10, 50, 200, 300, this);
    ListControl fileList = new ListControl(500, 50, 200, 300, this);
    
    public FileDialog() {
        this.setBounds(0, 0, 512, 256);
        this.setFlyDirection(direction.SOUTH);
        addChild(new Button(Button.ButtonType.BUTTON, 10, 10, 100, 25, this).setTitle("Ok").setCommand("OK"));
        addChild(new Button(Button.ButtonType.BUTTON, 125, 10, 100, 25, this).setTitle("Cancel").setCommand("CANCEL"));
        addChild(dirList);
        addChild(fileList);
        
                File[] files = new File(System.getProperty("user.home")).listFiles();
                for (File file : files) {
                    if (!file.getName().startsWith(".")) {
                        if (file.isDirectory()) {
                            System.out.println("Directory: " + file.getName());
                            dirList.addItem(file.getName(), file);
//            showFiles(file.listFiles()); // Calls same method again.
                        } else {
                            System.out.println("File: " + file.getName());
                            fileList.addItem(file.getName(), file);
                        }
                    }
                }
                
                dirList.sort();
                fileList.sort();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println(this + ": " + e.getActionCommand());
        switch (e.getActionCommand()) {
            case "OK":
            case "CANCEL":
//                File[] roots = File.listRoots();
//                for (int i = 0; i < roots.length; i++) {
//                    System.out.println("Root[" + i + "]:" + roots[i]);
//                }
//                System.out.println(System.getProperty("user.home"));
//                File[] files = new File(System.getProperty("user.home")).listFiles();
//                for (File file : files) {
//                    if (!file.getName().startsWith(".")) {
//                        if (file.isDirectory()) {
//                            System.out.println("Directory: " + file.getName());
////            showFiles(file.listFiles()); // Calls same method again.
//                        } else {
//                            System.out.println("File: " + file.getName());
//                        }
//                    }
//                }
                flyin();
                break;
        }
    }    
}
