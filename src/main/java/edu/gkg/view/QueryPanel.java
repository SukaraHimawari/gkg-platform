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

    // ---------- 组合查询控件（供 QueryController 绑定）----------
    public final JTextField dateFromField   = new JTextField("2024-01-15");
    public final JTextField dateToField     = new JTextField("2024-01-15");
    public final JTextField themeCodeField  = new JTextField();
    public final JTextField personNameField = new JTextField();
    public final JTextField orgNameField    = new JTextField();
    public final JTextField locationNameField = new JTextField();
    public final JButton    searchButton    = UiUtil.primaryButton("搜索");
    public final JButton    resetButton     = UiUtil.secondaryButton("重置");
    public final JButton    exportButton    = UiUtil.secondaryButton("导出结果");
    public final DefaultTableModel resultTableModel = new DefaultTableModel(
            new Object[]{"记录ID", "发布时间", "媒体", "情感", "URL"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    public final JLabel  pageLabel  = new JLabel("第 1 页 / 共 1 页");
    public final JButton prevButton = UiUtil.secondaryButton("« 上一页");
    public final JButton nextButton = UiUtil.secondaryButton("下一页 »");

    // ---------- 人物/组织档案控件（供 QueryController 绑定）----------
    public final JTextField personSearchField  = new JTextField("Elon Musk", 24);
    public final JButton    personSearchButton = UiUtil.primaryButton("查询");
    public final JTextField orgSearchField     = new JTextField("Tesla", 24);
    public final JButton    orgSearchButton    = UiUtil.primaryButton("查询");

    // 人物档案 StatCard
    public final StatCard personNewsCard  = new StatCard("新闻量",     Theme.BRAND,   "#").value("—");
    public final StatCard personToneCard  = new StatCard("平均情感",   Theme.SUCCESS, "+").value("—");
    public final StatCard personThemeCard = new StatCard("关联主题",   Theme.WARN,    "T").value("—");
    public final StatCard personRatioCard = new StatCard("正向报道比", Theme.BRAND,   "%").value("—");

    // 组织档案 StatCard
    public final StatCard orgNewsCard  = new StatCard("新闻量",     Theme.BRAND,   "#").value("—");
    public final StatCard orgToneCard  = new StatCard("平均情感",   Theme.SUCCESS, "+").value("—");
    public final StatCard orgThemeCard = new StatCard("关联主题",   Theme.WARN,    "T").value("—");
    public final StatCard orgRatioCard = new StatCard("正向报道比", Theme.BRAND,   "%").value("—");

    public QueryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);

        JTabbedPane sub = new JTabbedPane();
        sub.setFont(Theme.FONT_DEFAULT);
        sub.putClientProperty("JTabbedPane.tabType", "underlined");
        sub.addTab("组合查询",  buildSearchTab());
        sub.addTab("人物档案",  buildPersonTab());
        sub.addTab("组织档案",  buildOrgTab());
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
        grid.add(UiUtil.mutedLabel("起始日期")); grid.add(dateFromField);
        grid.add(UiUtil.mutedLabel("结束日期")); grid.add(dateToField);
        grid.add(UiUtil.mutedLabel("主题代码")); grid.add(themeCodeField);
        grid.add(UiUtil.mutedLabel("人物名"));   grid.add(personNameField);
        grid.add(UiUtil.mutedLabel("组织名"));   grid.add(orgNameField);
        grid.add(UiUtil.mutedLabel("地点名"));   grid.add(locationNameField);

        JPanel condBody = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        condBody.setOpaque(false);
        condBody.add(grid, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        actions.setOpaque(false);
        actions.add(resetButton);
        actions.add(searchButton);
        actions.add(exportButton);
        condBody.add(actions, BorderLayout.SOUTH);
        cond.body(condBody);

        Card resultCard = new Card("查询结果");
        JTable table = new JTable(resultTableModel);
        UiUtil.styleTable(table);
        JPanel resBody = new JPanel(new BorderLayout(0, Theme.SPACE_SM));
        resBody.setOpaque(false);
        resBody.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SPACE_SM, 0));
        pager.setOpaque(false);
        pager.add(prevButton);
        pager.add(pageLabel);
        pager.add(nextButton);
        resBody.add(pager, BorderLayout.SOUTH);
        resultCard.body(resBody);

        wrap.add(cond,       BorderLayout.NORTH);
        wrap.add(resultCard, BorderLayout.CENTER);
        return wrap;
    }

    // ---------- 人物档案 ----------
    private JComponent buildPersonTab() {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card searchCard = new Card("人物搜索");
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        searchRow.setOpaque(false);
        searchRow.add(personSearchField);
        searchRow.add(personSearchButton);
        searchCard.body(searchRow);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, Theme.SPACE_LG, 0));
        statsRow.setOpaque(false);
        statsRow.add(personNewsCard);
        statsRow.add(personToneCard);
        statsRow.add(personThemeCard);
        statsRow.add(personRatioCard);

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

    // ---------- 组织档案 ----------
    private JComponent buildOrgTab() {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card searchCard = new Card("组织搜索");
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        searchRow.setOpaque(false);
        searchRow.add(orgSearchField);
        searchRow.add(orgSearchButton);
        searchCard.body(searchRow);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, Theme.SPACE_LG, 0));
        statsRow.setOpaque(false);
        statsRow.add(orgNewsCard);
        statsRow.add(orgToneCard);
        statsRow.add(orgThemeCard);
        statsRow.add(orgRatioCard);

        Card relCard = new Card("关联关系 TOP10");
        JPanel rel = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
        rel.setOpaque(false);
        rel.add(buildTopBar("关联人物 TOP10",
                new String[]{"Elon Musk", "Tim Cook", "Satya Nadella", "Andy Jassy",
                        "Jensen Huang", "Lisa Su", "Sam Altman", "Sundar Pichai",
                        "Mark Zuckerberg", "Jeff Bezos"},
                new double[]{1850, 1230, 1100, 980, 870, 760, 650, 580, 520, 450}));
        rel.add(buildTopBar("关联组织 TOP10",
                new String[]{"Apple", "Microsoft", "Google", "Amazon", "Meta",
                        "NVIDIA", "OpenAI", "IBM", "Intel", "Oracle"},
                new double[]{2100, 1950, 1800, 1650, 1400, 1200, 900, 750, 600, 500}));
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
