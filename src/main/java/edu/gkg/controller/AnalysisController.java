package edu.gkg.controller;

import edu.gkg.algorithm.PageRankRunner;
import edu.gkg.algorithm.SentimentTrend;
import edu.gkg.algorithm.ThemeRanker;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.FocusNode;
import edu.gkg.common.SharedRecords.SentimentSeries;
import edu.gkg.dao.CooccurEdgeDao;
import edu.gkg.dao.OrganizationDao;
import edu.gkg.dao.PersonDao;
import edu.gkg.model.CooccurEdge;
import edu.gkg.model.Organization;
import edu.gkg.model.Person;
import edu.gkg.service.AnalysisService;
import edu.gkg.service.QueryService;
import edu.gkg.view.AnalysisPanel;

import javax.swing.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AnalysisController {

    private final AnalysisPanel panel;
    private final QueryService queryService;
    private final AnalysisService analysisService;
    private final SentimentTrend sentimentTrend = new SentimentTrend();
    private final CooccurEdgeDao edgeDao = new CooccurEdgeDao();
    private final PersonDao personDao = new PersonDao();
    private final OrganizationDao orgDao = new OrganizationDao();
    private final AtomicReference<SwingWorker<?, ?>> currentWorker = new AtomicReference<>();

    public AnalysisController(AnalysisPanel panel, QueryService queryService, AnalysisService analysisService) {
        this.panel = panel;
        this.queryService = queryService;
        this.analysisService = analysisService;
        wire();
    }

    private void wire() {
        panel.rebuildButton.addActionListener(e -> rebuildCooccurrence());
        panel.runPRButton.addActionListener(e -> runPageRank());
        panel.runHeatButton.addActionListener(e -> runThemeHeat());
        panel.runKMeansButton.addActionListener(e -> runKMeans());
        panel.runSentButton.addActionListener(e -> runSentiment());
        panel.entityTypeCombo.addActionListener(e -> wireEntityNameSuggestions());
        wireEntityNameSuggestions();
    }

    private void wireEntityNameSuggestions() {
        String entityType = (String) panel.entityTypeCombo.getSelectedItem();
        if ("组织".equals(entityType)) {
            panel.entityNameField.setSuggestionProvider(prefix -> queryService.suggestOrg(prefix, 10));
        } else if ("人物".equals(entityType)) {
            panel.entityNameField.setSuggestionProvider(prefix -> queryService.suggestPerson(prefix, 10));
        } else {
            panel.entityNameField.setSuggestionProvider(prefix -> List.of());
        }
    }

    private void rebuildCooccurrence() {
        setAnalysisButtons(false);
        panel.setBusy("正在构建共现网络...", 0);

        SwingWorker<Void, ProgressUpdate> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws SQLException {
                analysisService.rebuildCooccurrence((pct, msg) -> publish(new ProgressUpdate(pct, msg)));
                return null;
            }

            @Override protected void process(List<ProgressUpdate> chunks) {
                ProgressUpdate last = chunks.get(chunks.size() - 1);
                panel.setBusy(last.message(), last.percent());
            }

            @Override protected void done() {
                setAnalysisButtons(true);
                if (isCancelled()) return;
                try {
                    get();
                    panel.setBusy("共现网络构建完成", 100);
                    JOptionPane.showMessageDialog(panel, "共现网络构建完成。", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    panel.setBusy("共现网络构建失败", 0);
                    showError("共现网络构建失败", ex);
                }
            }
        };
        currentWorker.set(w);
        w.execute();
    }

    void cancelCurrentTaskForTests() {
        SwingWorker<?, ?> worker = currentWorker.get();
        if (worker != null) worker.cancel(true);
    }

    private void runPageRank() {
        int topN = panel.topNSlider.getValue();
        setAnalysisButtons(false);
        panel.setBusy("正在计算 PageRank...", 10);

        SwingWorker<NetworkResult, Void> w = new SwingWorker<>() {
            @Override protected NetworkResult doInBackground() throws SQLException {
                List<CooccurEdge> modelEdges = edgeDao.findTopEdges(Math.max(500, topN * 20));
                if (modelEdges.isEmpty()) return new NetworkResult(List.of(), List.of());

                Map<Long, String> idToName = new HashMap<>();
                for (Person p : personDao.findAll()) idToName.put(p.getPersonId(), p.getName());
                for (Organization o : orgDao.findAll()) idToName.put(o.getOrgId(), o.getName());

                List<edu.gkg.common.SharedRecords.CooccurEdge> prEdges = new ArrayList<>();
                for (CooccurEdge e : modelEdges) {
                    prEdges.add(new edu.gkg.common.SharedRecords.CooccurEdge(
                            e.getE1Id().intValue(), idToName.getOrDefault(e.getE1Id(), "ID:" + e.getE1Id()),
                            e.getE2Id().intValue(), idToName.getOrDefault(e.getE2Id(), "ID:" + e.getE2Id()),
                            e.getCoCount()));
                }
                List<FocusNode> ranks = new PageRankRunner(prEdges).topN(topN);
                Set<Integer> visible = ranks.stream().map(FocusNode::nodeId).collect(Collectors.toSet());
                List<edu.gkg.common.SharedRecords.CooccurEdge> visibleEdges = prEdges.stream()
                        .filter(e -> visible.contains(e.e1Id()) && visible.contains(e.e2Id()))
                        .limit(Math.max(100, topN * 4L))
                        .toList();
                return new NetworkResult(visibleEdges, ranks);
            }

            @Override protected void done() {
                setAnalysisButtons(true);
                try {
                    NetworkResult result = get();
                    if (result.ranks().isEmpty()) {
                        panel.setBusy("暂无共现数据，请先构建共现网络", 0);
                        JOptionPane.showMessageDialog(panel, "暂无共现数据，请先导入数据并构建共现网络。", "提示", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    panel.network.render(result.edges(), result.ranks());
                    panel.showPanel("network");
                    panel.setBusy("PageRank 完成，显示 " + result.ranks().size() + " 个节点", 100);
                } catch (Exception ex) {
                    panel.setBusy("PageRank 失败", 0);
                    showError("PageRank 计算失败", ex);
                }
            }
        };
        w.execute();
    }

    private void runThemeHeat() {
        panel.runHeatButton.setEnabled(false);
        panel.setBusy("正在统计主题热度...", 20);

        SwingWorker<List<ThemeRanker.ThemeHeat>, Void> w = new SwingWorker<>() {
            @Override protected List<ThemeRanker.ThemeHeat> doInBackground() throws SQLException {
                String gran = switch ((String) panel.granCombo.getSelectedItem()) {
                    case "按周" -> "WEEK";
                    case "按月" -> "MONTH";
                    default -> "DAY";
                };
                String range = (String) panel.rangeCombo.getSelectedItem();
                LocalDate now = LocalDate.now();
                LocalDate from = switch (range) {
                    case "最近 30 天" -> now.minusDays(30);
                    case "最近 6 个月" -> now.minusMonths(6);
                    case "全部数据" -> LocalDate.of(1900, 1, 1);
                    default -> now.minusDays(7);
                };
                String to = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
                List<ThemeRanker.ThemeHeat> heat = analysisService.getThemeHeat(
                        gran, from.format(DateTimeFormatter.ISO_LOCAL_DATE), to, 80);
                if (heat.isEmpty() && !"全部数据".equals(range)) {
                    return analysisService.getThemeHeat(gran, "1900-01-01", to, 80);
                }
                return heat;
            }

            @Override protected void done() {
                panel.runHeatButton.setEnabled(true);
                try {
                    List<ThemeRanker.ThemeHeat> heat = get();
                    if (heat.isEmpty()) {
                        panel.setBusy("暂无主题热度数据", 0);
                        JOptionPane.showMessageDialog(panel, "暂无主题热度数据，请先导入 GKG 数据或选择其他时间范围。", "提示", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        renderHeatmap(heat);
                        panel.showPanel("heatmap");
                        panel.setBusy("主题热度分析完成", 100);
                    }
                } catch (Exception ex) {
                    panel.setBusy("主题热度分析失败", 0);
                    showError("主题热度分析失败", ex);
                }
            }
        };
        currentWorker.set(w);
        w.execute();
    }

    private void renderHeatmap(List<ThemeRanker.ThemeHeat> heat) {
        LinkedHashSet<String> themeSet = new LinkedHashSet<>();
        LinkedHashSet<String> bucketSet = new LinkedHashSet<>();
        for (ThemeRanker.ThemeHeat h : heat) {
            themeSet.add(h.themeCode());
            bucketSet.add(h.bucket());
        }
        List<String> themes = new ArrayList<>(themeSet);
        List<String> buckets = new ArrayList<>(bucketSet);
        double[][] heatData = new double[themes.size()][buckets.size()];
        Map<String, Integer> themeIndex = indexOf(themes);
        Map<String, Integer> bucketIndex = indexOf(buckets);
        for (ThemeRanker.ThemeHeat h : heat) {
            heatData[themeIndex.get(h.themeCode())][bucketIndex.get(h.bucket())] = h.count();
        }
        panel.heatmap.rebuild(themes, buckets, heatData);
    }

    private Map<String, Integer> indexOf(List<String> values) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < values.size(); i++) result.put(values.get(i), i);
        return result;
    }

    private void runKMeans() {
        int k = (Integer) panel.kSpinner.getValue();
        panel.runKMeansButton.setEnabled(false);
        panel.setBusy("正在运行 K-Means 聚类...", 30);

        SwingWorker<List<String>, Void> w = new SwingWorker<>() {
            @Override protected List<String> doInBackground() {
                return analysisService.clusterByKMeans(k, 100);
            }

            @Override protected void done() {
                panel.runKMeansButton.setEnabled(true);
                try {
                    List<String> clusters = get();
                    if (clusters.isEmpty()) {
                        panel.setBusy("暂无足够主题数据", 0);
                        JOptionPane.showMessageDialog(panel, "暂无足够主题数据，请先导入 GKG 数据。", "提示", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        panel.clusters.render(clusters);
                        panel.showPanel("cluster");
                        panel.setBusy("K-Means 聚类完成", 100);
                    }
                } catch (Exception ex) {
                    panel.setBusy("K-Means 聚类失败", 0);
                    showError("K-Means 聚类失败", ex);
                }
            }
        };
        currentWorker.set(w);
        w.execute();
    }

    private void runSentiment() {
        String entityType = (String) panel.entityTypeCombo.getSelectedItem();
        String name = panel.entityNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "请输入实体名称。", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        panel.runSentButton.setEnabled(false);
        panel.setBusy("正在计算情感趋势...", 30);

        SwingWorker<SentimentSeries, Void> w = new SwingWorker<>() {
            @Override protected SentimentSeries doInBackground() {
                String type = switch (entityType) {
                    case "组织" -> "ORG";
                    case "主题" -> "THEME";
                    default -> "PERSON";
                };
                List<DateTone> series = queryService.getEntityTrend(type, name,
                        LocalDate.now().minusMonths(12), LocalDate.now());
                return sentimentTrend.compute(series);
            }

            @Override protected void done() {
                panel.runSentButton.setEnabled(true);
                try {
                    SentimentSeries series = get();
                    panel.dash.renderSeries(series, name + " (" + entityType + ")");
                    panel.showPanel("sentiment");
                    panel.setBusy(series.series().isEmpty() ? "未找到该实体的情感数据" : "情感趋势分析完成", 100);
                } catch (Exception ex) {
                    panel.setBusy("情感分析失败", 0);
                    showError("情感分析失败", ex);
                }
            }
        };
        w.execute();
    }

    private void setAnalysisButtons(boolean enabled) {
        panel.rebuildButton.setEnabled(enabled);
        panel.runPRButton.setEnabled(enabled);
        panel.runHeatButton.setEnabled(enabled);
        panel.runKMeansButton.setEnabled(enabled);
        panel.runSentButton.setEnabled(enabled);
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(panel, title + "：" + rootMessage(ex), "错误", JOptionPane.ERROR_MESSAGE);
    }

    private String rootMessage(Exception ex) {
        Throwable t = ex;
        if (t instanceof ExecutionException && t.getCause() != null) t = t.getCause();
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage();
    }

    private record ProgressUpdate(int percent, String message) {}
    private record NetworkResult(List<edu.gkg.common.SharedRecords.CooccurEdge> edges,
                                 List<FocusNode> ranks) {}
}
