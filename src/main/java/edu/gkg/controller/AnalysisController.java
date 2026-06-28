package edu.gkg.controller;

import edu.gkg.algorithm.PageRankRunner;
import edu.gkg.algorithm.SentimentTrend;
import edu.gkg.common.SharedRecords.*;
import edu.gkg.service.QueryService;
import edu.gkg.view.AnalysisPanel;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

public class AnalysisController {

    private final AnalysisPanel  panel;
    private final QueryService   queryService;
    private final SentimentTrend sentimentTrend = new SentimentTrend();

    public AnalysisController(AnalysisPanel panel, QueryService queryService) {
        this.panel        = panel;
        this.queryService = queryService;
        wire();
    }

    private void wire() {
        // F-12/F-13 共现网络 + PageRank
        panel.rebuildButton.addActionListener(e -> runPageRank());
        panel.runPRButton.addActionListener(e -> runPageRank());

        // F-14 主题热度 — 切换视图 (真实数据等 B 实现 AnalysisService 后填入)
        panel.runHeatButton.addActionListener(e -> {
            panel.showPanel("heatmap");
            // TODO: 等 B 完成 AnalysisServiceImpl.getThemeHeat() 后替换
        });

        // F-15 主题聚类 — 等 B 完成 KMeansClusterer 后实现
        panel.runKMeansButton.addActionListener(e ->
                JOptionPane.showMessageDialog(panel,
                        "主题聚类功能等待 B 同学提交 KMeansClusterer 后开放。",
                        "提示", JOptionPane.INFORMATION_MESSAGE));

        // F-16 情感趋势
        panel.runSentButton.addActionListener(e -> runSentiment());
    }

    private void runPageRank() {
        int topN = panel.topNSlider.getValue();
        panel.runPRButton.setEnabled(false);
        panel.rebuildButton.setEnabled(false);

        SwingWorker<List<FocusNode>, Void> w = new SwingWorker<>() {
            @Override protected List<FocusNode> doInBackground() {
                // TODO: 等 B 完成 CooccurEdgeDao.findAll() 后从 DAO 拉真实边
                // 当前以空边集运行，图为空
                PageRankRunner runner = new PageRankRunner(List.of());
                return runner.topN(topN);
            }
            @Override protected void done() {
                panel.runPRButton.setEnabled(true);
                panel.rebuildButton.setEnabled(true);
                try {
                    List<FocusNode> nodes = get();
                    panel.network.render(List.of(), nodes);
                    panel.showPanel("network");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "PageRank 计算失败：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void runSentiment() {
        String entityType = (String) panel.entityTypeCombo.getSelectedItem();
        String name = panel.entityNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "请输入实体名称", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        panel.runSentButton.setEnabled(false);

        SwingWorker<SentimentSeries, Void> w = new SwingWorker<>() {
            @Override protected SentimentSeries doInBackground() {
                // 从 QueryService 获取主题时序数据（人物/组织等待 B 补 DAO 方法）
                List<DateTone> series;
                if ("主题".equals(entityType)) {
                    series = queryService.getThemeTrend(name,
                            LocalDate.now().minusMonths(12), LocalDate.now());
                } else {
                    // TODO: 等 B 补充按实体过滤的情感序列查询
                    series = List.of();
                }
                return sentimentTrend.compute(series);
            }
            @Override protected void done() {
                panel.runSentButton.setEnabled(true);
                try {
                    SentimentSeries result = get();
                    panel.dash.renderSeries(result, name + "（" + entityType + "）");
                    panel.showPanel("sentiment");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "情感分析失败：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }
}
