package edu.gkg.common;

import java.time.LocalDate;
import java.util.List;

/**
 * 临时承载所有"共享 record"，等 B 把正式的 model package 推过来后，
 * 把同名 record 搬过去，这里整文件删除即可。
 *
 * 字段以协作文档 §5 接口契约为准，B 不在 model 里出的 record（QueryCondition、
 * EntityRef、FocusNode 等）就放这里，让 A 这边能编译。
 */
public final class SharedRecords {

    private SharedRecords() {}

    // ===== 实体 / 查询条件 =====

    public enum EntityType { PERSON, ORG, THEME }

    public record EntityRef(EntityType type, String name) {}

    public record QueryCondition(
            LocalDate from,
            LocalDate to,
            String themeCode,
            String personName,
            String orgName,
            String locationName) {}

    public record Page<T>(List<T> rows, int page, int pageSize, long total) {}

    // ===== 共现 / 焦点节点 =====

    public record CooccurEdge(int e1Id, String e1Name, int e2Id, String e2Name, int coCount) {}

    public record FocusNode(int nodeId, String name, double pageRank, int degree) {}

    // ===== 情感时序 =====

    public record DateTone(LocalDate date, double avgTone, int recordCount) {}

    public record Inflection(LocalDate date, String direction, double magnitude) {}

    public record SentimentSeries(List<DateTone> series, List<Inflection> inflectionPoints) {}

    // ===== 主题分析 =====

    public record ThemeHeat(String themeCode, String bucket, int count) {}

    public record Cluster(int clusterId, List<String> keywords, int recordCount) {}

    public enum Granularity { DAY, WEEK, MONTH }

    // ===== 查询返回（UI 展示用扁平行）=====

    /** 综合查询表格一行（人类可读） */
    public record SearchRow(
            String recordId,
            String publishTime,
            String source,
            double tone,
            String url) {}

    /** 名字+数值，用于"关联组织 TOP10"、"关联人物 TOP10" 等横向柱状图 */
    public record NameCount(String name, double value) {}

    /** 人物/组织档案聚合 */
    public record EntityProfile(
            String name,
            EntityType type,
            int newsCount,
            double avgTone,
            int relatedThemes,
            double positiveRatio,
            List<NameCount> relatedOrgs,
            List<NameCount> relatedPersons,
            List<DateTone> monthlyDistribution) {}

    // ===== 导入服务 =====

    /** 一次导入结果汇总（成功/跳过/失败 + 耗时） */
    public record ImportResult(int success, int skipped, int failed, long elapsedMs) {}

    /** 导入进度回调单元，UI 侧消费 */
    public record ProgressTick(int percent, String currentFile, String message) {}

    // ===== 在线下载 =====

    /** 一次在线下载结果汇总（成功/失败/总数 + 耗时） */
    public record DownloadResult(int total, int success, int failed, long elapsedMs) {}
}
