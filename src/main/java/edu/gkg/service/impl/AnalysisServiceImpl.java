package edu.gkg.service.impl;

import edu.gkg.algorithm.CooccurrenceBuilder;
import edu.gkg.algorithm.KMeansClusterer;
import edu.gkg.algorithm.PageRankRunner;
import edu.gkg.algorithm.SentimentTrend;
import edu.gkg.algorithm.ThemeRanker;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.FocusNode;
import edu.gkg.common.SharedRecords.SentimentSeries;
import edu.gkg.dao.CooccurEdgeDao;
import edu.gkg.dao.OrganizationDao;
import edu.gkg.dao.PersonDao;
import edu.gkg.model.CooccurEdge;
import edu.gkg.model.Organization;
import edu.gkg.model.Person;
import edu.gkg.service.AnalysisService;
import edu.gkg.service.ProgressListener;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisServiceImpl implements AnalysisService {

    private static final DateTimeFormatter OUTPUT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CooccurrenceBuilder coBuilder = new CooccurrenceBuilder();
    private final ThemeRanker themeRanker = new ThemeRanker();
    private final KMeansClusterer kMeansClusterer = new KMeansClusterer();
    private final CooccurEdgeDao edgeDao = new CooccurEdgeDao();
    private final PersonDao personDao = new PersonDao();
    private final OrganizationDao orgDao = new OrganizationDao();
    private final QueryServiceImpl queryService = new QueryServiceImpl();
    private final SentimentTrend sentimentTrend = new SentimentTrend();

    @Override
    public void rebuildCooccurrence() throws SQLException {
        coBuilder.rebuild();
    }

    @Override
    public void rebuildCooccurrence(ProgressListener listener) throws SQLException {
        coBuilder.rebuild(listener);
    }

    @Override
    public List<String> getTopFocusPersons(int topN) {
        try {
            List<CooccurEdge> modelEdges = edgeDao.findAllEdges();
            if (modelEdges.isEmpty()) return List.of();

            Map<Long, String> idToName = new HashMap<>();
            for (Person p : personDao.findAll()) idToName.put(p.getPersonId(), p.getName());
            for (Organization o : orgDao.findAll()) idToName.put(o.getOrgId(), o.getName());

            List<edu.gkg.common.SharedRecords.CooccurEdge> prEdges = new ArrayList<>();
            for (CooccurEdge e : modelEdges) {
                prEdges.add(new edu.gkg.common.SharedRecords.CooccurEdge(
                        e.getE1Id().intValue(), idToName.getOrDefault(e.getE1Id(), "ID:" + e.getE1Id()),
                        e.getE2Id().intValue(), idToName.getOrDefault(e.getE2Id(), "ID:" + e.getE2Id()),
                        e.getCoCount()));
            }
            return new PageRankRunner(prEdges).topN(Math.max(1, topN)).stream()
                    .map(this::formatFocusNode)
                    .toList();
        } catch (SQLException e) {
            throw new IllegalStateException("PageRank 焦点实体计算失败", e);
        }
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
            return kMeansClusterer.cluster(k, maxIter).stream()
                    .map(KMeansClusterer.ClusterResult::toString)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("K-Means 聚类失败", e);
        }
    }

    @Override
    public List<String> sentimentTrend(String entityRef, String from, String to) {
        if (entityRef == null || entityRef.isBlank()) return List.of();
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        List<DateTone> series = queryService.getThemeTrend(entityRef.trim(), fromDate, toDate);
        SentimentSeries trend = sentimentTrend.compute(series);
        List<String> rows = new ArrayList<>();
        for (DateTone point : trend.series()) {
            rows.add(String.format("%s avgTone=%.3f count=%d",
                    point.date().format(OUTPUT_DATE), point.avgTone(), point.recordCount()));
        }
        trend.inflectionPoints().forEach(point -> rows.add(String.format(
                "%s inflection=%s magnitude=%.3f",
                point.date().format(OUTPUT_DATE), point.direction(), point.magnitude())));
        return rows;
    }

    private String formatFocusNode(FocusNode node) {
        return String.format("%s PR=%.6f degree=%d", node.name(), node.pageRank(), node.degree());
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();
        if (trimmed.length() == 8 && trimmed.chars().allMatch(Character::isDigit)) {
            return LocalDate.parse(trimmed, DateTimeFormatter.BASIC_ISO_DATE);
        }
        return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
