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
package org.fusfoundation.kranion;

//import com.sun.scenario.effect.impl.BufferUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.fusfoundation.kranion.TextBox.getScreenCoords;
import static org.fusfoundation.kranion.TextBox.getWorldCoords;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.ARBClearTexture.glClearTexSubImage;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author John Snell
 */

// Some shared bookeeping for GUI controls
public abstract class GUIControl extends Renderable implements org.fusfoundation.kranion.MouseListener, Observer {
    private String command = new String();
    private String title = new String();
    private String propertyPrefix = new String();
    protected boolean isVisible = true;
    protected boolean isEnabled = true;
    protected boolean mouseInside = false;
    protected Rectangle bounds = new Rectangle();
    protected ArrayList<ActionListener> listeners = new ArrayList<>();
    protected GUIControl parent = null;
    protected GUIControl grabbedChild = null; // child control that has grabbed the mouse
    protected float xgrab, ygrab;
    protected List<Renderable> children = new CopyOnWriteArrayList<>();
    protected boolean isTextEditable = false;
    
    protected Thread myThread = Thread.currentThread();
    protected UpdateEventQueue updateEventQueue = new UpdateEventQueue();
    protected static Font stdfont = null;
            
    public GUIControl() {
        if (stdfont == null) {
            String os = System.getenv("OS");
            if (os != null && os.startsWith("Windows")) {
                stdfont = new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT , 16);
            }
            else {
                stdfont = new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT , 15);
            }
        }
    }
    
    public void addChild(Renderable child) {
        if (child instanceof GUIControl) {
            ((GUIControl)child).parent = this;
        }
        children.add(child);
    }
    
    public void removeChild(Renderable child) {
        children.remove(child);
    }
    
    @Override
    public boolean getIsDirty() {
        
        updateEventQueue.handleEvents(this);

        advanceChildren(); // all animators will set dirty as needed
        
        if (isDirty) {
//            System.out.println("I am dirty: " + this);
            return true;
        }
        
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child.getIsDirty()) {
//                System.out.println("  Dirty child: " + child);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Renderable setIsDirty(boolean dirty) {
        isDirty = dirty;
//        if (dirty == false) {
            Iterator<Renderable> i = children.iterator();
            while (i.hasNext()) {
                Renderable child = i.next();
                child.setIsDirty(dirty);
            }
//        }
        return this;
    }
    
    public void setIsEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            setIsDirty(true);
        }
        this.isEnabled = enabled;
    }
    
    public boolean getIsEnabled() {
        return this.isEnabled;
    }
        
    // Some children who are animators may need to be checked to
    // see if they are "dirty" before the render cycle, otherwise
    // animation changes might not get rendered.
    public void advanceChildren() {
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child instanceof Animator) {
                ((Animator) child).advanceFrame();
            }
        }
    }
    
    public void renderChildren() {
        glMatrixMode(GL_MODELVIEW);
        Main.glPushMatrix();
            glTranslatef(bounds.x, bounds.y, 0.1f);
            
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child instanceof Animator) {
                ((Animator) child).advanceFrame();
            }
//            int startStackDepth = glGetInteger(GL_ATTRIB_STACK_DEPTH);
            child.render();
//            int endStackDepth = glGetInteger(GL_ATTRIB_STACK_DEPTH);
//            if (startStackDepth != endStackDepth) {
//                System.out.println("ATTRIB stack leak: " + child + "[" + startStackDepth + "->" + endStackDepth + "]");
//            }
            
//System.out.println(child);
//if (Main.checkForGLError() != GL_NO_ERROR) {
//   System.out.println(child);
//   System.out.println("MODELVIEW stack depth: " + glGetInteger(GL_MODELVIEW_STACK_DEPTH));
//   System.out.println("MODELVIEW max stack depth: " + glGetInteger(GL_MAX_MODELVIEW_STACK_DEPTH));
//   System.out.println("PROJECTIONVIEW stack depth: " + glGetInteger(GL_PROJECTION_STACK_DEPTH));
//   System.out.println("PROJECTIONVIEW max stack depth: " + glGetInteger(GL_MAX_PROJECTION_STACK_DEPTH));
//}
        }
        Main.glPopMatrix();
     }
    
    public void bringToTop() {
        // If we have a parent, move us to the last to be drawn and first to get mouse events
        if (parent != null) {
            if (parent.children.remove(this)) {
                parent.children.add(this);
            }
        }
    }
    
    public void sendToBottom() {
        // If we have a parent, move us to the last to be drawn and first to get mouse events
        if (parent != null) {
            if (parent.children.remove(this)) {
                parent.children.add(0, this);
            }
        }
    }
    
    public void renderPickableChildren() {
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            if (child instanceof Pickable) {
                ((Pickable) child).renderPickable();
            }
            else if (child instanceof Trackball) { // TODO: kind of a hack, if Trackball doesn't render then orientation isn't correct
                child.render();
            }
        }
    }  
    
    public void setTextEditable(boolean isEditable) {
        this.isTextEditable = isEditable;
    }
    
    public boolean getTextEditable() { return isTextEditable; }
    
    @Override
    public void release() {
        Iterator<Renderable> i = children.iterator();
        while (i.hasNext()) {
            Renderable child = i.next();
            child.release();
        }
        
       this.removeFromSet();
        
        listeners.clear();        
    }
    
    protected void grabMouse(float x, float y) {
        if (parent != null) {
            if (parent.grabbedChild == null) {
                parent.grabbedChild = this;
                xgrab = x;
                ygrab = y;
            }
        }
    }
    
    protected void ungrabMouse() {
        if (parent != null) {
            if (parent.grabbedChild == this) {
                parent.grabbedChild = null;
                xgrab = ygrab = -1;
            }
        }
    }
        
    protected boolean hasGrabbed() {
        if (parent!=null && parent.grabbedChild == this) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public GUIControl setCommand(String cmd) {
        this.command = new String(cmd);
        
        return this;
    }
    
    public String getCommand() { return command; }
    
    public GUIControl setTitle(String cmd) {
        this.title = new String(cmd);
        setIsDirty(true);
        
        return this;
    }
    
    public String getTitle() { return title; }
    
    @Override
    public boolean getVisible() { return isVisible; }
    
    @Override
    public Renderable setVisible(boolean visible) {
        if (isVisible != visible) {
            setIsDirty(true);
        }
        isVisible = visible;
        
        return this;
    }
    
    public void setBounds(float x, float y, float width, float height) {
        if (x != bounds.x || y != bounds.y || width != bounds.width || height != bounds.height) {
            setIsDirty(true);
            bounds.setBounds(x, y, width, height);
        }
    }
    
    public void setBounds(Rectangle r) {
        if (r.x != bounds.x || r.y != bounds.y || r.width != bounds.width || r.height != bounds.height) {
            setIsDirty(true);
            bounds.setBounds(r);
        }
    }
    
    public Rectangle getBounds() { return bounds; }
    
    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }
    
    public void addActionListener(ActionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public void removeActionListener(ActionListener listener) {listeners.remove(listener);}
    
    public void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        fireActionEvent(event);
    }
    
    public void fireActionEvent(ActionEvent event) {
        Iterator<ActionListener> i = listeners.iterator();
        while (i.hasNext()) {
            i.next().actionPerformed(event);
        }
    }
    
    public void fireActionEvent(ActionListener dontNotifyMe) {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        fireActionEvent(event, dontNotifyMe);
    }
    
    public void fireActionEvent(ActionEvent event, ActionListener dontNotifyMe) {
        Iterator<ActionListener> i = listeners.iterator();
        while (i.hasNext()) {
            ActionListener al = i.next();
            if (al != dontNotifyMe) {
                al.actionPerformed(event);
            }
        }
    }
    
    public void fireActionEvent(String altcmd) {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, altcmd);
        Iterator<ActionListener> i = listeners.iterator();
        while (i.hasNext()) {
            i.next().actionPerformed(event);
        }
    }
    
    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        
        boolean currentInside = MouseIsInside(x, y);
        
        if (mouseInside != currentInside) {
            mouseInside = currentInside;
            setIsDirty(true);
        }
//        if (currentInside && !mouseInside) {
//            mouseInside = true;
//            setIsDirty(true);
//        }
//        else if (!currentInside && mouseInside) {
//            mouseInside = false;
//            setIsDirty(true);
//        }
        
        if (getVisible()) {
            
            if (this.grabbedChild != null) {
                return grabbedChild.OnMouse(x - bounds.getIntX(), y - bounds.getIntY(), button1down, button2down, dwheel);
            }
            else {
                // things are drawn in list order, so mouse order is reversed (things drawn last, get mouse first)
                ListIterator<Renderable> i = children.listIterator(children.size());
                while(i.hasPrevious()) {
                    Renderable child = i.previous();
                    if (child.getVisible() && child instanceof MouseListener) {
                        if ( ((MouseListener)child).OnMouse(x - bounds.getIntX(), y - bounds.getIntY(), button1down, button2down, dwheel) ) {
                            return true;
                        }
                    }
                }
            }
        }
                
        return false;
    }
    
    @Override
    public boolean MouseIsInside(float x, float y) {
        return contains(x, y);
    }

    // This one get's called when the property name == command name
    // This is a shortcut for simple, one property controls
    public void update(Object newValue) {
        // Override to support property change notifications
    }
    
    // If property name != command name, this one gets called
    // For objects that have multiple bound properties
    public void update(String propertyName, Object newValue) {
        // Override to support property change notifications
    }
    

    
    @Override
    public void update(Observable o, Object arg) {
        if (arg != null && arg instanceof PropertyChangeEvent) {
            
            // If the update is coming from other than the main (OpenGL) thread
            // then queue it for later when it can be handled on main thread
            if (myThread != Thread.currentThread()) {
                this.updateEventQueue.push(o, arg);
                return;
            }
            
            PropertyChangeEvent propEvt = (PropertyChangeEvent) arg;
            String propName = this.getFilteredPropertyName(propEvt);
            if (propName.length() > 0) {
                if (propName.equals(this.getCommand())) {
                    update(propEvt.getNewValue());
                }
//            else {
                update(propName, propEvt.getNewValue()); // TODO: Always pass?
//            }
            }
        }
    }
    
    public GUIControl setPropertyPrefix(String name) {
        this.propertyPrefix = name;
        return this;
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
    
    public float getGuiScale() {
//        FloatBuffer modelMatrix = BufferUtil.newFloatBuffer(16);
//	glGetFloat(GL_MODELVIEW_MATRIX, modelMatrix);
//
//        Matrix4f modelview = new Matrix4f();
//        modelview.load(modelMatrix);
//        
//        Vector4f scale = new Vector4f(1f, 0f, 0f, 0f);
//        Matrix4f.transform(modelview, scale, scale);
//        
//        return scale.x;
        //Disabling for now, needs to be rethought
        return 1f;
    }
    
    public static enum VPosFormat {
        VPOSITION_TOP,
        VPOSITION_CENTER,
        VPOSITION_BOTTOM
    }
    
    public static enum HPosFormat {
        HPOSITION_LEFT,
        HPOSITION_CENTER,
        HPOSITION_RIGHT
    }
    
    public void renderText(String str, Rectangle rect, Font font, Color color, boolean shadowed, VPosFormat vpos, HPosFormat hpos) {
        renderText(str, rect, font, color, shadowed, vpos, hpos, false, -1, -1);
    }
    
    public int calculateCaretPos(String str, Rectangle rect, Font font, float mouseX, float mouseY, VPosFormat vpos, HPosFormat hpos, int currentCursorPosition) {
        if (str != null && str.length() != 0) {
        } else {
            return 0;
        }
        
        BufferedImage img = new BufferedImage(rect.getIntWidth(), rect.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D)img.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        if (font == null) font = stdfont;
        
        FontMetrics metrics = gc.getFontMetrics(font);
        
            float cursorXPos = -1f;
        
        try {
            cursorXPos = metrics.stringWidth(str.substring(0, currentCursorPosition));
        }
        catch(StringIndexOutOfBoundsException e) {
            cursorXPos = -1f;
        }
//    float cursorXPos = (float)metrics.getStringBounds(str, 0, currentCursorPosition, gc).getMaxX();

        float hScroll = 0f;
        float textHPos = 0f;
        switch(hpos) {
            case HPOSITION_LEFT:
                textHPos = 1;
                if (currentCursorPosition > -1) {
                    hScroll = Math.max(0f, cursorXPos - (rect.width));
                }
                break;
            case HPOSITION_RIGHT:
//                textHPos = rect.width - newWidth;
                if (currentCursorPosition > -1) {
////                    hScroll = Math.min(0, Math.round(cursorXPos) + (rect.getIntWidth() - 1 - (int)newWidth));
                }
                break;
            case HPOSITION_CENTER:
//                textHPos = rect.width/2 - newWidth/2;
                break;
        }
        
        for (int i=0; i<=str.length(); i++) {
            Rectangle2D r1 = metrics.getStringBounds(str, 0, Math.max(i-1, 0), gc);
            Rectangle2D r2 = metrics.getStringBounds(str, 0, i, gc);
            float index = (float)((r2.getMaxX() - r1.getMaxX())/2f + r1.getMaxX());
             System.out.println("pos " + i + " = " + index + " ? " + (mouseX-(rect.x-hScroll)) + " hscroll " + hScroll);
            if (index >= (mouseX-(rect.x-hScroll)) ) {
//                if (hScroll > 0f) {
//                    return Math.max(0, i);
//                }
//                else {
System.out.println();
                    return Math.max(0, i-1);
//                }
            }
        }
        
        return str.length();
        
//        float textVPos = 0f;               
//        int hScroll = 0;
//        switch(vpos) {
//            case VPOSITION_TOP:
//               textVPos = metrics.getAscent();
//                break;
//            case VPOSITION_CENTER:
//                textVPos = (metrics.getAscent() + (rect.height - (metrics.getAscent() + metrics.getDescent())) / 2);
//                break;
//            case VPOSITION_BOTTOM:
//                textVPos = rect.height - metrics.getDescent();
//        }
//        

        
    }
    public void renderTextCore(BufferedImage dest, String str, Rectangle rect, Font font, Color color, Color fill, boolean shadowed, VPosFormat vpos, HPosFormat hpos, boolean showCaret, int cursorPos) {
        renderTextCore(dest, str, rect, font, color, fill, shadowed, vpos, hpos, showCaret, cursorPos, -1);
    }

    public void renderTextCore(BufferedImage dest, String str, Rectangle rect, Font font, Color color, Color fill, boolean shadowed, VPosFormat vpos, HPosFormat hpos, boolean showCaret, int cursorPos, int selectEndPos) {

        Graphics2D gc = (Graphics2D) dest.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                
        if (font == null) font = stdfont;
        
        FontMetrics metrics = gc.getFontMetrics(font);
        
        Rectangle2D textBound = metrics.getStringBounds(str, gc);
                
        float newWidth = metrics.stringWidth(str);
        float textHeight = metrics.getHeight();
        
        float cursorXPos = -1f;
        float selectEndXPos = -1f;
        
        try {
            cursorXPos = metrics.stringWidth(str.substring(0, cursorPos));
        }
        catch(StringIndexOutOfBoundsException e) {
            cursorXPos = -1f;
        }
        
        try {
            selectEndXPos = metrics.stringWidth(str.substring(0, selectEndPos));
        }
        catch(StringIndexOutOfBoundsException e) {
            selectEndXPos = -1f;
        }
        
        float textVPos = 0f;               
        int hScroll = 0;
        switch(vpos) {
            case VPOSITION_TOP:
               textVPos = metrics.getAscent();
                break;
            case VPOSITION_CENTER:
                textVPos = (metrics.getAscent() + (rect.height - (metrics.getAscent() + metrics.getDescent())) / 2);
                break;
            case VPOSITION_BOTTOM:
                textVPos = rect.height - metrics.getDescent();
        }
        
        float textHPos = 0f;
        switch(hpos) {
            case HPOSITION_LEFT:
                textHPos = 1;
                if (cursorXPos > -1) {
                    hScroll = Math.max(0, Math.round(cursorXPos) - (rect.getIntWidth() - 1));
                }
                break;
            case HPOSITION_RIGHT:
                textHPos = rect.width - newWidth;
                if (cursorXPos > -1) {
                    hScroll = Math.min(0, Math.round(cursorXPos) + (rect.getIntWidth() - 1 - (int)newWidth));
                }
                break;
            case HPOSITION_CENTER:
                textHPos = rect.width/2 - newWidth/2;
                break;
        }
        
        // defensive clear, prob not necessary
        gc.setColor(new Color(0, 0, 0, 0));
        gc.fillRect(0, 0, rect.getIntWidth()+2, rect.getIntHeight()+2);

        gc.setFont(font);
        
        // draw selection if any
        if (cursorXPos != -1 && selectEndXPos != -1) {
            gc.setColor(new Color(0, 0.5f, 0, 1));
            if (selectEndXPos > cursorXPos) {
                gc.fillRect((int)cursorXPos, 3, (int)(selectEndXPos - cursorXPos)+2, rect.getIntHeight()-5);
            }
            else {
                gc.fillRect((int)selectEndXPos, 3, (int)(cursorXPos - selectEndXPos)+2, rect.getIntHeight()-5);
            }
        }
  
        if (fill != null) {
            gc.setColor(fill);
            gc.fillRect(0, 0, rect.getIntWidth(), rect.getIntHeight());
        }
        else {
            gc.setColor(new Color(0, 0, 0, 0));
            gc.fillRect(0, 0, dest.getWidth(), dest.getHeight());
        }
        
        if (shadowed) {
            gc.setColor(new Color(0.0f, 0.0f, 0.0f, 1.0f));
            gc.drawString(str, textHPos+1 - hScroll, textVPos+1);
        }
        
        if (isEnabled) {
            gc.setColor(color);
        }
        else {
            gc.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));            
        }
        gc.drawString(str, textHPos - hScroll, textVPos);
        
        if (isTextEditable && hasKeyboardFocus() && cursorPos > -1 && showCaret) {
            gc.drawLine(
                (int)textHPos + (int)cursorXPos - 1 - hScroll,
                (int)textVPos + metrics.getDescent(),
                (int)textHPos + (int)cursorXPos - 1 - hScroll,
                (int)textVPos + metrics.getDescent() - (int)textHeight);
        }    
    }
    
    public void renderText(String str, Rectangle rect, Font font, Color color, boolean shadowed, VPosFormat vpos, HPosFormat hpos, boolean showCaret, int cursorPos, int selectionEndPos) {
        
        if (rect.width <= 0 || rect.height <= 0) return;
        
        BufferedImage img = new BufferedImage(rect.getIntWidth(), rect.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        
        renderTextCore(img, str, rect, font, color, null, shadowed, vpos, hpos, showCaret, cursorPos, selectionEndPos);
        
        renderBufferedImageViaTexture(img, rect);
    }
    
    public void renderText(String str, Rectangle rect, Font font, Color color, Color fill, boolean shadowed, VPosFormat vpos, HPosFormat hpos, boolean showCaret, int cursorPos, int selectionEndPos) {
        
        if (rect.width <= 0 || rect.height <= 0) return;
        
        BufferedImage img = new BufferedImage(rect.getIntWidth(), rect.getIntHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        
        renderTextCore(img, str, rect, font, color, fill, shadowed, vpos, hpos, showCaret, cursorPos, selectionEndPos);
        
        renderBufferedImageViaTexture(img, rect);
    }
    
    public Rectangle getStringBounds(String str, Font font) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) img.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                
        if (font == null) font = stdfont;
        
        FontMetrics metrics = gc.getFontMetrics(font);
        
        float textWidth = metrics.stringWidth(str);
        float textHeight = metrics.getHeight();
    
        return new Rectangle(0, 0, textWidth, textHeight);
    }
        
    public void renderBufferedImage(BufferedImage img, Rectangle rect) {

        // Don't modify the Rectangle passed in
        Rectangle imgRect = new Rectangle(rect);
        
        if (img != null) {
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);

                    float xoffset = 0;
                    float yoffset = 0;
                    BufferedImage imageToUse = img;
                    
                    Vector2f screenPos = getScreenCoords(imgRect.x, imgRect.y, 0);
                    if (screenPos == null) screenPos = new Vector2f(0, 0);
                    
                    if (screenPos.x < 0 || screenPos.y < 0) {
                        
                        
                        if (screenPos.x < 0) xoffset = -screenPos.x;
                        if (screenPos.y < 0) yoffset = -screenPos.y;
                        
                        if (Math.round(xoffset) >= img.getWidth() ||
                            Math.round(yoffset) >= img.getHeight()) {
                            Main.glPopAttrib();
                            return; // we are totally off screen
                        }
                                                
                        imageToUse = img.getSubimage(
                                Math.round(xoffset),
                                Math.round(yoffset),
                                Math.round(imgRect.width - xoffset),
                                Math.round(imgRect.height - yoffset));
                    }
                    
                    byte buf[] = (byte[]) imageToUse.getRaster().getDataElements(0, 0, imageToUse.getWidth(), imageToUse.getHeight(), null);

                    glPixelZoom(1.0f, -1.0f);

                    glDisable(GL_CLIP_PLANE0);
                    glDisable(GL_CLIP_PLANE1);
                    glDisable(GL_DEPTH_TEST);

                    ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
                    bbuf.put(buf, 0, buf.length);
                    bbuf.flip();
                    
                    Vector3f woffset;
                    if (xoffset > 0f || yoffset > 0f) {
                        woffset = getWorldCoords(0, 0);
                        if (woffset == null) woffset = new Vector3f();
                        
                        if (xoffset > 0) imgRect.x = woffset.x;
                        if (yoffset > 0) imgRect.y = woffset.y;
                    }
                                       
                    glRasterPos2f(Math.max(0f, imgRect.x), Math.max(0f, imgRect.y+imageToUse.getHeight()));
                    glDrawPixels(imageToUse.getWidth(), imageToUse.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
                                        
//                    glColor3f(1, 1, 1);
//                    glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
//                    glBegin(GL_QUADS);
//                        glVertex2f(imgRect.x, imgRect.y);
//                        glVertex2f(imgRect.x + imageToUse.getWidth(), imgRect.y);
//                        glVertex2f(imgRect.x + imageToUse.getWidth(), imgRect.y + imageToUse.getHeight());
//                        glVertex2f(imgRect.x, imgRect.y + imageToUse.getHeight());
//                    glEnd();

            Main.glPopAttrib();
        }
    }
    
    // This is a scratch texture for displaying Java 2D graphics   
    private static int backingTextureName = 0;
    private static int backingTextureWidth = 0;
    private static int backingTextureHeight = 0;
    
    private void createBackingTexture() {
        if (backingTextureName == 0 || backingTextureWidth != Display.getWidth() || backingTextureHeight != Display.getHeight()) {
            
            if (backingTextureName != 0) {
                glBindTexture(GL_TEXTURE_2D, 0);
                glDeleteTextures(backingTextureName);
                backingTextureName = 0;
            }
            
            backingTextureName = glGenTextures();

            backingTextureWidth = Display.getWidth();
            backingTextureHeight = Display.getHeight();

            glBindTexture(GL_TEXTURE_2D, backingTextureName);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, backingTextureWidth, backingTextureHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        }
    }
        
    public void renderBufferedImageViaTexture(BufferedImage img, Rectangle rect) {

        // Don't modify the Rectangle passed in
        Rectangle imgRect = new Rectangle(rect);

        if (img != null) {
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);
                    
                    
                    byte buf[] = (byte[]) img.getRaster().getDataElements(0, 0, img.getWidth(), img.getHeight(), null);


                    glDisable(GL_CLIP_PLANE0);
                    glDisable(GL_CLIP_PLANE1);
                    glDisable(GL_DEPTH_TEST);

                    ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);
                    bbuf.put(buf, 0, buf.length);
                    bbuf.flip();
                    
                    createBackingTexture();

// This doesn't seem to be necessary since we always init the rectangular region specified
// These calls are REALLY SLOW on the Linux NVIDIA driver
//                    glClearTexImage(backingTextureName, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
//                    glClearTexSubImage(backingTextureName, 0, 0, 0, 0, img.getWidth()+2, img.getHeight()+2, 1, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
                    
                    glBindTexture(GL_TEXTURE_2D, backingTextureName);
//                int textureName = glGenTextures();
//                glBindTexture(GL_TEXTURE_2D, textureName);
//                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
//                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
//                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
//                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imageToUse.getWidth(), imageToUse.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

//            ByteBuffer clearValue = BufferUtils.createByteBuffer(4);
//            org.lwjgl.opengl.GL44.glClearTexSubImage(backingTextureName, 0, 0, 0, 0, img.getWidth(), img.getHeight(), 1, GL_RGBA, GL_UNSIGNED_BYTE, clearValue);
            
            // write new texture data
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, img.getWidth(), img.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
                                       
//                    glRasterPos2f(Math.max(0f, imgRect.x), Math.max(0f, imgRect.y+imageToUse.getHeight()));
//                    glDrawPixels(imageToUse.getWidth(), imageToUse.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
            glLoadIdentity();
            glMatrixMode(GL_MODELVIEW);
                                        
                    Rectangle texRect = new Rectangle(
                            0,
                            0,
                            ((float)img.getWidth())/(backingTextureWidth),
                            ((float)img.getHeight())/(backingTextureHeight)
                    );
                    
                    glColor3f(1, 1, 1);
                    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                    glEnable(GL_TEXTURE_2D);
                    glBegin(GL_QUADS);
                        glTexCoord2f(texRect.x, texRect.y+texRect.height);
                        glVertex2f(imgRect.getIntX(), imgRect.getIntY());
                        
                        glTexCoord2f(texRect.x+texRect.width, texRect.y+texRect.height);
                        glVertex2f(imgRect.getIntX() + imgRect.getIntWidth(), imgRect.getIntY());
                        
                        glTexCoord2f(texRect.x+texRect.width, texRect.y);
                        glVertex2f(imgRect.getIntX() + imgRect.getIntWidth(), imgRect.getIntY() + imgRect.getIntHeight());
                        
                        glTexCoord2f(texRect.x, texRect.y);
                        glVertex2f(imgRect.getIntX(), imgRect.getIntY() + imgRect.getIntHeight());
                    glEnd();
                    glDisable(GL_TEXTURE_2D);
                    
                glBindTexture(GL_TEXTURE_2D, 0);
//                glDeleteTextures(textureName);

            glMatrixMode(GL_TEXTURE);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
            
            Main.glPopAttrib();
            
        }
    }
    
    // Allocate for reuse below. Not thread safe!
    private static FloatBuffer  worldCoords = BufferUtils.createFloatBuffer(4);
    private static FloatBuffer  screenCoords = BufferUtils.createFloatBuffer(4);
    private static IntBuffer    viewport = BufferUtils.createIntBuffer(16);
    private static FloatBuffer  modelView = BufferUtils.createFloatBuffer(16);
    private static FloatBuffer  projection = BufferUtils.createFloatBuffer(16);
    
    public static Vector2f getScreenCoords(double x, double y, double z) {
        
        glGetFloat(GL_MODELVIEW_MATRIX, modelView);
        glGetFloat(GL_PROJECTION_MATRIX, projection);
        glGetInteger(GL_VIEWPORT, viewport);

        boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, screenCoords);
                
        if (result) {
            return new Vector2f(screenCoords.get(0), screenCoords.get(1));
        }
        return null;
    }
    
    public static Vector3f getWorldCoords(double screenx, double screeny) {
        return getWorldCoords(screenx, screeny, 0);
    }
    
    public static Vector3f getWorldCoords(double screenx, double screeny, double screenZ) {

        glGetFloat(GL_MODELVIEW_MATRIX, modelView);
        glGetFloat(GL_PROJECTION_MATRIX, projection);
        glGetInteger(GL_VIEWPORT, viewport);

        float winX = (float) screenx;
        float winY = (float) viewport.get(3) - (float) screeny;

        boolean result = GLU.gluUnProject((float) screenx, (float) screeny, (float)screenZ, modelView, projection, viewport, worldCoords);

        if (result) {
            return new Vector3f(worldCoords.get(0), worldCoords.get(1), worldCoords.get(2));
        }
        return null;
    }
}
