package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;

import java.sql.*;
import java.util.*;

public class CooccurrenceBuilder {

    private static final int MAX_ENTITIES_PER_NEWS = 20;

    public void rebuild() throws SQLException {
        System.out.println("🔄 开始构建共现网络...");
        long startTime = System.currentTimeMillis();

        clearCooccurrence();

        Map<String, List<EntityRef>> newsEntities = loadNewsEntities();
        System.out.println("📊 加载了 " + newsEntities.size() + " 条新闻的实体");

        Map<String, Integer> cooccurCount = new HashMap<>();
        int processed = 0;

        for (Map.Entry<String, List<EntityRef>> entry : newsEntities.entrySet()) {
            List<EntityRef> entities = entry.getValue();
            if (entities.size() < 2) continue;

            if (entities.size() > MAX_ENTITIES_PER_NEWS) {
                entities = entities.subList(0, MAX_ENTITIES_PER_NEWS);
            }

            for (int i = 0; i < entities.size(); i++) {
                for (int j = i + 1; j < entities.size(); j++) {
                    String key = makeKey(entities.get(i), entities.get(j));
                    cooccurCount.put(key, cooccurCount.getOrDefault(key, 0) + 1);
                }
            }

            processed++;
            if (processed % 1000 == 0) {
                System.out.println("  已处理 " + processed + " 条新闻");
            }
        }

        System.out.println("✅ 计算完成，共 " + cooccurCount.size() + " 对共现");

        saveEdges(cooccurCount);

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("✅ 共现网络构建完成！耗时 " + elapsed / 1000 + " 秒");
    }

    private void clearCooccurrence() throws SQLException {
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM cooccurrence");
        }
        System.out.println("✅ 清空旧共现数据");
    }

    private Map<String, List<EntityRef>> loadNewsEntities() throws SQLException {
        Map<String, List<EntityRef>> result = new HashMap<>();

        String sql = """
            SELECT
                record_id,
                'PERSON' as type,
                person_id as entity_id
            FROM record_person

            UNION ALL

            SELECT
                record_id,
                'ORGANIZATION' as type,
                org_id as entity_id
            FROM record_organization
            """;

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String recordId = rs.getString("record_id");
                String type = rs.getString("type");
                Long entityId = rs.getLong("entity_id");

                result.computeIfAbsent(recordId, k -> new ArrayList<>())
                        .add(new EntityRef(entityId, type));
            }
        }

        return result;
    }

    private String makeKey(EntityRef e1, EntityRef e2) {
        if (e1.id < e2.id || (e1.id.equals(e2.id) && e1.type.compareTo(e2.type) < 0)) {
            return e1.id + "|" + e1.type + "|" + e2.id + "|" + e2.type;
        } else {
            return e2.id + "|" + e2.type + "|" + e1.id + "|" + e1.type;
        }
    }

    private void saveEdges(Map<String, Integer> cooccurCount) throws SQLException {
        String sql = "INSERT OR IGNORE INTO cooccurrence (e1_id, e1_type, e2_id, e2_type, co_count) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            int count = 0;
            for (Map.Entry<String, Integer> entry : cooccurCount.entrySet()) {
                String[] parts = entry.getKey().split("\\|");
                if (parts.length != 4) continue;

                stmt.setLong(1, Long.parseLong(parts[0]));
                stmt.setString(2, parts[1]);
                stmt.setLong(3, Long.parseLong(parts[2]));
                stmt.setString(4, parts[3]);
                stmt.setInt(5, entry.getValue());
                stmt.addBatch();

                count++;
                if (count % 1000 == 0) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }

        System.out.println("✅ 保存 " + cooccurCount.size() + " 条边到数据库");
    }

    public record EntityRef(Long id, String type) {}
}
