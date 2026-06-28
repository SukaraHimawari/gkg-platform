package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.Organization;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrganizationDao {

    public Long insertOrGet(String name) throws SQLException {
        String selectSql = "SELECT org_id FROM organization WHERE name = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("org_id");
            }
        }

        String insertSql = "INSERT INTO organization (name) VALUES (?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("插入 Organization 失败");
    }

    public Organization findByName(String name) throws SQLException {
        String sql = "SELECT org_id, name FROM organization WHERE name = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Organization o = new Organization();
                o.setOrgId(rs.getLong("org_id"));
                o.setName(rs.getString("name"));
                return o;
            }
            return null;
        }
    }

    public List<Organization> findAll() throws SQLException {
        List<Organization> list = new ArrayList<>();
        String sql = "SELECT org_id, name FROM organization ORDER BY name";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Organization o = new Organization();
                o.setOrgId(rs.getLong("org_id"));
                o.setName(rs.getString("name"));
                list.add(o);
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM organization";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }
}