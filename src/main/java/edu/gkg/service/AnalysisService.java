package edu.gkg.service;

import edu.gkg.algorithm.ThemeRanker;

import java.sql.SQLException;
import java.util.List;

public interface AnalysisService {

    // F-12: 构建共现网络
    void rebuildCooccurrence() throws SQLException;

    // F-13: PageRank 焦点人物（A 同学实现）
    List<String> getTopFocusPersons(int topN);

    // F-14: 主题热度排行
    List<ThemeRanker.ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) throws SQLException;

    List<ThemeRanker.ThemeHeat> getAllThemeHeat() throws SQLException;

    // F-15: K-Means
    List<String> clusterByKMeans(int k, int maxIter);

    // F-16: 情感趋势
    List<String> sentimentTrend(String entityRef, String from, String to);
}
