package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.Quote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuoteDao {

    public void insert(Quote quote) throws SQLException {
        String sql = "INSERT INTO quote (record_id, char_offset, length, verb, content, sentiment) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, quote.getRecordId());
            stmt.setObject(2, quote.getCharOffset());
            stmt.setObject(3, quote.getLength());
            stmt.setString(4, quote.getVerb());
            stmt.setString(5, quote.getContent());
            stmt.setObject(6, quote.getSentiment());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                quote.setQuoteId(rs.getLong(1));
            }
        }
    }

    public List<Quote> findByRecordId(String recordId) throws SQLException {
        List<Quote> list = new ArrayList<>();
        String sql = "SELECT quote_id, record_id, char_offset, length, verb, content, sentiment FROM quote WHERE record_id = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recordId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Quote q = new Quote();
                q.setQuoteId(rs.getLong("quote_id"));
                q.setRecordId(rs.getString("record_id"));
                q.setCharOffset(rs.getInt("char_offset"));
                q.setLength(rs.getInt("length"));
                q.setVerb(rs.getString("verb"));
                q.setContent(rs.getString("content"));
                q.setSentiment(rs.getInt("sentiment"));
                list.add(q);
            }
        }
        return list;
    }
}