package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;
import edu.gkg.service.ProgressListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CooccurrenceBuilder {

    private static final int MAX_ENTITIES_PER_NEWS = 20;
    private static final int BATCH_SIZE = 2000;

    public void rebuild() throws SQLException {
        rebuild(null);
    }

    public void rebuild(ProgressListener listener) throws SQLException {
        long startTime = System.currentTimeMillis();
        update(listener, 0, "正在清空旧共现网络");

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                clearCooccurrence(conn);
                long totalRecords = countRecordsWithEntities(conn);
                int[] counters = new int[2];
                List<EntityRef> current = new ArrayList<>();
                String[] currentRecord = new String[1];

                try (PreparedStatement query = conn.prepareStatement(entitySql());
                     PreparedStatement upsert = conn.prepareStatement(upsertSql());
                     ResultSet rs = query.executeQuery()) {
                    while (rs.next()) {
                        String recordId = rs.getString("record_id");
                        if (currentRecord[0] != null && !currentRecord[0].equals(recordId)) {
                            addPairs(current, upsert, counters);
                            reportProgress(listener, counters[0], totalRecords, startTime);
                            current.clear();
                        }
                        currentRecord[0] = recordId;
                        if (current.size() < MAX_ENTITIES_PER_NEWS) {
                            current.add(new EntityRef(rs.getLong("entity_id"), rs.getString("type")));
                        }
                    }
                    if (currentRecord[0] != null) {
                        addPairs(current, upsert, counters);
                        reportProgress(listener, counters[0], totalRecords, startTime);
                    }
                    upsert.executeBatch();
                }
                conn.commit();
                update(listener, 100, "共现网络构建完成，处理 " + counters[0] + " 条新闻，生成/累加 "
                        + counters[1] + " 对关系");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void clearCooccurrence(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM cooccurrence");
        }
    }

    private long countRecordsWithEntities(Connection conn) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT record_id FROM record_person
                    UNION
                    SELECT record_id FROM record_organization
                )
                """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private String entitySql() {
        return """
                SELECT record_id, type, entity_id
                FROM (
                    SELECT record_id, 'PERSON' AS type, person_id AS entity_id, char_offset
                    FROM record_person
                    UNION ALL
                    SELECT record_id, 'ORGANIZATION' AS type, org_id AS entity_id, char_offset
                    FROM record_organization
                )
                ORDER BY record_id, char_offset, type, entity_id
                """;
    }

    private String upsertSql() {
        return """
                INSERT INTO cooccurrence (e1_id, e1_type, e2_id, e2_type, co_count)
                VALUES (?, ?, ?, ?, 1)
                ON CONFLICT(e1_id, e1_type, e2_id, e2_type)
                DO UPDATE SET co_count = co_count + 1
                """;
    }

    private void addPairs(List<EntityRef> entities, PreparedStatement stmt, int[] counters) throws SQLException {
        counters[0]++;
        if (entities.size() < 2) return;
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                EntityPair pair = ordered(entities.get(i), entities.get(j));
                if (pair.left().equals(pair.right())) continue;
                stmt.setLong(1, pair.left().id());
                stmt.setString(2, pair.left().type());
                stmt.setLong(3, pair.right().id());
                stmt.setString(4, pair.right().type());
                stmt.addBatch();
                counters[1]++;
                if (counters[1] % BATCH_SIZE == 0) {
                    stmt.executeBatch();
                }
            }
        }
    }

    private EntityPair ordered(EntityRef a, EntityRef b) {
        int byType = a.type().compareTo(b.type());
        if (byType < 0 || (byType == 0 && a.id() <= b.id())) {
            return new EntityPair(a, b);
        }
        return new EntityPair(b, a);
    }

    private void reportProgress(ProgressListener listener, int processed, long total, long startTime) {
        if (listener == null || processed % 500 != 0) return;
        int pct = total <= 0 ? 90 : (int) Math.min(99, 5 + processed * 90 / total);
        long elapsedSec = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
        update(listener, pct, "已处理 " + processed + "/" + total + " 条新闻，速度 "
                + (processed / elapsedSec) + " 条/秒");
    }

    private void update(ProgressListener listener, int pct, String message) {
        if (listener != null) listener.onProgress(Math.max(0, Math.min(100, pct)), message);
    }

    public record EntityRef(Long id, String type) {}
    private record EntityPair(EntityRef left, EntityRef right) {}
}
