package edu.gkg.service;

import edu.gkg.algorithm.ThemeRanker;

import java.sql.SQLException;
import java.util.List;

public interface AnalysisService {

    void rebuildCooccurrence() throws SQLException;

    default void rebuildCooccurrence(ProgressListener listener) throws SQLException {
        rebuildCooccurrence();
    }

    List<String> getTopFocusPersons(int topN);

    List<ThemeRanker.ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) throws SQLException;

    List<ThemeRanker.ThemeHeat> getAllThemeHeat() throws SQLException;

    List<String> clusterByKMeans(int k, int maxIter);

    List<String> sentimentTrend(String entityRef, String from, String to);
}
