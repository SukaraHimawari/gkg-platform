package edu.gkg.controller;

import edu.gkg.algorithm.ThemeRanker;
import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.EntityProfile;
import edu.gkg.common.SharedRecords.Page;
import edu.gkg.common.SharedRecords.QueryCondition;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.AnalysisService;
import edu.gkg.service.ProgressListener;
import edu.gkg.service.QueryService;
import edu.gkg.view.AnalysisPanel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisControllerTest {

    @Test
    void kMeansRendersClusterCardsInAnalysisPanel() throws Exception {
        AnalysisPanel panel = new AnalysisPanel();
        CountDownLatch clusterRequested = new CountDownLatch(1);
        AnalysisController controller = new AnalysisController(
                panel,
                new StubQueryService(),
                new ClusterAnalysisService(clusterRequested));

        panel.runKMeansButton.doClick();

        assertTrue(clusterRequested.await(3, TimeUnit.SECONDS));
        waitForStatus(panel, "K-Means 聚类完成");
        assertEquals(2, panel.clusters.getClusterCardCount());
        assertEquals(100, panel.progressBar.getValue());

        controller.cancelCurrentTaskForTests();
    }

    @Test
    void themeHeatFallsBackToAllDataWhenSelectedRecentRangeIsEmpty() throws Exception {
        AnalysisPanel panel = new AnalysisPanel();
        CountDownLatch allDataRequested = new CountDownLatch(1);
        AtomicReference<String> requestedFrom = new AtomicReference<>();
        AtomicReference<String> requestedTo = new AtomicReference<>();
        AnalysisController controller = new AnalysisController(
                panel,
                new StubQueryService(),
                new HeatFallbackAnalysisService(allDataRequested, requestedFrom, requestedTo));

        panel.runHeatButton.doClick();

        assertTrue(allDataRequested.await(3, TimeUnit.SECONDS));
        waitForStatus(panel, "主题热度分析完成");
        assertEquals("1900-01-01", requestedFrom.get());
        assertFalse(requestedTo.get().isBlank());
        assertEquals(100, panel.progressBar.getValue());

        controller.cancelCurrentTaskForTests();
    }

    @Test
    void rebuildCooccurrenceProgressUpdatesAnalysisStatusUi() throws Exception {
        AnalysisPanel panel = new AnalysisPanel();
        CountDownLatch progressPublished = new CountDownLatch(1);
        CountDownLatch mayFinish = new CountDownLatch(1);
        AnalysisController controller = new AnalysisController(
                panel,
                new StubQueryService(),
                new ProgressAnalysisService(progressPublished, mayFinish));

        panel.rebuildButton.doClick();

        assertTrue(progressPublished.await(3, TimeUnit.SECONDS));
        waitForAnalysisProgress(panel);
        assertEquals(55, panel.progressBar.getValue());
        assertEquals("已处理 500/1000 条新闻", panel.statusLabel.getText());
        assertFalse(panel.rebuildButton.isEnabled());

        controller.cancelCurrentTaskForTests();
        mayFinish.countDown();
    }

    private static void waitForAnalysisProgress(AnalysisPanel panel) throws Exception {
        waitForStatus(panel, "已处理 500/1000 条新闻");
    }

    private static void waitForStatus(AnalysisPanel panel, String expectedStatus) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
            });
            if (expectedStatus.equals(panel.statusLabel.getText())) return;
            Thread.sleep(25);
        }
    }

    private static class HeatFallbackAnalysisService extends ProgressAnalysisService {
        private final CountDownLatch allDataRequested;
        private final AtomicReference<String> requestedFrom;
        private final AtomicReference<String> requestedTo;

        HeatFallbackAnalysisService(CountDownLatch allDataRequested,
                                    AtomicReference<String> requestedFrom,
                                    AtomicReference<String> requestedTo) {
            super(new CountDownLatch(0), new CountDownLatch(0));
            this.allDataRequested = allDataRequested;
            this.requestedFrom = requestedFrom;
            this.requestedTo = requestedTo;
        }

        @Override
        public List<ThemeRanker.ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) {
            requestedFrom.set(from);
            requestedTo.set(to);
            if ("1900-01-01".equals(from)) {
                allDataRequested.countDown();
                return List.of(new ThemeRanker.ThemeHeat("TEST_THEME", "2024-01-01", 2));
            }
            return List.of();
        }
    }

    private static class ClusterAnalysisService extends ProgressAnalysisService {
        private final CountDownLatch clusterRequested;

        ClusterAnalysisService(CountDownLatch clusterRequested) {
            super(new CountDownLatch(0), new CountDownLatch(0));
            this.clusterRequested = clusterRequested;
        }

        @Override
        public List<String> clusterByKMeans(int k, int maxIter) {
            clusterRequested.countDown();
            return List.of(
                    "Cluster 1 (12 records)" + System.lineSeparator() + "Top themes: TAX_WORLD, MEDIA_SOCIAL",
                    "Cluster 2 (8 records)" + System.lineSeparator() + "Top themes: HEALTH_PANDEMIC, MEDICAL"
            );
        }
    }

    private static class ProgressAnalysisService implements AnalysisService {
        private final CountDownLatch progressPublished;
        private final CountDownLatch mayFinish;

        ProgressAnalysisService(CountDownLatch progressPublished, CountDownLatch mayFinish) {
            this.progressPublished = progressPublished;
            this.mayFinish = mayFinish;
        }

        @Override
        public void rebuildCooccurrence() {
        }

        @Override
        public void rebuildCooccurrence(ProgressListener listener) {
            listener.onProgress(55, "已处理 500/1000 条新闻");
            progressPublished.countDown();
            try {
                mayFinish.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public List<String> getTopFocusPersons(int topN) {
            return List.of();
        }

        @Override
        public List<ThemeRanker.ThemeHeat> getThemeHeat(String granularity, String from, String to, int topN) {
            return List.of();
        }

        @Override
        public List<ThemeRanker.ThemeHeat> getAllThemeHeat() {
            return List.of();
        }

        @Override
        public List<String> clusterByKMeans(int k, int maxIter) {
            return List.of();
        }

        @Override
        public List<String> sentimentTrend(String entityRef, String from, String to) {
            return List.of();
        }
    }

    private static class StubQueryService implements QueryService {
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
            return List.of();
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
}
