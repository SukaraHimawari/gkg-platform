package edu.gkg.service;

import java.io.File;
import java.util.List;

/**
 * F-04 结果导出。
 * 行类型推荐用 record（自动反射读取字段名作为表头），也支持 Map/POJO。
 */
public interface ExportService {

    void exportCsv(List<?> rows, File target) throws Exception;

    void exportExcel(List<?> rows, File target) throws Exception;

    /** data 任意对象（List / Map / record / POJO 均可），Jackson 美化输出。 */
    void exportJson(Object data, File target) throws Exception;
}
