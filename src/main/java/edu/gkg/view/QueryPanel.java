package edu.gkg.view;

import edu.gkg.common.Card;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.RelatedItem;
import edu.gkg.common.StatCard;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
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
import java.util.List;

public class QueryPanel extends JPanel {

    public final JTextField dateFromField = new JTextField();
    public final JTextField dateToField = new JTextField();
    public final JTextField themeCodeField = new JTextField();
    public final AutoSuggestTextField personNameField = new AutoSuggestTextField();
    public final AutoSuggestTextField orgNameField = new AutoSuggestTextField();
    public final AutoSuggestTextField locationNameField = new AutoSuggestTextField();
    public final ThemeTrackPanel themeTrackPanel = new ThemeTrackPanel();
    public final JButton searchButton = UiUtil.primaryButton("查询");
    public final JButton resetButton = UiUtil.secondaryButton("重置");
    public final JButton exportButton = UiUtil.secondaryButton("导出结果");
    public final DefaultTableModel resultTableModel = new DefaultTableModel(
            new Object[]{"记录ID", "发布时间", "媒体", "情感", "URL"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    public final JLabel pageLabel = new JLabel("第 1 页 / 共 1 页");
    public final JButton prevButton = UiUtil.secondaryButton("上一页");
    public final JButton nextButton = UiUtil.secondaryButton("下一页");

    public final AutoSuggestTextField personSearchField = new AutoSuggestTextField();
    public final JButton personSearchButton = UiUtil.primaryButton("查询");
    public final AutoSuggestTextField orgSearchField = new AutoSuggestTextField();
    public final JButton orgSearchButton = UiUtil.primaryButton("查询");

    public final StatCard personNewsCard = new StatCard("新闻量", Theme.BRAND, "#").value("-");
    public final StatCard personToneCard = new StatCard("平均情感", Theme.SUCCESS, "+").value("-");
    public final StatCard personThemeCard = new StatCard("关联主题", Theme.WARN, "T").value("-");
    public final StatCard personRatioCard = new StatCard("正向报道比", Theme.BRAND, "%").value("-");

    public final StatCard orgNewsCard = new StatCard("新闻量", Theme.BRAND, "#").value("-");
    public final StatCard orgToneCard = new StatCard("平均情感", Theme.SUCCESS, "+").value("-");
    public final StatCard orgThemeCard = new StatCard("关联主题", Theme.WARN, "T").value("-");
    public final StatCard orgRatioCard = new StatCard("正向报道比", Theme.BRAND, "%").value("-");

    private final DefaultCategoryDataset personOrgDataset = new DefaultCategoryDataset();
    private final DefaultCategoryDataset personPeopleDataset = new DefaultCategoryDataset();
    private final TimeSeries personTimelineSeries = new TimeSeries("新闻量");
    private final DefaultCategoryDataset orgPeopleDataset = new DefaultCategoryDataset();
    private final DefaultCategoryDataset orgOrgDataset = new DefaultCategoryDataset();
    private final TimeSeries orgTimelineSeries = new TimeSeries("新闻量");

    public QueryPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_APP);

        JTabbedPane sub = new JTabbedPane();
        sub.setFont(Theme.FONT_DEFAULT);
        sub.putClientProperty("JTabbedPane.tabType", "underlined");
        sub.addTab("组合查询", buildSearchTab());
        sub.addTab("人物档案", buildPersonTab());
        sub.addTab("组织档案", buildOrgTab());
        sub.addTab("主题追踪", themeTrackPanel);
        add(sub, BorderLayout.CENTER);
    }

    private JComponent buildSearchTab() {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card cond = new Card("查询条件");
        JPanel grid = new JPanel(new GridLayout(2, 6, Theme.SPACE_SM, Theme.SPACE_SM));
        grid.setOpaque(false);
        grid.add(UiUtil.mutedLabel("开始日期")); grid.add(dateFromField);
        grid.add(UiUtil.mutedLabel("结束日期")); grid.add(dateToField);
        grid.add(UiUtil.mutedLabel("主题代码")); grid.add(themeCodeField);
        grid.add(UiUtil.mutedLabel("人物名")); grid.add(personNameField);
        grid.add(UiUtil.mutedLabel("组织名")); grid.add(orgNameField);
        grid.add(UiUtil.mutedLabel("地点名")); grid.add(locationNameField);

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

        wrap.add(cond, BorderLayout.NORTH);
        wrap.add(resultCard, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildPersonTab() {
        JPanel wrap = profileWrap();
        Card searchCard = new Card("人物查询");
        JPanel searchRow = searchRow(personSearchField, personSearchButton);
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
        rel.add(buildTopBar("关联组织 TOP10", personOrgDataset));
        rel.add(buildTopBar("关联人物 TOP10", personPeopleDataset));
        relCard.body(rel);

        Card timelineCard = new Card("时间分布");
        timelineCard.body(buildMiniTimeChart(personTimelineSeries));
        wrap.add(profileCenter(statsRow, relCard, timelineCard), BorderLayout.CENTER);
        wrap.add(searchCard, BorderLayout.NORTH);
        return wrap;
    }

    private JComponent buildOrgTab() {
        JPanel wrap = profileWrap();
        Card searchCard = new Card("组织查询");
        JPanel searchRow = searchRow(orgSearchField, orgSearchButton);
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
        rel.add(buildTopBar("关联人物 TOP10", orgPeopleDataset));
        rel.add(buildTopBar("关联组织 TOP10", orgOrgDataset));
        relCard.body(rel);

        Card timelineCard = new Card("时间分布");
        timelineCard.body(buildMiniTimeChart(orgTimelineSeries));
        wrap.add(profileCenter(statsRow, relCard, timelineCard), BorderLayout.CENTER);
        wrap.add(searchCard, BorderLayout.NORTH);
        return wrap;
    }

    private JPanel profileWrap() {
        JPanel wrap = new JPanel(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        wrap.setBackground(Theme.BG_APP);
        wrap.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));
        return wrap;
    }

    private JPanel searchRow(JTextField field, JButton button) {
        field.setColumns(24);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        row.setOpaque(false);
        row.add(field);
        row.add(button);
        return row;
    }

    private JScrollPane profileCenter(JPanel statsRow, Card relCard, Card timelineCard) {
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

        return new JScrollPane(center, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER) {{
            setBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
        }};
    }

    public void renderPersonProfileCharts(List<RelatedItem> organizations,
                                          List<RelatedItem> people,
                                          List<DateTone> timeline) {
        fillRelatedDataset(personOrgDataset, organizations);
        fillRelatedDataset(personPeopleDataset, people);
        fillTimeline(personTimelineSeries, timeline);
    }

    public void renderOrgProfileCharts(List<RelatedItem> people,
                                       List<RelatedItem> organizations,
                                       List<DateTone> timeline) {
        fillRelatedDataset(orgPeopleDataset, people);
        fillRelatedDataset(orgOrgDataset, organizations);
        fillTimeline(orgTimelineSeries, timeline);
    }

    private void fillRelatedDataset(DefaultCategoryDataset dataset, List<RelatedItem> items) {
        dataset.clear();
        if (items == null) return;
        for (RelatedItem item : items) {
            dataset.addValue(item.count(), "数量", item.name());
        }
    }

    private void fillTimeline(TimeSeries series, List<DateTone> timeline) {
        series.clear();
        if (timeline == null) return;
        for (DateTone point : timeline) {
            series.addOrUpdate(new Day(point.date().getDayOfMonth(), point.date().getMonthValue(), point.date().getYear()),
                    point.recordCount());
        }
    }

    private ChartPanel buildTopBar(String title, DefaultCategoryDataset ds) {
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

    private ChartPanel buildMiniTimeChart(TimeSeries ts) {
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
