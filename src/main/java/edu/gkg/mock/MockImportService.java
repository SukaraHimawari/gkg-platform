package edu.gkg.mock;

import edu.gkg.common.SharedRecords.ImportResult;
import edu.gkg.common.SharedRecords.ProgressTick;
import edu.gkg.service.ImportService;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Mock 导入服务：让 ImportPanel 的进度条 / 统计卡能跑动效。
 * 每"导入"一份文件，分 20 个 tick 推进 0~100%，间隔 60 ms。
 */
public class MockImportService implements ImportService {

    @Override
    public ImportResult importFiles(List<File> files, Consumer<ProgressTick> sink) {
        if (files == null || files.isEmpty()) {
            return new ImportResult(0, 0, 0, 0);
        }
        long t0 = System.currentTimeMillis();
        Random rnd = new Random(1);
        int success = 0, skipped = 0, failed = 0;

        for (int f = 0; f < files.size(); f++) {
            File file = files.get(f);
            String name = file.getName();
            int rowsInFile = 5_000 + rnd.nextInt(15_000);

            for (int tick = 1; tick <= 20; tick++) {
                int pctInFile = tick * 5;                       // 0..100
                int pct = (int) ((f * 100.0 + pctInFile) / files.size());
                if (sink != null) {
                    sink.accept(new ProgressTick(pct, name,
                            String.format("已处理 %d / %d 行", rowsInFile * tick / 20, rowsInFile)));
                }
                try { Thread.sleep(60); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return summary(success, skipped, failed, t0); }
            }
            int sk = (int) (rowsInFile * 0.02);
            int fl = (int) (rowsInFile * 0.005);
            success += (rowsInFile - sk - fl);
            skipped += sk;
            failed  += fl;
        }
        if (sink != null) sink.accept(new ProgressTick(100, "(完成)", "全部文件导入完成"));
        return summary(success, skipped, failed, t0);
    }

    private static ImportResult summary(int s, int k, int f, long t0) {
        return new ImportResult(s, k, f, System.currentTimeMillis() - t0);
    }

    @Override
    public int cleanInvalidData() {
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        return new Random(7).nextInt(200) + 50;
    }
}
