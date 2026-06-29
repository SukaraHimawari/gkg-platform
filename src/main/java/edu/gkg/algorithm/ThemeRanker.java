package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ThemeRanker {

    public List<ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) throws SQLException {
        String bucketExpr = switch (granularity == null ? "DAY" : granularity.toUpperCase()) {
            case "DAY" -> "substr(r.publish_date, 1, 4) || '-' || substr(r.publish_date, 5, 2) || '-' || substr(r.publish_date, 7, 2)";
            case "WEEK" -> "substr(r.publish_date, 1, 4) || '-W' || strftime('%W', substr(r.publish_date,1,4) || '-' || substr(r.publish_date,5,2) || '-' || substr(r.publish_date,7,2))";
            case "MONTH" -> "substr(r.publish_date, 1, 4) || '-' || substr(r.publish_date, 5, 2)";
            default -> throw new IllegalArgumentException("Granularity must be DAY, WEEK, or MONTH");
        };
        String sql = """
                SELECT t.code, %s AS bucket, COUNT(*) AS cnt
                FROM record_theme rt
                JOIN theme t ON rt.theme_id = t.theme_id
                JOIN gkg_record r ON rt.record_id = r.record_id
                WHERE substr(r.publish_date, 1, 8) BETWEEN ? AND ?
                GROUP BY t.code, bucket
                ORDER BY cnt DESC
                LIMIT ?
                """.formatted(bucketExpr);

        List<ThemeHeat> results = new ArrayList<>();
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeDate(from, "00000000"));
            stmt.setString(2, normalizeDate(to, "99999999"));
            stmt.setInt(3, Math.max(1, topN));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ThemeHeat(
                            rs.getString("code"),
                            rs.getString("bucket"),
                            rs.getInt("cnt")
                    ));
                }
            }
        }
        return results;
    }

    public List<ThemeHeat> getAllThemeHeat() throws SQLException {
        String sql = """
                SELECT t.code, 'ALL' AS bucket, COUNT(*) AS cnt
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

    private static String normalizeDate(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String digits = value.replaceAll("[^0-9]", "");
        return digits.length() >= 8 ? digits.substring(0, 8) : fallback;
    }

    public record ThemeHeat(String themeCode, String bucket, int count) {}
}
