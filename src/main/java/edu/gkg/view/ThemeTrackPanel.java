package edu.gkg.view;

import edu.gkg.common.Card;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.view.chart.TrendLineChart;

import javax.swing.*;
import java.awt.*;

public class ThemeTrackPanel extends JPanel {

    private final TrendLineChart chart = new TrendLineChart();

    public ThemeTrackPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card formCard = new Card("主题追踪");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        form.setOpaque(false);
        form.add(UiUtil.mutedLabel("主题代码"));
        form.add(new JTextField("SANCTIONS", 16));
        form.add(UiUtil.mutedLabel("起始"));
        form.add(new JTextField("2024-01-15", 10));
        form.add(UiUtil.mutedLabel("结束"));
        form.add(new JTextField("2024-01-15", 10));
        form.add(UiUtil.primaryButton("绘制趋势"));
        formCard.body(form);

        Card chartCard = new Card("频次趋势");
        chart.demoData();
        chartCard.body(chart);

        add(formCard, BorderLayout.NORTH);
        add(chartCard, BorderLayout.CENTER);
    }

    public TrendLineChart chart() { return chart; }
}
