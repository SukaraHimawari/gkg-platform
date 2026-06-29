package edu.gkg.controller;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.EntityProfile;
import edu.gkg.common.SharedRecords.Page;
import edu.gkg.common.SharedRecords.QueryCondition;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.ExportService;
import edu.gkg.service.QueryService;
import edu.gkg.view.QueryPanel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryControllerTest {

    @Test
    void themeTrackButtonQueriesThemeTrend() throws Exception {
        QueryPanel panel = new QueryPanel();
        CountDownLatch trendRequested = new CountDownLatch(1);
        AtomicReference<String> requestedTheme = new AtomicReference<>();
        AtomicReference<LocalDate> requestedFrom = new AtomicReference<>();
        AtomicReference<LocalDate> requestedTo = new AtomicReference<>();
        new QueryController(panel, new ThemeTrendQueryService(
                trendRequested, requestedTheme, requestedFrom, requestedTo), new StubExportService());

        panel.themeTrackPanel.themeCodeField.setText("tax_world");
        panel.themeTrackPanel.fromDateField.setText("2024-01-01");
        panel.themeTrackPanel.toDateField.setText("2024-01-31");
        panel.themeTrackPanel.plotButton.doClick();

        assertTrue(trendRequested.await(3, TimeUnit.SECONDS));
        waitForSwing();
        assertEquals("TAX_WORLD", requestedTheme.get());
        assertEquals(LocalDate.of(2024, 1, 1), requestedFrom.get());
        assertEquals(LocalDate.of(2024, 1, 31), requestedTo.get());
    }

    private static void waitForSwing() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
        });
    }

    private static class ThemeTrendQueryService implements QueryService {
        private final CountDownLatch trendRequested;
        private final AtomicReference<String> requestedTheme;
        private final AtomicReference<LocalDate> requestedFrom;
        private final AtomicReference<LocalDate> requestedTo;

        ThemeTrendQueryService(CountDownLatch trendRequested,
                               AtomicReference<String> requestedTheme,
                               AtomicReference<LocalDate> requestedFrom,
                               AtomicReference<LocalDate> requestedTo) {
            this.trendRequested = trendRequested;
            this.requestedTheme = requestedTheme;
            this.requestedFrom = requestedFrom;
            this.requestedTo = requestedTo;
        }

        @Override
        public Page<GkgRecord> search(QueryCondition cond, int page, int pageSize) {
            return new Page<>(List.of(), page, pageSize, 0);
        }

        @Override
        public EntityProfile getPersonProfile(String name) {
            return null;
        }

        @Override
        public EntityProfile getOrgProfile(String name) {
            return null;
        }

        @Override
        public List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to) {
            requestedTheme.set(themeCode);
            requestedFrom.set(from);
            requestedTo.set(to);
            trendRequested.countDown();
            return List.of(new DateTone(LocalDate.of(2024, 1, 15), 1.2, 3));
        }

        @Override
        public List<DateTone> getEntityTrend(String entityType, String name, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<String> suggestPerson(String prefix, int limit) {
            return List.of();
        }

        @Override
        public List<String> suggestOrg(String prefix, int limit) {
            return List.of();
        }

        @Override
        public List<String> suggestLocation(String prefix, int limit) {
            return List.of();
        }

        @Override
        public long countAll() {
            return 0;
        }
    }

    private static class StubExportService implements ExportService {
        @Override
        public void exportCsv(List<?> rows, File target) {
        }

        @Override
        public void exportExcel(List<?> rows, File target) {
        }

        @Override
        public void exportJson(Object data, File target) {
        }
    }
}
