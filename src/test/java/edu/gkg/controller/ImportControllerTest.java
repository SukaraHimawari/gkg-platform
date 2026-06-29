package edu.gkg.controller;

import edu.gkg.service.CleanResult;
import edu.gkg.service.ExportService;
import edu.gkg.service.ImportResult;
import edu.gkg.service.ImportService;
import edu.gkg.service.ProgressListener;
import edu.gkg.view.ImportPanel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportControllerTest {

    @Test
    void wiresImportActionsToUiEntryPoints() {
        ImportPanel panel = new ImportPanel();

        new ImportController(panel, new StubImportService(), new StubExportService());

        assertTrue(panel.runBtn.getActionListeners().length > 0);
        assertTrue(panel.drop.browseButton().getActionListeners().length > 0);
    }

    @Test
    void importPanelStartsWithChineseLabels() {
        ImportPanel panel = new ImportPanel();

        assertEquals("当前文件：—", panel.currentFileLabel().getText());
        assertEquals("已用时 00:00:00   预计剩余 —", panel.speedLabel().getText());
        assertTrue(panel.runBtn.getText().contains("开始导入"));
        assertTrue(panel.drop.browseButton().getText().contains("选择文件"));
        assertEquals("时间", panel.historyTable.getColumnName(0));
        assertEquals("数据源", panel.historyTable.getColumnName(1));
    }

    @Test
    void progressListenerUpdatesImportProgressUi() throws Exception {
        ImportPanel panel = new ImportPanel();
        CountDownLatch progressPublished = new CountDownLatch(1);
        CountDownLatch mayFinish = new CountDownLatch(1);
        ImportController controller = new ImportController(
                panel,
                new ProgressImportService(progressPublished, mayFinish),
                new StubExportService());

        File file = File.createTempFile("gkg-progress", ".csv");
        controller.startImport(file);

        assertTrue(progressPublished.await(3, TimeUnit.SECONDS));
        waitForImportProgress(panel);
        assertEquals(65, panel.progressBar().getValue());
        assertTrue(panel.currentFileLabel().getText().contains("已处理 650 行"));
        assertTrue(panel.speedLabel().getText().contains("已用时"));
        assertTrue(!panel.runBtn.isEnabled());

        controller.cancelCurrentTaskForTests();
        mayFinish.countDown();
        file.delete();
    }

    @Test
    void importCompletionNotifiesDataChangedListener() throws Exception {
        ImportPanel panel = new ImportPanel();
        CountDownLatch dataChanged = new CountDownLatch(1);
        ImportController controller = new ImportController(
                panel,
                new StubImportService(),
                new StubExportService(),
                dataChanged::countDown);

        File file = File.createTempFile("gkg-refresh", ".csv");
        controller.startImport(file);

        assertTrue(dataChanged.await(3, TimeUnit.SECONDS));
        file.delete();
    }

    @Test
    void clearCompletionNotifiesDataChangedListener() throws Exception {
        ImportPanel panel = new ImportPanel();
        CountDownLatch dataChanged = new CountDownLatch(1);
        ImportController controller = new ImportController(
                panel,
                new StubImportService(),
                new StubExportService(),
                dataChanged::countDown);

        controller.applyClearResultForTests(new CleanResult(0, 0, 10));

        assertTrue(dataChanged.await(3, TimeUnit.SECONDS));
        assertEquals(0, panel.progressBar().getValue());
    }

    private static void waitForImportProgress(ImportPanel panel) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
            });
            if (panel.progressBar().getValue() == 65) return;
            Thread.sleep(25);
        }
    }

    private static class StubImportService implements ImportService {
        @Override
        public ImportResult importFile(File csvFile, ProgressListener listener) {
            return new ImportResult(0, 0, 0, 0);
        }

        @Override
        public ImportResult importDirectory(File dir, ProgressListener listener) {
            return new ImportResult(0, 0, 0, 0);
        }

        @Override
        public CleanResult cleanInvalidData() {
            return new CleanResult(0, 0, 0);
        }

        @Override
        public CleanResult clearDatabase() {
            return new CleanResult(0, 0, 0);
        }
    }

    private static class ProgressImportService extends StubImportService {
        private final CountDownLatch progressPublished;
        private final CountDownLatch mayFinish;

        ProgressImportService(CountDownLatch progressPublished, CountDownLatch mayFinish) {
            this.progressPublished = progressPublished;
            this.mayFinish = mayFinish;
        }

        @Override
        public ImportResult importFile(File csvFile, ProgressListener listener) {
            listener.onProgress(65, "已处理 650 行");
            progressPublished.countDown();
            try {
                mayFinish.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new ImportResult(650, 640, 10, 10);
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
