/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fusfoundation.KranionGroovyConsolePlugin;

import groovy.ui.Console;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import javax.swing.JFrame;
import javax.swing.JWindow;
import org.fusfoundation.kranion.Button;
import org.fusfoundation.kranion.FlyoutPanel;
import org.fusfoundation.kranion.GUIControlModelBinding;
import org.fusfoundation.kranion.Renderable;
import org.fusfoundation.kranion.TransducerRayTracer;
import org.fusfoundation.kranion.controller.Controller;
import org.fusfoundation.kranion.model.Model;
import org.fusfoundation.kranion.plugin.Plugin;
import org.fusfoundation.kranion.view.DefaultView;

/**
 *
 * @author jsnell
 */
public class ConsolePlugin implements Plugin, ActionListener {
    
    private groovy.ui.Console console;
    private Model model;
    private Controller controller;
    private Button consoleButton;
    private FlyoutPanel flyout;
    
    private class myconsole extends groovy.ui.Console {
        ConsolePlugin outer;
        
        public myconsole (ConsolePlugin parent) {
            super();
            outer = parent;
        }

        @Override
        public void exitDesktop() {
            outer.releaseConsole();
            super.exitDesktop(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void exitDesktop(EventObject evt) {
            outer.releaseConsole();
            super.exitDesktop(evt); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean exit(EventObject evt) {
            outer.releaseConsole();
            return super.exit(evt); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void exitDesktop(EventObject evt, Object quitResponse) {
            outer.releaseConsole();
            super.exitDesktop(evt, quitResponse); //To change body of generated methods, choose Tools | Templates.
        }
        
        @Override
        public boolean exit() {
            outer.releaseConsole();
            boolean result = super.exit(); //To change body of generated methods, choose Tools | Templates.
            
            return result;
        }
        
    }

    @Override
    public void init(Controller c) {

        controller = c;
        
        Renderable mainPanel = Renderable.lookupByTag("MainFlyout");
        if (mainPanel != null && mainPanel instanceof FlyoutPanel) {
            flyout = (FlyoutPanel) mainPanel;
        } else {
            System.out.println("*** KranionGroovyConsole failed to initialize.");
            return;
        }
                
        if (consoleButton == null) {
            consoleButton = (Button)new Button(Button.ButtonType.BUTTON, 445, 150, 220, 25, this).setTitle("Scripting Console").setCommand("launchGroovyConsole");
            flyout.addChild("File", consoleButton);
            
            // if the groovy console is already open, keep the console button disabled
            if (console != null) {
                consoleButton.setIsEnabled(false);
                                
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        console.getFrame().getRootPane().setVisible(true);
//                        JFrame frame = (JFrame)console.getFrame().getRootPane().getParent();
//                        frame.setVisible(true);
//                        frame.toFront();
//                    }
//                });
            }
        }
        

        bindScriptingVariables();
        
    }
    
    // Overloaded groovy console object will call this
    // to enable the console launch button when the console exits
    private void releaseConsole() {
        consoleButton.setIsEnabled(true);
        console = null;
    }
    
    private void bindScriptingVariables() {
        model = controller.getModel();
        
        if (console == null) {
            console = new myconsole(this);
        }
        console.setVariable("model", model);

        try {
            DefaultView view = (DefaultView) Renderable.lookupByTag("DefaultView");
            console.setVariable("view", view);
        } catch (Exception ex) {
        }

        try {
            TransducerRayTracer rayTracer = (TransducerRayTracer) Renderable.lookupByTag("TransducerRayTracer");
            console.setVariable("raytracer", rayTracer);
        } catch (Exception ex) {
        }
    }

    @Override
    public void release() {
        //console.exit();
        //console = null;
        model = null;
        
        if (flyout != null) {
            flyout.removeChild(consoleButton);
            flyout = null;
            consoleButton = null;
        }
        
//        if (console != null) {
//            console.exit();
//            console = null;
//        }
    }

    @Override
    public String getName() {
        return "KranionGroovyConsolePlugin";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        Object source = e.getSource();

        switch (command) {
            // ACPC Planning commands
            case "launchGroovyConsole":
            {
                consoleButton.setIsEnabled(false);
                bindScriptingVariables();

                console.run();
             }
            break;
        }
    }
    
}
