package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;

import java.sql.*;
import java.util.*;

public class ThemeRanker {

    /**
     * 按日/周/月统计主题热度
     * @param granularity DAY / WEEK / MONTH
     * @param from 开始日期
     * @param to 结束日期
     * @param topN 返回前 N 个主题
     */
    public List<ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) throws SQLException {
        String dateFormat = switch (granularity.toUpperCase()) {
            case "DAY" -> "%Y-%m-%d";
            case "WEEK" -> "%Y-%W";
            case "MONTH" -> "%Y-%m";
            default -> throw new IllegalArgumentException("粒度必须是 DAY/WEEK/MONTH");
        };

        String sql = """
            SELECT
                t.code,
                strftime(?, r.publish_date) AS bucket,
                COUNT(*) AS cnt
            FROM record_theme rt
            JOIN theme t ON rt.theme_id = t.theme_id
            JOIN gkg_record r ON rt.record_id = r.record_id
            WHERE r.publish_date BETWEEN ? AND ?
            GROUP BY t.code, bucket
            ORDER BY cnt DESC
            LIMIT ?
            """;

        List<ThemeHeat> results = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dateFormat);
            stmt.setString(2, from);
            stmt.setString(3, to);
            stmt.setInt(4, topN);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(new ThemeHeat(
                        rs.getString("code"),
                        rs.getString("bucket"),
                        rs.getInt("cnt")
                ));
            }
        }
        return results;
    }

    /**
     * 获取所有主题及其总热度
     */
    public List<ThemeHeat> getAllThemeHeat() throws SQLException {
        String sql = """
            SELECT
                t.code,
                'ALL' AS bucket,
                COUNT(*) AS cnt
            FROM record_theme rt
            JOIN theme t ON rt.theme_id = t.theme_id
            GROUP BY t.code
            ORDER BY cnt DESC
            """;

        List<ThemeHeat> results = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new ThemeHeat(
                        rs.getString("code"),
                        rs.getString("bucket"),
                        rs.getInt("cnt")
                ));
            }
        }
        return results;
    }

    public record ThemeHeat(String themeCode, String bucket, int count) {}
}
