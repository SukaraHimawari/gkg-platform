package edu.gkg.view;

import edu.gkg.common.*;
import edu.gkg.common.SharedRecords.*;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * 查询检索面板：组合查询 / 人物档案 / 组织档案 / 主题追踪 四个子页。
 * 暴露按钮 / 输入框 / 表格模型，由 QueryController 挂事件。
 */
public class QueryPanel extends JPanel {

    // ---- 组合查询 ----
    private final JTextField fromField   = new JTextField("2024-01-15");
    private final JTextField toField     = new JTextField("2024-01-15");
    private final JTextField themeField  = new JTextField();
    private final JTextField personField = new JTextField();
    private final JTextField orgField    = new JTextField();
    private final JTextField locField    = new JTextField();
    private final JButton    searchBtn   = UiUtil.primaryButton("搜索");
    private final JButton    resetBtn    = UiUtil.secondaryButton("重置");
    private final JButton    exportBtn   = UiUtil.secondaryButton("导出结果");
    private final JButton    prevPageBtn = UiUtil.secondaryButton("« 上一页");
    private final JButton    nextPageBtn = UiUtil.secondaryButton("下一页 »");
    private final JLabel     pageLabel   = UiUtil.mutedLabel("第 1 页 / 共 — 页");
    private final DefaultTableModel resultModel = new DefaultTableModel(
            new Object[]{"记录ID", "发布时间", "媒体", "情感", "URL"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable resultTable = new JTable(resultModel);

    // ---- 人物档案 ----
    private final JTextField personNameField = new JTextField("Elon Musk", 24);
    private final JButton    personSearchBtn = UiUtil.primaryButton("查询");
    private final StatCard   personNews   = new StatCard("新闻量",     Theme.BRAND,   "#");
    private final StatCard   personTone   = new StatCard("平均情感",   Theme.SUCCESS, "+");
    private final StatCard   personThemes = new StatCard("关联主题",   Theme.WARN,    "T");
    private final StatCard   personRatio  = new StatCard("正向报道比", Theme.BRAND,   "%");
    private final JPanel     personRelHolder   = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
    private final JPanel     personTimelineHolder = new JPanel(new BorderLayout());

    // ---- 组织档案 ----
    private final JTextField orgNameField = new JTextField("United Nations", 24);
    private final JButton    orgSearchBtn = UiUtil.primaryButton("查询");
    private final StatCard   orgNews   = new StatCard("新闻量",     Theme.BRAND,   "#");
    private final StatCard   orgTone   = new StatCard("平均情感",   Theme.SUCCESS, "+");
    private final StatCard   orgThemes = new StatCard("关联主题",   Theme.WARN,    "T");
    private final StatCard   orgRatio  = new StatCard("正向报道比", Theme.BRAND,   "%");
    private final JPanel     orgRelHolder      = new JPanel(new GridLayout(1, 2, Theme.SPACE_LG, 0));
    private final JPanel     orgTimelineHolder = new JPanel(new BorderLayout());

    // ---- 主题追踪 ----
    private final ThemeTrackPanel themeTrackPanel = new ThemeTrackPanel();

    public QueryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);

        JTabbedPane sub = new JTabbedPane();
        sub.setFont(Theme.FONT_DEFAULT);
        sub.putClientProperty("JTabbedPane.tabType", "underlined");
        sub.addTab("组合查询",  buildSearchTab());
        sub.addTab("人物档案",  buildProfileTab(EntityType.PERSON));
        sub.addTab("组织档案",  buildProfileTab(EntityType.ORG));
        sub.addTab("主题追踪",  themeTrackPanel);
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
        grid.add(UiUtil.mutedLabel("起始日期")); grid.add(fromField);
        grid.add(UiUtil.mutedLabel("结束日期")); grid.add(toField);
        grid.add(UiUtil.mutedLabel("主题代码")); grid.add(themeField);
        grid.add(UiUtil.mutedLabel("人物名"));   grid.add(personField);
        grid.add(UiUtil.mutedLabel("组织名"));   grid.add(orgField);
        grid.add(UiUtil.mutedLabel("地点名"));   grid.add(locField);

        JPanel condBody = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        condBody.setOpaque(false);
        condBody.add(grid, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        actions.setOpaque(false);
        actions.add(resetBtn);
        actions.add(searchBtn);
        actions.add(exportBtn);
        condBody.add(actions, BorderLayout.SOUTH);
        cond.body(condBody);

        Card resultCard = new Card("查询结果");
        UiUtil.styleTable(resultTable);
        JPanel resBody = new JPanel(new BorderLayout(0, Theme.SPACE_SM));
        resBody.setOpaque(false);
        resBody.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SPACE_SM, 0));
        pager.setOpaque(false);
        pager.add(prevPageBtn);
        pager.add(pageLabel);
        pager.add(nextPageBtn);
        resBody.add(pager, BorderLayout.SOUTH);
        resultCard.body(resBody);

        wrap.add(cond,       BorderLayout.NORTH);
        wrap.add(resultCard, BorderLayout.CENTER);
        return wrap;
    }

    public void renderSearchPage(Page<SearchRow> page) {
        resultModel.setRowCount(0);
        for (SearchRow r : page.rows()) {
            resultModel.addRow(new Object[]{
                    r.recordId(), r.publishTime(), r.source(),
                    String.format("%.2f", r.tone()), r.url()
            });
        }
        long totalPages = Math.max(1, (page.total() + page.pageSize() - 1) / page.pageSize());
        pageLabel.setText("第 " + (page.page() + 1) + " 页 / 共 " + totalPages + " 页");
    }

    public QueryCondition currentCondition() {
        return new QueryCondition(
                parseDate(fromField.getText()),
                parseDate(toField.getText()),
                blankToNull(themeField.getText()),
                blankToNull(personField.getText()),
                blankToNull(orgField.getText()),
                blankToNull(locField.getText())
        );
    }

    public void resetCondition() {
        fromField.setText("");
        toField.setText("");
        themeField.setText("");
        personField.setText("");
        orgField.setText("");
        locField.setText("");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    // ---------- 人物 / 组织档案 ----------
    private JComponent buildProfileTab(EntityType type) {
        boolean isPerson = (type == EntityType.PERSON);
        String word = isPerson ? "人物" : "组织";
        JTextField nameField = isPerson ? personNameField : orgNameField;
        JButton    searchBt  = isPerson ? personSearchBtn : orgSearchBtn;

        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card searchCard = new Card(word + "搜索");
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        searchRow.setOpaque(false);
        searchRow.add(nameField);
        searchRow.add(searchBt);
        searchCard.body(searchRow);

        JPanel statsRow = new JPanel(new GridLayout(1, 4, Theme.SPACE_LG, 0));
        statsRow.setOpaque(false);
        statsRow.add(isPerson ? personNews   : orgNews);
        statsRow.add(isPerson ? personTone   : orgTone);
        statsRow.add(isPerson ? personThemes : orgThemes);
        statsRow.add(isPerson ? personRatio  : orgRatio);

        Card relCard = new Card("关联关系 TOP10");
        JPanel relHolder = isPerson ? personRelHolder : orgRelHolder;
        relHolder.setOpaque(false);
        relCard.body(relHolder);

        Card timelineCard = new Card("时间分布（按月）");
        JPanel timelineHolder = isPerson ? personTimelineHolder : orgTimelineHolder;
        timelineHolder.setOpaque(false);
        timelineCard.body(timelineHolder);

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

    public void renderProfile(EntityProfile p) {
        boolean isPerson = (p.type() == EntityType.PERSON);
        StatCard news   = isPerson ? personNews   : orgNews;
        StatCard tone   = isPerson ? personTone   : orgTone;
        StatCard themes = isPerson ? personThemes : orgThemes;
        StatCard ratio  = isPerson ? personRatio  : orgRatio;
        JPanel   rel    = isPerson ? personRelHolder : orgRelHolder;
        JPanel   tl     = isPerson ? personTimelineHolder : orgTimelineHolder;

        NumberFormat nf = NumberFormat.getIntegerInstance();
        news.value(nf.format(p.newsCount()));
        tone.value(String.format("%+.2f", p.avgTone())).hint(p.avgTone() >= 0 ? "正向" : "负向");
        themes.value(nf.format(p.relatedThemes()));
        ratio.value(String.format("%.2f%%", p.positiveRatio() * 100));

        rel.removeAll();
        rel.add(buildTopBar(isPerson ? "关联组织 TOP10" : "关联人物 TOP10", p.relatedOrgs()));
        rel.add(buildTopBar(isPerson ? "关联人物 TOP10" : "关联组织 TOP10", p.relatedPersons()));
        rel.revalidate(); rel.repaint();

        tl.removeAll();
        tl.add(buildMiniTimeChart(p.monthlyDistribution()), BorderLayout.CENTER);
        tl.revalidate(); tl.repaint();
    }

    private static ChartPanel buildTopBar(String title, List<NameCount> data) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NameCount nc : data) ds.addValue(nc.value(), "频次", nc.name());
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

    private static ChartPanel buildMiniTimeChart(List<DateTone> series) {
        TimeSeries ts = new TimeSeries("新闻量");
        for (DateTone d : series) {
            ts.addOrUpdate(new Day(java.sql.Date.valueOf(d.date())), d.recordCount());
        }
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

    // ---------- 暴露给 Controller ----------

    public JButton searchButton()    { return searchBtn; }
    public JButton resetButton()     { return resetBtn; }
    public JButton exportButton()    { return exportBtn; }
    public JButton prevPageButton()  { return prevPageBtn; }
    public JButton nextPageButton()  { return nextPageBtn; }
    public JTable  resultTable()     { return resultTable; }
    public DefaultTableModel resultModel() { return resultModel; }

    public JTextField personNameField() { return personNameField; }
    public JButton    personSearchButton() { return personSearchBtn; }

    public JTextField orgNameField() { return orgNameField; }
    public JButton    orgSearchButton() { return orgSearchBtn; }

    public ThemeTrackPanel themeTrackPanel() { return themeTrackPanel; }
}
