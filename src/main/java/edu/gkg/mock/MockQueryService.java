package edu.gkg.mock;

import edu.gkg.common.SharedRecords.*;
import edu.gkg.service.QueryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Mock 查询服务：等 B 出 DAO 期间，让查询 Tab 能跑端到端流程。
 * 数据生成是确定性的（固定 seed），方便截图复盘。
 */
public class MockQueryService implements QueryService {

    private static final String[] SOURCES = {"Reuters", "CNN", "BBC", "AP", "Bloomberg",
            "Xinhua", "Le Monde", "Al Jazeera"};
    private static final String[] PERSONS = {"Joe Biden", "Donald Trump", "Xi Jinping",
            "Vladimir Putin", "Volodymyr Zelensky", "Emmanuel Macron", "Elon Musk", "Olaf Scholz"};
    private static final String[] ORGS    = {"United Nations", "NATO", "European Union", "G7",
            "WHO", "OpenAI", "Tesla", "SpaceX"};

    private final long totalRecords;

    public MockQueryService() { this(12_458_721L); }
    public MockQueryService(long totalRecords) { this.totalRecords = totalRecords; }

    @Override
    public Page<SearchRow> search(QueryCondition cond, int page, int pageSize) {
        // 用一个稳定的 seed，让相同条件得到相同结果
        long seed = 7L * page + pageSize + (cond == null ? 0 : cond.hashCode());
        Random rnd = new Random(seed);
        List<SearchRow> rows = new ArrayList<>();
        LocalDate base = cond != null && cond.from() != null ? cond.from() : LocalDate.of(2024, 1, 15);
        for (int i = 0; i < pageSize; i++) {
            int globalIdx = page * pageSize + i;
            rows.add(new SearchRow(
                    String.format("%s%06d-T1", base.toString().replace("-", ""), globalIdx * 137 % 999999),
                    base.plusDays(globalIdx % 30) + " " + String.format("%02d:%02d", rnd.nextInt(24), rnd.nextInt(60)),
                    SOURCES[rnd.nextInt(SOURCES.length)],
                    Math.round((rnd.nextDouble() * 8 - 4) * 100) / 100.0,
                    "https://example.com/news/" + globalIdx
            ));
        }
        return new Page<>(rows, page, pageSize, 488L * pageSize);
    }

    @Override
    public EntityProfile getPersonProfile(String name) { return buildProfile(name, EntityType.PERSON); }

    @Override
    public EntityProfile getOrgProfile(String name)    { return buildProfile(name, EntityType.ORG); }

    private EntityProfile buildProfile(String name, EntityType type) {
        Random rnd = new Random(name == null ? 1 : name.hashCode());
        int news = 5000 + rnd.nextInt(80_000);
        double avgTone = Math.round((rnd.nextDouble() * 2 - 0.5) * 100) / 100.0;
        int themes = 50 + rnd.nextInt(200);
        double posRatio = Math.round((0.3 + rnd.nextDouble() * 0.6) * 100) / 100.0;

        List<NameCount> orgs    = topN(rnd, type == EntityType.ORG ? PERSONS : ORGS, 10);
        List<NameCount> persons = topN(rnd, type == EntityType.PERSON ? ORGS : PERSONS, 10);

        List<DateTone> monthly = new ArrayList<>();
        LocalDate d = LocalDate.of(2024, 1, 1);
        for (int m = 0; m < 12; m++) {
            int cnt = 500 + rnd.nextInt(3500);
            double tone = (rnd.nextDouble() * 4 - 2);
            monthly.add(new DateTone(d.plusMonths(m), Math.round(tone * 100) / 100.0, cnt));
        }
        return new EntityProfile(name, type, news, avgTone, themes, posRatio, orgs, persons, monthly);
    }

    private static List<NameCount> topN(Random rnd, String[] pool, int n) {
        List<NameCount> out = new ArrayList<>();
        double base = 2500;
        for (int i = 0; i < Math.min(n, pool.length); i++) {
            base = base * (0.85 + rnd.nextDouble() * 0.05);
            out.add(new NameCount(pool[i], Math.round(base)));
        }
        return out;
    }

    @Override
    public List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to) {
        Random rnd = new Random(themeCode == null ? 0 : themeCode.hashCode());
        List<DateTone> out = new ArrayList<>();
        if (from == null) from = LocalDate.of(2024, 1, 1);
        if (to == null || !to.isAfter(from)) to = from.plusDays(14);
        LocalDate d = from;
        double tone = rnd.nextDouble() * 4 - 2;
        while (!d.isAfter(to)) {
            tone += (rnd.nextDouble() * 1.6 - 0.8);
            tone = Math.max(-8, Math.min(8, tone));
            int cnt = 30 + rnd.nextInt(250);
            out.add(new DateTone(d, Math.round(tone * 100) / 100.0, cnt));
            d = d.plusDays(1);
        }
        return out;
    }

    @Override
    public List<String> suggestPerson(String prefix, int limit) { return suggest(PERSONS, prefix, limit); }

    @Override
    public List<String> suggestOrg(String prefix, int limit)    { return suggest(ORGS, prefix, limit); }

    private static List<String> suggest(String[] pool, String prefix, int limit) {
        String p = prefix == null ? "" : prefix.toLowerCase();
        return IntStream.range(0, pool.length)
                .mapToObj(i -> pool[i])
                .filter(s -> s.toLowerCase().startsWith(p))
                .limit(limit)
                .toList();
    }

    @Override
    public long countAll() { return totalRecords; }
}
