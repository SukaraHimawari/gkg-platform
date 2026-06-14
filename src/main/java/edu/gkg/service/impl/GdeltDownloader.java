package edu.gkg.service.impl;

import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ProgressTick;
import edu.gkg.service.DownloadService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 从 GDELT 官网下载指定日期全部 96 个 15 分钟 GKG 切片文件。
 *
 * 镜像切换策略：
 *   HTTP 4xx（文件不存在）→ 立即放弃该文件，不再尝试其他镜像；
 *   HTTP 5xx / 网络超时   → 当前镜像最多重试 3 次（指数退避），再换下一镜像；
 *   所有镜像全败          → 计入 failed，记日志，继续下一个文件。
 *
 * 早停机制：
 *   连续 NOT_FOUND（4xx）达到 EARLY_ABORT_THRESHOLD 个后，
 *   判定该日期无 GKG 数据，立即终止，避免浪费 96 次无效请求。
 *
 * 断点续传：本地已存在且大小 > 0 的文件直接跳过。
 */
public class GdeltDownloader implements DownloadService {

    private static final Logger LOG = Logger.getLogger(GdeltDownloader.class.getName());

    private static final List<String> MIRROR_BASES = List.of(
            "http://data.gdeltproject.org/gdeltv2/",
            "https://data.gdeltproject.org/gdeltv2/"
    );

    private static final int      MAX_RETRY_PER_MIRROR  = 3;
    private static final int      TOTAL_SLOTS           = 96;
    /**
     * 连续 NOT_FOUND 数达到此阈值即判定该日期无数据，提前终止。
     * 4 = 第一小时全部 404，足以确定该天无数据。
     */
    private static final int      EARLY_ABORT_THRESHOLD = 4;
    private static final Duration CONNECT_TIMEOUT       = Duration.ofSeconds(20);
    private static final Duration REQUEST_TIMEOUT       = Duration.ofSeconds(60);

    /** 内部下载单文件的三种结局。 */
    private enum DlStatus { OK, NOT_FOUND, NETWORK_ERR }

    private final HttpClient http;

    public GdeltDownloader() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink) {
        long t0 = System.currentTimeMillis();
        try { Files.createDirectories(destDir); } catch (IOException e) {
            LOG.log(Level.SEVERE, "无法创建目录: " + destDir, e);
        }

        List<String> slots = buildSlots(date);
        int success = 0, failed = 0;
        int consecutiveNotFound = 0;

        for (int i = 0; i < slots.size(); i++) {
            if (Thread.currentThread().isInterrupted()) break;

            String filename = slots.get(i) + ".gkg.csv.zip";
            Path   target   = destDir.resolve(filename);

            // 断点续传
            if (Files.exists(target) && isNonEmpty(target)) {
                success++;
                consecutiveNotFound = 0;
                tick(sink, i + 1, slots.size(), filename, "已存在，跳过");
                continue;
            }

            DlStatus status = downloadWithFallback(filename, target, i, slots.size(), sink);
            switch (status) {
                case OK        -> { success++; consecutiveNotFound = 0; }
                case NOT_FOUND -> {
                    failed++;
                    consecutiveNotFound++;
                    // 早停：连续 4 个 404，该日期没有 GKG 数据
                    if (consecutiveNotFound >= EARLY_ABORT_THRESHOLD) {
                        String msg = String.format(
                                "前 %d 个文件均返回 404，该日期无 GKG 数据，已提前终止", i + 1);
                        LOG.warning(msg);
                        tick(sink, slots.size(), slots.size(), "(终止)", "⛔ " + msg);
                        // 把剩余未处理的都计入 failed，让 total = slots.size()
                        failed += slots.size() - (i + 1);
                        return new DownloadResult(slots.size(), success, failed,
                                System.currentTimeMillis() - t0);
                    }
                }
                case NETWORK_ERR -> { failed++; consecutiveNotFound = 0; }
            }
        }

        return new DownloadResult(slots.size(), success, failed, System.currentTimeMillis() - t0);
    }

    // ---------- 核心下载（镜像 + 重试） ----------

    private DlStatus downloadWithFallback(String filename, Path target,
                                          int idx, int total, Consumer<ProgressTick> sink) {
        boolean anyNetworkAttempt = false;

        for (String base : MIRROR_BASES) {
            String url = base + filename;
            for (int attempt = 1; attempt <= MAX_RETRY_PER_MIRROR; attempt++) {
                if (Thread.currentThread().isInterrupted()) return DlStatus.NETWORK_ERR;
                anyNetworkAttempt = true;
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(REQUEST_TIMEOUT)
                            .GET()
                            .build();

                    HttpResponse<InputStream> resp =
                            http.send(req, HttpResponse.BodyHandlers.ofInputStream());

                    int code = resp.statusCode();
                    if (code == 200) {
                        writeStream(resp.body(), target);
                        tick(sink, idx + 1, total, filename,
                                String.format("✓ %.1f MB", fileSize(target) / 1e6));
                        return DlStatus.OK;
                    } else if (code >= 400 && code < 500) {
                        // 4xx = 文件不存在。换镜像也没用（GDELT 只有一个权威源），立即返回。
                        LOG.warning(url + " → HTTP " + code + "（文件不存在）");
                        tick(sink, idx + 1, total, filename, "⚠ 404 文件不存在");
                        return DlStatus.NOT_FOUND;
                    } else {
                        // 5xx：退避重试
                        LOG.warning(url + " → HTTP " + code + "，第 " + attempt + " 次重试");
                        backoff(attempt);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return DlStatus.NETWORK_ERR;
                } catch (Exception e) {
                    LOG.log(Level.WARNING,
                            url + " 第 " + attempt + " 次异常: " + e.getMessage(), e);
                    backoff(attempt);
                }
            }
            // 当前镜像全部 5xx / 超时，换下一个镜像
        }

        if (anyNetworkAttempt) {
            LOG.severe("所有镜像均失败（网络错误）：" + filename);
            tick(sink, idx + 1, total, filename, "⚠ 网络错误，已跳过");
            deleteIfExists(target);
        }
        return DlStatus.NETWORK_ERR;
    }

    // ---------- 工具方法 ----------

    private static List<String> buildSlots(LocalDate date) {
        String day = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<String> slots = new ArrayList<>(TOTAL_SLOTS);
        for (int h = 0; h < 24; h++)
            for (int m = 0; m < 60; m += 15)
                slots.add(String.format("%s%02d%02d%02d", day, h, m, 0));
        return slots;
    }

    private static void writeStream(InputStream in, Path target) throws IOException {
        try (InputStream is = in; OutputStream os = Files.newOutputStream(target)) {
            is.transferTo(os);
        }
    }

    private static void tick(Consumer<ProgressTick> sink, int done, int total,
                              String file, String msg) {
        if (sink == null) return;
        int pct = total == 0 ? 100 : (int) Math.round(done * 100.0 / total);
        sink.accept(new ProgressTick(pct,
                String.format("下载中 %d/%d: %s", done, total, file), msg));
    }

    private static boolean isNonEmpty(Path p) {
        try { return Files.size(p) > 0; } catch (IOException e) { return false; }
    }

    private static long fileSize(Path p) {
        try { return Files.size(p); } catch (IOException e) { return 0; }
    }

    private static void deleteIfExists(Path p) {
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    private static void backoff(int attempt) {
        try { Thread.sleep(attempt * 2000L); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
