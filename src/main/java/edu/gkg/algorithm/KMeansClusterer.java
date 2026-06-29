package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KMeansClusterer {

    public List<ClusterResult> cluster(int k, int maxIter) throws Exception {
        Map<String, Integer> themeCounts = loadThemeCounts();
        if (themeCounts.size() < 2) {
            return List.of();
        }

        int clusterCount = Math.max(1, Math.min(k, themeCounts.size()));
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("record_count"));
        Instances dataset = new Instances("ThemeClusters", attributes, themeCounts.size());

        for (Integer count : themeCounts.values()) {
            dataset.add(new DenseInstance(1.0, new double[]{count}));
        }

        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setNumClusters(clusterCount);
        kmeans.setMaxIterations(maxIter);
        kmeans.setSeed(42);
        kmeans.setPreserveInstancesOrder(true);
        kmeans.buildClusterer(dataset);

        int[] assignments = kmeans.getAssignments();
        List<String> themes = new ArrayList<>(themeCounts.keySet());
        Map<Integer, List<String>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            grouped.computeIfAbsent(assignments[i], ignored -> new ArrayList<>()).add(themes.get(i));
        }

        List<ClusterResult> results = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
            List<String> ordered = entry.getValue().stream()
                    .sorted((a, b) -> Integer.compare(themeCounts.get(b), themeCounts.get(a)))
                    .limit(5)
                    .toList();
            int recordCount = entry.getValue().stream().mapToInt(themeCounts::get).sum();
            results.add(new ClusterResult(entry.getKey(), ordered, recordCount));
        }
        return results;
    }

    private Map<String, Integer> loadThemeCounts() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = """
                SELECT t.code, COUNT(DISTINCT rt.record_id) AS cnt
                FROM theme t
                JOIN record_theme rt ON t.theme_id = rt.theme_id
                GROUP BY t.theme_id, t.code
                ORDER BY cnt DESC, t.code
                """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("code"), rs.getInt("cnt"));
            }
        }
        return result;
    }

    public record ClusterResult(int clusterId, List<String> topThemes, int recordCount) {
        @Override
        public String toString() {
            return "Cluster " + clusterId + " (" + recordCount + " records)"
                    + System.lineSeparator()
                    + "Top themes: " + String.join(", ", topThemes);
        }
    }
}
