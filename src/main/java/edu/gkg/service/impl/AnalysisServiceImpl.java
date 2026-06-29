package edu.gkg.service.impl;

import edu.gkg.algorithm.CooccurrenceBuilder;
import edu.gkg.algorithm.KMeansClusterer;
import edu.gkg.algorithm.ThemeRanker;
import edu.gkg.service.AnalysisService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisServiceImpl implements AnalysisService {

    private final CooccurrenceBuilder coBuilder = new CooccurrenceBuilder();
    private final ThemeRanker themeRanker = new ThemeRanker();
    private final KMeansClusterer kmeans = new KMeansClusterer();

    @Override
    public void rebuildCooccurrence() throws SQLException {
        coBuilder.rebuild();
    }

    @Override
    public List<String> getTopFocusPersons(int topN) {
        // 待 A 同学实现 PageRank 后接入
        return List.of();
    }

    @Override
    public List<ThemeRanker.ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) throws SQLException {
        return themeRanker.getThemeHeat(granularity, from, to, topN);
    }

    @Override
    public List<ThemeRanker.ThemeHeat> getAllThemeHeat() throws SQLException {
        return themeRanker.getAllThemeHeat();
    }

    @Override
    public List<String> clusterByKMeans(int k, int maxIter) {
        try {
            List<KMeansClusterer.ClusterResult> results = kmeans.cluster(k, maxIter);
            List<String> output = new ArrayList<>();
            for (KMeansClusterer.ClusterResult r : results) {
                output.add(r.toString());
            }
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return List.of("聚类失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> sentimentTrend(String entityRef, String from, String to) {
        return List.of();
    }
}
