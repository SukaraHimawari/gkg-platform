package edu.gkg.service;

import edu.gkg.common.SharedRecords.*;

import java.time.LocalDate;
import java.util.List;

/**
 * F-12~F-16 挖掘分析接口。
 *
 * 临时占位：本接口归属同学 B，A 这边先放一份本地副本让 UI / Controller 编译；
 * B 推上来后直接合并。装配时 B 会在 AnalysisServiceImpl 里调用：
 *   - A 的 algorithm.PageRankRunner（F-13）
 *   - A 的 algorithm.SentimentTrend（F-16）
 *   - B 自己的 CooccurrenceBuilder（F-12）/ ThemeRanker（F-14）/ KMeansClusterer（F-15）
 */
public interface AnalysisService {

    /** F-12：扫描 gkg_record 重建 cooccurrence 表，全量覆盖。 */
    void rebuildCooccurrence();

    /** 取已构建的共现边（PageRank、网络图都要用） */
    List<CooccurEdge> findAllEdges();

    /** F-13：返回 PageRank Top-N 焦点节点。 */
    List<FocusNode> topFocusPersons(int topN);

    /** F-14：主题热度（按粒度聚合频次）。 */
    List<ThemeHeat> themeHeatRank(Granularity granularity, LocalDate from, LocalDate to, int topN);

    /** F-15：K-Means 主题聚类。 */
    List<Cluster> clusterByKMeans(int k, int maxIter);

    /** F-16：情感趋势 + 拐点检测。 */
    SentimentSeries sentimentTrend(EntityRef ref, LocalDate from, LocalDate to);
}
