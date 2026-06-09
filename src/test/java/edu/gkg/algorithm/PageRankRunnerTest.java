package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.CooccurEdge;
import edu.gkg.common.SharedRecords.FocusNode;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
