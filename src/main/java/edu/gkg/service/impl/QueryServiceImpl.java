package edu.gkg.service.impl;

import edu.gkg.common.DbHelper;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.EntityProfile;
import edu.gkg.common.SharedRecords.Page;
import edu.gkg.common.SharedRecords.QueryCondition;
import edu.gkg.common.SharedRecords.RelatedItem;
import edu.gkg.dao.GkgRecordDao;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.QueryService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class QueryServiceImpl implements QueryService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final GkgRecordDao recordDao = new GkgRecordDao();

    @Override
    public Page<GkgRecord> search(QueryCondition cond, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = pageSize <= 0 ? 20 : pageSize;
        try (Connection conn = DbHelper.getConnection()) {
            QuerySql query = buildSearchSql(cond, false);
            QuerySql count = buildSearchSql(cond, true);
            long total;
            try (PreparedStatement stmt = conn.prepareStatement(count.sql())) {
                count.bind(stmt);
                try (ResultSet rs = stmt.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0;
                }
            }

            List<GkgRecord> rows = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(query.sql())) {
                int next = query.bind(stmt);
                stmt.setInt(next++, safePageSize);
                stmt.setInt(next, (safePage - 1) * safePageSize);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) rows.add(mapRecord(rs));
                }
            }
            return new Page<>(rows, safePage, safePageSize, total);
        } catch (SQLException e) {
            return new Page<>(List.of(), safePage, safePageSize, 0);
        }
    }

    @Override
    public EntityProfile getPersonProfile(String name) {
        String sql = """
                WITH records AS (
                    SELECT DISTINCT r.record_id, r.tone
                    FROM person p
                    JOIN record_person rp ON p.person_id = rp.person_id
                    JOIN gkg_record r ON rp.record_id = r.record_id
                    WHERE p.name = ?
                )
                SELECT COUNT(*) AS news_count,
                       COALESCE(AVG(tone), 0) AS avg_tone,
                       (
                           SELECT COUNT(DISTINCT rt.theme_id)
                           FROM records rec
                           JOIN record_theme rt ON rec.record_id = rt.record_id
                       ) AS theme_count
                FROM records
                """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EntityProfile(name, rs.getLong("news_count"), rs.getDouble("avg_tone"),
                            rs.getInt("theme_count"),
                            topRelatedForPerson(conn, name),
                            topPeopleForPerson(conn, name),
                            topOrganizationsForPerson(conn, name));
                }
            }
        } catch (SQLException ignored) {
        }
        return new EntityProfile(name, 0, 0.0, 0, List.of());
    }

    @Override
    public EntityProfile getOrgProfile(String name) {
        String sql = """
                WITH records AS (
                    SELECT DISTINCT r.record_id, r.tone
                    FROM organization o
                    JOIN record_organization ro ON o.org_id = ro.org_id
                    JOIN gkg_record r ON ro.record_id = r.record_id
                    WHERE o.name = ?
                )
                SELECT COUNT(*) AS news_count,
                       COALESCE(AVG(tone), 0) AS avg_tone,
                       (
                           SELECT COUNT(DISTINCT rt.theme_id)
                           FROM records rec
                           JOIN record_theme rt ON rec.record_id = rt.record_id
                       ) AS theme_count
                FROM records
                """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new EntityProfile(name, rs.getLong("news_count"), rs.getDouble("avg_tone"),
                            rs.getInt("theme_count"),
                            topRelatedForOrg(conn, name),
                            topPeopleForOrg(conn, name),
                            topOrganizationsForOrg(conn, name));
                }
            }
        } catch (SQLException ignored) {
        }
        return new EntityProfile(name, 0, 0.0, 0, List.of());
    }

    @Override
    public List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to) {
        String sql = """
                SELECT substr(r.publish_date, 1, 8) AS day,
                       AVG(r.tone) AS avg_tone,
                       COUNT(*) AS cnt
                FROM gkg_record r
                JOIN record_theme rt ON r.record_id = rt.record_id
                JOIN theme t ON rt.theme_id = t.theme_id
                WHERE t.code = ?
                  AND (? IS NULL OR substr(r.publish_date, 1, 8) >= ?)
                  AND (? IS NULL OR substr(r.publish_date, 1, 8) <= ?)
                  AND r.tone IS NOT NULL
                GROUP BY day
                ORDER BY day
                """;
        List<DateTone> result = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String fromText = from == null ? null : from.format(BASIC_DATE);
            String toText = to == null ? null : to.format(BASIC_DATE);
            stmt.setString(1, themeCode);
            stmt.setString(2, fromText);
            stmt.setString(3, fromText);
            stmt.setString(4, toText);
            stmt.setString(5, toText);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new DateTone(LocalDate.parse(rs.getString("day"), BASIC_DATE),
                            rs.getDouble("avg_tone"), rs.getInt("cnt")));
                }
            }
        } catch (SQLException ignored) {
        }
        return result;
    }

    @Override
    public List<DateTone> getEntityTrend(String entityType, String name, LocalDate from, LocalDate to) {
        if ("THEME".equalsIgnoreCase(entityType)) {
            return getThemeTrend(name, from, to);
        }
        String joinSql = "ORG".equalsIgnoreCase(entityType) || "ORGANIZATION".equalsIgnoreCase(entityType)
                ? "JOIN record_organization link ON r.record_id = link.record_id JOIN organization e ON link.org_id = e.org_id"
                : "JOIN record_person link ON r.record_id = link.record_id JOIN person e ON link.person_id = e.person_id";
        String sql = """
                SELECT substr(r.publish_date, 1, 8) AS day,
                       AVG(r.tone) AS avg_tone,
                       COUNT(*) AS cnt
                FROM gkg_record r
                %s
                WHERE e.name = ?
                  AND (? IS NULL OR substr(r.publish_date, 1, 8) >= ?)
                  AND (? IS NULL OR substr(r.publish_date, 1, 8) <= ?)
                  AND r.tone IS NOT NULL
                GROUP BY day
                ORDER BY day
                """.formatted(joinSql);
        List<DateTone> result = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String fromText = from == null ? null : from.format(BASIC_DATE);
            String toText = to == null ? null : to.format(BASIC_DATE);
            stmt.setString(1, name);
            stmt.setString(2, fromText);
            stmt.setString(3, fromText);
            stmt.setString(4, toText);
            stmt.setString(5, toText);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new DateTone(LocalDate.parse(rs.getString("day"), BASIC_DATE),
                            rs.getDouble("avg_tone"), rs.getInt("cnt")));
                }
            }
        } catch (SQLException ignored) {
        }
        return result;
    }

    @Override
    public List<String> suggestPerson(String prefix, int limit) {
        return suggestNames("""
                SELECT p.name, COUNT(rp.record_id) AS cnt
                FROM person p
                LEFT JOIN record_person rp ON p.person_id = rp.person_id
                WHERE lower(p.name) LIKE ?
                GROUP BY p.person_id, p.name
                ORDER BY cnt DESC, p.name
                LIMIT ?
                """, prefix, limit);
    }

    @Override
    public List<String> suggestOrg(String prefix, int limit) {
        return suggestNames("""
                SELECT o.name, COUNT(ro.record_id) AS cnt
                FROM organization o
                LEFT JOIN record_organization ro ON o.org_id = ro.org_id
                WHERE lower(o.name) LIKE ?
                GROUP BY o.org_id, o.name
                ORDER BY cnt DESC, o.name
                LIMIT ?
                """, prefix, limit);
    }

    @Override
    public List<String> suggestLocation(String prefix, int limit) {
        return suggestNames("""
                SELECT l.name, COUNT(rl.record_id) AS cnt
                FROM location l
                LEFT JOIN record_location rl ON l.location_id = rl.location_id
                WHERE lower(l.name) LIKE ?
                GROUP BY l.location_id, l.name
                ORDER BY cnt DESC, l.name
                LIMIT ?
                """, prefix, limit);
    }

    private List<String> suggestNames(String sql, String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + prefix.toLowerCase() + "%");
            stmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            return List.of();
        }
        return result;
    }

    @Override
    public long countAll() {
        try {
            return recordDao.count();
        } catch (SQLException e) {
            return 0;
        }
    }

    private QuerySql buildSearchSql(QueryCondition cond, boolean countOnly) {
        StringBuilder sql = new StringBuilder(countOnly ? "SELECT COUNT(*) FROM gkg_record r" : "SELECT r.* FROM gkg_record r");
        List<String> params = new ArrayList<>();
        StringJoiner where = new StringJoiner(" AND ");
        if (cond != null) {
            if (cond.from() != null) {
                where.add("substr(r.publish_date, 1, 8) >= ?");
                params.add(cond.from().format(BASIC_DATE));
            }
            if (cond.to() != null) {
                where.add("substr(r.publish_date, 1, 8) <= ?");
                params.add(cond.to().format(BASIC_DATE));
            }
            if (notBlank(cond.themeCode())) {
                where.add("EXISTS (SELECT 1 FROM record_theme rt JOIN theme t ON rt.theme_id = t.theme_id WHERE rt.record_id = r.record_id AND t.code = ?)");
                params.add(cond.themeCode());
            }
            if (notBlank(cond.personName())) {
                where.add("EXISTS (SELECT 1 FROM record_person rp JOIN person p ON rp.person_id = p.person_id WHERE rp.record_id = r.record_id AND p.name = ?)");
                params.add(cond.personName());
            }
            if (notBlank(cond.orgName())) {
                where.add("EXISTS (SELECT 1 FROM record_organization ro JOIN organization o ON ro.org_id = o.org_id WHERE ro.record_id = r.record_id AND o.name = ?)");
                params.add(cond.orgName());
            }
            if (notBlank(cond.locationName())) {
                where.add("EXISTS (SELECT 1 FROM record_location rl JOIN location l ON rl.location_id = l.location_id WHERE rl.record_id = r.record_id AND l.name = ?)");
                params.add(cond.locationName());
            }
        }
        if (where.length() > 0) sql.append(" WHERE ").append(where);
        if (!countOnly) sql.append(" ORDER BY r.publish_date DESC LIMIT ? OFFSET ?");
        return new QuerySql(sql.toString(), params);
    }

    private List<String> topRelatedForPerson(Connection conn, String name) throws SQLException {
        return topOrganizationsForPerson(conn, name).stream().map(RelatedItem::name).toList();
    }

    private List<RelatedItem> topOrganizationsForPerson(Connection conn, String name) throws SQLException {
        String sql = """
                SELECT o.name, COUNT(*) AS cnt
                FROM person p
                JOIN record_person rp ON p.person_id = rp.person_id
                JOIN record_organization ro ON rp.record_id = ro.record_id
                JOIN organization o ON ro.org_id = o.org_id
                WHERE p.name = ?
                GROUP BY o.name
                ORDER BY cnt DESC, o.name
                LIMIT 10
                """;
        return queryRelatedItems(conn, sql, name);
    }

    private List<RelatedItem> topPeopleForPerson(Connection conn, String name) throws SQLException {
        String sql = """
                SELECT other.name, COUNT(*) AS cnt
                FROM person p
                JOIN record_person rp ON p.person_id = rp.person_id
                JOIN record_person rp2 ON rp.record_id = rp2.record_id
                JOIN person other ON rp2.person_id = other.person_id
                WHERE p.name = ? AND other.name <> p.name
                GROUP BY other.name
                ORDER BY cnt DESC, other.name
                LIMIT 10
                """;
        return queryRelatedItems(conn, sql, name);
    }

    private List<String> topRelatedForOrg(Connection conn, String name) throws SQLException {
        return topPeopleForOrg(conn, name).stream().map(RelatedItem::name).toList();
    }

    private List<RelatedItem> topPeopleForOrg(Connection conn, String name) throws SQLException {
        String sql = """
                SELECT p.name, COUNT(*) AS cnt
                FROM organization o
                JOIN record_organization ro ON o.org_id = ro.org_id
                JOIN record_person rp ON ro.record_id = rp.record_id
                JOIN person p ON rp.person_id = p.person_id
                WHERE o.name = ?
                GROUP BY p.name
                ORDER BY cnt DESC, p.name
                LIMIT 10
                """;
        return queryRelatedItems(conn, sql, name);
    }

    private List<RelatedItem> topOrganizationsForOrg(Connection conn, String name) throws SQLException {
        String sql = """
                SELECT other.name, COUNT(*) AS cnt
                FROM organization o
                JOIN record_organization ro ON o.org_id = ro.org_id
                JOIN record_organization ro2 ON ro.record_id = ro2.record_id
                JOIN organization other ON ro2.org_id = other.org_id
                WHERE o.name = ? AND other.name <> o.name
                GROUP BY other.name
                ORDER BY cnt DESC, other.name
                LIMIT 10
                """;
        return queryRelatedItems(conn, sql, name);
    }

    private List<RelatedItem> queryRelatedItems(Connection conn, String sql, String arg) throws SQLException {
        List<RelatedItem> result = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, arg);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) result.add(new RelatedItem(rs.getString(1), rs.getInt(2)));
            }
        }
        return result;
    }

    private GkgRecord mapRecord(ResultSet rs) throws SQLException {
        GkgRecord r = new GkgRecord();
        r.setRecordId(rs.getString("record_id"));
        r.setPublishDate(rs.getString("publish_date"));
        r.setSourceCollection(rs.getObject("source_collection") != null ? rs.getInt("source_collection") : null);
        r.setSourceCommonName(rs.getString("source_common_name"));
        r.setDocumentId(rs.getString("document_id"));
        r.setTone(rs.getObject("tone") != null ? rs.getDouble("tone") : null);
        r.setPositiveScore(rs.getObject("positive_score") != null ? rs.getDouble("positive_score") : null);
        r.setNegativeScore(rs.getObject("negative_score") != null ? rs.getDouble("negative_score") : null);
        r.setPolarity(rs.getObject("polarity") != null ? rs.getDouble("polarity") : null);
        r.setWordCount(rs.getObject("word_count") != null ? rs.getInt("word_count") : null);
        return r;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record QuerySql(String sql, List<String> params) {
        int bind(PreparedStatement stmt) throws SQLException {
            int index = 1;
            for (String param : params) stmt.setString(index++, param);
            return index;
        }
    }
}
