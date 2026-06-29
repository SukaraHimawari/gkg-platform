package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;
import edu.gkg.service.ImportResult;
import edu.gkg.service.impl.ImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportServiceImplTest {

    @TempDir
    Path tempDir;

    @org.junit.jupiter.api.BeforeEach
    void useIsolatedDatabase() {
        DbHelper.useDatabaseForTests(tempDir.resolve("gkg-test.db"));
    }

    @org.junit.jupiter.api.AfterEach
    void closeIsolatedDatabase() {
        DbHelper.close();
    }

    @Test
    void importsGkgRowsAndSkipsDuplicateRecords() throws Exception {
        String recordId = "test-" + UUID.randomUUID();
        Path file = tempDir.resolve("sample.gkg.csv");
        Files.writeString(file, gkgLine(recordId, "TAX_WORLD;MEDIA_SOCIAL")
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportServiceImpl service = new ImportServiceImpl();
        ImportResult first = service.importFile(file.toFile(), null);
        ImportResult second = service.importFile(file.toFile(), null);

        assertEquals(1, first.total());
        assertEquals(1, first.success());
        assertEquals(0, first.skipped());
        assertEquals(1, second.total());
        assertEquals(0, second.success());
        assertEquals(1, second.skipped());

        assertEquals(1, countForRecord("gkg_record", recordId));
        assertEquals(2, countForRecord("record_theme", recordId));
        assertEquals(1, countForRecord("record_person", recordId));
        assertEquals(1, countForRecord("record_organization", recordId));
        assertEquals(1, countForRecord("record_location", recordId));
    }

    @Test
    void importsNestedDirectoriesAndTsvEntriesInsideZipFiles() throws Exception {
        String plainRecordId = "nested-" + UUID.randomUUID();
        String zippedRecordId = "zip-" + UUID.randomUUID();
        Path nested = Files.createDirectories(tempDir.resolve("data").resolve("20240115"));
        Files.writeString(nested.resolve("plain.gkg.csv"),
                gkgLine(plainRecordId, "TAX_WORLD") + System.lineSeparator(),
                StandardCharsets.UTF_8);

        Path zip = nested.resolve("archive.gkg.csv.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip), StandardCharsets.UTF_8)) {
            out.putNextEntry(new ZipEntry("inner.gkg.tsv"));
            out.write((gkgLine(zippedRecordId, "MEDIA_SOCIAL") + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        ImportServiceImpl service = new ImportServiceImpl();
        ImportResult result = service.importDirectory(tempDir.resolve("data").toFile(), null);

        assertEquals(2, result.total());
        assertEquals(2, result.success());
        assertEquals(0, result.skipped());
        assertEquals(1, countForRecord("gkg_record", plainRecordId));
        assertEquals(1, countForRecord("gkg_record", zippedRecordId));
    }

    @Test
    void importsLargeFilesWithParallelParsingAndSingleWriter() throws Exception {
        String prefix = "parallel-" + UUID.randomUUID() + "-";
        Path file = tempDir.resolve("large.gkg.csv");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1_200; i++) {
            content.append(gkgLine(prefix + i, i % 2 == 0 ? "TAX_WORLD;MEDIA_SOCIAL" : "ECON_STOCKMARKET"))
                    .append(System.lineSeparator());
        }
        Files.writeString(file, content.toString(), StandardCharsets.UTF_8);

        ImportServiceImpl service = new ImportServiceImpl();
        List<String> messages = new ArrayList<>();
        ImportResult first = service.importFile(file.toFile(), (pct, msg) -> messages.add(msg));
        ImportResult second = service.importFile(file.toFile(), null);

        assertEquals(1_200, first.total());
        assertEquals(1_200, first.success());
        assertEquals(0, first.skipped());
        assertEquals(1_200, second.total());
        assertEquals(0, second.success());
        assertEquals(1_200, second.skipped());
        assertEquals(1, countForRecord("gkg_record", prefix + "1199"));
        assertEquals(1_200, countTable("gkg_record"));
        org.junit.jupiter.api.Assertions.assertTrue(
                messages.stream().anyMatch(msg -> msg.contains("并行解析")),
                "large imports should report the parallel parsing path");
    }

    @Test
    void clearsImportedDatabaseTables() throws Exception {
        String recordId = "clear-" + UUID.randomUUID();
        Path file = tempDir.resolve("clear.gkg.csv");
        Files.writeString(file, gkgLine(recordId, "TAX_CLEAR")
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportServiceImpl service = new ImportServiceImpl();
        ImportResult imported = service.importFile(file.toFile(), null);

        assertEquals(1, imported.success());
        assertEquals(1, countForRecord("gkg_record", recordId));

        service.clearDatabase();

        assertEquals(0, countForRecord("gkg_record", recordId));
        assertEquals(0, countTable("person"));
        assertEquals(0, countTable("theme"));
    }

    private static int countForRecord(String table, String recordId) throws Exception {
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE record_id = ?")) {
            stmt.setString(1, recordId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.getInt(1);
            }
        }
    }

    private static int countTable(String table) throws Exception {
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = stmt.executeQuery()) {
            return rs.getInt(1);
        }
    }

    static String gkgLine(String recordId, String themes) {
        String[] cols = new String[27];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = "";
        }
        cols[0] = recordId;
        cols[1] = "20240115120000";
        cols[2] = "1";
        cols[3] = "example.com";
        cols[4] = "https://example.com/world/story";
        cols[7] = themes;
        cols[9] = "1#United States#US#US#38#-97#0";
        cols[11] = "jane doe";
        cols[13] = "openai";
        cols[15] = "2.5,4.0,1.5,5.5,0,0,321";
        return String.join("\t", cols);
    }
}
