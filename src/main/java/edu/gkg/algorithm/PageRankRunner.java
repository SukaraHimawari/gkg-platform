package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.CooccurEdge;
import edu.gkg.common.SharedRecords.FocusNode;

import java.util.*;

/**
 * F-13 PageRank 焦点人物算法（幂迭代实现）。
 *
 * 设计原则：算法本体只依赖 List&lt;CooccurEdge&gt; 这种纯数据结构，不耦合 DAO。
 * 真实场景下由 AnalysisServiceImpl 从 CooccurrenceDao 拉边列表再传进来。
 *
 * 公式：pr_new[i] = (1 - d) / N + d * Σ_j pr[j] * w_ji / outWeight[j]
 *   d   阻尼因子 = 0.85
 *   N   节点数
 *   收敛条件 max|pr_new - pr| &lt; 1e-6 或迭代到 maxIter
 */
public class PageRankRunner {

    private static final double DAMPING = 0.85;
    private static final double EPS     = 1e-6;
    private static final int    MAX_ITER = 100;

    private final List<CooccurEdge> edges;

    public PageRankRunner(List<CooccurEdge> edges) {
        this.edges = Objects.requireNonNull(edges);
    }

    public List<FocusNode> topN(int topN) {
        if (edges.isEmpty()) return List.of();

        Map<Integer, String> idToName = new HashMap<>();
        for (CooccurEdge e : edges) {
            idToName.putIfAbsent(e.e1Id(), e.e1Name());
            idToName.putIfAbsent(e.e2Id(), e.e2Name());
        }
        List<Integer> nodes = new ArrayList<>(idToName.keySet());
        Collections.sort(nodes);
        Map<Integer, Integer> idx = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) idx.put(nodes.get(i), i);

        int N = nodes.size();
        double[][] w = new double[N][N];
        int[] degree = new int[N];
        for (CooccurEdge e : edges) {
            int i = idx.get(e.e1Id());
            int j = idx.get(e.e2Id());
            if (i == j) continue;
            w[i][j] += e.coCount();
            w[j][i] += e.coCount();
            degree[i]++;
            degree[j]++;
        }
        double[] outWeight = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0;
            for (int j = 0; j < N; j++) s += w[i][j];
            outWeight[i] = s;
        }

        double[] pr = new double[N];
        Arrays.fill(pr, 1.0 / N);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            double[] next = new double[N];
            double danglingMass = 0;
            for (int i = 0; i < N; i++) if (outWeight[i] == 0) danglingMass += pr[i];

            for (int i = 0; i < N; i++) {
                double sum = 0;
                for (int j = 0; j < N; j++) {
                    if (outWeight[j] > 0 && w[j][i] > 0) {
                        sum += pr[j] * w[j][i] / outWeight[j];
                    }
                }
                next[i] = (1 - DAMPING) / N + DAMPING * (sum + danglingMass / N);
            }

            double delta = 0;
            for (int i = 0; i < N; i++) delta = Math.max(delta, Math.abs(next[i] - pr[i]));
            pr = next;
            if (delta < EPS) break;
        }

        List<FocusNode> all = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            int id = nodes.get(i);
            all.add(new FocusNode(id, idToName.get(id), pr[i], degree[i]));
        }
        all.sort((a, b) -> Double.compare(b.pageRank(), a.pageRank()));
        return all.subList(0, Math.min(topN, all.size()));
    }
}
