package edu.gkg.controller;

import edu.gkg.common.SharedRecords.*;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.ExportService;
import edu.gkg.service.QueryService;
import edu.gkg.view.QueryPanel;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class QueryController {

    private final QueryPanel   panel;
    private final QueryService queryService;
    private final ExportService exportService;

    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;

    public QueryController(QueryPanel panel,
                           QueryService queryService,
                           ExportService exportService) {
        this.panel         = panel;
        this.queryService  = queryService;
        this.exportService = exportService;
        wire();
    }

    private void wire() {
        wireSuggestions();

        // 搜索按钮
        panel.searchButton.addActionListener(e -> doSearch(1));

        // 重置按钮
        panel.resetButton.addActionListener(e -> {
            panel.dateFromField.setText("2024-01-15");
            panel.dateToField.setText("2024-01-15");
            panel.themeCodeField.setText("");
            panel.personNameField.setText("");
            panel.orgNameField.setText("");
            panel.locationNameField.setText("");
            panel.resultTableModel.setRowCount(0);
            panel.pageLabel.setText("第 1 页 / 共 1 页");
            currentPage = 1;
        });

        // 分页
        panel.prevButton.addActionListener(e -> {
            if (currentPage > 1) doSearch(currentPage - 1);
        });
        panel.nextButton.addActionListener(e -> doSearch(currentPage + 1));

        // 导出结果
        panel.exportButton.addActionListener(e -> {
            if (panel.resultTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(panel,
                        "请先执行搜索", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("query_result.csv"));
            if (fc.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                QueryCondition cond = buildCondition();
                SwingWorker<Void, Void> w = new SwingWorker<>() {
                    @Override protected Void doInBackground() throws Exception {
                        Page<GkgRecord> page = queryService.search(cond, 1, Integer.MAX_VALUE);
                        exportService.exportCsv(page.rows(), fc.getSelectedFile());
                        return null;
                    }
                    @Override protected void done() {
                        try {
                            get();
                            JOptionPane.showMessageDialog(panel,
                                    "导出成功：" + fc.getSelectedFile().getName(),
                                    "完成", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(panel,
                                    "导出失败：" + ex.getMessage(),
                                    "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                w.execute();
            }
        });

        // 人物档案查询
        panel.personSearchButton.addActionListener(e -> {
            String name = panel.personSearchField.getText().trim();
            if (name.isEmpty()) return;
            SwingWorker<ProfileResult, Void> w = new SwingWorker<>() {
                @Override protected ProfileResult doInBackground() {
                    EntityProfile profile = queryService.getPersonProfile(name);
                    List<DateTone> timeline = queryService.getEntityTrend("PERSON", name, null, null);
                    return new ProfileResult(profile, timeline);
                }
                @Override protected void done() {
                    try {
                        ProfileResult result = get();
                        EntityProfile prof = result.profile();
                        panel.personNewsCard.value(String.valueOf(prof.newsCount()));
                        panel.personToneCard.value(String.format("%.2f", prof.avgTone()));
                        panel.personThemeCard.value(String.valueOf(prof.themeCount()));
                        panel.personRatioCard.value("—");
                        panel.renderPersonProfileCharts(prof.relatedOrganizations(), prof.relatedPeople(), result.timeline());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel,
                                "查询失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            w.execute();
        });

        // 组织档案查询
        panel.orgSearchButton.addActionListener(e -> {
            String name = panel.orgSearchField.getText().trim();
            if (name.isEmpty()) return;
            SwingWorker<ProfileResult, Void> w = new SwingWorker<>() {
                @Override protected ProfileResult doInBackground() {
                    EntityProfile profile = queryService.getOrgProfile(name);
                    List<DateTone> timeline = queryService.getEntityTrend("ORG", name, null, null);
                    return new ProfileResult(profile, timeline);
                }
                @Override protected void done() {
                    try {
                        ProfileResult result = get();
                        EntityProfile prof = result.profile();
                        panel.orgNewsCard.value(String.valueOf(prof.newsCount()));
                        panel.orgToneCard.value(String.format("%.2f", prof.avgTone()));
                        panel.orgThemeCard.value(String.valueOf(prof.themeCount()));
                        panel.orgRatioCard.value("—");
                        panel.renderOrgProfileCharts(prof.relatedPeople(), prof.relatedOrganizations(), result.timeline());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel,
                                "查询失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            w.execute();
        });

        panel.themeTrackPanel.plotButton.addActionListener(e -> plotThemeTrend());
    }

    private void wireSuggestions() {
        panel.personNameField.setSuggestionProvider(prefix -> queryService.suggestPerson(prefix, 10));
        panel.personSearchField.setSuggestionProvider(prefix -> queryService.suggestPerson(prefix, 10));
        panel.orgNameField.setSuggestionProvider(prefix -> queryService.suggestOrg(prefix, 10));
        panel.orgSearchField.setSuggestionProvider(prefix -> queryService.suggestOrg(prefix, 10));
        panel.locationNameField.setSuggestionProvider(prefix -> queryService.suggestLocation(prefix, 10));
    }

    private void doSearch(int page) {
        QueryCondition cond = buildCondition();
        panel.searchButton.setEnabled(false);
        SwingWorker<Page<GkgRecord>, Void> w = new SwingWorker<>() {
            @Override protected Page<GkgRecord> doInBackground() {
                return queryService.search(cond, page, PAGE_SIZE);
            }
            @Override protected void done() {
                panel.searchButton.setEnabled(true);
                try {
                    Page<GkgRecord> result = get();
                    currentPage = result.page();
                    panel.resultTableModel.setRowCount(0);
                    for (GkgRecord r : result.rows()) {
                        panel.resultTableModel.addRow(new Object[]{
                                r.getRecordId(),
                                r.getPublishDate(),
                                r.getSourceCommonName(),
                                r.getTone() != null ? String.format("%.2f", r.getTone()) : "—",
                                r.getDocumentId()
                        });
                    }
                    panel.pageLabel.setText(String.format("第 %d 页 / 共 %d 页",
                            result.page(), result.totalPages()));
                    panel.prevButton.setEnabled(result.page() > 1);
                    panel.nextButton.setEnabled(result.page() < result.totalPages());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "查询失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void plotThemeTrend() {
        String theme = panel.themeTrackPanel.themeCodeField.getText().trim();
        if (theme.isEmpty()) return;
        LocalDate from = parseDate(panel.themeTrackPanel.fromDateField.getText(), null);
        LocalDate to = parseDate(panel.themeTrackPanel.toDateField.getText(), null);
        String normalizedTheme = theme.toUpperCase(java.util.Locale.ROOT);
        panel.themeTrackPanel.plotButton.setEnabled(false);
        SwingWorker<List<DateTone>, Void> w = new SwingWorker<>() {
            @Override protected List<DateTone> doInBackground() {
                return queryService.getThemeTrend(normalizedTheme, from, to);
            }

            @Override protected void done() {
                panel.themeTrackPanel.plotButton.setEnabled(true);
                try {
                    panel.themeTrackPanel.chart().renderFrequencySeries(normalizedTheme, get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "查询失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private QueryCondition buildCondition() {
        LocalDate from = parseDate(panel.dateFromField.getText(), null);
        LocalDate to   = parseDate(panel.dateToField.getText(), null);
        String theme   = panel.themeCodeField.getText().trim();
        String person  = panel.personNameField.getText().trim();
        String org     = panel.orgNameField.getText().trim();
        String loc     = panel.locationNameField.getText().trim();
        return new QueryCondition(
                from, to,
                theme.isEmpty()  ? null : theme,
                person.isEmpty() ? null : person,
                org.isEmpty()    ? null : org,
                loc.isEmpty()    ? null : loc);
    }

    private static LocalDate parseDate(String text, LocalDate fallback) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private record ProfileResult(EntityProfile profile, List<DateTone> timeline) {}
}
