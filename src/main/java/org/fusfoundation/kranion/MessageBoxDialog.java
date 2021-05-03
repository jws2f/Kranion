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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import org.fusfoundation.kranion.view.DefaultView;

/**
 *
 * @author jsnell
 */
public class MessageBoxDialog extends FlyoutDialog {
    private boolean choice = false;
    private TextBox titleDisplay = new TextBox();
    private TextBox messageDisplay = new TextBox();
    private Button okButton = new Button(Button.ButtonType.BUTTON, 10, 10, 100, 25, this);
    private Button cancelButton = new Button(Button.ButtonType.BUTTON, 125, 10, 100, 25, this);
    private static Font msgfont = new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, 20);

    public MessageBoxDialog(String message) {
        this.setBounds(0, 0, 300, 128);
        this.setFlyDirection(direction.SOUTH);
        messageDisplay.setText(message);
        messageDisplay.showBackgroundBox(false);
        messageDisplay.setTextHorzAlignment(HPosFormat.HPOSITION_CENTER);
        messageDisplay.setTextFont(msgfont);
        
        titleDisplay.setText("");
        titleDisplay.showBackgroundBox(true);
        titleDisplay.setTextHorzAlignment(HPosFormat.HPOSITION_CENTER);
        titleDisplay.setTextFont(msgfont);
        
        addChild(okButton.setTitle("Ok").setCommand("OK"));
        addChild(cancelButton.setTitle("Cancel").setCommand("CANCEL"));
        addChild(messageDisplay);
        addChild(titleDisplay);
    }
    
    public void setMessageText(String message) {
        messageDisplay.setText(message);
    }
    
    public void setDialogTitle(String message) {
        titleDisplay.setText(message);
        if (message == null || message.length() == 0) {
            titleDisplay.setVisible(false);
        }
        else {
            titleDisplay.setVisible(true);
        }
    }
    
    public boolean open() {
        show();
        return choice;
    }
    
   @Override
    public void actionPerformed(ActionEvent e) {
//        System.out.println(this + ": " + e.getActionCommand());
        switch (e.getActionCommand()) {
            case "OK":
                choice = true;
                flyin();
                isClosed = true;
                break;
            case "CANCEL":
                choice = false;
                flyin();
                isClosed = true;
                break;
        }
    }
    
    @Override
    public void doLayout() {
        super.doLayout(); //To change body of generated methods, choose Tools | Templates.
        
        okButton.setBounds(bounds.width - 220, 10, 100, 25);
        cancelButton.setBounds(bounds.width - 110, 10, 100, 25);
        
        titleDisplay.setBounds(10, bounds.height-50, bounds.width-20, 50);
        messageDisplay.setBounds(10, bounds.height/2-50, bounds.width-20, 100);
    }
}
