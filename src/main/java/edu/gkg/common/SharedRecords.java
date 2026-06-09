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

    public record CooccurEdge(int e1Id, String e1Name, int e2Id, String e2Name, int coCount) {}

    public record FocusNode(int nodeId, String name, double pageRank, int degree) {}

    public record DateTone(LocalDate date, double avgTone, int recordCount) {}

    public record Inflection(LocalDate date, String direction, double magnitude) {}

    public record SentimentSeries(List<DateTone> series, List<Inflection> inflectionPoints) {}

    public record ThemeHeat(String themeCode, String bucket, int count) {}

    public record Cluster(int clusterId, List<String> keywords, int recordCount) {}

    public enum Granularity { DAY, WEEK, MONTH }
}
