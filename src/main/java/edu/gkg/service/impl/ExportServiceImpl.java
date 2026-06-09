package edu.gkg.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import edu.gkg.service.ExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExportServiceImpl implements ExportService {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void exportCsv(List<?> rows, File target) throws Exception {
        ensureParent(target);
        if (rows.isEmpty()) { writeEmpty(target); return; }
        Schema schema = Schema.of(rows.get(0));
        try (OutputStream os = new FileOutputStream(target);
             CSVWriter w = new CSVWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            os.write(UTF8_BOM);
            w.writeNext(schema.headers.toArray(new String[0]));
            for (Object row : rows) {
                Object[] vals = schema.read(row);
                String[] strs = new String[vals.length];
                for (int i = 0; i < vals.length; i++) strs[i] = vals[i] == null ? "" : vals[i].toString();
                w.writeNext(strs);
            }
        }
    }

    @Override
    public void exportExcel(List<?> rows, File target) throws Exception {
        ensureParent(target);
        try (Workbook wb = new XSSFWorkbook(); OutputStream os = new FileOutputStream(target)) {
            Sheet sheet = wb.createSheet("Result");
            if (rows.isEmpty()) { wb.write(os); return; }
            Schema schema = Schema.of(rows.get(0));
            Row header = sheet.createRow(0);
            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); bold.setFont(f);
            for (int i = 0; i < schema.headers.size(); i++) {
                Cell c = header.createCell(i);
                c.setCellValue(schema.headers.get(i));
                c.setCellStyle(bold);
            }
            int r = 1;
            for (Object row : rows) {
                Row excelRow = sheet.createRow(r++);
                Object[] vals = schema.read(row);
                for (int i = 0; i < vals.length; i++) writeCell(excelRow.createCell(i), vals[i]);
            }
            for (int i = 0; i < schema.headers.size(); i++) sheet.autoSizeColumn(i);
            wb.write(os);
        }
    }

    @Override
    public void exportJson(Object data, File target) throws Exception {
        ensureParent(target);
        json.writeValue(target, data);
    }

    private static void writeCell(Cell cell, Object v) {
        if (v == null)                  cell.setBlank();
        else if (v instanceof Number n) cell.setCellValue(n.doubleValue());
        else if (v instanceof Boolean b)cell.setCellValue(b);
        else                            cell.setCellValue(v.toString());
    }

    private static void writeEmpty(File target) throws IOException {
        try (OutputStream os = new FileOutputStream(target)) { os.write(UTF8_BOM); }
    }

    private static void ensureParent(File target) {
        File p = target.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
    }

    /** 反射拿到字段名 + getter，支持 record 和带 getter 的 POJO；不支持的就当 toString。 */
    private record Schema(List<String> headers, List<Method> getters) {
        static Schema of(Object sample) {
            Class<?> cls = sample.getClass();
            if (cls.isRecord()) {
                List<String> h = new ArrayList<>();
                List<Method> g = new ArrayList<>();
                for (RecordComponent rc : cls.getRecordComponents()) {
                    h.add(rc.getName());
                    g.add(rc.getAccessor());
                }
                return new Schema(h, g);
            }
            List<String> h = new ArrayList<>();
            List<Method> g = new ArrayList<>();
            for (Method m : cls.getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String n = m.getName();
                if ((n.startsWith("get") && n.length() > 3 && !n.equals("getClass"))
                        || (n.startsWith("is") && n.length() > 2)) {
                    h.add(decapitalize(n.startsWith("get") ? n.substring(3) : n.substring(2)));
                    g.add(m);
                }
            }
            return new Schema(h, g);
        }

        Object[] read(Object row) {
            Object[] out = new Object[getters.size()];
            for (int i = 0; i < out.length; i++) {
                try { out[i] = getters.get(i).invoke(row); }
                catch (Exception e) { out[i] = null; }
            }
            return out;
        }

        private static String decapitalize(String s) {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }
}
