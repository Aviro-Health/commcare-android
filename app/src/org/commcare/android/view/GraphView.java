package org.commcare.android.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.commcare.dalvik.R;
import org.commcare.suite.model.graph.AnnotationData;
import org.commcare.suite.model.graph.BubblePointData;
import org.commcare.suite.model.graph.Graph;
import org.commcare.suite.model.graph.GraphData;
import org.commcare.suite.model.graph.SeriesData;
import org.commcare.suite.model.graph.XYPointData;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.LinearLayout;

/*
 * View containing a graph. Note that this does not derive from View; call renderView to get a view for adding to other views, etc.
 * @author jschweers
 */
public class GraphView {
    private static final int TEXT_SIZE = 21;

    private Context mContext;
    private GraphData mData;
    private XYMultipleSeriesDataset mDataset;
    private XYMultipleSeriesRenderer mRenderer; 

    public GraphView(Context context) {
        mContext = context;
        mDataset = new XYMultipleSeriesDataset();
        mRenderer = new XYMultipleSeriesRenderer();
    } 
    
    /*
     * Set title of graph, and adjust spacing accordingly. Caller should re-render afterwards.
     */
    public void setTitle(String title) {
        mRenderer.setChartTitle(title);
        mRenderer.setChartTitleTextSize(TEXT_SIZE);
    }
    
    /*
     * Set margins.
     */
    private void setMargins() {
        int textAllowance = (int) mContext.getResources().getDimension(R.dimen.graph_text_margin);
        int topMargin = (int) mContext.getResources().getDimension(R.dimen.graph_y_margin);
        if (!mRenderer.getChartTitle().equals("")) {
            topMargin += textAllowance;
        }
        int rightMargin = (int) mContext.getResources().getDimension(R.dimen.graph_x_margin);
        int leftMargin = (int) mContext.getResources().getDimension(R.dimen.graph_x_margin);
        if (!mRenderer.getYTitle().equals("")) {
            leftMargin += textAllowance;
        }
        int bottomMargin = (int) mContext.getResources().getDimension(R.dimen.graph_y_margin);
        if (!mRenderer.getXTitle().equals("")) {
            bottomMargin += textAllowance;
        }
        mRenderer.setMargins(new int[]{topMargin, leftMargin, bottomMargin, rightMargin});
    }
        
    /*
     * Get a View object that will display this graph. This should be called after making
     * any changes to graph's configuration, title, etc.
     */
    public View renderView(GraphData data) {
        mData = data;
        mRenderer.setInScroll(true);
        for (SeriesData s : data.getSeries()) {
            XYSeriesRenderer currentRenderer = new XYSeriesRenderer();
            mRenderer.addSeriesRenderer(currentRenderer);
            
            configureSeries(s, currentRenderer);
            renderSeries(s);
        }
        
        renderAnnotations();

        configure();
        setMargins();
        
        if (mData.getType().equals(Graph.TYPE_BUBBLE)) {
            return ChartFactory.getBubbleChartView(mContext, mDataset, mRenderer);
        }
        return ChartFactory.getLineChartView(mContext, mDataset, mRenderer);
    }
    
    /*
     * Set up a single series.
     */
    private void renderSeries(SeriesData s) {
        XYSeries series;
        if (mData.getType().equals(Graph.TYPE_BUBBLE)) {
            series = new RangeXYValueSeries("");
            if (s.getConfiguration("radius-max") != null) {
                ((RangeXYValueSeries) series).setMaxValue(Double.valueOf(s.getConfiguration("radius-max")));
            }
        }
        else {
            series = new XYSeries("");
        }
        mDataset.addSeries(series);

        // Bubble charts will throw an index out of bounds exception if given points out of order
        Vector<XYPointData> sortedPoints = new Vector<XYPointData>(s.size());
        for (XYPointData d : s.getPoints()) {
            sortedPoints.add(d);
        }
        Collections.sort(sortedPoints, new PointComparator());
        
        for (XYPointData p : sortedPoints) {
            if (mData.getType().equals(Graph.TYPE_BUBBLE)) {
                BubblePointData b = (BubblePointData) p;
                ((RangeXYValueSeries) series).add(b.getX(), b.getY(), b.getRadius());
            }
            else {
                series.add(p.getX(), p.getY());
            }
        }
    }
    
    /*
     * Get layout params for this graph, which assume that graph will fill parent
     * unless dimensions have been provided via setWidth and/or setHeight.
     */
    public LinearLayout.LayoutParams getLayoutParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);    
    }
    
    /*
     * Set up any annotations.
     */
    private void renderAnnotations() {
        Vector<AnnotationData> annotations = mData.getAnnotations();
        if (!annotations.isEmpty()) {
            // Create a fake series for the annotations
            XYSeries series = new XYSeries("");
            for (AnnotationData a : annotations) {
                series.addAnnotation(a.getAnnotation(), a.getX(), a.getY());
            }
            
            // Annotations won't display unless the series has some data in it
            series.add(0.0, 0.0);

            mDataset.addSeries(series);
            XYSeriesRenderer currentRenderer = new XYSeriesRenderer();
            currentRenderer.setAnnotationsTextSize(TEXT_SIZE);
            currentRenderer.setAnnotationsColor(mContext.getResources().getColor(R.drawable.black));
            mRenderer.addSeriesRenderer(currentRenderer);
        }
    }

    /*
     * Apply any user-requested look and feel changes to graph.
     */
    private void configureSeries(SeriesData s, XYSeriesRenderer currentRenderer) {
        // Default to circular points, but allow Xs or no points at all
        String pointStyle = s.getConfiguration("point-style", "circle").toLowerCase();
        if (!pointStyle.equals("none")) {
            PointStyle style = null;
            if (pointStyle.equals("circle")) {
                style = PointStyle.CIRCLE;
            }
            else if (pointStyle.equals("x")) {
                style = PointStyle.X;
            }
            currentRenderer.setPointStyle(style);
            currentRenderer.setFillPoints(true);
        }
        
        String lineColor = s.getConfiguration("line-color", "#ff000000");
        currentRenderer.setColor(Color.parseColor(lineColor));
        
        fillOutsideLine(s, currentRenderer, "fill-above", XYSeriesRenderer.FillOutsideLine.Type.ABOVE);
        fillOutsideLine(s, currentRenderer, "fill-below", XYSeriesRenderer.FillOutsideLine.Type.BELOW);
    }
    
    /*
     * Helper function for setting up color fills above or below a series.
     */
    private void fillOutsideLine(SeriesData s, XYSeriesRenderer currentRenderer, String property, XYSeriesRenderer.FillOutsideLine.Type type) {
        property = s.getConfiguration(property);
        if (property != null) {
            XYSeriesRenderer.FillOutsideLine fill = new XYSeriesRenderer.FillOutsideLine(type);
            fill.setColor(Color.parseColor(property));
            currentRenderer.addFillOutsideLine(fill);
        }
    }

    /*
     * Configure graph's look and feel based on default assumptions and user-requested configuration.
     */
    private void configure() {
        // Default options
        mRenderer.setBackgroundColor(mContext.getResources().getColor(R.drawable.white));
        mRenderer.setMarginsColor(mContext.getResources().getColor(R.drawable.white));
        mRenderer.setLabelsColor(mContext.getResources().getColor(R.drawable.black));
        mRenderer.setXLabelsColor(mContext.getResources().getColor(R.drawable.black));
        mRenderer.setYLabelsColor(0, mContext.getResources().getColor(R.drawable.black));
        mRenderer.setXLabelsAlign(Paint.Align.CENTER);
        mRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        mRenderer.setYLabelsPadding(10);
        mRenderer.setAxesColor(mContext.getResources().getColor(R.drawable.black));
        mRenderer.setLabelsTextSize(TEXT_SIZE);
        mRenderer.setAxisTitleTextSize(TEXT_SIZE);
        mRenderer.setShowLabels(true);
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setShowLegend(false);
        mRenderer.setShowGrid(true);

        boolean panAndZoom = Boolean.valueOf(mData.getConfiguration("zoom", "false")).equals(Boolean.TRUE);
        mRenderer.setPanEnabled(panAndZoom);
        mRenderer.setZoomEnabled(panAndZoom);
        mRenderer.setZoomButtonsVisible(panAndZoom);

        // User-configurable options
        mRenderer.setXTitle(mData.getConfiguration("x-axis-title", ""));
        mRenderer.setYTitle(mData.getConfiguration("y-axis-title", ""));

        if (mData.getConfiguration("x-axis-min") != null) {
            mRenderer.setXAxisMin(Double.valueOf(mData.getConfiguration("x-axis-min")));
        }
        if (mData.getConfiguration("y-axis-min") != null) {
            mRenderer.setYAxisMin(Double.valueOf(mData.getConfiguration("y-axis-min")));
        }
        
        if (mData.getConfiguration("x-axis-max") != null) {
            mRenderer.setXAxisMax(Double.valueOf(mData.getConfiguration("x-axis-max")));
        }
        if (mData.getConfiguration("y-axis-max") != null) {
            mRenderer.setYAxisMax(Double.valueOf(mData.getConfiguration("y-axis-max")));
        }
        
        String showGrid = mData.getConfiguration("show-grid", "true");
        if (Boolean.valueOf(showGrid).equals(Boolean.FALSE)) {
            mRenderer.setShowGridX(false);
            mRenderer.setShowGridY(false);
        }

        String showAxes = mData.getConfiguration("show-axes", "true");
        if (Boolean.valueOf(showAxes).equals(Boolean.FALSE)) {
            mRenderer.setShowAxes(false);
        }
        
        Integer xLabelCount = mData.getConfiguration("x-label-count") == null ? null : new Integer(mData.getConfiguration("x-label-count"));
        Integer yLabelCount = mData.getConfiguration("y-label-count") == null ? null : new Integer(mData.getConfiguration("y-label-count"));
        if (xLabelCount == 0 && yLabelCount == 0) {
            mRenderer.setShowLabels(false);
        }
        else {
            if (xLabelCount != null) {
                mRenderer.setXLabels(Integer.valueOf(xLabelCount));
            }
            if (yLabelCount != null) {
                mRenderer.setYLabels(Integer.valueOf(yLabelCount));
            }
        }
    }
    
    /**
     * Comparator to sort PointData objects by x value.
     * @author jschweers
     */
    private class PointComparator implements Comparator<XYPointData> {
        @Override
        public int compare(XYPointData lhs, XYPointData rhs) {
            if (lhs.getX() > rhs.getX()) {
                return 1;
            }
            if (lhs.getX() < rhs.getX()) {
                return -1;
            }
            return 0;
        }
    }
    
    /*
     * Subclass of XYValueSeries allowing user to set a maximum radius.
     * Useful when creating multiple bubble charts (or series on the same chart)
     * and wanting their bubbles to be on the same scale.
     */
    private class RangeXYValueSeries extends XYValueSeries {
        private Double max = null;

        public RangeXYValueSeries(String title) {
            super(title);
        }
        
        /*
         * (non-Javadoc)
         * @see org.achartengine.model.XYValueSeries#getMaxValue()
         */
        @Override
        public double getMaxValue() {
            return max == null ? super.getMaxValue() : max;
        }
        
        /*
         * Set largest desired radius. No guarantees on what happens if the data
         * actually contains a larger value.
         */
        public void setMaxValue(double value) {
            max = value;
        }
    }
}
