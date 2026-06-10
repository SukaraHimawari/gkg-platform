package edu.gkg.controller;

import edu.gkg.common.GuiTask;
import edu.gkg.common.SharedRecords.*;
import edu.gkg.common.UiUtil;
import edu.gkg.service.AnalysisService;
import edu.gkg.view.AnalysisPanel;

import java.util.List;

/**
 * 协调 AnalysisPanel ←→ AnalysisService。
 * 串接 5 个挖掘动作：F-12 共现重建、F-13 PageRank、F-14 主题热度、F-15 K-Means、F-16 情感趋势。
 */
public class AnalysisController {

    private final AnalysisPanel   panel;
    private final AnalysisService service;

    public AnalysisController(AnalysisPanel panel, AnalysisService service) {
        this.panel = panel;
        this.service = service;
        bind();
    }

    private void bind() {
        panel.rebuildButton().addActionListener(e -> rebuildCooccurrence());
        panel.runPageRankButton().addActionListener(e -> runPageRank());
        panel.runHeatButton().addActionListener(e -> runHeat());
        panel.runKMeansButton().addActionListener(e -> runKMeans());
        panel.runSentButton().addActionListener(e -> runSentiment());
    }

    private void rebuildCooccurrence() {
        new GuiTask<Void>(panel, null,
                v -> {
                    UiUtil.info(panel, "共现表已重建。");
                    // 重建后顺便预渲染一次网络（用空 ranks 占位，等用户点 PageRank 再上色）
                    panel.renderNetwork(service.findAllEdges(), List.of());
                    panel.showNetwork();
                },
                (owner, ex) -> UiUtil.error(owner, "共现重建失败", ex)) {
            @Override protected Void doWork() { service.rebuildCooccurrence(); return null; }
        }.execute();
    }

    private void runPageRank() {
        int topN = panel.topN();
        new GuiTask<List<FocusNode>>(panel, null,
                ranks -> {
                    panel.renderNetwork(service.findAllEdges(), ranks);
                    panel.renderPageRankTable(ranks);
                    panel.showNetwork();
                },
                (owner, ex) -> UiUtil.error(owner, "PageRank 运行失败", ex)) {
            @Override protected List<FocusNode> doWork() { return service.topFocusPersons(topN); }
        }.execute();
    }

    private void runHeat() {
        new GuiTask<List<ThemeHeat>>(panel, null,
                rows -> { panel.renderThemeHeat(rows); panel.showThemeHeat(); },
                (owner, ex) -> UiUtil.error(owner, "主题热度运行失败", ex)) {
            @Override protected List<ThemeHeat> doWork() {
                return service.themeHeatRank(panel.granularity(),
                        panel.rangeFrom(), panel.rangeTo(), 10);
            }
        }.execute();
    }

    private void runKMeans() {
        int k = panel.kValue();
        new GuiTask<List<Cluster>>(panel, null,
                clusters -> { panel.renderClusters(clusters); panel.showCluster(); },
                (owner, ex) -> UiUtil.error(owner, "K-Means 运行失败", ex)) {
            @Override protected List<Cluster> doWork() { return service.clusterByKMeans(k, 50); }
        }.execute();
    }

    private void runSentiment() {
        EntityRef ref = panel.sentimentEntity();
        if (ref.name() == null || ref.name().isEmpty()) {
            UiUtil.warn(panel, "请输入实体名称");
            return;
        }
        new GuiTask<SentimentSeries>(panel, null,
                series -> {
                    panel.renderSentiment(series, ref.name() + " · 情感趋势");
                    panel.showSentiment();
                },
                (owner, ex) -> UiUtil.error(owner, "情感分析失败", ex)) {
            @Override protected SentimentSeries doWork() {
                return service.sentimentTrend(ref,
                        java.time.LocalDate.now().minusDays(30),
                        java.time.LocalDate.now());
            }
        }.execute();
    }
}
