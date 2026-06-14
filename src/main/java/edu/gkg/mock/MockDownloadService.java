package edu.gkg.mock;

import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ProgressTick;
import edu.gkg.service.DownloadService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Mock 下载服务：不发起真实 HTTP 请求，仅模拟 96 个文件的下载进度动效，
 * 并在 destDir 创建 96 个 0 字节占位文件（这样「自动导入」流程也能演示）。
 */
public class MockDownloadService implements DownloadService {

    private static final int TOTAL = 96;
    private static final int TICK_MS = 40;   // 每文件耗时 40ms → 全天约 3.8 秒

    @Override
    public DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink) {
        long t0 = System.currentTimeMillis();
        try { Files.createDirectories(destDir); } catch (IOException ignored) {}

        String day = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        int success = 0, failed = 0;

        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (Thread.currentThread().isInterrupted()) {
                    return new DownloadResult(TOTAL, success, failed,
                            System.currentTimeMillis() - t0);
                }
                String filename = String.format("%s%02d%02d%02d.gkg.csv.zip", day, h, m, 0);
                Path target = destDir.resolve(filename);

                try {
                    if (!Files.exists(target)) Files.createFile(target);
                    success++;
                } catch (IOException e) {
                    failed++;
                }

                int done = h * 4 + m / 15 + 1;
                int pct  = done * 100 / TOTAL;
                if (sink != null) {
                    sink.accept(new ProgressTick(pct,
                            String.format("下载中 %d/%d: %s", done, TOTAL, filename),
                            "✓ 模拟下载完成"));
                }
                try { Thread.sleep(TICK_MS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new DownloadResult(TOTAL, success, failed,
                            System.currentTimeMillis() - t0);
                }
            }
        }
        return new DownloadResult(TOTAL, success, failed, System.currentTimeMillis() - t0);
    }
}
