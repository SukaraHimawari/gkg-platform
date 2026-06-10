package edu.gkg.controller;

import edu.gkg.common.GuiTask;
import edu.gkg.common.SharedRecords.ImportResult;
import edu.gkg.common.SharedRecords.ProgressTick;
import edu.gkg.common.UiUtil;
import edu.gkg.service.ExportService;
import edu.gkg.service.ImportService;
import edu.gkg.view.ImportPanel;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 协调 ImportPanel ←→ ImportService / ExportService。
 * 把按钮事件 + SwingWorker 进度回报 + UI 状态绑在一起，UI 类不直接调 Service。
 */
public class ImportController {

    private final ImportPanel    panel;
    private final ImportService  importer;
    private final ExportService  exporter;
    private final Runnable       afterImport;
    private final AtomicReference<GuiTask<?>> currentTask = new AtomicReference<>();

    public ImportController(ImportPanel panel,
                            ImportService importer,
                            ExportService exporter,
                            Runnable afterImport) {
        this.panel = panel;
        this.importer = importer;
        this.exporter = exporter;
        this.afterImport = afterImport != null ? afterImport : () -> {};
        bind();
    }

    private void bind() {
        panel.setOnFilesChosen(files -> { /* pendingFiles 已由 Panel 内部存 */ });
        panel.runButton().addActionListener(e -> startImport());
        panel.stopButton().addActionListener(e -> stopImport());
        panel.cleanButton().addActionListener(e -> runClean());
        panel.exportButton().addActionListener(e -> chooseAndExport());
    }

    private void startImport() {
        List<File> files = panel.pendingFiles();
        if (files.isEmpty()) {
            UiUtil.warn(panel, "请先选择要导入的文件或文件夹。");
            return;
        }
        if (currentTask.get() != null && !currentTask.get().isDone()) {
            UiUtil.warn(panel, "当前已有导入任务在跑，请先停止。");
            return;
        }
        panel.resetProgress();

        GuiTask<ImportResult> task = new GuiTask<>(
                panel,
                null, // 进度直接走 invokeLater，不经 GuiTask 的 Progress 通道
                result -> {
                    panel.applyResult(result);
                    String src = files.size() == 1 ? files.get(0).getName() : files.size() + " 项";
                    panel.appendHistory(src, "GKG/CSV", "—", result);
                    panel.clearPending();
                    afterImport.run();
                },
                (owner, ex) -> UiUtil.error(owner, "导入失败", ex)) {
            @Override
            protected ImportResult doWork() {
                return importer.importFiles(files, tick ->
                        SwingUtilities.invokeLater(() -> panel.applyProgress(tick)));
            }
        };
        currentTask.set(task);
        task.execute();
    }

    private void stopImport() {
        GuiTask<?> t = currentTask.get();
        if (t == null || t.isDone()) {
            UiUtil.info(panel, "当前没有正在跑的导入任务。");
            return;
        }
        t.cancel(true);
        panel.resetProgress();
    }

    private void runClean() {
        if (!UiUtil.confirm(panel, "确定要扫描并删除无效数据吗？")) return;
        new GuiTask<Integer>(panel, null,
                deleted -> {
                    UiUtil.info(panel, "已清理 " + deleted + " 条无效记录。");
                    afterImport.run();
                },
                (owner, ex) -> UiUtil.error(owner, "清理失败", ex)) {
            @Override protected Integer doWork() { return importer.cleanInvalidData(); }
        }.execute();
    }

    private void chooseAndExport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("gkg_export.csv"));
        if (fc.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();
        new GuiTask<File>(panel, null,
                done -> UiUtil.info(panel, "已导出到：" + done.getAbsolutePath()),
                (owner, ex) -> UiUtil.error(owner, "导出失败", ex)) {
            @Override protected File doWork() throws Exception {
                exporter.exportCsv(java.util.List.of(), target); // 暂用空集合占位
                return target;
            }
        }.execute();
    }
}
