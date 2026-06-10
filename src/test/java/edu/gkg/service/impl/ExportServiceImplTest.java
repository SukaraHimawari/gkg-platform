package edu.gkg.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.gkg.common.SharedRecords.SearchRow;
import edu.gkg.service.ExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceImplTest {

    private final ExportService svc = new ExportServiceImpl();

    private List<SearchRow> sample() {
        return List.of(
                new SearchRow("R1", "2024-01-15 10:00", "Reuters", 2.3, "https://e/1"),
                new SearchRow("R2", "2024-01-15 11:00", "BBC",     -1.5, "https://e/2"),
                new SearchRow("R3", "2024-01-15 12:00", "CNN",      0.0, "https://e/3"));
    }

    @Test
    void csvWritesUtf8BomAndHeader(@TempDir Path tmp) throws Exception {
        File out = tmp.resolve("out.csv").toFile();
        svc.exportCsv(sample(), out);

        byte[] all = Files.readAllBytes(out.toPath());
        // UTF-8 BOM
        assertEquals((byte) 0xEF, all[0]);
        assertEquals((byte) 0xBB, all[1]);
        assertEquals((byte) 0xBF, all[2]);

        String text = new String(all, 3, all.length - 3, StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        assertTrue(lines[0].contains("recordId") && lines[0].contains("source") && lines[0].contains("tone"),
                "表头应包含 record 字段名，实际：" + lines[0]);
        assertEquals(4, lines.length, "应有 1 行表头 + 3 行数据 = 4 行（实际：" + lines.length + ")");
        assertTrue(text.contains("Reuters") && text.contains("BBC"));
    }

    @Test
    void csvEmptyListStillWritesBom(@TempDir Path tmp) throws Exception {
        File out = tmp.resolve("empty.csv").toFile();
        svc.exportCsv(List.of(), out);
        byte[] all = Files.readAllBytes(out.toPath());
        assertEquals(3, all.length);
        assertEquals((byte) 0xEF, all[0]);
    }

    @Test
    void excelWritesHeaderRowAndDataRows(@TempDir Path tmp) throws Exception {
        File out = tmp.resolve("out.xlsx").toFile();
        svc.exportExcel(sample(), out);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(out))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertNotNull(header);
            boolean foundRecordId = false, foundSource = false;
            for (int i = 0; i < header.getLastCellNum(); i++) {
                String v = header.getCell(i).getStringCellValue();
                if ("recordId".equals(v)) foundRecordId = true;
                if ("source".equals(v))   foundSource = true;
            }
            assertTrue(foundRecordId && foundSource, "Excel 表头应包含 recordId 和 source");
            assertEquals(3, sheet.getLastRowNum(), "应有第 0~3 行（含表头共 4 行）");
        }
    }

    @Test
    void jsonRoundTripsRecords(@TempDir Path tmp) throws Exception {
        File out = tmp.resolve("out.json").toFile();
        svc.exportJson(sample(), out);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        List<Map<String, Object>> back = mapper.readValue(out, mapper.getTypeFactory()
                .constructCollectionType(List.class, Map.class));
        assertEquals(3, back.size());
        assertEquals("R1", back.get(0).get("recordId"));
        assertEquals("Reuters", back.get(0).get("source"));
        assertEquals(2.3, ((Number) back.get(0).get("tone")).doubleValue(), 1e-9);
    }

    @Test
    void csvAutoCreatesParentDir(@TempDir Path tmp) throws Exception {
        File deep = tmp.resolve("a/b/c/out.csv").toFile();
        svc.exportCsv(sample(), deep);
        assertTrue(deep.exists());
        assertTrue(deep.length() > 3); // BOM + 内容
    }
}
