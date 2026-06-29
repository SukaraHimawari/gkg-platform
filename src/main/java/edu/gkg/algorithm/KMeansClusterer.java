package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;
import weka.clusterers.SimpleKMeans;
import weka.core.*;

import java.sql.*;
import java.util.*;

public class KMeansClusterer {

    public List<ClusterResult> cluster(int k, int maxIter) throws Exception {
        System.out.println("🔄 开始 K-Means 聚类 (k=" + k + ", maxIter=" + maxIter + ")...");
        long startTime = System.currentTimeMillis();

        Map<String, Integer> themeDocCount = loadThemeDocCounts();
        if (themeDocCount.size() < k) {
            System.out.println("⚠️ 主题数量 (" + themeDocCount.size() + ") 小于 k (" + k + ")，调整 k = " + Math.max(1, themeDocCount.size()));
            k = Math.max(1, themeDocCount.size());
        }

        System.out.println("📊 加载了 " + themeDocCount.size() + " 个主题");

        if (themeDocCount.size() < 2) {
            System.out.println("⚠️ 主题数量不足，无法聚类");
            return List.of();
        }

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("doc_count"));

        Instances dataset = new Instances("ThemeClusters", attributes, themeDocCount.size());

        for (Map.Entry<String, Integer> entry : themeDocCount.entrySet()) {
            double[] values = new double[1];
            values[0] = entry.getValue();
            Instance instance = new DenseInstance(1.0, values);
            instance.setDataset(dataset);
            dataset.add(instance);
        }

        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setNumClusters(k);
        kmeans.setMaxIterations(maxIter);
        kmeans.setSeed(42);
        kmeans.buildClusterer(dataset);

        int[] assignments = kmeans.getAssignments();
        List<String> themeList = new ArrayList<>(themeDocCount.keySet());

        Map<Integer, List<String>> clusterMap = new HashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            int clusterIdx = assignments[i];
            clusterMap.computeIfAbsent(clusterIdx, key -> new ArrayList<>())
                    .add(themeList.get(i));
        }

        List<ClusterResult> results = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : clusterMap.entrySet()) {
            List<String> themes = entry.getValue();
            List<String> topThemes = themes.stream().limit(5).toList();
            results.add(new ClusterResult(entry.getKey(), topThemes, themes.size()));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("✅ K-Means 聚类完成！共 " + results.size() + " 个簇，耗时 " + elapsed / 1000 + " 秒");

        return results;
    }

    private Map<String, Integer> loadThemeDocCounts() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = """
            SELECT t.code, COUNT(DISTINCT rt.record_id) AS cnt
            FROM theme t
            JOIN record_theme rt ON t.theme_id = rt.theme_id
            GROUP BY t.theme_id
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

    public record ClusterResult(int clusterId, List<String> topThemes, int size) {
        @Override
        public String toString() {
            return "Cluster " + clusterId + " (size=" + size + "): " + String.join(", ", topThemes);
        }
    }
}
