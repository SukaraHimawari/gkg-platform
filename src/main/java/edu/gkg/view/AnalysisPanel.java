package edu.gkg.view;

import edu.gkg.common.Card;
import edu.gkg.common.SharedRecords.*;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.view.chart.CooccurNetworkPanel;
import edu.gkg.view.chart.SentimentDashboard;
import edu.gkg.view.chart.ThemeHeatmap;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class AnalysisPanel extends JPanel {

    private final CardLayout         cards    = new CardLayout();
    private final JPanel             rightPane= new JPanel(cards);
    private final CooccurNetworkPanel network = new CooccurNetworkPanel();
    private final ThemeHeatmap       heatmap  = new ThemeHeatmap();
    private final SentimentDashboard dash     = new SentimentDashboard();

    // 集群结果（表格）
    private final DefaultTableModel clusterModel = new DefaultTableModel(
            new Object[]{"簇ID", "关键词", "记录数"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable clusterTable = new JTable(clusterModel);

    // 主题热度结果（表格）
    private final DefaultTableModel themeHeatModel = new DefaultTableModel(
            new Object[]{"主题代码", "时间桶", "频次"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable themeHeatTable = new JTable(themeHeatModel);

    // PageRank 结果（表格）
    private final DefaultTableModel pageRankModel = new DefaultTableModel(
            new Object[]{"排名", "实体名", "PageRank", "度数"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable pageRankTable = new JTable(pageRankModel);

    // 控件
    private final JButton   rebuildBtn = UiUtil.primaryButton("构建 / 刷新");
    private final JSlider   topNSlider = new JSlider(10, 200, 50);
    private final JButton   runPageRankBtn = UiUtil.primaryButton("运行 PageRank");
    private final JComboBox<String> granCombo  = new JComboBox<>(new String[]{"按日", "按周", "按月"});
    private final JComboBox<String> rangeCombo = new JComboBox<>(new String[]{"最近 7 天", "最近 30 天", "最近 6 个月"});
    private final JButton   runHeatBtn = UiUtil.primaryButton("运行热度分析");
    private final JSpinner  kSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
    private final JButton   runKMeansBtn = UiUtil.primaryButton("运行聚类");
    private final JComboBox<EntityType> entityTypeCombo = new JComboBox<>(EntityType.values());
    private final JTextField sentNameField = new JTextField("Elon Musk");
    private final JButton    runSentBtn = UiUtil.primaryButton("运行分析");

    public AnalysisPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        rightPane.setOpaque(false);
        UiUtil.styleTable(clusterTable);
        UiUtil.styleTable(themeHeatTable);
        UiUtil.styleTable(pageRankTable);

        Card rightCard = new Card();
        rightPane.add(wrap(network), "network");
        rightPane.add(wrap(heatmap), "heatmap");
        rightPane.add(wrap(dash),    "sentiment");
        rightPane.add(buildClusterCard(),    "cluster");
        rightPane.add(buildThemeHeatCard(),  "themeheat");
        rightPane.add(buildPageRankCard(),   "pagerank");
        rightCard.body(rightPane);

        JScrollPane leftScroll = new JScrollPane(buildLeftControlPanel(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.setPreferredSize(new Dimension(280, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightCard);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(Theme.SPACE_LG);
        split.setDividerLocation(280);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
    }

    private JComponent wrap(JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildClusterCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel hdr = UiUtil.sectionLabel("F-15 K-Means 主题聚类结果");
        hdr.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_SM, Theme.SPACE_LG, Theme.SPACE_SM, Theme.SPACE_LG));
        p.add(hdr, BorderLayout.NORTH);
        p.add(new JScrollPane(clusterTable), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildThemeHeatCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel hdr = UiUtil.sectionLabel("F-14 主题热度 TOP 排行");
        hdr.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_SM, Theme.SPACE_LG, Theme.SPACE_SM, Theme.SPACE_LG));
        p.add(hdr, BorderLayout.NORTH);
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(themeHeatTable), heatmap);
        sp.setResizeWeight(0.4);
        sp.setBorder(null);
        sp.setOpaque(false);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildPageRankCard() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel hdr = UiUtil.sectionLabel("F-13 PageRank 焦点节点 Top-N");
        hdr.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_SM, Theme.SPACE_LG, Theme.SPACE_SM, Theme.SPACE_LG));
        p.add(hdr, BorderLayout.NORTH);
        p.add(new JScrollPane(pageRankTable), BorderLayout.CENTER);
        return p;
    }

    private JComponent buildLeftControlPanel() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        container.add(sectionCard("F-12 共现网络", UiUtil.vbox(rebuildBtn)));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        topNSlider.setMajorTickSpacing(50);
        topNSlider.setPaintTicks(true);
        topNSlider.setPaintLabels(true);
        topNSlider.setOpaque(false);
        container.add(sectionCard("F-13 PageRank ⭐",
                UiUtil.vbox(UiUtil.mutedLabel("Top-N"), topNSlider, runPageRankBtn)));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-14 主题热度",
                UiUtil.vbox(
                        UiUtil.mutedLabel("粒度"), granCombo,
                        UiUtil.mutedLabel("范围"), rangeCombo,
                        runHeatBtn)));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-15 主题聚类",
                UiUtil.vbox(UiUtil.mutedLabel("k 值"), kSpinner, runKMeansBtn)));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-16 情感趋势 ⭐",
                UiUtil.vbox(
                        UiUtil.mutedLabel("实体类型"), entityTypeCombo,
                        UiUtil.mutedLabel("名称"), sentNameField,
                        runSentBtn)));
        container.add(Box.createVerticalGlue());

        return container;
    }

    private Card sectionCard(String title, JComponent body) {
        Card c = new Card(title);
        c.body(body);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // ---------- 切换右侧卡片 ----------

    public void showNetwork()   { cards.show(rightPane, "network"); }
    public void showHeatmap()   { cards.show(rightPane, "heatmap"); }
    public void showSentiment() { cards.show(rightPane, "sentiment"); }
    public void showCluster()   { cards.show(rightPane, "cluster"); }
    public void showThemeHeat() { cards.show(rightPane, "themeheat"); }
    public void showPageRank()  { cards.show(rightPane, "pagerank"); }

    // ---------- 渲染数据 ----------

    public void renderNetwork(List<CooccurEdge> edges, List<FocusNode> ranks) {
        network.render(edges, ranks);
    }

    public void renderPageRankTable(List<FocusNode> ranks) {
        pageRankModel.setRowCount(0);
        int rank = 1;
        for (FocusNode n : ranks) {
            pageRankModel.addRow(new Object[]{
                    rank++, n.name(),
                    String.format("%.4f", n.pageRank()),
                    n.degree()
            });
        }
    }

    public void renderClusters(List<Cluster> clusters) {
        clusterModel.setRowCount(0);
        for (Cluster c : clusters) {
            clusterModel.addRow(new Object[]{
                    c.clusterId(),
                    String.join(", ", c.keywords()),
                    c.recordCount()
            });
        }
    }

    public void renderThemeHeat(List<ThemeHeat> rows) {
        themeHeatModel.setRowCount(0);
        for (ThemeHeat h : rows) {
            themeHeatModel.addRow(new Object[]{h.themeCode(), h.bucket(), h.count()});
        }
        // 同步刷新热力图为 1×N 矩阵
        if (!rows.isEmpty()) {
            int n = rows.size();
            double[][] m = new double[1][n];
            java.util.List<String> themes = new java.util.ArrayList<>();
            java.util.List<String> buckets = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) m[0][i] = rows.get(i).count();
            themes.add("count");
            for (ThemeHeat h : rows) buckets.add(h.themeCode());
            heatmap.rebuild(themes, buckets, m);
        }
    }

    public void renderSentiment(SentimentSeries series, String label) {
        dash.renderSeries(series, label);
    }

    // ---------- 暴露控件给 Controller ----------

    public JButton rebuildButton()      { return rebuildBtn; }
    public JButton runPageRankButton()  { return runPageRankBtn; }
    public JButton runHeatButton()      { return runHeatBtn; }
    public JButton runKMeansButton()    { return runKMeansBtn; }
    public JButton runSentButton()      { return runSentBtn; }

    public int   topN()         { return topNSlider.getValue(); }
    public int   kValue()       { return (Integer) kSpinner.getValue(); }
    public Granularity granularity() {
        return switch (granCombo.getSelectedIndex()) {
            case 0  -> Granularity.DAY;
            case 1  -> Granularity.WEEK;
            default -> Granularity.MONTH;
        };
    }
    public LocalDate rangeFrom() {
        LocalDate now = LocalDate.now();
        return switch (rangeCombo.getSelectedIndex()) {
            case 0  -> now.minusDays(7);
            case 1  -> now.minusDays(30);
            default -> now.minusMonths(6);
        };
    }
    public LocalDate rangeTo()    { return LocalDate.now(); }

    public EntityRef sentimentEntity() {
        EntityType t = (EntityType) entityTypeCombo.getSelectedItem();
        return new EntityRef(t == null ? EntityType.PERSON : t, sentNameField.getText().trim());
    }

    public CooccurNetworkPanel networkPanel()  { return network; }
    public ThemeHeatmap        heatmapPanel()  { return heatmap; }
    public SentimentDashboard  sentimentPanel(){ return dash; }
}
