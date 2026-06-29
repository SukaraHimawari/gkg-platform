package edu.gkg.controller;

import edu.gkg.common.DatePickerField;
import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.UiUtil;
import edu.gkg.mock.MockDownloadService;
import edu.gkg.service.DownloadService;
import edu.gkg.service.impl.GdeltDownloader;
import edu.gkg.view.ImportPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 协调 ImportPanel（在线下载区块）←→ DownloadService。
 * 下载完成后若勾选「自动导入」，复用 ImportController.startImport() 触发导入。
 */
public class DownloadController {

    private static final LocalDate V2_START = LocalDate.of(2015, 2, 19);

    private final ImportPanel          panel;
    private final DownloadService      downloader;
    private final ImportController     importCtrl;
    private final AtomicReference<SwingWorker<?,?>> current = new AtomicReference<>();

    /** 默认注入 Mock 下载，方便 A 独立演示。 */
    public DownloadController(ImportPanel panel, ImportController importCtrl) {
        this(panel, chooseDownloader(), importCtrl);
    }

    public DownloadController(ImportPanel panel, DownloadService downloader,
                              ImportController importCtrl) {
        this.panel      = panel;
        this.downloader = downloader;
        this.importCtrl = importCtrl;
        wire();
    }

    private static DownloadService chooseDownloader() {
        return "false".equalsIgnoreCase(System.getProperty("mock", "true"))
                ? new GdeltDownloader() : new MockDownloadService();
    }

    private void wire() {
        panel.downloadBtn.addActionListener(e -> start());
    }

    private void start() {
        if (current.get() != null && !current.get().isDone()) {
            UiUtil.warn(panel, "当前已有任务在运行，请稍候。"); return;
        }
        LocalDate date  = panel.datePicker.getSelectedDate();
        LocalDate today = LocalDate.now();

        // 日期预检
        if (date.isAfter(today)) { UiUtil.warn(panel, date + " 是未来日期，尚未收录。"); return; }
        if (date.isBefore(V2_START)) { UiUtil.warn(panel, "GDELT v2 从 " + V2_START + " 起才有数据。"); return; }
        if (date.isEqual(today)) {
            int c = JOptionPane.showConfirmDialog(panel,
                    "今天的数据可能尚未完整（每 15 分钟更新一次）。继续吗？",
                    "提示", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }

        String ds = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir  = resolve(ds);
        panel.progressBar().setValue(0);

        SwingWorker<DownloadResult, Void> w = new SwingWorker<>() {
            @Override protected DownloadResult doInBackground() {
                return downloader.downloadDate(date, dir,
                        t -> SwingUtilities.invokeLater(() -> {
                            panel.progressBar().setValue(t.percent());
                            panel.currentFileLabel().setText(t.currentFile());
                            panel.speedLabel().setText(t.message());
                        }));
            }
            @Override protected void done() {
                try {
                    DownloadResult r = get();
                    panel.speedLabel().setText("完成，耗时 " + fmtMs(r.elapsedMs()));
                    panel.progressBar().setValue(100);
                    panel.historyModel.insertRow(0,
                            new Object[]{java.time.LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                    "GDELT 在线 " + ds, "在线下载", "~350 MB",
                                    r.success(), "—", r.failed(), fmtMs(r.elapsedMs())});

                    if (r.success() == 0 && r.failed() > 0) {
                        JOptionPane.showMessageDialog(panel,
                                "日期 " + ds + " 无 GKG 数据（所有文件均返回 404）。\n请换一个日期重试。",
                                "下载结果", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String msg = r.failed() > 0
                            ? String.format("完成（部分成功）\n成功 %d / %d，失败 %d，耗时 %s",
                                    r.success(), r.total(), r.failed(), fmtMs(r.elapsedMs()))
                            : String.format("完成！共 %d 个文件，耗时 %s",
                                    r.success(), fmtMs(r.elapsedMs()));
                    UiUtil.info(panel, msg);

                    // 自动导入
                    if (panel.autoImportCb.isSelected() && r.success() > 0) {
                        SwingUtilities.invokeLater(() -> importCtrl.startImport(dir.toFile()));
                    }
                } catch (Exception ex) {
                    UiUtil.error(panel, "下载异常", ex);
                }
            }
        };
        current.set(w);
        w.execute();
    }

    private static Path resolve(String ds) {
        Path c = Paths.get("data", "gkg-raw", ds);
        if (Files.isDirectory(c.getParent())) return c;
        return Paths.get(System.getProperty("user.home"), "gkg-data", ds);
    }

    private static String fmtMs(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
