package edu.gkg;

import com.formdev.flatlaf.FlatLightLaf;
import edu.gkg.common.ServiceRegistry;
import edu.gkg.common.Theme;
import edu.gkg.common.UiUtil;
import edu.gkg.mock.MockAnalysisService;
import edu.gkg.mock.MockDownloadService;
import edu.gkg.mock.MockImportService;
import edu.gkg.mock.MockQueryService;
import edu.gkg.service.AnalysisService;
import edu.gkg.service.DownloadService;
import edu.gkg.service.ImportService;
import edu.gkg.service.QueryService;
import edu.gkg.service.impl.ExportServiceImpl;
import edu.gkg.service.impl.GdeltDownloader;
import edu.gkg.view.MainFrame;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

/**
 * 程序入口。
 *
 * 启动模式：
 *   - 默认（或 -Dmock=true）：使用 mock/* 三件套，方便 A 独立开发
 *   - -Dmock=false：使用真实现（待 B 的 DAO + AnalysisServiceImpl 推上来后才能跑通）
 *
 * W1 联调点之后追加：DbHelper.init() 数据库初始化。
 */
public class App {

    public static void main(String[] args) {
        configureFlatLaf();
        UiUtil.applyGlobalFont(Theme.FONT_DEFAULT);

        ServiceRegistry services = buildServices();

        SwingUtilities.invokeLater(() -> new MainFrame(services).setVisible(true));
    }

    private static ServiceRegistry buildServices() {
        boolean useMock = !"false".equalsIgnoreCase(System.getProperty("mock", "true"));
        if (useMock) {
            System.out.println("[App] 使用 Mock 服务启动（-Dmock=false 可切换到真实现）");
            QueryService    q  = new MockQueryService();
            AnalysisService an = new MockAnalysisService();
            ImportService   im = new MockImportService();
            DownloadService dl = new MockDownloadService();
            return new ServiceRegistry(q, an, im, dl, new ExportServiceImpl());
        }
        // 真实现路径：等 B 推 DbHelper / DAO / AnalysisServiceImpl 后填
        // 当下 QueryServiceImpl 一调就抛 UnsupportedOperationException，提示明确
        return new ServiceRegistry(
                new edu.gkg.service.impl.QueryServiceImpl(),
                placeholderAnalysis(),
                placeholderImport(),
                new GdeltDownloader(),
                new ExportServiceImpl());
    }

    private static AnalysisService placeholderAnalysis() {
        return new AnalysisService() {
            @Override public void rebuildCooccurrence() { throw notReady(); }
            @Override public java.util.List<edu.gkg.common.SharedRecords.CooccurEdge> findAllEdges() { throw notReady(); }
            @Override public java.util.List<edu.gkg.common.SharedRecords.FocusNode> topFocusPersons(int topN) { throw notReady(); }
            @Override public java.util.List<edu.gkg.common.SharedRecords.ThemeHeat> themeHeatRank(
                    edu.gkg.common.SharedRecords.Granularity g, java.time.LocalDate f, java.time.LocalDate t, int n) { throw notReady(); }
            @Override public java.util.List<edu.gkg.common.SharedRecords.Cluster> clusterByKMeans(int k, int maxIter) { throw notReady(); }
            @Override public edu.gkg.common.SharedRecords.SentimentSeries sentimentTrend(
                    edu.gkg.common.SharedRecords.EntityRef ref, java.time.LocalDate f, java.time.LocalDate t) { throw notReady(); }
        };
    }

    private static ImportService placeholderImport() {
        return new ImportService() {
            @Override public edu.gkg.common.SharedRecords.ImportResult importFiles(
                    java.util.List<java.io.File> files,
                    java.util.function.Consumer<edu.gkg.common.SharedRecords.ProgressTick> sink) { throw notReady(); }
            @Override public int cleanInvalidData() { throw notReady(); }
        };
    }

    private static UnsupportedOperationException notReady() {
        return new UnsupportedOperationException("真实现尚未交付（等 B 推 DAO / Service Impl）");
    }

    private static void configureFlatLaf() {
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.arc",            Theme.RADIUS_MD);
        UIManager.put("Button.arc",               Theme.RADIUS_MD);
        UIManager.put("TextComponent.arc",        Theme.RADIUS_SM);
        UIManager.put("ProgressBar.arc",          Theme.RADIUS_LG);
        UIManager.put("ScrollBar.thumbArc",       Theme.RADIUS_SM);

        UIManager.put("Component.accentColor",    Theme.BRAND);
        UIManager.put("Button.default.background",Theme.BRAND);
        UIManager.put("Button.default.foreground",java.awt.Color.WHITE);

        UIManager.put("TabbedPane.tabHeight",     36);
        UIManager.put("TabbedPane.selectedBackground", Theme.BG_CARD);
        UIManager.put("TabbedPane.underlineColor",     Theme.BRAND);
        UIManager.put("TabbedPane.contentAreaColor",   Theme.BG_APP);

        UIManager.put("Panel.background",         Theme.BG_APP);
        UIManager.put("TableHeader.background",   Theme.BG_APP);
        UIManager.put("Table.gridColor",          Theme.BORDER_LIGHT);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines",   false);
        UIManager.put("Table.rowHeight",          28);

        UIManager.put("MenuBar.background",       Theme.BG_CARD);
        UIManager.put("MenuItem.selectionBackground", Theme.BRAND_SOFT);
        UIManager.put("MenuItem.selectionForeground", Theme.TEXT_PRIMARY);

        UIManager.put("defaultFont", new FontUIResource(Theme.FONT_DEFAULT));

        try { FlatLightLaf.setup(); } catch (Exception ignored) {}
    }
}
