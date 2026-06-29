package edu.gkg.controller;

import edu.gkg.service.CleanResult;
import edu.gkg.service.ExportService;
import edu.gkg.service.ImportResult;
import edu.gkg.service.ImportService;
import edu.gkg.view.ImportPanel;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class ImportController {

    private final ImportPanel  panel;
    private final ImportService importService;
    private final ExportService exportService;
    private final Runnable dataChangedListener;

    private final AtomicReference<SwingWorker<?,?>> currentWorker = new AtomicReference<>();
    private volatile File lastImportedFile;
    private volatile ImportSummary lastImportSummary;

    public ImportController(ImportPanel panel,
                            ImportService importService,
                            ExportService exportService) {
        this(panel, importService, exportService, () -> {});
    }

    public ImportController(ImportPanel panel,
                            ImportService importService,
                            ExportService exportService,
                            Runnable dataChangedListener) {
        this.panel         = panel;
        this.importService = importService;
        this.exportService = exportService;
        this.dataChangedListener = dataChangedListener != null ? dataChangedListener : () -> {};
        wire();
    }

    private void wire() {
        // DropZone 拖入文件
        panel.drop.setConsumer(files -> {
            if (!files.isEmpty()) startImport(files.get(0));
        });

        // 开始导入
        panel.runBtn.addActionListener(e -> chooseAndImport());
        panel.drop.browseButton().addActionListener(e -> chooseAndImport());

        // 停止导入
        panel.stopBtn.addActionListener(e -> {
            SwingWorker<?,?> w = currentWorker.get();
            if (w != null && !w.isDone()) {
                w.cancel(true);
                panel.currentFileLabel().setText("当前文件：已停止");
                panel.speedLabel().setText("已停止");
                setButtons(true);
            }
        });

        // 数据清理
        panel.cleanBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(panel,
                    "确认清理无效数据？此操作不可撤销。",
                    "数据清理", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            SwingWorker<CleanResult, Void> worker = new SwingWorker<>() {
                @Override protected CleanResult doInBackground() {
                    return importService.cleanInvalidData();
                }
                @Override protected void done() {
                    try {
                        CleanResult r = get();
                        int total = r.duplicateRemoved() + r.nullThemeRemoved() + r.invalidRowRemoved();
                        JOptionPane.showMessageDialog(panel,
                                String.format("清理完成：重复 %d 条，空主题 %d 条，无效行 %d 条，共删除 %d 条",
                                        r.duplicateRemoved(), r.nullThemeRemoved(),
                                        r.invalidRowRemoved(), total),
                                "清理完成", JOptionPane.INFORMATION_MESSAGE);
                        dataChangedListener.run();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel,
                                "清理失败：" + ex.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        panel.clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(panel,
                    "将清空所有已导入记录、实体、主题、位置、引用和共现分析结果，且不可恢复。确认继续？",
                    "清空数据库", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            SwingWorker<CleanResult, Void> worker = new SwingWorker<>() {
                @Override protected CleanResult doInBackground() {
                    return importService.clearDatabase();
                }
                @Override protected void done() {
                    try {
                        CleanResult r = get();
                        applyClearResult(r);
                        JOptionPane.showMessageDialog(panel,
                                "数据库已清空，共删除 " + r.invalidRowRemoved() + " 条表记录。",
                                "清空完成", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel,
                                "清空失败：" + ex.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        // 结果导出
        panel.exportBtn.addActionListener(e -> {
            if (lastImportedFile == null) {
                JOptionPane.showMessageDialog(panel,
                        "请先完成一次导入操作", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("import_result.csv"));
            if (fc.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                try {
                    exportService.exportCsv(java.util.List.of(lastImportSummary), fc.getSelectedFile());
                    JOptionPane.showMessageDialog(panel,
                            "导出成功：" + fc.getSelectedFile().getName(),
                            "导出完成", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "导出失败：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public void startImport(File file) {
        setButtons(false);
        panel.progressBar().setValue(0);
        panel.currentFileLabel().setText("当前文件：" + file.getName());
        panel.successCard().value("0");
        panel.skipCard().value("0");
        panel.failCard().value("0");

        SwingWorker<ImportResult, Object[]> worker = new SwingWorker<>() {
            final long startMs = System.currentTimeMillis();

            @Override protected ImportResult doInBackground() {
                return file.isDirectory()
                    ? importService.importDirectory(file, (pct, msg) ->
                            publish(new Object[]{pct, msg}))
                    : importService.importFile(file, (pct, msg) ->
                            publish(new Object[]{pct, msg}));
            }

            @Override protected void process(java.util.List<Object[]> chunks) {
                Object[] last = chunks.get(chunks.size() - 1);
                int pct = (int) last[0];
                String msg = (String) last[1];
                panel.progressBar().setValue(pct);
                panel.currentFileLabel().setText("当前文件：" + file.getName() + "  " + msg);
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                panel.speedLabel().setText(String.format("已用时 %02d:%02d:%02d",
                        elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60));
            }

            @Override protected void done() {
                setButtons(true);
                if (isCancelled()) return;
                try {
                    ImportResult r = get();
                    lastImportedFile = file;
                    lastImportSummary = ImportSummary.from(file, r);
                    panel.successCard().value(String.valueOf(r.success()));
                    panel.skipCard().value(String.valueOf(r.skipped()));
                    panel.failCard().value(String.valueOf(r.total() - r.success() - r.skipped()));
                    panel.progressBar().setValue(100);
                    String elapsed = formatMs(r.elapsedMs());
                    String size = file.isFile()
                            ? String.format("%.1f MB", file.length() / 1048576.0)
                            : "目录";
                    String ts = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    panel.addHistoryRow(ts, file.getName(),
                            file.isDirectory() ? "目录" : "文件",
                            size, r.success(), r.skipped(),
                            r.total() - r.success() - r.skipped(), elapsed);
                    dataChangedListener.run();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel,
                            "导入失败：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        currentWorker.set(worker);
        worker.execute();
    }

    void applyClearResultForTests(CleanResult result) {
        applyClearResult(result);
    }

    void cancelCurrentTaskForTests() {
        SwingWorker<?, ?> worker = currentWorker.get();
        if (worker != null) worker.cancel(true);
    }

    private void applyClearResult(CleanResult result) {
        panel.successCard().value("0");
        panel.skipCard().value("0");
        panel.failCard().value("0");
        panel.progressBar().setValue(0);
        panel.currentFileLabel().setText("当前文件：—");
        dataChangedListener.run();
    }

    private void chooseAndImport() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (fc.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            startImport(fc.getSelectedFile());
        }
    }

    private void setButtons(boolean enabled) {
        panel.runBtn.setEnabled(enabled);
        panel.cleanBtn.setEnabled(enabled);
        panel.clearBtn.setEnabled(enabled);
        panel.exportBtn.setEnabled(enabled);
        panel.stopBtn.setEnabled(!enabled);
    }

    private static String formatMs(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    private record ImportSummary(
            String importedAt,
            String source,
            String type,
            long total,
            long success,
            long skipped,
            long failed,
            long elapsedMs) {
        static ImportSummary from(File file, ImportResult result) {
            String importedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return new ImportSummary(
                    importedAt,
                    file.getAbsolutePath(),
                    file.isDirectory() ? "directory" : "file",
                    result.total(),
                    result.success(),
                    result.skipped(),
                    result.total() - result.success() - result.skipped(),
                    result.elapsedMs());
        }
    }
}
