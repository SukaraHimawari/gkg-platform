package edu.gkg.controller;

import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.UiUtil;
import edu.gkg.mock.MockDownloadService;
import edu.gkg.service.DownloadService;
import edu.gkg.service.impl.GdeltDownloader;
import edu.gkg.view.ImportPanel;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class DownloadController {

    private static final LocalDate V2_START = LocalDate.of(2015, 2, 19);

    private final ImportPanel panel;
    private final DownloadService downloader;
    private final ImportController importCtrl;
    private final AtomicReference<SwingWorker<?, ?>> current = new AtomicReference<>();

    public DownloadController(ImportPanel panel, ImportController importCtrl) {
        this(panel, chooseDownloader(), importCtrl);
    }

    public DownloadController(ImportPanel panel, DownloadService downloader, ImportController importCtrl) {
        this.panel = panel;
        this.downloader = downloader;
        this.importCtrl = importCtrl;
        wire();
    }

    static DownloadService chooseDownloader() {
        return "true".equalsIgnoreCase(System.getProperty("mock", "false"))
                ? new MockDownloadService() : new GdeltDownloader();
    }

    private void wire() {
        panel.downloadBtn.addActionListener(e -> start());
    }

    private void start() {
        if (current.get() != null && !current.get().isDone()) {
            UiUtil.warn(panel, "当前已有下载或导入任务在运行，请稍候。");
            return;
        }
        LocalDate date = panel.datePicker.getSelectedDate();
        LocalDate today = LocalDate.now();

        if (date.isAfter(today)) {
            UiUtil.warn(panel, date + " 是未来日期，GDELT 尚未收录。");
            return;
        }
        if (date.isBefore(V2_START)) {
            UiUtil.warn(panel, "GDELT v2 从 " + V2_START + " 起才有数据。");
            return;
        }
        if (date.isEqual(today)) {
            int c = JOptionPane.showConfirmDialog(panel,
                    "今天的数据可能尚未完整。继续下载吗？",
                    "提示", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }

        String ds = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = resolve(ds);
        panel.progressBar().setValue(0);
        panel.currentFileLabel().setText("当前任务：下载 " + ds + " 到 " + dir);
        panel.speedLabel().setText("正在连接 GDELT...");
        panel.downloadBtn.setEnabled(false);
        panel.runBtn.setEnabled(false);

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
                panel.downloadBtn.setEnabled(true);
                panel.runBtn.setEnabled(true);
                if (isCancelled()) return;
                try {
                    DownloadResult r = get();
                    panel.speedLabel().setText("下载完成，耗时 " + fmtMs(r.elapsedMs()));
                    panel.progressBar().setValue(100);
                    panel.historyModel.insertRow(0,
                            new Object[]{java.time.LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                    "GDELT 在线 " + ds, "在线下载", dir.toString(),
                                    r.success(), "-", r.failed(), fmtMs(r.elapsedMs())});

                    if (r.success() == 0 && r.failed() > 0) {
                        JOptionPane.showMessageDialog(panel,
                                "日期 " + ds + " 没有可下载的 GKG 文件，或全部下载失败。请换一个日期重试。",
                                "下载结果", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String msg = r.failed() > 0
                            ? String.format("下载完成（部分成功）：成功 %d / %d，失败 %d，耗时 %s",
                                    r.success(), r.total(), r.failed(), fmtMs(r.elapsedMs()))
                            : String.format("下载完成：共 %d 个文件，耗时 %s", r.success(), fmtMs(r.elapsedMs()));
                    UiUtil.info(panel, msg);

                    if (panel.autoImportCb.isSelected() && r.success() > 0) {
                        panel.currentFileLabel().setText("当前任务：下载完成，开始自动导入");
                        panel.progressBar().setValue(0);
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

    void cancelCurrentTaskForTests() {
        SwingWorker<?, ?> worker = current.get();
        if (worker != null) worker.cancel(true);
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
