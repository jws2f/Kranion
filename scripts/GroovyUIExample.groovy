import org.fusfoundation.kranion.*;
import org.fusfoundation.kranion.model.Model;
import org.lwjgl.util.vector.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class VRNeuroNav implements ActionListener {
    private Model model;
    private TextBox serverAddress;
    
    public VRNeuroNav(Model m) {
        model = m;
        
        Renderable mainPanel = Renderable.lookupByTag("MainFlyout");
        FlyoutPanel flyout = (FlyoutPanel) mainPanel;
        flyout.addTab("VR NeuroNav", this);
        
        Button button = new Button(Button.ButtonType.BUTTON, 10, 205, 125, 25, this);
        button.setTitle("Send Targets").setCommand("VRNeuroNav.sendTargets");
        button.setPropertyPrefix("Model.Attribute");
        button.setColor(0.35f, 0.55f, 0.65f, 1f);
        flyout.addChild("VR NeuroNav", button);
        
        button = new Button(Button.ButtonType.BUTTON, 365, 205, 125, 25, this);
        button.setTitle("Connect").setCommand("VRNeuroNav.serverConnect");
        button.setColor(0.35f, 0.55f, 0.65f, 1f);
        flyout.addChild("VR NeuroNav", button);
        
        serverAddress = (TextBox)new TextBox(225, 205, 150, 25, "", this).setTitle("Server").setCommand("VRNeuroNav.server");
        serverAddress.setText("10.10.1.22");
        serverAddress.setTextEditable(true);
        serverAddress.setIsNumeric(false);
        flyout.addChild(serverAddress);

    }
    
    // ActionListener interface
    public void actionPerformed(ActionEvent e) {        
        switch (e.getActionCommand()) {
            case "VRNeuroNav.sendTargets":
                print "VRNeuroNav sending targets now.\n";
                for (int i=0; i<model.getSonicationCount(); i++) {    
                    Vector3f loc = model.getSonication(i).getNaturalFocusLocation();    
                    print "Sonication " + i + " -> " + loc + "\n";    
                }
                break;
            case "VRNeuroNav.serverConnect":
                String address = serverAddress.getText();
                print "VRNeuroNav connecting to: " + address + "\n";
                break;
       }
    }
}

vrnn = new VRNeuroNav(model);