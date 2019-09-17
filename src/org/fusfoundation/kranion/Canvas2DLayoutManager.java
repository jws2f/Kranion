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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.lwjgl.opengl.*;
/**
 *
 * @author john
 */
public class Canvas2DLayoutManager extends GUIControl implements Animator, Resizeable, ActionListener {
    
    private ImageCanvas2D[] canvases = new ImageCanvas2D[3];
    private Button[] buttons = new Button[3];

    private boolean[] canvasBig = new boolean[3];
    private float[] startSize = new float[3];
    private float[] endSize = new float[3];
    
    private AnimatorUtil anim = new AnimatorUtil();
    
    private float small = 300f;
    private float big = 800f;

    public Canvas2DLayoutManager(ImageCanvas2D c1, ImageCanvas2D c2, ImageCanvas2D c3) {
        canvases[0] = c1;
        canvases[1] = c2;
        canvases[2] = c3;
                
        for (int i=0; i<3; i++) {
            canvases[i].setCanvasSize(small);
            canvasBig[i] = false;
            
            Button expandButton = new Button(Button.ButtonType.TOGGLE_BUTTON, 0, 0, 21, 21, this);
            expandButton.setTitle("");
            expandButton.setTag(Integer.toString(i));
            expandButton.setIndicatorRadius(8f);
            expandButton.setColor(0,0,0,0);
            canvases[i].addChild(expandButton);
            buttons[i] = expandButton;
        }
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        boolean result =  super.OnMouse(x, y, button1down, button2down, dwheel);
        
        if (result == false) {
            for (int i=0; i<canvases.length; i++) {
                if (canvases[i].MouseIsInside(x, y)) {
                    this.bringToTop();
                    if (button1down || button2down) {
                        result = true;
                    }
                    break;
                }
            }
        }
        
        return result;
    }
    
    
    public void toggleCanvas(ImageCanvas2D canvas, float durationSecs) {
        for (int j=0; j<3; j++) {
            if (canvas == canvases[j]) {
                toggleCanvas(j, durationSecs);
                return;
            }
        }
    }
    
    public void toggleCanvas(int i, float durationSecs) {
        if (i<0 || i>=3 || durationSecs <= 0f) return;
        
        for (int j=0; j<3; j++ ) {
            startSize[j] = canvasBig[j] ? big : small;
        }
        
        canvasBig[i] = !canvasBig[i];
        if (canvasBig[i]) {
            canvases[i].setCanvasSize(big);
        }
        else {
            canvases[i].setCanvasSize(small);
        }
        
        for (int j=0; j<3; j++) {
            if (canvasBig[j] == true && i!= j) {
                canvasBig[j] = false;
            }
            
            endSize[j] = canvasBig[j] ? big : small;
        }
                
        anim.set(0f, 1f, durationSecs);
    }
    
    @Override
    public void doLayout() {
        int screenWidth = Display.getWidth();
        int screenHeight = Display.getHeight();
        
        //canvases[1].setCanvasSize(512f);
        
        float totalHeight = 0f;
        for (int i=0; i<3; i++) {
            totalHeight += canvases[i].getCanvasSize();
        }
        
        float ratio = (float)screenHeight/Math.max(Display.getHeight(), totalHeight);
        
        
        float yoff = (float)Display.getHeight();
        for (int i=0; i<3; i++) {
            float newSize = canvases[i].getCanvasSize() * ratio;
            yoff -= newSize;
            canvases[i].setCanvasPosition(Display.getWidth() - newSize, yoff, newSize, canvases[i].getCanvasFOV());
        }
    }
    
    @Override
    public void cancelAnimation() { anim.cancelAnimation(); }
    
    @Override
    public boolean isAnimationDone() {
        return anim.isAnimationDone();
    }
        
    @Override
    public void advanceFrame() {
        if (isAnimationDone()) return;
        
        anim.advanceFrame();
        float scale = anim.getCurrent();

        for (int i=0; i<3; i++) {
            canvases[i].setCanvasSize(scale * (endSize[i] - startSize[i]) + startSize[i]);
        }

        if (this.parent != null && this.parent instanceof Resizeable) {
            ((Resizeable)parent).doLayout();
        }
//        else {
            doLayout();
//        }
    }

    @Override
    public void render() {
        renderChildren();
    }

    @Override
    public boolean getIsDirty() {
        // if children are dirty or we are animating then we are dirty
        return super.getIsDirty() || !isAnimationDone();
    }

    @Override
    public Renderable setIsDirty(boolean dirty) {
        return this;
    }

    @Override
    public void release() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (int i=0; i<3; i++) {
            Button b = (Button)e.getSource();
            if ( b.getTag().equals(Integer.toString(i))) {
                this.toggleCanvas(i, 0.5f);
                for (int j=0; j<3; j++) {
                    buttons[j].setIndicator(this.canvasBig[j]);
                }
            }
        }
    }
    
}
