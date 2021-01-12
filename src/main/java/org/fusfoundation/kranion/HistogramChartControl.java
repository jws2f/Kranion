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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import org.knowm.xchart.*;
import org.knowm.xchart.style.XYStyler.*;
import org.knowm.xchart.style.markers.*;
import org.knowm.xchart.BitmapEncoder.*;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.style.CategoryStyler;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Vector4f;

/**
 *
 * @author john
 */
public class HistogramChartControl extends GUIControl {

    private CategoryChart chart;
    private BufferedImage image;
    private String xAxisTitle = "X Axis";
    private String yAxisTitle = "Y Axis";
    private String xAxisFormat = "#.#";

    public HistogramChartControl() {

    }

    public HistogramChartControl(String xTitle, String yTitle, float x, float y, float width, float height) {
        super.setBounds(x, y, width, height);
        xAxisTitle = xTitle;
        yAxisTitle = yTitle;
        newChart();
        List<Double> values = new ArrayList<>();
        addSeries("stuff", values, new Vector4f(0.1f, 0.8f, 0.1f, 1f), 10, 0f, 10f);
        generateChart();
    }

    @Override
    public void render() {
        setIsDirty(false);

        if (!this.getVisible()) return;
        
        if (image == null) {
            generateChart();
        }
        
        if (image != null) {
            setIsDirty(false);
            
            Main.glPushAttrib(GL_POLYGON_BIT | GL_LINE_BIT | GL_ENABLE_BIT | GL_TRANSFORM_BIT);
            renderBufferedImage(image, bounds);
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

    public void setXAxisFormat(String format) {
        xAxisFormat = format;
    }
    
    public void newChart() {

        image = null;
        
        // Create Chart
        chart = new CategoryChartBuilder().width((int) bounds.width).height((int) bounds.height).title(getClass().getSimpleName()).xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();

        // Customize Chart
        CategoryStyler styler = chart.getStyler();
        
        styler.setChartTitleVisible(false);
        styler.setChartFontColor(new java.awt.Color(0.9f, 0.9f, 0.9f, 1f));
        styler.setChartBackgroundColor(new java.awt.Color(0.25f, 0.25f, 0.25f, 0.6f));
        styler.setLegendPosition(org.knowm.xchart.style.CategoryStyler.LegendPosition.InsideNE);
        styler.setLegendBackgroundColor(new java.awt.Color(0.35f, 0.35f, 0.35f, 0.5f));
        styler.setPlotGridHorizontalLinesVisible(true);
        styler.setPlotGridLinesColor(new java.awt.Color(0.2f, 0.2f, 0.2f, 0.55f));
        styler.setPlotGridVerticalLinesVisible(false);
        styler.setPlotBackgroundColor(new java.awt.Color(0.05f, 0.05f, 0.05f, 0.7f));
        styler.setAxisTitlesVisible(true);
        styler.setXAxisTickMarkSpacingHint(5);
        styler.setAxisTicksMarksVisible(true);
        styler.setAxisTickLabelsColor(new java.awt.Color(1f, 1f, 1f, 1f));
        styler.setXAxisDecimalPattern(xAxisFormat);
        styler.setAxisTickLabelsFont(new Font("Helvetica", Font.PLAIN | Font.TRUETYPE_FONT, 8));
        styler.setLegendVisible(false);
        
        styler.setAvailableSpaceFill(0.95f);
        styler.setOverlapped(true);
        
//        styler.setYAxisMax(100.0);

        setIsDirty(true);

    }

    public CategoryChart getChart() {
        return chart;
    }

    public void addSeries(String title, List<Double> xData, Vector4f color, int bins, float low, float high) {
//        Histogram histogram = new Histogram(xData, 35, 0f, 36f);
        Histogram histogram = new Histogram(xData, bins, low, high);
        
        CategorySeries series = chart.addSeries(title, histogram.getxAxisData(), histogram.getyAxisData());
        series.setShowInLegend(true);
    //    series.setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
        series.setFillColor(new java.awt.Color(color.x, color.y, color.z, color.w));
        series.setLineColor(new java.awt.Color(color.x*0.8f, color.y*0.8f, color.z*0.8f, color.w));
        
        List<Double> cumValues = new ArrayList<>();
        Iterator<Double> i = xData.iterator();
        while(i.hasNext()) {
            double val = i.next();
            for (int j=0; j<histogram.getNumBins(); j++) {
                double hval = histogram.getxAxisData().get(j);
                if (hval >= val) {
                    cumValues.add(hval);
                }
            }
        }
        Histogram cumhistogram = new Histogram(cumValues, bins, low, high);
        
        double max = 0;
        double cumMax = 0;
        
        for (int k=0; k<histogram.getNumBins(); k++) {
            double val = histogram.getyAxisData().get(k);
            if (val > max) max = val;
            
            val = cumhistogram.getyAxisData().get(k);
            if (val > cumMax) cumMax = val;
        }
        
        
        
        for (int k=0; k<histogram.getNumBins(); k++) {
            double val = cumhistogram.getyAxisData().get(k) * max / cumMax;
            cumhistogram.getyAxisData().set(k, val);
        }
        
//        cumhistogram = new Histogram(cumValues, 35, 0f, 36f);
        
        series = chart.addSeries("Cumulative", cumhistogram.getxAxisData(), cumhistogram.getyAxisData());
        series.setShowInLegend(true);
        series.setChartCategorySeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
        series.setMarker(SeriesMarkers.NONE);
       // series.setLineColor(new java.awt.Color(0.7f, 0.1f, 0.1f, 0.6f));
        series.setLineColor(new java.awt.Color(1f-color.x*0.7f, 1f-color.y*0.7f, 1f-color.z*0.7f, 0.8f));
        
        setIsDirty(true);

    }

    @Override
    public boolean OnMouse(float x, float y, boolean button1down, boolean button2down, int dwheel) {
        if (super.OnMouse(x, y, button1down, button2down, dwheel)) {
            return true;
        }
        else if (this.MouseIsInside(x, y) && (button1down || button2down)) {
            // eat all mouse clicks
            return true;
        }
        else {
            return false;
        }
    }

    public void generateChart() {
        image = new BufferedImage(Math.round(bounds.width), Math.round(bounds.height), BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D gc = (Graphics2D) image.getGraphics();
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        chart.paint(gc, image.getWidth(), image.getHeight());
    }

}
