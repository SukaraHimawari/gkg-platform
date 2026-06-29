package edu.gkg.controller;

import edu.gkg.algorithm.PageRankRunner;
import edu.gkg.algorithm.SentimentTrend;
import edu.gkg.algorithm.ThemeRanker;
import edu.gkg.common.SharedRecords.*;
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
import java.util.*;

public class AnalysisController {

    private final AnalysisPanel  panel;
    private final QueryService   queryService;
    private final AnalysisService analysisService;
    private final SentimentTrend sentimentTrend = new SentimentTrend();
    private final CooccurEdgeDao edgeDao = new CooccurEdgeDao();
    private final PersonDao personDao = new PersonDao();
    private final OrganizationDao orgDao = new OrganizationDao();

    public AnalysisController(AnalysisPanel panel, QueryService queryService,
                              AnalysisService analysisService) {
        this.panel           = panel;
        this.queryService    = queryService;
        this.analysisService = analysisService;
        wire();
    }

    private void wire() {
        // F-12: 构建共现网络
        panel.rebuildButton.addActionListener(e -> rebuildCooccurrence());

        // F-13: PageRank
        panel.runPRButton.addActionListener(e -> runPageRank());

        // F-14: 主题热度
        panel.runHeatButton.addActionListener(e -> runThemeHeat());

        // F-15: K-Means 聚类 — 待重写
        panel.runKMeansButton.addActionListener(e ->
                JOptionPane.showMessageDialog(panel,
                        "主题聚类功能等待重写。",
                        "提示", JOptionPane.INFORMATION_MESSAGE));

        // F-16: 情感趋势
        panel.runSentButton.addActionListener(e -> runSentiment());
    }

    private void rebuildCooccurrence() {
        panel.rebuildButton.setEnabled(false);
        panel.runPRButton.setEnabled(false);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws SQLException {
                analysisService.rebuildCooccurrence();
                return null;
            }
            @Override protected void done() {
                panel.rebuildButton.setEnabled(true);
                panel.runPRButton.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(panel,
                            "共现网络构建完成！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "共现网络构建失败：" + ex.getCause().getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void runPageRank() {
        int topN = panel.topNSlider.getValue();
        panel.runPRButton.setEnabled(false);
        panel.rebuildButton.setEnabled(false);

        SwingWorker<List<FocusNode>, Void> w = new SwingWorker<>() {
            @Override protected List<FocusNode> doInBackground() throws SQLException {
                List<CooccurEdge> modelEdges = edgeDao.findAllEdges();
                if (modelEdges.isEmpty()) return List.of();

                // 查找名称映射
                Map<Long, String> idToName = new HashMap<>();
                for (Person p : personDao.findAll()) {
                    idToName.put(p.getPersonId(), p.getName());
                }
                for (Organization o : orgDao.findAll()) {
                    idToName.put(o.getOrgId(), o.getName());
                }

                List<edu.gkg.common.SharedRecords.CooccurEdge> prEdges = new ArrayList<>();
                for (CooccurEdge e : modelEdges) {
                    String e1Name = idToName.getOrDefault(e.getE1Id(), "ID:" + e.getE1Id());
                    String e2Name = idToName.getOrDefault(e.getE2Id(), "ID:" + e.getE2Id());
                    prEdges.add(new edu.gkg.common.SharedRecords.CooccurEdge(
                            e.getE1Id().intValue(), e1Name,
                            e.getE2Id().intValue(), e2Name,
                            e.getCoCount()));
                }

                PageRankRunner runner = new PageRankRunner(prEdges);
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

    private void runThemeHeat() {
        panel.runHeatButton.setEnabled(false);

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
                    default -> now.minusDays(7);
                };

                return analysisService.getThemeHeat(gran,
                        from.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        now.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        50);
            }
            @Override protected void done() {
                panel.runHeatButton.setEnabled(true);
                try {
                    List<ThemeRanker.ThemeHeat> heat = get();
                    if (heat.isEmpty()) {
                        JOptionPane.showMessageDialog(panel,
                                "暂无主题热度数据，请先导入数据。",
                                "提示", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // 转换为热力图格式
                        LinkedHashSet<String> themeSet = new LinkedHashSet<>();
                        LinkedHashSet<String> bucketSet = new LinkedHashSet<>();
                        for (ThemeRanker.ThemeHeat h : heat) {
                            themeSet.add(h.themeCode());
                            bucketSet.add(h.bucket());
                        }
                        List<String> themes = new ArrayList<>(themeSet);
                        List<String> buckets = new ArrayList<>(bucketSet);
                        double[][] heatData = new double[themes.size()][buckets.size()];
                        for (ThemeRanker.ThemeHeat h : heat) {
                            int ti = themes.indexOf(h.themeCode());
                            int bi = buckets.indexOf(h.bucket());
                            heatData[ti][bi] = h.count();
                        }
                        panel.heatmap.rebuild(themes, buckets, heatData);
                    }
                    panel.showPanel("heatmap");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "主题热度分析失败：" + ex.getMessage(),
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
                List<DateTone> series;
                if ("主题".equals(entityType)) {
                    series = queryService.getThemeTrend(name,
                            LocalDate.now().minusMonths(12), LocalDate.now());
                } else {
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
