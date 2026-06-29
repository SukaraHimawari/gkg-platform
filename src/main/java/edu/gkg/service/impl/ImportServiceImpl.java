package edu.gkg.service.impl;

import edu.gkg.common.DbHelper;
import edu.gkg.model.GkgRecord;
import edu.gkg.model.Location;
import edu.gkg.service.CleanResult;
import edu.gkg.service.ImportResult;
import edu.gkg.service.ImportService;
import edu.gkg.service.ProgressListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;

public class ImportServiceImpl implements ImportService {
    private static final int BATCH_SIZE = 500;
    private static final int PARSE_CHUNK_SIZE = 2_000;
    private static final int PARSE_THREADS = Math.max(2, Math.min(
            Runtime.getRuntime().availableProcessors(),
            Integer.getInteger("gkg.import.parseThreads", Runtime.getRuntime().availableProcessors())));
    private final GkgLineParser parser = new GkgLineParser();

    @Override
    public ImportResult importFile(File csvFile, ProgressListener listener) {
        long start = System.currentTimeMillis();
        ImportAccumulator acc = new ImportAccumulator();
        update(listener, 0, "开始导入 " + csvFile.getName());
        try {
            if (!csvFile.isFile()) {
                throw new IllegalArgumentException("不是有效文件: " + csvFile.getAbsolutePath());
            }
            if (csvFile.getName().toLowerCase().endsWith(".zip")) {
                importZip(csvFile, listener, acc);
            } else {
                try (InputStream input = new FileInputStream(csvFile)) {
                    importStreamParallel(input, listener, acc, csvFile.length());
                }
            }
            update(listener, 100, "导入完成: 成功 " + acc.success + " 条，跳过 " + acc.skipped + " 条");
        } catch (Exception e) {
            update(listener, 100, "导入失败: " + e.getMessage());
        }
        return new ImportResult(acc.total, acc.success, acc.skipped, System.currentTimeMillis() - start);
    }

    @Override
    public ImportResult importDirectory(File dir, ProgressListener listener) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("不是有效目录: " + dir.getAbsolutePath());
        }
        long start = System.currentTimeMillis();
        int total = 0;
        int success = 0;
        int skipped = 0;
        List<File> files = findImportableFiles(dir);
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            int fileBase = files.isEmpty() ? 100 : i * 100 / files.size();
            int fileSpan = files.isEmpty() ? 0 : Math.max(1, 100 / files.size());
            update(listener, fileBase, "导入 " + file.getName());
            ImportResult r = importFile(file, (pct, msg) ->
                    update(listener, Math.min(99, fileBase + pct * fileSpan / 100), msg));
            total += r.total();
            success += r.success();
            skipped += r.skipped();
        }
        update(listener, 100, "目录导入完成: " + files.size() + " 个文件");
        return new ImportResult(total, success, skipped, System.currentTimeMillis() - start);
    }

    @Override
    public CleanResult cleanInvalidData() {
        int orphanLinks = 0;
        int invalidRows = 0;
        try (Connection conn = DbHelper.getConnection(); Statement stmt = conn.createStatement()) {
            invalidRows = stmt.executeUpdate("""
                    DELETE FROM gkg_record
                    WHERE tone IS NULL AND positive_score IS NULL AND negative_score IS NULL
                    """);
            orphanLinks += stmt.executeUpdate("DELETE FROM record_person WHERE record_id NOT IN (SELECT record_id FROM gkg_record)");
            orphanLinks += stmt.executeUpdate("DELETE FROM record_organization WHERE record_id NOT IN (SELECT record_id FROM gkg_record)");
            orphanLinks += stmt.executeUpdate("DELETE FROM record_theme WHERE record_id NOT IN (SELECT record_id FROM gkg_record)");
            orphanLinks += stmt.executeUpdate("DELETE FROM record_location WHERE record_id NOT IN (SELECT record_id FROM gkg_record)");
        } catch (SQLException e) {
            throw new IllegalStateException("清理无效数据失败", e);
        }
        return new CleanResult(0, orphanLinks, invalidRows);
    }

    @Override
    public CleanResult clearDatabase() {
        int deleted = 0;
        try (Connection conn = DbHelper.getConnection(); Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            try {
                deleted += stmt.executeUpdate("DELETE FROM cooccurrence");
                deleted += stmt.executeUpdate("DELETE FROM quote");
                deleted += stmt.executeUpdate("DELETE FROM record_location");
                deleted += stmt.executeUpdate("DELETE FROM record_theme");
                deleted += stmt.executeUpdate("DELETE FROM record_organization");
                deleted += stmt.executeUpdate("DELETE FROM record_person");
                deleted += stmt.executeUpdate("DELETE FROM location");
                deleted += stmt.executeUpdate("DELETE FROM theme");
                deleted += stmt.executeUpdate("DELETE FROM organization");
                deleted += stmt.executeUpdate("DELETE FROM person");
                deleted += stmt.executeUpdate("DELETE FROM gkg_record");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("清空数据库失败", e);
        }
        return new CleanResult(0, 0, deleted);
    }

    private List<File> findImportableFiles(File dir) {
        try (Stream<java.nio.file.Path> paths = Files.walk(dir.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(java.nio.file.Path::toFile)
                    .filter(this::isImportable)
                    .sorted(Comparator.comparing(File::getAbsolutePath))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void importZip(File zipFile, ProgressListener listener, ImportAccumulator acc) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && isImportableName(entry.getName())) {
                    update(listener, Math.min(99, acc.total / 1000 + 1), "解压并导入 " + entry.getName());
                    importStreamParallel(zip, listener, acc, -1);
                }
                zip.closeEntry();
            }
        }
    }

    private void importStreamParallel(InputStream input, ProgressListener listener, ImportAccumulator acc, long totalBytes) throws IOException {
        ExecutorService parserPool = Executors.newFixedThreadPool(PARSE_THREADS);
        List<LineWork> chunk = new ArrayList<>(PARSE_CHUNK_SIZE);
        long bytesRead = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("导入已中断");
                }
                acc.total++;
                bytesRead += line.getBytes(StandardCharsets.UTF_8).length + 1L;
                chunk.add(new LineWork(line));
                if (chunk.size() >= PARSE_CHUNK_SIZE) {
                    importParsedChunk(parserPool, chunk, listener, acc);
                    chunk.clear();
                }
                if (acc.total % PARSE_CHUNK_SIZE == 0) {
                    int pct = totalBytes > 0
                            ? (int) Math.min(99, bytesRead * 100 / totalBytes)
                            : Math.min(99, acc.total / 1000 + 1);
                    update(listener, pct, progressMessage("已处理 " + acc.total + " 行", acc));
                }
            }
            if (!chunk.isEmpty()) {
                importParsedChunk(parserPool, chunk, listener, acc);
            }
        } finally {
            parserPool.shutdownNow();
        }
    }

    private void importParsedChunk(ExecutorService parserPool,
                                   List<LineWork> chunk,
                                   ProgressListener listener,
                                   ImportAccumulator acc) throws IOException {
        if (chunk.isEmpty()) return;
        List<Callable<ParsedLine>> tasks = new ArrayList<>(chunk.size());
        for (LineWork work : chunk) {
            tasks.add(() -> parse(work));
        }

        List<GkgLineParser.ParseResult> batch = new ArrayList<>(BATCH_SIZE);
        try {
            List<Future<ParsedLine>> futures = parserPool.invokeAll(tasks);
            for (Future<ParsedLine> future : futures) {
                ParsedLine parsed = future.get();
                if (parsed.result == null) {
                    acc.skipped++;
                    continue;
                }
                batch.add(parsed.result);
                if (batch.size() >= BATCH_SIZE) {
                    saveParsedBatch(batch, acc);
                    batch.clear();
                }
            }
            saveParsedBatch(batch, acc);
            update(listener, Math.min(99, acc.total / 1000 + 1), progressMessage("写入批次完成", acc));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("导入已中断", e);
        } catch (Exception e) {
            throw new IOException("并行解析导入批次失败", e);
        }
    }

    private ParsedLine parse(LineWork work) {
        try {
            return new ParsedLine(parser.parseLine(work.line()));
        } catch (Exception ignored) {
            return new ParsedLine(null);
        }
    }

    private void saveParsedBatch(List<GkgLineParser.ParseResult> batch, ImportAccumulator acc) throws SQLException {
        if (batch.isEmpty()) return;
        int inserted = saveBatch(batch);
        acc.success += inserted;
        acc.skipped += batch.size() - inserted;
    }

    private String progressMessage(String prefix, ImportAccumulator acc) {
        return "并行解析 " + PARSE_THREADS + " 线程，" + prefix
                + "，成功 " + acc.success + " 条，跳过 " + acc.skipped + " 条";
    }

    private record LineWork(String line) {}

    private static class ParsedLine {
        final GkgLineParser.ParseResult result;

        ParsedLine(GkgLineParser.ParseResult result) {
            this.result = result;
        }
    }

    private int saveBatch(List<GkgLineParser.ParseResult> batch) throws SQLException {
        if (batch.isEmpty()) return 0;
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<GkgLineParser.ParseResult> insertedBatch = insertRecords(conn, batch);
                int inserted = insertedBatch.size();
                insertEntities(conn, insertedBatch);
                insertLinks(conn, insertedBatch);
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private List<GkgLineParser.ParseResult> insertRecords(Connection conn, List<GkgLineParser.ParseResult> batch) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO gkg_record
                (record_id, publish_date, source_collection, source_common_name, document_id,
                 tone, positive_score, negative_score, polarity, word_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        List<GkgLineParser.ParseResult> inserted = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (GkgLineParser.ParseResult result : batch) {
                GkgRecord r = result.record();
                stmt.setString(1, r.getRecordId());
                stmt.setString(2, r.getPublishDate());
                stmt.setObject(3, r.getSourceCollection());
                stmt.setString(4, r.getSourceCommonName());
                stmt.setString(5, r.getDocumentId());
                stmt.setObject(6, r.getTone());
                stmt.setObject(7, r.getPositiveScore());
                stmt.setObject(8, r.getNegativeScore());
                stmt.setObject(9, r.getPolarity());
                stmt.setObject(10, r.getWordCount());
                stmt.addBatch();
            }
            int[] counts = stmt.executeBatch();
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > 0) {
                    inserted.add(batch.get(i));
                }
            }
        }
        return inserted;
    }

    private void insertEntities(Connection conn, List<GkgLineParser.ParseResult> batch) throws SQLException {
        try (PreparedStatement person = conn.prepareStatement("INSERT OR IGNORE INTO person (name) VALUES (?)");
             PreparedStatement org = conn.prepareStatement("INSERT OR IGNORE INTO organization (name) VALUES (?)");
             PreparedStatement theme = conn.prepareStatement("INSERT OR IGNORE INTO theme (code) VALUES (?)");
             PreparedStatement location = conn.prepareStatement("INSERT OR IGNORE INTO location (name, country_code, lat, lng) VALUES (?, ?, ?, ?)")) {
            for (GkgLineParser.ParseResult result : batch) {
                for (GkgLineParser.PersonOffset item : result.persons()) {
                    person.setString(1, item.name());
                    person.addBatch();
                }
                for (GkgLineParser.OrgOffset item : result.organizations()) {
                    org.setString(1, item.name());
                    org.addBatch();
                }
                for (GkgLineParser.ThemeOffset item : result.themes()) {
                    theme.setString(1, item.theme().getCode());
                    theme.addBatch();
                }
                for (GkgLineParser.LocationOffset item : result.locations()) {
                    Location loc = item.location();
                    location.setString(1, loc.getName());
                    location.setString(2, loc.getCountryCode());
                    location.setObject(3, loc.getLat());
                    location.setObject(4, loc.getLng());
                    location.addBatch();
                }
            }
            person.executeBatch();
            org.executeBatch();
            theme.executeBatch();
            location.executeBatch();
        }
    }

    private void insertLinks(Connection conn, List<GkgLineParser.ParseResult> batch) throws SQLException {
        try (PreparedStatement person = conn.prepareStatement("INSERT OR IGNORE INTO record_person (record_id, person_id, char_offset) VALUES (?, ?, ?)");
             PreparedStatement org = conn.prepareStatement("INSERT OR IGNORE INTO record_organization (record_id, org_id, char_offset) VALUES (?, ?, ?)");
             PreparedStatement theme = conn.prepareStatement("INSERT OR IGNORE INTO record_theme (record_id, theme_id, char_offset) VALUES (?, ?, ?)");
             PreparedStatement location = conn.prepareStatement("INSERT OR IGNORE INTO record_location (record_id, location_id, char_offset) VALUES (?, ?, ?)")) {
            for (GkgLineParser.ParseResult result : batch) {
                String recordId = result.record().getRecordId();
                for (GkgLineParser.PersonOffset item : result.persons()) addLink(person, recordId, findId(conn, "person", "person_id", "name", item.name()), item.offset());
                for (GkgLineParser.OrgOffset item : result.organizations()) addLink(org, recordId, findId(conn, "organization", "org_id", "name", item.name()), item.offset());
                for (GkgLineParser.ThemeOffset item : result.themes()) addLink(theme, recordId, findId(conn, "theme", "theme_id", "code", item.theme().getCode()), item.offset());
                for (GkgLineParser.LocationOffset item : result.locations()) addLink(location, recordId, findLocationId(conn, item.location()), item.offset());
            }
            person.executeBatch();
            org.executeBatch();
            theme.executeBatch();
            location.executeBatch();
        }
    }

    private void addLink(PreparedStatement stmt, String recordId, Long entityId, int offset) throws SQLException {
        if (entityId == null) return;
        stmt.setString(1, recordId);
        stmt.setLong(2, entityId);
        stmt.setInt(3, offset);
        stmt.addBatch();
    }

    private Long findId(Connection conn, String table, String idColumn, String keyColumn, String value) throws SQLException {
        String sql = "SELECT " + idColumn + " FROM " + table + " WHERE " + keyColumn + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private Long findLocationId(Connection conn, Location location) throws SQLException {
        String sql = "SELECT location_id FROM location WHERE name = ? AND (country_code = ? OR (country_code IS NULL AND ? IS NULL))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location.getName());
            stmt.setString(2, location.getCountryCode());
            stmt.setString(3, location.getCountryCode());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private boolean isImportable(File file) {
        return isImportableName(file.getName());
    }

    private boolean isImportableName(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".gkg")
                || lower.endsWith(".csv.zip") || lower.endsWith(".tsv.zip") || lower.endsWith(".zip");
    }

    private void update(ProgressListener listener, int pct, String message) {
        if (listener != null) listener.onProgress(Math.max(0, Math.min(100, pct)), message);
    }

    private static class ImportAccumulator {
        int total;
        int success;
        int skipped;
    }
}
