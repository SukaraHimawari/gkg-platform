package edu.gkg.dao;

import edu.gkg.common.DbHelper;
import edu.gkg.model.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonDao {

    // 插入一个人物（如果已存在则返回已有ID）
    public Long insertOrGet(String name) throws SQLException {
        // 先查是否存在
        String selectSql = "SELECT person_id FROM person WHERE name = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("person_id");
            }
        }

        // 不存在则插入
        String insertSql = "INSERT INTO person (name) VALUES (?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("插入 Person 失败");
    }

    // 根据名字查询人物
    public Person findByName(String name) throws SQLException {
        String sql = "SELECT person_id, name FROM person WHERE name = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Person p = new Person();
                p.setPersonId(rs.getLong("person_id"));
                p.setName(rs.getString("name"));
                return p;
            }
            return null;
        }
    }

    // 查询所有人
    public List<Person> findAll() throws SQLException {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT person_id, name FROM person ORDER BY name";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Person p = new Person();
                p.setPersonId(rs.getLong("person_id"));
                p.setName(rs.getString("name"));
                list.add(p);
            }
        }
        return list;
    }

    // 统计总数
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM person";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        }
    }
}