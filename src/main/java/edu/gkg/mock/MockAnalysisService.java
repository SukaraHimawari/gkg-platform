package edu.gkg.mock;

import edu.gkg.algorithm.PageRankRunner;
import edu.gkg.algorithm.SentimentTrend;
import edu.gkg.common.SharedRecords.*;
import edu.gkg.service.AnalysisService;

import java.time.LocalDate;
import java.util.*;

/**
 * Mock 挖掘服务：等 B 出 AnalysisServiceImpl 期间，让挖掘 Tab 跑端到端流程。
 *
 * 关键设计：内部直接调用 A 自己的真算法实现：
 *   - F-13 PageRank → PageRankRunner（在 mock 生成的 cooccurrence 上跑）
 *   - F-16 情感趋势 → SentimentTrend（在 mock 生成的日序列上跑）
 *
 * F-12 共现 / F-14 主题热度 / F-15 K-Means 是 B 的算法，这里只返回造的"看起来合理"的数据。
 */
public class MockAnalysisService implements AnalysisService {

    private static final String[] NAMES = {
            "Joe Biden", "Donald Trump", "Xi Jinping", "Vladimir Putin", "Volodymyr Zelensky",
            "Emmanuel Macron", "Olaf Scholz", "Narendra Modi", "Kishida Fumio", "Rishi Sunak",
            "United Nations", "NATO", "European Union", "G7", "WHO", "WTO", "IMF",
            "Tesla", "SpaceX", "OpenAI", "Microsoft", "Google", "Apple"
    };

    private final List<CooccurEdge> edges = new ArrayList<>();
    private final PageRankRunner pageRankRunner;
    private boolean built = false;

    public MockAnalysisService() {
        buildSyntheticEdges();
        this.pageRankRunner = new PageRankRunner(edges);
    }

    private void buildSyntheticEdges() {
        edges.clear();
        Random rnd = new Random(42);
        for (int i = 0; i < NAMES.length; i++) {
            for (int j = i + 1; j < NAMES.length; j++) {
                if (rnd.nextDouble() < 0.25) {
                    edges.add(new CooccurEdge(i, NAMES[i], j, NAMES[j],
                            5 + rnd.nextInt(80)));
                }
            }
        }
    }

    @Override
    public void rebuildCooccurrence() {
        // Mock：用同样的 seed 重建，并标志 built=true（B 真实现是 SQL 聚合）
        buildSyntheticEdges();
        built = true;
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
    }

    @Override
    public List<CooccurEdge> findAllEdges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public List<FocusNode> topFocusPersons(int topN) {
        // 直接用 A 的 PageRankRunner
        return new PageRankRunner(edges).topN(topN);
    }

    @Override
    public List<ThemeHeat> themeHeatRank(Granularity granularity, LocalDate from, LocalDate to, int topN) {
        String[] themes = {"SANCTIONS", "PROTEST", "ELECTION", "HEALTH", "CYBER", "CLIMATE",
                "ECON_BANKRUPTCY", "WAR_KILL", "MEDIA_CENSORSHIP", "TECH_AI"};
        Random rnd = new Random(31 * (granularity == null ? 0 : granularity.ordinal())
                + (from == null ? 0 : from.toEpochDay()));
        List<ThemeHeat> out = new ArrayList<>();
        int n = Math.min(topN, themes.length);
        for (int i = 0; i < n; i++) {
            String bucket = bucketLabel(granularity, from);
            out.add(new ThemeHeat(themes[i], bucket, 5000 - i * 320 + rnd.nextInt(400)));
        }
        return out;
    }

    private static String bucketLabel(Granularity g, LocalDate from) {
        if (from == null) from = LocalDate.now();
        return switch (g == null ? Granularity.MONTH : g) {
            case DAY   -> from.toString();
            case WEEK  -> from.toString() + " 周";
            case MONTH -> String.format("%04d-%02d", from.getYear(), from.getMonthValue());
        };
    }

    @Override
    public List<Cluster> clusterByKMeans(int k, int maxIter) {
        String[][] keywords = {
                {"sanctions", "embargo", "tariff", "trade", "export-control"},
                {"election", "vote", "ballot", "candidate", "campaign"},
                {"covid", "pandemic", "vaccine", "hospital", "outbreak"},
                {"cyber", "hacker", "ransomware", "phishing", "breach"},
                {"climate", "emission", "carbon", "renewable", "ipcc"},
                {"protest", "rally", "demonstration", "march", "riot"},
                {"AI", "GPT", "LLM", "OpenAI", "regulation"},
                {"war", "front", "missile", "drone", "casualty"},
        };
        Random rnd = new Random(101L * k + maxIter);
        List<Cluster> out = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            String[] kws = keywords[i % keywords.length];
            out.add(new Cluster(i, List.of(kws), 200 + rnd.nextInt(1200)));
        }
        return out;
    }

    @Override
    public SentimentSeries sentimentTrend(EntityRef ref, LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.of(2024, 1, 1);
        if (to == null || !to.isAfter(from)) to = from.plusDays(20);
        Random rnd = new Random(ref == null ? 7 : ref.name().hashCode());

        List<DateTone> rows = new ArrayList<>();
        // 故意造一段先正后负再回正的趋势，方便拐点检测演示
        long days = from.until(to).getDays() + 1;
        for (int i = 0; i < days; i++) {
            double t;
            if      (i < days * 0.3) t = 1.5 + rnd.nextDouble() * 1.2;
            else if (i < days * 0.6) t = -2.5 + rnd.nextDouble() * 1.2;
            else                      t = 1.8 + rnd.nextDouble() * 1.2;
            t = Math.round(t * 100) / 100.0;
            rows.add(new DateTone(from.plusDays(i), t, 10 + rnd.nextInt(50)));
        }
        return new SentimentTrend().compute(rows);
    }

    /** 仅供测试代码内省，UI 不应依赖。 */
    public boolean isBuilt() { return built; }
}
