package edu.gkg.service;

import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ProgressTick;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * F-01 扩展：从 GDELT 官网在线下载指定日期的 GKG 数据文件。
 *
 * GDELT v2 每 15 分钟产生一个文件，一天共 96 个，命名格式：
 *   YYYYMMDDHHmmss.gkg.csv.zip
 * 下载后存放到 data/gkg-raw/YYYYMMDD/ 目录，目录结构与本地示例数据一致。
 */
public interface DownloadService {

    /**
     * 下载指定日期的全部 GKG 切片文件（96 个 / 天）。
     *
     * @param date    目标日期
     * @param destDir 本地保存目录（通常 data/gkg-raw/YYYYMMDD/）
     * @param sink    进度回调：每下载完一个文件回调一次
     * @return        下载结果统计（total / success / failed / elapsedMs）
     */
    DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink);
}
