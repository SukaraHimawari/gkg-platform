package edu.gkg.view;

import edu.gkg.common.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class QueryPanel extends JPanel {

    public QueryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);

        JTabbedPane sub = new JTabbedPane();
        sub.setFont(Theme.FONT_DEFAULT);
        sub.putClientProperty("JTabbedPane.tabType", "underlined");
        sub.addTab("组合查询",  buildSearchTab());
        sub.addTab("人物档案",  buildProfileTab("人物"));
        sub.addTab("组织档案",  buildProfileTab("组织"));
        sub.addTab("主题追踪",  new ThemeTrackPanel());
        add(sub, BorderLayout.CENTER);
    }

    // ---------- 组合查询 ----------
    private JComponent buildSearchTab() {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card cond = new Card("查询条件");
        JPanel grid = new JPanel(new GridLayout(2, 6, Theme.SPACE_SM, Theme.SPACE_SM));
        grid.setOpaque(false);
        grid.add(UiUtil.mutedLabel("起始日期")); grid.add(new JTextField("2024-01-15"));
        grid.add(UiUtil.mutedLabel("结束日期")); grid.add(new JTextField("2024-01-15"));
        grid.add(UiUtil.mutedLabel("主题代码")); grid.add(new JTextField());
        grid.add(UiUtil.mutedLabel("人物名"));   grid.add(new JTextField());
        grid.add(UiUtil.mutedLabel("组织名"));   grid.add(new JTextField());
        grid.add(UiUtil.mutedLabel("地点名"));   grid.add(new JTextField());

        JPanel condBody = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        condBody.setOpaque(false);
        condBody.add(grid, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        actions.setOpaque(false);
        actions.add(UiUtil.secondaryButton("重置"));
        actions.add(UiUtil.primaryButton("搜索"));
        actions.add(UiUtil.secondaryButton("导出结果"));
        condBody.add(actions, BorderLayout.SOUTH);
        cond.body(condBody);

        Card resultCard = new Card("查询结果");
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"记录ID", "发布时间", "媒体", "情感", "URL"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (int i = 0; i < 12; i++) {
            model.addRow(new Object[]{
                    "20240115" + String.format("%06d", i * 1500) + "-T1",
                    "2024-01-15 " + String.format("%02d:00", i * 2 % 24),
                    new String[]{"Reuters", "CNN", "BBC", "AP", "Bloomberg"}[i % 5],
                    String.format("%.2f", (Math.random() - 0.5) * 8),
                    "https://example.com/news/" + i
            });
        }
        JTable table = new JTable(model);
        UiUtil.styleTable(table);
        JPanel resBody = new JPanel(new BorderLayout(0, Theme.SPACE_SM));
        resBody.setOpaque(false);
        resBody.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SPACE_SM, 0));
        pager.setOpaque(false);
        pager.add(UiUtil.secondaryButton("« 上一页"));
        pager.add(UiUtil.mutedLabel("第 1 页 / 共 488 页"));
        pager.add(UiUtil.secondaryButton("下一页 »"));
        resBody.add(pager, BorderLayout.SOUTH);
        resultCard.body(resBody);

        wrap.add(cond,       BorderLayout.NORTH);
        wrap.add(resultCard, BorderLayout.CENTER);
        return wrap;
    }

    // ---------- 人物/组织档案 ----------
    private JComponent buildProfileTab(String entityWord) {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card searchCard = new Card(entityWord + "搜索");
        JTextField nameField = new JTextField("Elon Musk", 24);
        JButton    searchBtn = UiUtil.primaryButton("查询");
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        searchRow.setOpaque(false);
        searchRow.add(nameField);
        searchRow.add(searchBtn);
        searchCard.body(searchRow);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, Theme.SPACE_LG, 0));
        statsRow.setOpaque(false);
        statsRow.add(new StatCard("新闻量",     Theme.BRAND,   "#").value("48,762"));
        statsRow.add(new StatCard("平均情感",   Theme.SUCCESS, "+").value("0.28").hint("正向"));
        statsRow.add(new StatCard("关联主题",   Theme.WARN,    "T").value("152"));
        statsRow.add(new StatCard("正向报道比", Theme.BRAND,   "%").value("1.87%"));

        Card relCard = new Card("关联关系 TOP10");
        JPanel rel = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
        rel.setOpaque(false);
        rel.add(buildTopBar("关联组织 TOP10",
                new String[]{"SpaceX", "Tesla", "X (Twitter)", "Neuralink", "OpenAI",
                        "The Boring Co", "PayPal", "SolarCity", "NASA", "Korea Co."},
                new double[]{2432, 2087, 1235, 985, 770, 645, 623, 498, 431, 412}));
        rel.add(buildTopBar("关联人物 TOP10",
                new String[]{"Donald Trump", "Jeff Bezos", "Mark Zuckerberg", "Larry Page",
                        "Sergey Brin", "Tim Cook", "Jack Dorsey", "Bill Gates", "Sam Altman", "Kimbal Musk"},
                new double[]{1982, 1256, 1103, 987, 875, 711, 623, 511, 431, 389}));
        relCard.body(rel);

        Card timelineCard = new Card("时间分布（按月）");
        timelineCard.body(buildMiniTimeChart());

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        relCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        timelineCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(statsRow);
        center.add(Box.createVerticalStrut(Theme.SPACE_LG));
        center.add(relCard);
        center.add(Box.createVerticalStrut(Theme.SPACE_LG));
        center.add(timelineCard);

        wrap.add(searchCard, BorderLayout.NORTH);
        wrap.add(new JScrollPane(center, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {{
            setBorder(null); setOpaque(false); getViewport().setOpaque(false);
        }}, BorderLayout.CENTER);
        return wrap;
    }

    private ChartPanel buildTopBar(String title, String[] cats, double[] vals) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (int i = 0; i < cats.length; i++) ds.addValue(vals[i], "频次", cats[i]);
        JFreeChart chart = ChartFactory.createBarChart(
                title, null, null, ds, PlotOrientation.HORIZONTAL, false, true, false);
        chart.setBackgroundPaint(Theme.BG_CARD);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Theme.BG_CARD);
        plot.setRangeGridlinePaint(Theme.BORDER_LIGHT);
        plot.setOutlineVisible(false);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, Theme.BRAND);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.1);

        plot.getDomainAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getDomainAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
        plot.getRangeAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getRangeAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
        chart.getTitle().setFont(Theme.FONT_SECTION);
        chart.getTitle().setPaint(Theme.TEXT_PRIMARY);

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(0, 280));
        return cp;
    }

    private ChartPanel buildMiniTimeChart() {
        TimeSeries ts = new TimeSeries("新闻量");
        int[] vals = {800, 1200, 1500, 1100, 1800, 2400, 2900, 3200, 2800, 3500, 4100, 3800};
        for (int m = 0; m < 12; m++) ts.addOrUpdate(new Day(1, m + 1, 2024), vals[m]);
        TimeSeriesCollection coll = new TimeSeriesCollection();
        coll.addSeries(ts);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null, null, null, coll, false, true, false);
        chart.setBackgroundPaint(Theme.BG_CARD);
        org.jfree.chart.plot.XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Theme.BG_CARD);
        plot.setRangeGridlinePaint(Theme.BORDER_LIGHT);
        plot.setDomainGridlinesVisible(false);
        plot.setOutlineVisible(false);
        plot.getRenderer().setSeriesPaint(0, Theme.BRAND);
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2f));
        plot.getDomainAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getDomainAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);
        plot.getRangeAxis().setTickLabelFont(Theme.FONT_LABEL);
        plot.getRangeAxis().setTickLabelPaint(Theme.TEXT_SECONDARY);

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(0, 200));
        return cp;
    }
}
