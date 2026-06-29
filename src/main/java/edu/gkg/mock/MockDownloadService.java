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

public class MockDownloadService implements DownloadService {

    private static final int TOTAL = 96;
    private static final int TICK_MS = 40;

    @Override
    public DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink) {
        long t0 = System.currentTimeMillis();
        try { Files.createDirectories(destDir); } catch (IOException ignored) {}
        String day = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        int ok = 0, fail = 0;
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 15) {
                if (Thread.currentThread().isInterrupted())
                    return new DownloadResult(TOTAL, ok, fail, System.currentTimeMillis() - t0);
                String fn = String.format("%s%02d%02d%02d.gkg.csv.zip", day, h, m, 0);
                Path tgt = destDir.resolve(fn);
                try { if (!Files.exists(tgt)) Files.createFile(tgt); ok++; }
                catch (IOException e) { fail++; }
                int done = h * 4 + m / 15 + 1;
                int pct  = done * 100 / TOTAL;
                if (sink != null) sink.accept(new ProgressTick(pct,
                        "下载中 " + done + "/" + TOTAL + ": " + fn, "✓ 模拟下载完成"));
                try { Thread.sleep(TICK_MS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return new DownloadResult(TOTAL, ok, fail, System.currentTimeMillis() - t0); }
            }
        }
        return new DownloadResult(TOTAL, ok, fail, System.currentTimeMillis() - t0);
    }
}
