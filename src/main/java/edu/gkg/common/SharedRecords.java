package edu.gkg.common;

import java.time.LocalDate;
import java.util.List;

/**
 * 跨模块共享的 record/enum 定义。
 * 等 B 把正式 model package 补齐后，可把同名 record 迁移过去，此文件整体删除。
 */
public final class SharedRecords {

    private SharedRecords() {}

    public enum EntityType { PERSON, ORG, THEME }

    public enum Granularity { DAY, WEEK, MONTH }

    public record EntityRef(EntityType type, String name) {}

    public record QueryCondition(
            LocalDate from,
            LocalDate to,
            String themeCode,
            String personName,
            String orgName,
            String locationName) {}

    public record Page<T>(List<T> rows, int page, int pageSize, long total) {
        public int totalPages() {
            return pageSize == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
        }
    }

    /** F-06 查询结果行（轻量展示用） */
    public record SearchRow(
            String recordId,
            String publishDate,
            String sourceCommonName,
            Double tone,
            String documentId) {}

    /** F-07/F-08 人物/组织档案 */
    public record EntityProfile(
            String name,
            long newsCount,
            double avgTone,
            int themeCount,
            List<String> topRelated,
            List<RelatedItem> relatedPeople,
            List<RelatedItem> relatedOrganizations) {

        public EntityProfile(String name, long newsCount, double avgTone, int themeCount, List<String> topRelated) {
            this(name, newsCount, avgTone, themeCount, topRelated, List.of(), List.of());
        }
    }

    public record RelatedItem(String name, int count) {}

    public record CooccurEdge(int e1Id, String e1Name, int e2Id, String e2Name, int coCount) {}

    public record FocusNode(int nodeId, String name, double pageRank, int degree) {}

    public record DateTone(LocalDate date, double avgTone, int recordCount) {}

    public record Inflection(LocalDate date, String direction, double magnitude) {}

    public record SentimentSeries(List<DateTone> series, List<Inflection> inflectionPoints) {}

    public record ThemeHeat(String themeCode, String bucket, int count) {}

    public record Cluster(int clusterId, List<String> keywords, int recordCount) {}

    // ===== 在线下载 =====

    /** 一次在线下载结果汇总 */
    public record DownloadResult(int total, int success, int failed, long elapsedMs) {}

    /** 下载/导入进度回调单元 */
    public record ProgressTick(int percent, String currentFile, String message) {}
}
