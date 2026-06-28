package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.GkgRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GkgRecordDao {

    public void insert(GkgRecord record) throws SQLException {
        String sql = """
            INSERT INTO gkg_record 
            (record_id, publish_date, source_collection, source_common_name, document_id, 
             tone, positive_score, negative_score, polarity, word_count) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getRecordId());
            stmt.setString(2, record.getPublishDate());
            stmt.setObject(3, record.getSourceCollection());
            stmt.setString(4, record.getSourceCommonName());
            stmt.setString(5, record.getDocumentId());
            stmt.setObject(6, record.getTone());
            stmt.setObject(7, record.getPositiveScore());
            stmt.setObject(8, record.getNegativeScore());
            stmt.setObject(9, record.getPolarity());
            stmt.setObject(10, record.getWordCount());
            stmt.executeUpdate();
        }
    }

    public GkgRecord findById(String recordId) throws SQLException {
        String sql = "SELECT * FROM gkg_record WHERE record_id = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    public List<GkgRecord> findAll() throws SQLException {
        List<GkgRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM gkg_record ORDER BY publish_date DESC LIMIT 1000";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM gkg_record";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }

    private GkgRecord mapRow(ResultSet rs) throws SQLException {
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
}