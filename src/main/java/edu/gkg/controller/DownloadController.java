package edu.gkg.controller;

import edu.gkg.common.GuiTask;
import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.UiUtil;
import edu.gkg.service.DownloadService;
import edu.gkg.service.ImportService;
import edu.gkg.view.ImportPanel;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 协调 ImportPanel（在线下载区块）←→ DownloadService。
 * 下载完成后若用户勾选「自动导入」，通过 ImportController 发起导入。
 */
public class DownloadController {

    /** GDELT v2 GKG 最早有数据的日期。 */
    private static final LocalDate GDELT_V2_START = LocalDate.of(2015, 2, 19);

    private final ImportPanel      panel;
    private final DownloadService  downloader;
    private final ImportService    importer;
    private final ImportController importCtrl;
    private final AtomicReference<GuiTask<?>> currentTask = new AtomicReference<>();

    public DownloadController(ImportPanel panel,
                              DownloadService downloader,
                              ImportService importer,
                              ImportController importCtrl) {
        this.panel       = panel;
        this.downloader  = downloader;
        this.importer    = importer;
        this.importCtrl  = importCtrl;
        bind();
    }

    private void bind() {
        panel.downloadButton().addActionListener(e -> startDownload());
    }

    private void startDownload() {
        if (currentTask.get() != null && !currentTask.get().isDone()) {
            UiUtil.warn(panel, "当前已有任务在运行，请稍候。");
            return;
        }

        LocalDate date  = panel.selectedDate();
        LocalDate today = LocalDate.now();

        // ===== 日期合法性预检（即时反馈，不发任何网络请求）=====
        if (date.isAfter(today)) {
            UiUtil.warn(panel,
                    "所选日期 " + date + " 是未来日期，GDELT 尚未收录。\n请选择今天或之前的日期。");
            return;
        }
        if (date.isBefore(GDELT_V2_START)) {
            UiUtil.warn(panel,
                    "GDELT v2 GKG 数据从 " + GDELT_V2_START + " 起才有记录。\n"
                    + "所选日期 " + date + " 过早，请选择 2015-02-19 及以后的日期。");
            return;
        }
        if (date.isEqual(today)) {
            // 当天数据可能仍在更新，给出提示但允许继续
            int choice = JOptionPane.showConfirmDialog(panel,
                    "今天（" + date + "）的 GKG 数据可能尚未完整（每 15 分钟更新一次）。\n"
                    + "已发布的文件将正常下载，未发布的会被跳过，继续吗？",
                    "提示", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
        }

        String dateStr = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        Path   destDir = resolveDestDir(dateStr);
        panel.resetProgress();

        GuiTask<DownloadResult> task = new GuiTask<>(
                panel,
                null,
                result -> onDownloadDone(result, dateStr, destDir),
                (owner, ex) -> UiUtil.error(owner, "下载过程中出现异常", ex)) {
            @Override
            protected DownloadResult doWork() {
                return downloader.downloadDate(date, destDir,
                        tick -> SwingUtilities.invokeLater(() -> panel.applyProgress(tick)));
            }
        };
        currentTask.set(task);
        task.execute();
    }

    private void onDownloadDone(DownloadResult result, String dateStr, Path destDir) {
        panel.appendDownloadHistory(dateStr, result);

        // ===== 按结果区分反馈 =====
        if (result.success() == 0 && result.failed() > 0) {
            // 全部失败：可能是该日期无数据，也可能是网络问题
            boolean likelyNoData = result.failed() >= result.total();
            String msg = likelyNoData
                    ? "日期 " + dateStr + " 在 GDELT 服务器上没有 GKG 数据（所有文件均返回 404）。\n"
                      + "可能原因：\n"
                      + "  • 该日期的 GKG 数据确实不存在（GDELT 偶有缺档）\n"
                      + "  • 日期过于久远或过于新近\n\n"
                      + "请换一个日期重试（建议从 2016 年起的常规日期）。"
                    : "日期 " + dateStr + " 下载失败（共 " + result.total()
                      + " 个文件，失败 " + result.failed() + " 个）。\n"
                      + "可能是网络问题，请检查网络后重试。";
            JOptionPane.showMessageDialog(panel, msg, "下载结果",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (result.success() > 0 && result.failed() > 0) {
            // 部分成功
            UiUtil.info(panel, String.format(
                    "日期 %s 下载完成（部分成功）\n成功 %d 个，失败 %d 个，耗时 %s\n\n"
                    + "失败的文件可能在 GDELT 有缺档，属正常情况。",
                    dateStr, result.success(), result.failed(),
                    formatElapsed(result.elapsedMs())));
        } else {
            // 全部成功
            UiUtil.info(panel, String.format(
                    "日期 %s 下载完成\n共 %d 个文件，全部成功，耗时 %s",
                    dateStr, result.success(), formatElapsed(result.elapsedMs())));
        }

        // 自动导入
        if (panel.autoImport() && result.success() > 0) {
            try {
                List<File> files = Files.list(destDir)
                        .filter(p -> p.toString().endsWith(".gkg.csv.zip"))
                        .filter(p -> {
                            try { return Files.size(p) > 0; } catch (Exception e) { return false; }
                        })
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                if (!files.isEmpty()) importCtrl.startImportFiles(files);
            } catch (Exception e) {
                UiUtil.error(panel, "自动导入启动失败", e);
            }
        }
    }

    private static Path resolveDestDir(String dateStr) {
        Path candidate = Paths.get("data", "gkg-raw", dateStr);
        if (Files.isDirectory(candidate.getParent())) return candidate;
        return Paths.get(System.getProperty("user.home"), "gkg-data", dateStr);
    }

    private static String formatElapsed(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
