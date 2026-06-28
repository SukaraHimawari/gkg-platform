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

    private final CardLayout         cards     = new CardLayout();
    private final JPanel             rightPane = new JPanel(cards);

    // 图表（供 AnalysisController 调用 render 方法）
    public final CooccurNetworkPanel network = new CooccurNetworkPanel();
    public final ThemeHeatmap        heatmap = new ThemeHeatmap();
    public final SentimentDashboard  dash    = new SentimentDashboard();

    // 控制参数（供 AnalysisController 读取）
    public final JSlider            topNSlider      = new JSlider(10, 200, 50);
    public final JButton            rebuildButton   = UiUtil.primaryButton("构建 / 刷新");
    public final JButton            runPRButton     = UiUtil.primaryButton("运行 PageRank");
    public final JComboBox<String>  granCombo       = new JComboBox<>(new String[]{"按日", "按周", "按月"});
    public final JComboBox<String>  rangeCombo      = new JComboBox<>(new String[]{"最近 7 天", "最近 30 天", "最近 6 个月"});
    public final JButton            runHeatButton   = UiUtil.primaryButton("运行热度分析");
    public final JSpinner           kSpinner        = new JSpinner(new SpinnerNumberModel(5, 2, 20, 1));
    public final JButton            runKMeansButton = UiUtil.primaryButton("运行聚类");
    public final JComboBox<String>  entityTypeCombo = new JComboBox<>(new String[]{"人物", "组织", "主题"});
    public final JTextField         entityNameField = new JTextField("Elon Musk");
    public final JButton            runSentButton   = UiUtil.primaryButton("运行分析");

    public AnalysisPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        rightPane.setOpaque(false);
        Card rightCard = new Card();
        rightPane.add(wrap(network),  "network");
        rightPane.add(wrap(heatmap),  "heatmap");
        rightPane.add(wrap(dash),     "sentiment");
        rightCard.body(rightPane);

        // 默认切换为 CardLayout（不调用服务，只切换视图）
        rebuildButton.addActionListener(e -> cards.show(rightPane, "network"));
        runPRButton.addActionListener(e   -> cards.show(rightPane, "network"));
        runHeatButton.addActionListener(e -> cards.show(rightPane, "heatmap"));
        runSentButton.addActionListener(e -> cards.show(rightPane, "sentiment"));

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

        container.add(sectionCard("F-12 共现网络",  buildRebuildSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));
        container.add(sectionCard("F-13 PageRank ⭐", buildPageRankSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));
        container.add(sectionCard("F-14 主题热度",  buildHeatSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));
        container.add(sectionCard("F-15 主题聚类",  buildClusterSection()));
        container.add(Box.createVerticalStrut(Theme.SPACE_MD));
        container.add(sectionCard("F-16 情感趋势 ⭐", buildSentSection()));
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
        return UiUtil.vbox(rebuildButton);
    }

    private JComponent buildPageRankSection() {
        topNSlider.setMajorTickSpacing(50);
        topNSlider.setPaintTicks(true);
        topNSlider.setPaintLabels(true);
        topNSlider.setOpaque(false);
        return UiUtil.vbox(UiUtil.mutedLabel("Top-N"), topNSlider, runPRButton);
    }

    private JComponent buildHeatSection() {
        return UiUtil.vbox(
                UiUtil.mutedLabel("粒度"), granCombo,
                UiUtil.mutedLabel("范围"), rangeCombo,
                runHeatButton);
    }

    private JComponent buildClusterSection() {
        return UiUtil.vbox(UiUtil.mutedLabel("k 值"), kSpinner, runKMeansButton);
    }

    private JComponent buildSentSection() {
        return UiUtil.vbox(
                UiUtil.mutedLabel("实体类型"), entityTypeCombo,
                UiUtil.mutedLabel("名称"), entityNameField,
                runSentButton);
    }

    /** 切换右侧显示的图表视图 */
    public void showPanel(String name) { cards.show(rightPane, name); }
}
