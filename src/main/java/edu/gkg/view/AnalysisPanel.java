package edu.gkg.view;

import edu.gkg.common.Card;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.view.chart.CooccurNetworkPanel;
import edu.gkg.view.chart.SentimentDashboard;
import edu.gkg.view.chart.ThemeHeatmap;

import javax.swing.*;
import java.awt.*;

public class AnalysisPanel extends JPanel {

    private final CardLayout         cards    = new CardLayout();
    private final JPanel             rightPane= new JPanel(cards);
    private final CooccurNetworkPanel network = new CooccurNetworkPanel();
    private final ThemeHeatmap       heatmap  = new ThemeHeatmap();
    private final SentimentDashboard dash     = new SentimentDashboard();

    public AnalysisPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        rightPane.setOpaque(false);
        Card rightCard = new Card();
        rightPane.add(wrap(network), "network");
        rightPane.add(wrap(heatmap), "heatmap");
        rightPane.add(wrap(dash),    "sentiment");
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

    private JComponent buildLeftControlPanel() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        container.add(sectionCard("F-12 共现网络",
                buildRebuildSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-13 PageRank ⭐",
                buildPageRankSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-14 主题热度",
                buildHeatSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-15 主题聚类",
                buildClusterSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));

        container.add(sectionCard("F-16 情感趋势 ⭐",
                buildSentSection()));
        container.add(Box.createVerticalGlue());

        return container;
    }

    private Card sectionCard(String title, JComponent body) {
        Card c = new Card(title);
        c.body(body);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private JComponent buildRebuildSection() {
        JButton rebuild = UiUtil.primaryButton("构建 / 刷新");
        rebuild.addActionListener(e -> cards.show(rightPane, "network"));
        return UiUtil.vbox(rebuild);
    }

    private JComponent buildPageRankSection() {
        JSlider topN = new JSlider(10, 200, 50);
        topN.setMajorTickSpacing(50);
        topN.setPaintTicks(true);
        topN.setPaintLabels(true);
        topN.setOpaque(false);

        JButton runPR = UiUtil.primaryButton("运行 PageRank");
        runPR.addActionListener(e -> cards.show(rightPane, "network"));

        return UiUtil.vbox(UiUtil.mutedLabel("Top-N"), topN, runPR);
    }

    private JComponent buildHeatSection() {
        JComboBox<String> gran = new JComboBox<>(new String[]{"按日", "按周", "按月"});
        JComboBox<String> range= new JComboBox<>(new String[]{"最近 7 天", "最近 30 天", "最近 6 个月"});
        JButton runHeat = UiUtil.primaryButton("运行热度分析");
        runHeat.addActionListener(e -> cards.show(rightPane, "heatmap"));

        return UiUtil.vbox(
                UiUtil.mutedLabel("粒度"), gran,
                UiUtil.mutedLabel("范围"), range,
                runHeat);
    }

    private JComponent buildClusterSection() {
        JSpinner k = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
        JButton runKMeans = UiUtil.primaryButton("运行聚类");
        return UiUtil.vbox(UiUtil.mutedLabel("k 值"), k, runKMeans);
    }

    private JComponent buildSentSection() {
        JComboBox<String> entityType = new JComboBox<>(new String[]{"人物", "组织", "主题"});
        JTextField name = new JTextField("Elon Musk");
        JButton runSent = UiUtil.primaryButton("运行分析");
        runSent.addActionListener(e -> cards.show(rightPane, "sentiment"));

        return UiUtil.vbox(
                UiUtil.mutedLabel("实体类型"), entityType,
                UiUtil.mutedLabel("名称"), name,
                runSent);
    }
}
