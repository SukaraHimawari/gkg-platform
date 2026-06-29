package edu.gkg.view.chart;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TrendLineChart extends JPanel {

    private final TimeSeriesCollection dataset = new TimeSeriesCollection();
    private final JFreeChart chart;
    private final ChartPanel chartPanel;

    public TrendLineChart() {
        setLayout(new BorderLayout());
        setOpaque(false);
        chart = ChartFactory.createTimeSeriesChart(
                null, null, null, dataset, true, true, false);
        applyTheme(chart);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBackground(Theme.BG_CARD);
        add(chartPanel, BorderLayout.CENTER);
    }

    public void renderFrequencySeries(String label, List<DateTone> points) {
        dataset.removeAllSeries();
        TimeSeries ts = new TimeSeries(label);
        for (DateTone p : points) {
            ts.addOrUpdate(new Day(java.sql.Date.valueOf(p.date())), p.recordCount());
        }
        dataset.addSeries(ts);
    }

    public void renderToneSeries(String label, List<DateTone> points) {
        dataset.removeAllSeries();
        TimeSeries ts = new TimeSeries(label);
        for (DateTone p : points) {
            ts.addOrUpdate(new Day(java.sql.Date.valueOf(p.date())), p.avgTone());
        }
        dataset.addSeries(ts);
    }

    /** 统一图表主题：卡片背景、中文字体、品牌色折线和简洁网格。 */
    public static void applyTheme(JFreeChart chart) {
        chart.setBackgroundPaint(Theme.BG_CARD);
        chart.setBorderVisible(false);
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(Theme.FONT_SECTION);
            chart.getTitle().setPaint(Theme.TEXT_PRIMARY);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(Theme.FONT_LABEL);
            chart.getLegend().setItemPaint(Theme.TEXT_SECONDARY);
            chart.getLegend().setBackgroundPaint(Theme.BG_CARD);
            chart.getLegend().setFrame(org.jfree.chart.block.BlockBorder.NONE);
        }
        if (chart.getPlot() instanceof XYPlot plot) {
            plot.setBackgroundPaint(Theme.BG_CARD);
            plot.setDomainGridlinesVisible(false);
            plot.setRangeGridlinePaint(Theme.BORDER_LIGHT);
            plot.setOutlineVisible(false);

            if (plot.getRenderer() instanceof XYLineAndShapeRenderer r) {
                for (int i = 0; i < Theme.CHART_PALETTE.length; i++) {
                    r.setSeriesPaint(i, Theme.CHART_PALETTE[i]);
                    r.setSeriesStroke(i, new BasicStroke(2f));
                    r.setSeriesShapesVisible(i, true);
                    r.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
                }
            }
            plot.getDomainAxis().setLabelFont(Theme.FONT_LABEL);
            plot.getDomainAxis().setTickLabelFont(Theme.FONT_LABEL);
            plot.getDomainAxis().setLabelPaint(Theme.TEXT_SECONDARY);
            plot.getDomainAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
            plot.getRangeAxis().setLabelFont(Theme.FONT_LABEL);
            plot.getRangeAxis().setTickLabelFont(Theme.FONT_LABEL);
            plot.getRangeAxis().setLabelPaint(Theme.TEXT_SECONDARY);
            plot.getRangeAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
            if (plot.getDomainAxis() instanceof DateAxis da) {
                da.setVerticalTickLabels(false);
            }
        }
    }

    /** 保留旧接口名，兼容已有调用。 */
    static void applyChineseFont(JFreeChart chart) { applyTheme(chart); }
}
