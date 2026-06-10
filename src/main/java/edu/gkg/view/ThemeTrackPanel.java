package edu.gkg.view;

import edu.gkg.common.Card;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.view.chart.TrendLineChart;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class ThemeTrackPanel extends JPanel {

    private final TrendLineChart chart = new TrendLineChart();
    private final JTextField themeField = new JTextField("SANCTIONS", 16);
    private final JTextField fromField  = new JTextField("2024-01-01", 10);
    private final JTextField toField    = new JTextField("2024-01-14", 10);
    private final JButton    drawBtn    = UiUtil.primaryButton("绘制趋势");

    public ThemeTrackPanel() {
        setLayout(new BorderLayout(Theme.SPACE_LG, Theme.SPACE_LG));
        setBackground(Theme.BG_APP);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        Card formCard = new Card("主题追踪");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        form.setOpaque(false);
        form.add(UiUtil.mutedLabel("主题代码")); form.add(themeField);
        form.add(UiUtil.mutedLabel("起始"));     form.add(fromField);
        form.add(UiUtil.mutedLabel("结束"));     form.add(toField);
        form.add(drawBtn);
        formCard.body(form);

        Card chartCard = new Card("频次趋势");
        chart.demoData();
        chartCard.body(chart);

        add(formCard, BorderLayout.NORTH);
        add(chartCard, BorderLayout.CENTER);
    }

    public TrendLineChart chart()      { return chart; }
    public JButton        drawButton() { return drawBtn; }
    public String         themeCode()  { return themeField.getText().trim(); }
    public LocalDate      from()       { return parse(fromField.getText()); }
    public LocalDate      to()         { return parse(toField.getText()); }

    public void renderTrend(String label, List<DateTone> series) {
        chart.renderFrequencySeries(label, series);
    }

    private static LocalDate parse(String s) {
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }
}
