package edu.gkg.controller;

import edu.gkg.mock.MockDownloadService;
import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ProgressTick;
import edu.gkg.service.DownloadService;
import edu.gkg.service.impl.GdeltDownloader;
import edu.gkg.view.ImportPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadControllerTest {

    @AfterEach
    void clearMockFlag() {
        System.clearProperty("mock");
    }

    @Test
    void defaultDownloaderUsesRealGdeltDownloader() {
        DownloadService downloader = DownloadController.chooseDownloader();

        assertInstanceOf(GdeltDownloader.class, downloader);
    }

    @Test
    void explicitMockFlagUsesMockDownloader() {
        System.setProperty("mock", "true");

        DownloadService downloader = DownloadController.chooseDownloader();

        assertInstanceOf(MockDownloadService.class, downloader);
    }

    @Test
    void progressTicksUpdateDownloadProgressUi() throws Exception {
        ImportPanel panel = new ImportPanel();
        CountDownLatch progressPublished = new CountDownLatch(1);
        CountDownLatch mayFinish = new CountDownLatch(1);
        DownloadService downloader = (date, destDir, sink) -> {
            sink.accept(new ProgressTick(42, "20240101000000.gkg.csv.zip", "下载中 42%"));
            progressPublished.countDown();
            try {
                mayFinish.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new DownloadResult(1, 1, 0, 10);
        };
        DownloadController controller = new DownloadController(panel, downloader, null);

        panel.downloadBtn.doClick();

        assertTrue(progressPublished.await(3, TimeUnit.SECONDS));
        flushEdt();
        assertEquals(42, panel.progressBar().getValue());
        assertEquals("20240101000000.gkg.csv.zip", panel.currentFileLabel().getText());
        assertEquals("下载中 42%", panel.speedLabel().getText());
        assertTrue(!panel.downloadBtn.isEnabled());

        controller.cancelCurrentTaskForTests();
        mayFinish.countDown();
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
