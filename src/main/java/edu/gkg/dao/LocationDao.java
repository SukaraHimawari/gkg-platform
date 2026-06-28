package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationDao {

    public Long insertOrGet(String name, String countryCode, Double lat, Double lng) throws SQLException {
        String selectSql = "SELECT location_id FROM location WHERE name = ? AND country_code = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, name);
            stmt.setString(2, countryCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("location_id");
            }
        }

        String insertSql = "INSERT INTO location (name, country_code, lat, lng) VALUES (?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, countryCode);
            stmt.setObject(3, lat);
            stmt.setObject(4, lng);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("插入 Location 失败");
    }

    public Location findByName(String name) throws SQLException {
        String sql = "SELECT location_id, name, country_code, lat, lng FROM location WHERE name = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Location l = new Location();
                l.setLocationId(rs.getLong("location_id"));
                l.setName(rs.getString("name"));
                l.setCountryCode(rs.getString("country_code"));
                l.setLat(rs.getDouble("lat"));
                l.setLng(rs.getDouble("lng"));
                return l;
            }
            return null;
        }
    }

    public List<Location> findAll() throws SQLException {
        List<Location> list = new ArrayList<>();
        String sql = "SELECT location_id, name, country_code, lat, lng FROM location";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Location l = new Location();
                l.setLocationId(rs.getLong("location_id"));
                l.setName(rs.getString("name"));
                l.setCountryCode(rs.getString("country_code"));
                l.setLat(rs.getDouble("lat"));
                l.setLng(rs.getDouble("lng"));
                list.add(l);
            }
        }
        return list;
    }
}