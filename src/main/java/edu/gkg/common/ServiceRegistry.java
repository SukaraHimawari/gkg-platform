package edu.gkg.common;

import edu.gkg.service.AnalysisService;
import edu.gkg.service.ExportService;
import edu.gkg.service.ImportService;
import edu.gkg.service.QueryService;

/**
 * 应用启动时统一构造、传递的服务壳子。
 * App.java 决定 Mock 还是真实现，MainFrame 拿到后下传给各 Controller。
 */
public record ServiceRegistry(
        QueryService     query,
        AnalysisService  analysis,
        ImportService    importer,
        ExportService    exporter) {
}
