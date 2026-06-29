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
 * 镜像策略：HTTP 4xx → 立即放弃该文件；5xx/超时 → 重试 3 次后换 HTTPS
 * 早停：连续 4 个 404 判定该日期无数据，立即终止
 * 断点续传：本地已有且非空直接跳过
 */
public class GdeltDownloader implements DownloadService {

    private static final Logger LOG = Logger.getLogger(GdeltDownloader.class.getName());

    private static final List<String> MIRROR_BASES = List.of(
            "http://data.gdeltproject.org/gdeltv2/",
            "https://data.gdeltproject.org/gdeltv2/");

    private static final int MAX_RETRY = 3;
    private static final int TOTAL_SLOTS = 96;
    private static final int EARLY_ABORT = 4;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration REQ_TIMEOUT = Duration.ofSeconds(60);

    private enum Dl { OK, NOT_FOUND, ERR }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();

    @Override
    public DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink) {
        long t0 = System.currentTimeMillis();
        try { Files.createDirectories(destDir); } catch (IOException e) {
            LOG.log(Level.SEVERE, "无法创建目录: " + destDir, e);
        }
        List<String> slots = buildSlots(date);
        int ok = 0, fail = 0, consecNotFound = 0;
        for (int i = 0; i < slots.size(); i++) {
            if (Thread.currentThread().isInterrupted()) break;
            String fn = slots.get(i) + ".gkg.csv.zip";
            Path tgt = destDir.resolve(fn);
            if (Files.exists(tgt) && isNonEmpty(tgt)) { ok++; consecNotFound = 0; tick(sink, i + 1, slots.size(), fn, "已存在，跳过"); continue; }
            Dl d = download(fn, tgt, i, slots.size(), sink);
            switch (d) {
                case OK        -> { ok++; consecNotFound = 0; }
                case NOT_FOUND -> { fail++; consecNotFound++;
                    if (consecNotFound >= EARLY_ABORT) {
                        String msg = "前 " + (i + 1) + " 个文件均 404，该日期无 GKG 数据，已提前终止";
                        LOG.warning(msg);
                        tick(sink, slots.size(), slots.size(), "(终止)", "⛔ " + msg);
                        fail += slots.size() - (i + 1);
                        return new DownloadResult(slots.size(), ok, fail, System.currentTimeMillis() - t0);
                    }
                }
                case ERR       -> { fail++; consecNotFound = 0; }
            }
        }
        return new DownloadResult(slots.size(), ok, fail, System.currentTimeMillis() - t0);
    }

    private Dl download(String filename, Path target, int idx, int total, Consumer<ProgressTick> sink) {
        for (String base : MIRROR_BASES) {
            String url = base + filename;
            for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
                if (Thread.currentThread().isInterrupted()) return Dl.ERR;
                try {
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                            .timeout(REQ_TIMEOUT).GET().build();
                    HttpResponse<InputStream> resp = http.send(req,
                            HttpResponse.BodyHandlers.ofInputStream());
                    int code = resp.statusCode();
                    if (code == 200) {
                        try (InputStream is = resp.body();
                             OutputStream os = Files.newOutputStream(target)) { is.transferTo(os); }
                        tick(sink, idx + 1, total, filename,
                                String.format("✓ %.1f MB", fileMb(target)));
                        return Dl.OK;
                    } else if (code >= 400 && code < 500) {
                        LOG.warning(url + " → HTTP " + code);
                        tick(sink, idx + 1, total, filename, "⚠ 404 文件不存在");
                        return Dl.NOT_FOUND;
                    } else {
                        LOG.warning(url + " → HTTP " + code + "，重试 " + attempt);
                        sleep(attempt * 2000L);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); return Dl.ERR; }
                catch (Exception e) { LOG.log(Level.WARNING, url + " 异常: " + e.getMessage()); sleep(attempt * 2000L); }
            }
        }
        LOG.severe("所有镜像均失败：" + filename);
        tick(sink, idx + 1, total, filename, "⚠ 网络错误，已跳过");
        try { Files.deleteIfExists(target); } catch (IOException ignored) {}
        return Dl.ERR;
    }

    private static List<String> buildSlots(LocalDate d) {
        String day = d.format(DateTimeFormatter.BASIC_ISO_DATE);
        List<String> s = new ArrayList<>(TOTAL_SLOTS);
        for (int h = 0; h < 24; h++)
            for (int m = 0; m < 60; m += 15)
                s.add(String.format("%s%02d%02d%02d", day, h, m, 0));
        return s;
    }

    private static void tick(Consumer<ProgressTick> sink, int done, int total, String f, String msg) {
        if (sink == null) return;
        int pct = (int) Math.round(done * 100.0 / total);
        sink.accept(new ProgressTick(pct, "下载中 " + done + "/" + total + ": " + f, msg));
    }

    private static boolean isNonEmpty(Path p) { try { return Files.size(p) > 0; } catch (IOException e) { return false; } }
    private static double fileMb(Path p) { try { return Files.size(p) / 1e6; } catch (IOException e) { return 0; } }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
