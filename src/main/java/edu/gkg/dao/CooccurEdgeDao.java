package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.CooccurEdge;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CooccurEdgeDao {

    /**
     * 清空共现表（重新构建前调用）
     */
    public void truncate() throws SQLException {
        String sql = "DELETE FROM cooccurrence";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 批量插入共现边
     */
    public void insertBatch(List<CooccurEdge> edges) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO cooccurrence (e1_id, e1_type, e2_id, e2_type, co_count)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (CooccurEdge edge : edges) {
                stmt.setLong(1, edge.getE1Id());
                stmt.setString(2, edge.getE1Type());
                stmt.setLong(3, edge.getE2Id());
                stmt.setString(4, edge.getE2Type());
                stmt.setInt(5, edge.getCoCount());
                stmt.addBatch();
                count++;
                // 每 1000 条提交一次
                if (count % 1000 == 0) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    /**
     * 查询所有共现边（给 PageRank 用）
     */
    public List<CooccurEdge> findAllEdges() throws SQLException {
        List<CooccurEdge> list = new ArrayList<>();
        String sql = "SELECT e1_id, e1_type, e2_id, e2_type, co_count FROM cooccurrence";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                CooccurEdge edge = new CooccurEdge();
                edge.setE1Id(rs.getLong("e1_id"));
                edge.setE1Type(rs.getString("e1_type"));
                edge.setE2Id(rs.getLong("e2_id"));
                edge.setE2Type(rs.getString("e2_type"));
                edge.setCoCount(rs.getInt("co_count"));
                list.add(edge);
            }
        }
        return list;
    }

    public List<CooccurEdge> findTopEdges(int limit) throws SQLException {
        List<CooccurEdge> list = new ArrayList<>();
        String sql = """
                SELECT e1_id, e1_type, e2_id, e2_type, co_count
                FROM cooccurrence
                ORDER BY co_count DESC
                LIMIT ?
                """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CooccurEdge edge = new CooccurEdge();
                    edge.setE1Id(rs.getLong("e1_id"));
                    edge.setE1Type(rs.getString("e1_type"));
                    edge.setE2Id(rs.getLong("e2_id"));
                    edge.setE2Type(rs.getString("e2_type"));
                    edge.setCoCount(rs.getInt("co_count"));
                    list.add(edge);
                }
            }
        }
        return list;
    }

    /**
     * 统计总边数
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cooccurrence";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }
}
