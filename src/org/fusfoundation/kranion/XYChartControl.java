/*
 * The MIT License
 *
 * Copyright 2017 Focused Ultrasound Foundation.
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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.*;
import org.knowm.xchart.*;
import org.knowm.xchart.style.XYStyler.*;
import org.knowm.xchart.style.markers.*;
import org.knowm.xchart.BitmapEncoder.*;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author john
 */
public class XYChartControl extends GUIControl {

    private XYChart chart;
    private BufferedImage image;
    private float selectedXValue = 0f;

    public XYChartControl() {

    }

    public XYChartControl(float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        newChart();
        generateChart();
    }

    public float getSelectedXValue() { return selectedXValue; }
    
    @Override
    public void render() {
        if (image != null) {
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);
            renderBufferedImageViaTexture(image, bounds);
//
//            byte buf[] = (byte[]) image.getRaster().getDataElements(0, 0, image.getWidth(), image.getHeight(), null);
//
//
//        //            glEnable(GL_BLEND);
//        //            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//                    glPixelZoom(1.0f, -1.0f);
//                    //glRasterPos2d(-1.0, 1.0);
//
//                    glDisable(GL_CLIP_PLANE0);
//                    glDisable(GL_CLIP_PLANE1);
//                    glDisable(GL_DEPTH_TEST);
//                    
////                    glDisable(GL_BLEND);
//
//                    ByteBuffer bbuf = ByteBuffer.allocateDirect(buf.length);                    
//                    bbuf.put(buf, 0, buf.length);
//                    bbuf.flip();
//                    
//                    glRasterPos2f(bounds.x, bounds.y+image.getHeight());
//                    glDrawPixels(image.getWidth(), image.getHeight(),  GL_RGBA, GL_UNSIGNED_BYTE, bbuf);
//
//                    //      }
//                    //     glPopMatrix();
//                    //  }
//                    //  glMatrixMode(GL_PROJECTION);
//                    //   glPopMatrix();
////                    glDisable(GL_BLEND);
//
            Main.glPopAttrib();
        }
    }

    public void newChart() {
        newChart("Time (s)", "Temp \u00b0C");
    }
    
    public void newChart(String xLabel, String yLabel) {
        newChart(xLabel, yLabel, -1);
    }
    
    public void newChart(String xLabel, String yLabel, int markerSize) {

        // Create Chart
        chart = new XYChartBuilder().width((int) bounds.width).height((int) bounds.height).title(getClass().getSimpleName()).xAxisTitle(xLabel).yAxisTitle(yLabel).build();

        // Customize Chart
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setChartFontColor(new java.awt.Color(0.9f, 0.9f, 0.9f, 1f));
        chart.getStyler().setChartBackgroundColor(new java.awt.Color(0.25f, 0.25f, 0.25f, 0.0f));
        chart.getStyler().setLegendPosition(org.knowm.xchart.style.XYStyler.LegendPosition.InsideNE);
        chart.getStyler().setLegendBackgroundColor(new java.awt.Color(0.35f, 0.35f, 0.35f, 0.5f));
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setPlotGridHorizontalLinesVisible(true);
        chart.getStyler().setPlotGridLinesColor(new java.awt.Color(0.2f, 0.2f, 0.2f, 0.55f));
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setPlotBackgroundColor(new java.awt.Color(0.05f, 0.05f, 0.05f, 0.7f));
        chart.getStyler().setAxisTitlesVisible(true);
        chart.getStyler().setAxisTickLabelsColor(new java.awt.Color(1f, 1f, 1f, 1f));
        if (markerSize > 0) {
            chart.getStyler().setMarkerSize(markerSize);
        }

    }

    public XYChart getChart() {
        return chart;
    }
    
    public void addSeries(String title, double[] xData, double[] yData, Vector4f color) {
        addSeries(title, xData, yData, color, true);
    }
    public void addSeries(String title, double[] xData, double[] yData, Vector4f color, boolean showMarkers) {
        addSeries(title, xData, yData, color, showMarkers, true);
    }
    
    public void addSeries(String title, double[] xData, double[] yData, Vector4f color, boolean showMarkers, boolean showLegend) {
        XYSeries series = chart.addSeries(title, xData, yData);
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        series.setMarker( showMarkers ? SeriesMarkers.CIRCLE : SeriesMarkers.NONE );
        series.setShowInLegend(showLegend);
        series.setLineColor(new java.awt.Color(color.x, color.y, color.z, color.w));
        series.setMarkerColor(new java.awt.Color(color.x, color.y, color.z, color.w));
    }

    public void generateChart() {
        image = new BufferedImage(Math.round(bounds.width), Math.round(bounds.height), BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) image.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        chart.paint(gc, image.getWidth(), image.getHeight());
        
        setIsDirty(true);
    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) return true;
        
        if (this.mouseInside && button1down) {
            if (!hasGrabbed()) {
                grabMouse(x, y);
            }
        }
        
        if (this.hasGrabbed() && !button1down) {
            this.ungrabMouse();
        }
        
        if (button1down && hasGrabbed()) {
            System.out.println("Chart x = " + x + " (" + (float)(x-71)/(bounds.width-71) + ")");
            selectedXValue = Math.max(0, Math.min(1f, (float)(x-71)/(bounds.width-71)));
            this.fireActionEvent();
            
            return true;
        }
        
        
        return false;
    }

}
