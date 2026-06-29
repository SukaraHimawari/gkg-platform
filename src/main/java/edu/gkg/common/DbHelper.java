package edu.gkg.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DbHelper {

    private static HikariDataSource dataSource;

    private static synchronized void configure(String jdbcUrl) throws SQLException {
        close();
        createParentDirectoryIfNeeded(jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(60000);
        config.setIdleTimeout(600000);
        config.setAutoCommit(true);
        config.addDataSourceProperty("busy_timeout", "30000");

        dataSource = new HikariDataSource(config);
        createTablesIfNotExists();
    }

    private static void createParentDirectoryIfNeeded(String jdbcUrl) throws SQLException {
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) return;
        String pathText = jdbcUrl.substring("jdbc:sqlite:".length());
        if (":memory:".equals(pathText)) return;
        Path parent = Paths.get(pathText).toAbsolutePath().getParent();
        if (parent == null) return;
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new SQLException("Failed to create database directory: " + parent, e);
        }
    }

    private static void createTablesIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS gkg_record (
                record_id TEXT PRIMARY KEY,
                publish_date TEXT NOT NULL,
                source_collection INTEGER,
                source_common_name TEXT,
                document_id TEXT,
                tone REAL,
                positive_score REAL,
                negative_score REAL,
                polarity REAL,
                word_count INTEGER
            );
            CREATE INDEX IF NOT EXISTS idx_gkg_date ON gkg_record(publish_date);

            CREATE TABLE IF NOT EXISTS person (
                person_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );

            CREATE TABLE IF NOT EXISTS organization (
                org_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL
            );

            CREATE TABLE IF NOT EXISTS theme (
                theme_id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT UNIQUE NOT NULL
            );

            CREATE TABLE IF NOT EXISTS location (
                location_id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                country_code TEXT,
                lat REAL,
                lng REAL,
                UNIQUE(name, country_code)
            );

            CREATE TABLE IF NOT EXISTS record_person (
                record_id TEXT,
                person_id INTEGER,
                char_offset INTEGER,
                PRIMARY KEY(record_id, person_id)
            );
            CREATE INDEX IF NOT EXISTS idx_rp_person ON record_person(person_id);
            CREATE INDEX IF NOT EXISTS idx_rp_record ON record_person(record_id);

            CREATE TABLE IF NOT EXISTS record_organization (
                record_id TEXT,
                org_id INTEGER,
                char_offset INTEGER,
                PRIMARY KEY(record_id, org_id)
            );
            CREATE INDEX IF NOT EXISTS idx_ro_org ON record_organization(org_id);
            CREATE INDEX IF NOT EXISTS idx_ro_record ON record_organization(record_id);

            CREATE TABLE IF NOT EXISTS record_theme (
                record_id TEXT,
                theme_id INTEGER,
                char_offset INTEGER,
                PRIMARY KEY(record_id, theme_id)
            );
            CREATE INDEX IF NOT EXISTS idx_rt_theme ON record_theme(theme_id);
            CREATE INDEX IF NOT EXISTS idx_rt_record ON record_theme(record_id);

            CREATE TABLE IF NOT EXISTS record_location (
                record_id TEXT,
                location_id INTEGER,
                char_offset INTEGER,
                PRIMARY KEY(record_id, location_id)
            );
            CREATE INDEX IF NOT EXISTS idx_rl_record ON record_location(record_id);

            CREATE TABLE IF NOT EXISTS quote (
                quote_id INTEGER PRIMARY KEY AUTOINCREMENT,
                record_id TEXT,
                char_offset INTEGER,
                length INTEGER,
                verb TEXT,
                content TEXT,
                sentiment INTEGER,
                FOREIGN KEY(record_id) REFERENCES gkg_record(record_id)
            );
            CREATE INDEX IF NOT EXISTS idx_quote_record ON quote(record_id);

            CREATE TABLE IF NOT EXISTS cooccurrence (
                e1_id INTEGER,
                e1_type TEXT,
                e2_id INTEGER,
                e2_type TEXT,
                co_count INTEGER,
                PRIMARY KEY(e1_id, e1_type, e2_id, e2_type)
            );
            CREATE INDEX IF NOT EXISTS idx_cooccurrence_count ON cooccurrence(co_count DESC);
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String s : sql.split(";")) {
                if (!s.trim().isEmpty()) {
                    stmt.execute(s.trim());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            configure("jdbc:sqlite:" + System.getProperty("gkg.db.path", "db/gkg.db"));
        }
        return dataSource.getConnection();
    }

    public static synchronized void useDatabaseForTests(Path dbPath) {
        try {
            configure("jdbc:sqlite:" + dbPath.toAbsolutePath());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to switch test database", e);
        }
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
