package edu.gkg.view.chart;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.Inflection;
import edu.gkg.common.SharedRecords.SentimentSeries;
import edu.gkg.common.Theme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.*;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class SentimentDashboard extends JPanel {

    private final DefaultValueDataset dialDataset = new DefaultValueDataset(0);
    private final TimeSeriesCollection trendDataset = new TimeSeriesCollection();
    private final RadarPanel radar = new RadarPanel();
    private XYPlot trendPlot;

    public SentimentDashboard() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel top = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
        top.setOpaque(false);
        top.add(buildDial());
        top.add(radar);

        ChartPanel trendChartPanel = buildTrendChart();
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, trendChartPanel);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(Theme.SPACE_LG);
        split.setDividerLocation(280);
        split.setResizeWeight(0.45);
        add(split, BorderLayout.CENTER);
        radar.setValues(new double[]{0, 0, 0, 0, 0, 0});
    }

    private ChartPanel buildDial() {
        DialPlot plot = new DialPlot();
        plot.setView(0, 0, 1, 1);
        plot.setDataset(dialDataset);
        plot.setDialFrame(new StandardDialFrame() {{ setBackgroundPaint(Theme.BG_CARD); setForegroundPaint(Theme.BORDER_LIGHT); }});
        plot.setBackground(new DialBackground(Theme.BG_CARD));

        StandardDialScale scale = new StandardDialScale(-10, 10, -120, -300, 5, 4);
        scale.setTickRadius(0.88);
        scale.setTickLabelOffset(0.20);
        scale.setTickLabelFont(Theme.FONT_LABEL);
        scale.setTickLabelPaint(Theme.TEXT_SECONDARY);
        scale.setMajorTickPaint(Theme.TEXT_MUTED);
        scale.setMinorTickPaint(Theme.BORDER_LIGHT);
        plot.addScale(0, scale);

        DialPointer.Pin pin = new DialPointer.Pin();
        pin.setRadius(0.85);
        pin.setPaint(Theme.BRAND);
        plot.addLayer(pin);

        DialValueIndicator val = new DialValueIndicator(0);
        val.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 16));
        val.setPaint(Theme.TEXT_PRIMARY);
        val.setBackgroundPaint(Theme.BG_CARD);
        val.setOutlinePaint(Theme.BORDER_LIGHT);
        plot.addLayer(val);

        JFreeChart chart = new JFreeChart("V2Tone 情感", plot);
        chart.setBackgroundPaint(Theme.BG_CARD);
        chart.setBorderVisible(false);
        chart.getTitle().setFont(Theme.FONT_SECTION);
        chart.getTitle().setPaint(Theme.TEXT_PRIMARY);

        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(Theme.BG_CARD);
        return cp;
    }

    private ChartPanel buildTrendChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "情感趋势", null, null, trendDataset, false, true, false);
        TrendLineChart.applyTheme(chart);
        trendPlot = (XYPlot) chart.getPlot();
        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(Theme.BG_CARD);
        return cp;
    }

    public void renderSeries(SentimentSeries series, String label) {
        trendDataset.removeAllSeries();
        TimeSeries ts = new TimeSeries(label);
        double sum = 0;
        for (DateTone p : series.series()) {
            ts.addOrUpdate(new Day(java.sql.Date.valueOf(p.date())), p.avgTone());
            sum += p.avgTone();
        }
        trendDataset.addSeries(ts);

        dialDataset.setValue(series.series().isEmpty() ? 0 : sum / series.series().size());

        if (trendPlot.getRenderer() instanceof XYLineAndShapeRenderer r) {
            r.setSeriesPaint(0, Theme.BRAND);
            r.setSeriesStroke(0, new BasicStroke(2f));
            r.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
            r.setSeriesShapesVisible(0, true);
        }

        trendPlot.clearAnnotations();
        for (Inflection ip : series.inflectionPoints()) {
            double x = new Day(java.sql.Date.valueOf(ip.date())).getFirstMillisecond();
            DateTone hit = series.series().stream()
                    .filter(s -> s.date().equals(ip.date())).findFirst().orElse(null);
            double y = hit == null ? 0 : hit.avgTone();
            XYPointerAnnotation ann = new XYPointerAnnotation(
                    String.format("%s %.1f", ip.direction(), ip.magnitude()),
                    x, y, 7 * Math.PI / 4);
            ann.setPaint(Theme.SENT_NEG);
            ann.setArrowPaint(Theme.SENT_NEG);
            ann.setBackgroundPaint(Theme.DANGER_SOFT);
            ann.setFont(Theme.FONT_LABEL);
            ann.setTextAnchor(TextAnchor.BOTTOM_LEFT);
            trendPlot.addAnnotation(ann);
        }
        radar.setValues(buildRadarValues(series));
    }

    private static double[] buildRadarValues(SentimentSeries s) {
        if (s.series().isEmpty()) return new double[]{0, 0, 0, 0, 0, 0};
        double avg = 0, pos = 0, neg = 0, vol = 0, ipCount = s.inflectionPoints().size(), peak = 0;
        for (DateTone p : s.series()) {
            avg += p.avgTone();
            if (p.avgTone() > 0) pos += p.avgTone();
            else                 neg += -p.avgTone();
            peak = Math.max(peak, Math.abs(p.avgTone()));
        }
        avg /= s.series().size();
        for (DateTone p : s.series()) vol += Math.abs(p.avgTone() - avg);
        vol /= s.series().size();
        return new double[]{ norm(avg + 10, 20), norm(pos, s.series().size() * 5.0),
                norm(neg, s.series().size() * 5.0), norm(vol, 5),
                norm(ipCount, 5), norm(peak, 10) };
    }

    private static double norm(double v, double max) {
        return Math.max(0, Math.min(1, v / max));
    }

    /** 自绘六轴情感雷达图。 */
    static class RadarPanel extends JPanel {
        private double[] values = {0, 0, 0, 0, 0, 0};
        private final String[] axes = {"均值", "正向", "负向", "波动", "拐点数", "峰值"};

        RadarPanel() {
            setBackground(Theme.BG_CARD);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 0, Theme.BORDER_LIGHT),
                    BorderFactory.createEmptyBorder(Theme.SPACE_LG, Theme.SPACE_LG,
                            Theme.SPACE_LG, Theme.SPACE_LG)));
        }

        void setValues(double[] v) { this.values = v; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g.setFont(Theme.FONT_SECTION);
            g.setColor(Theme.TEXT_PRIMARY);
            g.drawString("情感雷达", 4, 18);

            int cx = w / 2, cy = h / 2 + 14;
            int r = Math.min(w, h) / 2 - 56;
            int n = values.length;

            g.setStroke(new BasicStroke(1));
            g.setColor(Theme.BORDER_LIGHT);
            for (int level = 1; level <= 4; level++) {
                Path2D ring = ringPath(cx, cy, r * level / 4.0, n);
                g.draw(ring);
            }
            g.setColor(Theme.BORDER_LIGHT);
            for (int i = 0; i < n; i++) {
                Point2D p = polar(cx, cy, r, i, n);
                g.draw(new java.awt.geom.Line2D.Double(cx, cy, p.getX(), p.getY()));
            }

            Path2D poly = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                Point2D p = polar(cx, cy, r * values[i], i, n);
                if (i == 0) poly.moveTo(p.getX(), p.getY()); else poly.lineTo(p.getX(), p.getY());
            }
            poly.closePath();
            g.setColor(new Color(Theme.BRAND.getRed(), Theme.BRAND.getGreen(),
                    Theme.BRAND.getBlue(), 60));
            g.fill(poly);
            g.setColor(Theme.BRAND);
            g.setStroke(new BasicStroke(2));
            g.draw(poly);

            g.setFont(Theme.FONT_LABEL);
            g.setColor(Theme.TEXT_SECONDARY);
            for (int i = 0; i < n; i++) {
                Point2D p = polar(cx, cy, r + 16, i, n);
                int sw = g.getFontMetrics().stringWidth(axes[i]);
                g.drawString(axes[i], (float) (p.getX() - sw / 2.0), (float) p.getY());
            }
            g.dispose();
        }

        private Path2D ringPath(int cx, int cy, double r, int n) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                Point2D pt = polar(cx, cy, r, i, n);
                if (i == 0) p.moveTo(pt.getX(), pt.getY());
                else        p.lineTo(pt.getX(), pt.getY());
            }
            p.closePath();
            return p;
        }

        private Point2D polar(int cx, int cy, double r, int i, int n) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / n;
            return new Point2D.Double(cx + r * Math.cos(a), cy + r * Math.sin(a));
        }
    }
}
