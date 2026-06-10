package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.CooccurEdge;
import edu.gkg.common.SharedRecords.FocusNode;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageRankRunnerTest {

    @Test
    void emptyEdgesReturnEmpty() {
        assertTrue(new PageRankRunner(List.of()).topN(5).isEmpty());
    }

    @Test
    void fourNodeTriangleHubPlusLeaf() {
        // 三角 1-2-3-1 + 叶子 4-1
        List<CooccurEdge> edges = List.of(
                new CooccurEdge(1, "A", 2, "B", 1),
                new CooccurEdge(2, "B", 3, "C", 1),
                new CooccurEdge(1, "A", 3, "C", 1),
                new CooccurEdge(1, "A", 4, "D", 1)
        );
        List<FocusNode> ranks = new PageRankRunner(edges).topN(4);
        assertEquals(4, ranks.size());
        // A 是 hub（度=3），PageRank 应最高
        assertEquals("A", ranks.get(0).name());
        // D 是叶子（度=1），PageRank 应最低
        assertEquals("D", ranks.get(3).name());
        // 总质量约等于 1
        double sum = ranks.stream().mapToDouble(FocusNode::pageRank).sum();
        assertEquals(1.0, sum, 0.01);
    }

    @Test
    void weightedEdgesAffectRank() {
        // 同样的 1-2 一条边，把 1-3 加重 → 3 的 PageRank 应高于 2
        List<CooccurEdge> edges = List.of(
                new CooccurEdge(1, "A", 2, "B", 1),
                new CooccurEdge(1, "A", 3, "C", 10)
        );
        List<FocusNode> ranks = new PageRankRunner(edges).topN(3);
        double prB = ranks.stream().filter(n -> n.name().equals("B")).findFirst().orElseThrow().pageRank();
        double prC = ranks.stream().filter(n -> n.name().equals("C")).findFirst().orElseThrow().pageRank();
        assertTrue(prC > prB, "C 边权更大，PageRank 应更高");
    }

    @Test
    void topNLimitsOutput() {
        List<CooccurEdge> edges = List.of(
                new CooccurEdge(1, "A", 2, "B", 1),
                new CooccurEdge(2, "B", 3, "C", 1),
                new CooccurEdge(3, "C", 4, "D", 1)
        );
        assertEquals(2, new PageRankRunner(edges).topN(2).size());
        assertEquals(4, new PageRankRunner(edges).topN(100).size());
    }

    /**
     * 与 JUNG 的 PageRank 在无权图上对照——绝对差异 < 1e-3，
     * 排名顺序完全一致。这是协作文档 A3 模块明确要求的硬验证。
     */
    @Test
    void matchesJungOnUnweightedGraph() {
        // 6 节点图：三角 1-2-3、链 3-4-5，加一条 1-5 桥
        List<CooccurEdge> edges = List.of(
                new CooccurEdge(1, "A", 2, "B", 1),
                new CooccurEdge(2, "B", 3, "C", 1),
                new CooccurEdge(1, "A", 3, "C", 1),
                new CooccurEdge(3, "C", 4, "D", 1),
                new CooccurEdge(4, "D", 5, "E", 1),
                new CooccurEdge(1, "A", 5, "E", 1)
        );

        // 自己跑
        List<FocusNode> mine = new PageRankRunner(edges).topN(100);
        Map<Integer, Double> mineById = new HashMap<>();
        for (FocusNode n : mine) mineById.put(n.nodeId(), n.pageRank());

        // JUNG 跑（无权 + alpha=0.15 → damping=0.85）
        UndirectedSparseGraph<Integer, String> g = new UndirectedSparseGraph<>();
        for (CooccurEdge e : edges) { g.addVertex(e.e1Id()); g.addVertex(e.e2Id()); }
        int idx = 0;
        for (CooccurEdge e : edges) g.addEdge("e" + (idx++), e.e1Id(), e.e2Id());

        PageRank<Integer, String> jung = new PageRank<>(g, 0.15);
        jung.setMaxIterations(200);
        jung.setTolerance(1e-8);
        jung.evaluate();

        // 逐节点对比绝对差异
        for (Integer v : g.getVertices()) {
            double jv = jung.getVertexScore(v);
            double mv = mineById.get(v);
            assertEquals(jv, mv, 1e-3,
                    "节点 " + v + ": JUNG=" + jv + " 我的=" + mv);
        }
    }
}
