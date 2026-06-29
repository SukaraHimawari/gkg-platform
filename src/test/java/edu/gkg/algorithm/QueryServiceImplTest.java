package edu.gkg.algorithm;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.DbHelper;
import edu.gkg.common.SharedRecords.EntityProfile;
import edu.gkg.common.SharedRecords.Page;
import edu.gkg.common.SharedRecords.QueryCondition;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.ImportResult;
import edu.gkg.service.impl.ImportServiceImpl;
import edu.gkg.service.impl.QueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryServiceImplTest {

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
    void searchesByAllSupportedConditionsAndBuildsProfilesAndTrend() throws Exception {
        String prefix = "test-" + UUID.randomUUID() + "-";
        String themeA = "QUERY_THEME_A_" + prefix.replace("-", "_");
        String themeB = "QUERY_THEME_B_" + prefix.replace("-", "_");
        String themeShared = "QUERY_THEME_SHARED_" + prefix.replace("-", "_");
        String personName = "Jane Query " + prefix;
        String otherPerson = "Other Person " + prefix;
        String orgName = "Query Org " + prefix;
        String otherOrg = "Other Org " + prefix;
        String city = "Query City " + prefix;
        String otherCity = "Other City " + prefix;
        Path file = tempDir.resolve("query.gkg.csv");
        Files.writeString(file, String.join(System.lineSeparator(),
                gkgLine(prefix + "1", "20240115120000", themeA + ";" + themeShared,
                        personName, orgName, "1#" + city + "#QC#QC#10#20#0", "2.0,3.0,1.0,4.0,0,0,100"),
                gkgLine(prefix + "2", "20240115123000", themeA,
                        personName, otherOrg, "1#" + city + "#QC#QC#10#20#0", "4.0,5.0,1.0,6.0,0,0,100"),
                gkgLine(prefix + "3", "20240116120000", themeB,
                        otherPerson, orgName, "1#" + otherCity + "#OC#OC#30#40#0", "-1.0,1.0,2.0,3.0,0,0,100"))
                + System.lineSeparator(), StandardCharsets.UTF_8);

        ImportResult imported = new ImportServiceImpl().importFile(file.toFile(), null);
        QueryServiceImpl service = new QueryServiceImpl();

        QueryCondition condition = new QueryCondition(
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 15),
                themeA,
                titleCase(personName),
                null,
                city);
        Page<GkgRecord> page = service.search(condition, 1, 10);
        EntityProfile person = service.getPersonProfile(titleCase(personName));
        EntityProfile org = service.getOrgProfile(titleCase(orgName));
        List<DateTone> trend = service.getThemeTrend(themeA, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16));
        List<DateTone> personTrend = service.getEntityTrend("PERSON", titleCase(personName), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16));
        List<DateTone> orgTrend = service.getEntityTrend("ORG", titleCase(orgName), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16));
        List<DateTone> themeTrend = service.getEntityTrend("THEME", themeA, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 16));
        List<String> personSuggestions = service.suggestPerson("query " + prefix.toUpperCase(), 10);
        List<String> orgSuggestions = service.suggestOrg("org " + prefix.toUpperCase(), 10);
        List<String> locationSuggestions = service.suggestLocation("city " + prefix.toUpperCase(), 10);

        assertEquals(3, imported.success());
        assertEquals(2, page.total());
        assertTrue(page.rows().stream().allMatch(r -> r.getRecordId().startsWith(prefix)));
        assertEquals(2, person.newsCount());
        assertEquals(3.0, person.avgTone(), 0.001);
        assertEquals(2, person.themeCount());
        assertFalse(person.topRelated().isEmpty());
        assertFalse(person.relatedOrganizations().isEmpty());
        assertTrue(person.relatedOrganizations().stream()
                .anyMatch(item -> item.name().equals(titleCase(orgName)) && item.count() > 0));
        assertEquals(2, org.newsCount());
        assertEquals(0.5, org.avgTone(), 0.001);
        assertEquals(3, org.themeCount());
        assertFalse(org.relatedPeople().isEmpty());
        assertEquals(titleCase(personName), org.relatedPeople().get(0).name());
        assertTrue(org.relatedPeople().get(0).count() > 0);
        assertEquals(1, trend.size());
        assertEquals(LocalDate.of(2024, 1, 15), trend.get(0).date());
        assertEquals(3.0, trend.get(0).avgTone(), 0.001);
        assertEquals(2, trend.get(0).recordCount());
        assertEquals(1, personTrend.size());
        assertEquals(3.0, personTrend.get(0).avgTone(), 0.001);
        assertEquals(2, orgTrend.size());
        assertEquals(1, themeTrend.size());
        assertTrue(personSuggestions.contains(titleCase(personName)));
        assertTrue(orgSuggestions.contains(titleCase(orgName)));
        assertTrue(locationSuggestions.contains(city));
    }

    private static String gkgLine(String recordId, String publishDate, String themes,
                                  String persons, String orgs, String locations, String tone) {
        String[] cols = new String[27];
        for (int i = 0; i < cols.length; i++) cols[i] = "";
        cols[0] = recordId;
        cols[1] = publishDate;
        cols[2] = "1";
        cols[3] = "example.com";
        cols[4] = "https://example.com/" + recordId;
        cols[7] = themes;
        cols[9] = locations;
        cols[11] = persons;
        cols[13] = orgs;
        cols[15] = tone;
        return String.join("\t", cols);
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split("\\s+")) {
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(' ');
        }
        return result.toString().trim();
    }
}
