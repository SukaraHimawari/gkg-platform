package edu.gkg.controller;

import edu.gkg.common.GuiTask;
import edu.gkg.common.SharedRecords.*;
import edu.gkg.common.UiUtil;
import edu.gkg.service.ExportService;
import edu.gkg.service.QueryService;
import edu.gkg.view.QueryPanel;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * 协调 QueryPanel ←→ QueryService / ExportService。
 * 综合查询、人物档案、组织档案、主题追踪 四个子页都走这里。
 */
public class QueryController {

    private static final int PAGE_SIZE = 50;

    private final QueryPanel   panel;
    private final QueryService query;
    private final ExportService exporter;

    private int currentPage = 0;
    private Page<SearchRow> lastPage;

    public QueryController(QueryPanel panel, QueryService query, ExportService exporter) {
        this.panel    = panel;
        this.query    = query;
        this.exporter = exporter;
        bind();
    }

    private void bind() {
        panel.searchButton().addActionListener(e -> { currentPage = 0; runSearch(); });
        panel.resetButton().addActionListener(e -> panel.resetCondition());
        panel.exportButton().addActionListener(e -> exportCurrentPage());
        panel.prevPageButton().addActionListener(e -> {
            if (currentPage > 0) { currentPage--; runSearch(); }
        });
        panel.nextPageButton().addActionListener(e -> { currentPage++; runSearch(); });

        panel.personSearchButton().addActionListener(e -> runProfile(EntityType.PERSON));
        panel.orgSearchButton().addActionListener(e -> runProfile(EntityType.ORG));

        panel.themeTrackPanel().drawButton().addActionListener(e -> runThemeTrend());
    }

    private void runSearch() {
        QueryCondition cond = panel.currentCondition();
        new GuiTask<Page<SearchRow>>(panel, null,
                p -> { lastPage = p; panel.renderSearchPage(p); },
                (owner, ex) -> UiUtil.error(owner, "查询失败", ex)) {
            @Override protected Page<SearchRow> doWork() {
                return query.search(cond, currentPage, PAGE_SIZE);
            }
        }.execute();
    }

    private void runProfile(EntityType type) {
        String name = (type == EntityType.PERSON
                ? panel.personNameField()
                : panel.orgNameField()).getText().trim();
        if (name.isEmpty()) { UiUtil.warn(panel, "请输入" + (type == EntityType.PERSON ? "人物" : "组织") + "名"); return; }

        new GuiTask<EntityProfile>(panel, null,
                p -> panel.renderProfile(p),
                (owner, ex) -> UiUtil.error(owner, "档案查询失败", ex)) {
            @Override protected EntityProfile doWork() {
                return type == EntityType.PERSON
                        ? query.getPersonProfile(name)
                        : query.getOrgProfile(name);
            }
        }.execute();
    }

    private void runThemeTrend() {
        String code = panel.themeTrackPanel().themeCode();
        if (code.isEmpty()) { UiUtil.warn(panel, "请输入主题代码"); return; }
        new GuiTask<List<DateTone>>(panel, null,
                series -> panel.themeTrackPanel().renderTrend(code + " 趋势", series),
                (owner, ex) -> UiUtil.error(owner, "趋势查询失败", ex)) {
            @Override protected List<DateTone> doWork() {
                return query.getThemeTrend(code, panel.themeTrackPanel().from(), panel.themeTrackPanel().to());
            }
        }.execute();
    }

    private void exportCurrentPage() {
        if (lastPage == null || lastPage.rows().isEmpty()) {
            UiUtil.warn(panel, "请先执行一次查询。");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("query_page_" + (currentPage + 1) + ".csv"));
        if (fc.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();

        new GuiTask<File>(panel, null,
                done -> UiUtil.info(panel, "已导出到：" + done.getAbsolutePath()),
                (owner, ex) -> UiUtil.error(owner, "导出失败", ex)) {
            @Override protected File doWork() throws Exception {
                String n = target.getName().toLowerCase();
                if (n.endsWith(".xlsx"))      exporter.exportExcel(lastPage.rows(), target);
                else if (n.endsWith(".json")) exporter.exportJson(lastPage.rows(), target);
                else                          exporter.exportCsv(lastPage.rows(), target);
                return target;
            }
        }.execute();
    }
}
