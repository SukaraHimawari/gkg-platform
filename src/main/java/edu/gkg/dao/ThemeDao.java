package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.Theme;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeDao {

    public Long insertOrGet(String code) throws SQLException {
        String selectSql = "SELECT theme_id FROM theme WHERE code = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("theme_id");
            }
        }

        String insertSql = "INSERT INTO theme (code) VALUES (?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, code);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("插入 Theme 失败");
    }

    public Theme findByCode(String code) throws SQLException {
        String sql = "SELECT theme_id, code FROM theme WHERE code = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Theme t = new Theme();
                t.setThemeId(rs.getLong("theme_id"));
                t.setCode(rs.getString("code"));
                return t;
            }
            return null;
        }
    }

    public List<Theme> findAll() throws SQLException {
        List<Theme> list = new ArrayList<>();
        String sql = "SELECT theme_id, code FROM theme ORDER BY code";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Theme t = new Theme();
                t.setThemeId(rs.getLong("theme_id"));
                t.setCode(rs.getString("code"));
                list.add(t);
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM theme";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }
}