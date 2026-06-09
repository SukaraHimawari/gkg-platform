package edu.gkg.view.chart;

import edu.gkg.common.Theme;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.DefaultXYZDataset;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeHeatmap extends JPanel {

    public ThemeHeatmap() {
        setLayout(new BorderLayout());
        setOpaque(false);
        rebuild(sampleThemes(), sampleBuckets(), sampleMatrix());
    }

    public void rebuild(List<String> themes, List<String> buckets, double[][] heat) {
        removeAll();
        DefaultXYZDataset ds = new DefaultXYZDataset();
        ds.addSeries("count", toSeries(heat));

        NumberAxis xAxis = new SymbolAxis("时间", buckets.toArray(new String[0]));
        NumberAxis yAxis = new SymbolAxis("主题", themes.toArray(new String[0]));
        xAxis.setLowerMargin(0); xAxis.setUpperMargin(0);
        yAxis.setLowerMargin(0); yAxis.setUpperMargin(0);

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockHeight(1.0);
        renderer.setBlockWidth(1.0);

        double max = 1;
        for (double[] row : heat) for (double v : row) if (v > max) max = v;
        LookupPaintScale scale = new LookupPaintScale(0, max + 1, Theme.BG_APP);
        for (int i = 0; i <= 9; i++) {
            float t = i / 9f;
            scale.add(max * t, blend(Theme.BG_APP, Theme.BRAND, t));
        }
        renderer.setPaintScale(scale);

        XYPlot plot = new XYPlot(ds, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Theme.BG_CARD);
        plot.setRangeGridlinePaint(Theme.BORDER_LIGHT);
        plot.setOutlineVisible(false);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.setBackgroundPaint(Theme.BG_CARD);
        chart.setBorderVisible(false);

        NumberAxis legendAxis = new NumberAxis();
        legendAxis.setTickLabelFont(Theme.FONT_LABEL);
        legendAxis.setTickLabelPaint(Theme.TEXT_SECONDARY);
        PaintScaleLegend legend = new PaintScaleLegend(scale, legendAxis);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setMargin(8, 16, 8, 8);
        legend.setBackgroundPaint(Theme.BG_CARD);
        legend.setFrame(new BlockBorder(Theme.BORDER_LIGHT));
        chart.addSubtitle(legend);

        TrendLineChart.applyTheme(chart);
        plot.getDomainAxis().setLabelFont(Theme.FONT_LABEL);
        plot.getDomainAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getRangeAxis().setLabelFont(Theme.FONT_LABEL);
        plot.getRangeAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getDomainAxis().setLabelPaint(Theme.TEXT_SECONDARY);
        plot.getDomainAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
        plot.getRangeAxis().setLabelPaint(Theme.TEXT_SECONDARY);
        plot.getRangeAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);

        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(Theme.BG_CARD);
        add(cp, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    private static double[][] toSeries(double[][] heat) {
        int rows = heat.length;
        int cols = rows == 0 ? 0 : heat[0].length;
        double[] x = new double[rows * cols];
        double[] y = new double[rows * cols];
        double[] z = new double[rows * cols];
        int k = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                x[k] = c; y[k] = r; z[k] = heat[r][c];
                k++;
            }
        }
        return new double[][]{x, y, z};
    }

    private static Color blend(Color a, Color b, float t) {
        int r = Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl= Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }

    private static List<String> sampleThemes() {
        return List.of("SANCTIONS", "PROTEST", "ELECTION", "HEALTH", "CYBER", "CLIMATE");
    }
    private static List<String> sampleBuckets() {
        List<String> b = new ArrayList<>();
        for (int h = 0; h < 24; h += 2) b.add(String.format("%02d:00", h));
        return b;
    }
    private static double[][] sampleMatrix() {
        int rows = 6, cols = 12;
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                m[r][c] = (Math.sin(r + c / 2.0) + 1.2) * (10 + r * 5);
        return m;
    }
}
