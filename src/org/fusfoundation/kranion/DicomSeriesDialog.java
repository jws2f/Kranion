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
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JFileChooser;
import org.fusfoundation.kranion.model.image.io.DicomImageLoader;
import org.fusfoundation.kranion.view.DefaultView;

/**
 *
 * @author jsnell
 */
public class DicomSeriesDialog extends FlyoutDialog {
        
    private ListControl seriesList = new ListControl(10, 50, 400, 300, this);
    private ScrollBarControl seriesScroll = new ScrollBarControl();
    private TextBox dirLabel = new TextBox();
    private TextBox selectedFileDisplay = new TextBox();
    private Button okButton = new Button(Button.ButtonType.BUTTON, 10, 10, 100, 25, this);
    private Button cancelButton = new Button(Button.ButtonType.BUTTON, 125, 10, 100, 25, this);
    private TextBox titleDisplay = new TextBox();
    private static Font titleFont = new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, 20);
    
    private int selectedSeries = -1;
    private String selectedSeriesUID = "";
            
    public DicomSeriesDialog() {
        this.setBounds(0, 0, 512, 256);
        this.setFlyDirection(direction.SOUTH);
        
        selectedFileDisplay.setText("");
        selectedFileDisplay.setTitle("Selected series:");
        selectedFileDisplay.addActionListener(this);
        selectedFileDisplay.setCommand("fileNameTyping");
        
        titleDisplay.setText("Select a DICOM series to load");
        titleDisplay.showBackgroundBox(false);
        titleDisplay.setTextFont(titleFont);
        titleDisplay.setTextHorzAlignment(HPosFormat.HPOSITION_CENTER);
        
        
        addChild(okButton.setTitle("Ok").setCommand("OK"));
        addChild(cancelButton.setTitle("Cancel").setCommand("CANCEL"));
        addChild(seriesList);
        addChild(seriesScroll);
        
        dirLabel.setText("DICOM Series");
        dirLabel.showBackgroundBox(false);
        addChild(dirLabel);
        addChild(selectedFileDisplay);
        addChild(titleDisplay);
        
        seriesScroll.setCommand("seriesScroll");
        seriesScroll.addActionListener(this);
        
        seriesList.setCommand("seriesSelected");
                        
        seriesList.sort();
                
    }
    
    public void setDialogTitle(String title) {
        titleDisplay.setText(title);
    }
        
    public String open() {
        selectedSeries = -1;
        selectedSeriesUID = "";
        
        selectedFileDisplay.setText("");
        selectedFileDisplay.acquireKeyboardFocus();
        okButton.setIsEnabled(false);
        
        show();
        
        return selectedSeriesUID;
    }
        
    public void populateList(Map<String, DicomImageLoader.seriesDescriptor> series) {

        seriesList.clear();


        Iterator<String> i = series.keySet().iterator();
        while(i.hasNext()) {
            String seriesUID = i.next();
            DicomImageLoader.seriesDescriptor descriptor = series.get(seriesUID);
            if (descriptor != null) {
                String itemText = new String(descriptor.patientName);
                if (descriptor.modality != null) {
                    itemText = itemText + " - " + descriptor.modality;
                }
                if (descriptor.sliceFiles != null) {
                    itemText = itemText + " [" + descriptor.sliceFiles.size() + " slices]";
                }
                if (descriptor.protocolName != null) {
                    itemText = itemText + " - " + descriptor.protocolName;
                }
                if (descriptor.seriesDescription != null) {
                    itemText = itemText + " - " + descriptor.seriesDescription;
                }
                if (descriptor.acquisitionDate != null) {
                    itemText = itemText + " - " + descriptor.acquisitionDate;
                }
                
                seriesList.addItem(itemText, seriesUID);
            }
        }    

        seriesScroll.setValue(0);
        seriesList.setScroll(0);

        doLayout();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
//        System.out.println(this + ": " + e.getActionCommand());
        switch (e.getActionCommand()) {
            case "fileNameTyping":
                if (this.selectedFileDisplay.getText().length() > 0) {
                    okButton.setIsEnabled(true);
                }
                else {
                    okButton.setIsEnabled(false);
                }
                break;
            case "OK":
            case "CANCEL":
                flyin();
                isClosed = true;
                break;
            case "seriesScroll":
                {
                    ScrollBarControl scroll = (ScrollBarControl)e.getSource();
                    seriesList.setScroll(scroll.getValue());
                }
                break;
            case "seriesSelected":
                selectedFileDisplay.setText((String)seriesList.getSelectedValue());
                selectedSeriesUID = (String)seriesList.getSelectedValue();
                this.selectedSeries = seriesList.getSelected();
                System.out.println("Selected series: " + selectedSeries + ": " + selectedSeriesUID);
                if (selectedFileDisplay.getText().length() > 0) {
                    okButton.setIsEnabled(true);
                }
                else {
                    okButton.setIsEnabled(false);
                }
                break;
            case "doubleClick":
                if (e.getSource() instanceof ListControl) {
                    ListControl lc = (ListControl)e.getSource();
                    if (lc == seriesList) {
                        flyin();
                        isClosed = true;
                    }
                }
                break;
            case "listWheel":
                if (e.getSource() instanceof ListControl) {
                    ListControl lc = (ListControl)e.getSource();
                    if (lc == seriesList) {
                        seriesScroll.setValue(seriesList.getScroll());
                    }
                }
                break;
        }
    }    

    @Override
    public void doLayout() {
        super.doLayout(); //To change body of generated methods, choose Tools | Templates.
        
        okButton.setBounds(bounds.width - 220, 10, 100, 25);
        cancelButton.setBounds(bounds.width - 110, 10, 100, 25);
        
        seriesList.setBounds(10, 50, bounds.width - 50, bounds.height - 150);
        dirLabel.setBounds(10, 50+bounds.height-150, seriesList.getBounds().width, 25);
        
        Rectangle listBounds = seriesList.getBounds();
        seriesScroll.setBounds(listBounds.x + listBounds.width, listBounds.y, 25, listBounds.height);
        
        seriesScroll.setPageLength((float)seriesList.itemsDisplayed()/seriesList.size());
        
        selectedFileDisplay.setBounds(150, 10, bounds.width - 380, 25);
        
        titleDisplay.setBounds(10, bounds.height-50, bounds.width-20, 50);
    }
    
}
