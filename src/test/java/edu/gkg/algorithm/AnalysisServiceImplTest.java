package edu.gkg.algorithm;

import edu.gkg.common.DbHelper;
import edu.gkg.service.ImportResult;
import edu.gkg.service.impl.AnalysisServiceImpl;
import edu.gkg.service.impl.ImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceImplTest {

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
    void clustersImportedThemesWithKMeans() throws Exception {
        String prefix = "test-" + UUID.randomUUID() + "-";
        Path file = tempDir.resolve("cluster.gkg.csv");
        Files.writeString(file, String.join(System.lineSeparator(),
                ImportServiceImplTest.gkgLine(prefix + "1", "TAX_WORLD;MEDIA_SOCIAL"),
                ImportServiceImplTest.gkgLine(prefix + "2", "TAX_WORLD;ECON_STOCKMARKET"),
                ImportServiceImplTest.gkgLine(prefix + "3", "HEALTH_PANDEMIC;MEDICAL"),
                ImportServiceImplTest.gkgLine(prefix + "4", "HEALTH_PANDEMIC;SCIENCE"))
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportResult imported = new ImportServiceImpl().importFile(file.toFile(), null);
        List<String> clusters = new AnalysisServiceImpl().clusterByKMeans(2, 100);

        assertEquals(4, imported.success());
        assertFalse(clusters.isEmpty());
        assertEquals(2, clusters.size());
    }

    @Test
    void ranksFocusEntitiesAndBuildsSentimentTrend() throws Exception {
        String prefix = "test-" + UUID.randomUUID() + "-";
        String theme = "ANALYSIS_THEME_" + prefix.replace("-", "_");
        Path file = tempDir.resolve("analysis.gkg.csv");
        Files.writeString(file, String.join(System.lineSeparator(),
                gkgLine(prefix + "a", "20240101120000", theme, "Alice Focus " + prefix, "Focus Org " + prefix, "1.0,2.0,1.0,3.0,0,0,100"),
                gkgLine(prefix + "b", "20240102120000", theme, "Alice Focus " + prefix + ";Bob Focus " + prefix, "Focus Org " + prefix, "3.0,4.0,1.0,5.0,0,0,100"))
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportResult imported = new ImportServiceImpl().importFile(file.toFile(), null);
        AnalysisServiceImpl service = new AnalysisServiceImpl();
        service.rebuildCooccurrence();

        List<String> focus = service.getTopFocusPersons(5);
        List<String> sentiment = service.sentimentTrend(theme, "2024-01-01", "2024-01-02");

        assertEquals(2, imported.success());
        assertFalse(focus.isEmpty());
        assertFalse(sentiment.isEmpty());
        assertEquals(2, sentiment.size());
    }

    @Test
    void buildsThemeHeatFromImportedGkgDates() throws Exception {
        String prefix = "test-" + UUID.randomUUID() + "-";
        String theme = "HEAT_THEME_" + prefix.replace("-", "_");
        Path file = tempDir.resolve("heat.gkg.csv");
        Files.writeString(file, String.join(System.lineSeparator(),
                gkgLine(prefix + "h1", "20240101120000", theme, "Heat Person " + prefix, "Heat Org " + prefix, "1.0,2.0,1.0,3.0,0,0,100"),
                gkgLine(prefix + "h2", "20240101123000", theme, "Heat Person " + prefix, "Heat Org " + prefix, "2.0,3.0,1.0,4.0,0,0,100"))
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportResult imported = new ImportServiceImpl().importFile(file.toFile(), null);
        List<ThemeRanker.ThemeHeat> heat = new AnalysisServiceImpl()
                .getThemeHeat("DAY", "2024-01-01", "2024-01-01", 10);

        assertEquals(2, imported.success());
        assertTrue(heat.stream().anyMatch(row ->
                row.themeCode().equals(theme)
                        && row.bucket().equals("2024-01-01")
                        && row.count() == 2));
    }

    private static String gkgLine(String recordId, String publishDate, String themes,
                                  String persons, String orgs, String tone) {
        String[] cols = new String[27];
        for (int i = 0; i < cols.length; i++) cols[i] = "";
        cols[0] = recordId;
        cols[1] = publishDate;
        cols[2] = "1";
        cols[3] = "example.com";
        cols[4] = "https://example.com/" + recordId;
        cols[7] = themes;
        cols[11] = persons;
        cols[13] = orgs;
        cols[15] = tone;
        return String.join("\t", cols);
    }
}
