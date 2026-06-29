package edu.gkg.service.impl;

import edu.gkg.common.DbHelper;
import edu.gkg.model.*;
import edu.gkg.service.CleanResult;
import edu.gkg.service.ImportResult;
import edu.gkg.service.ProgressListener;
import edu.gkg.service.ImportService;
import edu.gkg.service.parser.GkgLineParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ImportServiceImpl implements ImportService {

    private final GkgLineParser parser = new GkgLineParser();

    @Override
    public ImportResult importFile(File csvFile, ProgressListener listener) {
        long startTime = System.currentTimeMillis();
        int total = 0;
        int success = 0;
        int skipped = 0;

        if (listener != null) {
            listener.onProgress(0, "开始导入: " + csvFile.getName());
        }

        List<GkgLineParser.ParseResult> batch = new ArrayList<>();
        int BATCH_SIZE = 500;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                try {
                    GkgLineParser.ParseResult result = parser.parseLine(line);
                    if (result.record() == null || result.record().getRecordId() == null) {
                        skipped++;
                        continue;
                    }
                    batch.add(result);
                    success++;

                    if (batch.size() >= BATCH_SIZE) {
                        saveBatch(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    skipped++;
                }

                if (total % 100 == 0 && listener != null) {
                    int progress = Math.min(99, total / 100);
                    listener.onProgress(progress, "已处理 " + total + " 行，成功 " + success + " 条");
                }
            }

            if (!batch.isEmpty()) {
                saveBatch(batch);
                batch.clear();
            }

        } catch (IOException e) {
            if (listener != null) {
                listener.onProgress(100, "导入失败: " + e.getMessage());
            }
            return new ImportResult(total, success, skipped, System.currentTimeMillis() - startTime);
        }

        if (listener != null) {
            listener.onProgress(100, "导入完成！共 " + total + " 行，成功 " + success + " 条，跳过 " + skipped + " 条");
        }

        return new ImportResult(total, success, skipped, System.currentTimeMillis() - startTime);
    }

    @Override
    public ImportResult importDirectory(File dir, ProgressListener listener) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("不是有效目录: " + dir.getAbsolutePath());
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv") || name.endsWith(".tsv"));
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("目录下没有 CSV/TSV 文件");
        }

        int totalAll = 0;
        int successAll = 0;
        int skippedAll = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < files.length; i++) {
            if (listener != null) {
                listener.onProgress(i * 100 / files.length, "正在导入: " + files[i].getName());
            }
            ImportResult r = importFile(files[i], null);
            totalAll += r.total();
            successAll += r.success();
            skippedAll += r.skipped();
        }

        if (listener != null) {
            listener.onProgress(100, "批量导入完成！共 " + files.length + " 个文件");
        }

        return new ImportResult(totalAll, successAll, skippedAll, System.currentTimeMillis() - startTime);
    }

    @Override
    public CleanResult cleanInvalidData() {
        int duplicateRemoved = 0;
        int nullThemeRemoved = 0;
        int invalidRowRemoved = 0;

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);

            String deleteNoTheme = """
                DELETE FROM gkg_record
                WHERE record_id NOT IN (SELECT DISTINCT record_id FROM record_theme)
                """;
            try (var stmt = conn.createStatement()) {
                nullThemeRemoved = stmt.executeUpdate(deleteNoTheme);
            }

            String deleteDuplicate = """
                DELETE FROM gkg_record
                WHERE record_id IN (
                    SELECT record_id FROM (
                        SELECT record_id, ROW_NUMBER() OVER (PARTITION BY document_id ORDER BY publish_date) AS rn
                        FROM gkg_record
                    ) t WHERE rn > 1
                )
                """;
            try (var stmt = conn.createStatement()) {
                duplicateRemoved = stmt.executeUpdate(deleteDuplicate);
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new CleanResult(duplicateRemoved, nullThemeRemoved, invalidRowRemoved);
    }

    private void saveBatch(List<GkgLineParser.ParseResult> batch) {
        if (batch.isEmpty()) return;

        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);

            String recordSql = """
                INSERT OR IGNORE INTO gkg_record
                (record_id, publish_date, source_collection, source_common_name, document_id,
                 tone, positive_score, negative_score, polarity, word_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            String personSql = "INSERT OR IGNORE INTO person (name) VALUES (?)";
            String orgSql = "INSERT OR IGNORE INTO organization (name) VALUES (?)";
            String themeSql = "INSERT OR IGNORE INTO theme (code) VALUES (?)";
            String locationSql = "INSERT OR IGNORE INTO location (name, country_code, lat, lng) VALUES (?, ?, ?, ?)";

            try (PreparedStatement recordStmt = conn.prepareStatement(recordSql);
                 PreparedStatement personStmt = conn.prepareStatement(personSql);
                 PreparedStatement orgStmt = conn.prepareStatement(orgSql);
                 PreparedStatement themeStmt = conn.prepareStatement(themeSql);
                 PreparedStatement locationStmt = conn.prepareStatement(locationSql)) {

                for (GkgLineParser.ParseResult result : batch) {
                    GkgRecord record = result.record();

                    recordStmt.setString(1, record.getRecordId());
                    recordStmt.setString(2, record.getPublishDate());
                    recordStmt.setObject(3, record.getSourceCollection());
                    recordStmt.setString(4, record.getSourceCommonName());
                    recordStmt.setString(5, record.getDocumentId());
                    recordStmt.setObject(6, record.getTone());
                    recordStmt.setObject(7, record.getPositiveScore());
                    recordStmt.setObject(8, record.getNegativeScore());
                    recordStmt.setObject(9, record.getPolarity());
                    recordStmt.setObject(10, record.getWordCount());
                    recordStmt.addBatch();

                    for (GkgLineParser.PersonOffset po : result.personOffsets()) {
                        personStmt.setString(1, po.name());
                        personStmt.addBatch();
                    }

                    for (GkgLineParser.OrgOffset oo : result.orgOffsets()) {
                        orgStmt.setString(1, oo.name());
                        orgStmt.addBatch();
                    }

                    for (Theme theme : result.themes()) {
                        themeStmt.setString(1, theme.getCode());
                        themeStmt.addBatch();
                    }

                    for (Location loc : result.locations()) {
                        locationStmt.setString(1, loc.getName());
                        locationStmt.setString(2, loc.getCountryCode());
                        locationStmt.setObject(3, loc.getLat());
                        locationStmt.setObject(4, loc.getLng());
                        locationStmt.addBatch();
                    }
                }

                recordStmt.executeBatch();
                personStmt.executeBatch();
                orgStmt.executeBatch();
                themeStmt.executeBatch();
                locationStmt.executeBatch();

            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                return;
            }

            // 查询本批次所有实体的 ID (批量查询)
            Set<String> personNames = new HashSet<>();
            Set<String> orgNames = new HashSet<>();
            Set<String> themeCodes = new HashSet<>();
            for (GkgLineParser.ParseResult result : batch) {
                for (GkgLineParser.PersonOffset po : result.personOffsets()) {
                    personNames.add(po.name());
                }
                for (GkgLineParser.OrgOffset oo : result.orgOffsets()) {
                    orgNames.add(oo.name());
                }
                for (Theme theme : result.themes()) {
                    themeCodes.add(theme.getCode());
                }
            }

            Map<String, Long> personIdMap = queryIds(conn, "person", "name", personNames);
            Map<String, Long> orgIdMap = queryIds(conn, "organization", "name", orgNames);
            Map<String, Long> themeIdMap = queryIds(conn, "theme", "code", themeCodes);

            // record_person
            if (!personIdMap.isEmpty()) {
                String linkPersonSql = "INSERT OR IGNORE INTO record_person (record_id, person_id, char_offset) VALUES (?, ?, ?)";
                try (PreparedStatement linkStmt = conn.prepareStatement(linkPersonSql)) {
                    for (GkgLineParser.ParseResult result : batch) {
                        String recordId = result.record().getRecordId();
                        for (GkgLineParser.PersonOffset po : result.personOffsets()) {
                            Long id = personIdMap.get(po.name());
                            if (id != null) {
                                linkStmt.setString(1, recordId);
                                linkStmt.setLong(2, id);
                                linkStmt.setInt(3, 0);
                                linkStmt.addBatch();
                            }
                        }
                    }
                    linkStmt.executeBatch();
                }
            }

            // record_organization
            if (!orgIdMap.isEmpty()) {
                String linkOrgSql = "INSERT OR IGNORE INTO record_organization (record_id, org_id, char_offset) VALUES (?, ?, ?)";
                try (PreparedStatement linkStmt = conn.prepareStatement(linkOrgSql)) {
                    for (GkgLineParser.ParseResult result : batch) {
                        String recordId = result.record().getRecordId();
                        for (GkgLineParser.OrgOffset oo : result.orgOffsets()) {
                            Long id = orgIdMap.get(oo.name());
                            if (id != null) {
                                linkStmt.setString(1, recordId);
                                linkStmt.setLong(2, id);
                                linkStmt.setInt(3, 0);
                                linkStmt.addBatch();
                            }
                        }
                    }
                    linkStmt.executeBatch();
                }
            }

            // record_theme
            if (!themeIdMap.isEmpty()) {
                String linkThemeSql = "INSERT OR IGNORE INTO record_theme (record_id, theme_id, char_offset) VALUES (?, ?, ?)";
                try (PreparedStatement linkStmt = conn.prepareStatement(linkThemeSql)) {
                    for (GkgLineParser.ParseResult result : batch) {
                        String recordId = result.record().getRecordId();
                        for (Theme theme : result.themes()) {
                            Long id = themeIdMap.get(theme.getCode());
                            if (id != null) {
                                linkStmt.setString(1, recordId);
                                linkStmt.setLong(2, id);
                                linkStmt.setInt(3, 0);
                                linkStmt.addBatch();
                            }
                        }
                    }
                    linkStmt.executeBatch();
                }
            }

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Long> queryIds(Connection conn, String table, String idColumn, String nameColumn, Set<String> names) throws SQLException {
        Map<String, Long> result = new HashMap<>();
        if (names.isEmpty()) return result;
        String inClause = String.join(",", Collections.nCopies(names.size(), "?"));
        String sql = "SELECT " + idColumn + ", " + nameColumn + " FROM " + table + " WHERE " + nameColumn + " IN (" + inClause + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String name : names) {
                stmt.setString(idx++, name);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.put(rs.getString(nameColumn), rs.getLong(idColumn));
            }
        }
        return result;
    }

    private Map<String, Long> queryIds(Connection conn, String table, String nameColumn, Set<String> names) throws SQLException {
        return queryIds(conn, table, table.equals("person") ? "person_id" :
                        table.equals("organization") ? "org_id" :
                        table.equals("theme") ? "theme_id" : "location_id",
                nameColumn, names);
    }
}
