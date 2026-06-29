package edu.gkg.service.impl;

import edu.gkg.model.GkgRecord;
import edu.gkg.model.Location;
import edu.gkg.model.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class GkgLineParser {
    ParseResult parseLine(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("empty line");
        }
        String[] columns = line.split("\t", -1);
        if (columns.length < 16) {
            throw new IllegalArgumentException("expected at least 16 GKG columns");
        }

        GkgRecord record = new GkgRecord();
        record.setRecordId(required(value(columns, 0), "record_id"));
        record.setPublishDate(required(value(columns, 1), "publish_date"));
        record.setSourceCollection(parseInteger(value(columns, 2)));
        record.setSourceCommonName(blankToNull(value(columns, 3)));
        record.setDocumentId(blankToNull(value(columns, 4)));
        parseTone(value(columns, 15), record);

        return new ParseResult(
                record,
                parseThemes(value(columns, 8), value(columns, 7)),
                parseLocations(value(columns, 10), value(columns, 9)),
                parsePersons(value(columns, 12), value(columns, 11)),
                parseOrganizations(value(columns, 14), value(columns, 13)));
    }

    private void parseTone(String toneField, GkgRecord record) {
        if (toneField == null || toneField.isBlank()) return;
        String[] parts = toneField.split(",", -1);
        if (parts.length > 0) record.setTone(parseDouble(parts[0]));
        if (parts.length > 1) record.setPositiveScore(parseDouble(parts[1]));
        if (parts.length > 2) record.setNegativeScore(parseDouble(parts[2]));
        if (parts.length > 3) record.setPolarity(parseDouble(parts[3]));
        if (parts.length > 6) record.setWordCount(parseInteger(parts[6]));
    }

    private List<ThemeOffset> parseThemes(String withOffsets, String plain) {
        List<ThemeOffset> result = new ArrayList<>();
        if (withOffsets != null && !withOffsets.isBlank()) {
            for (String item : withOffsets.split(";")) {
                String[] parts = item.split(",", -1);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    result.add(new ThemeOffset(new Theme(parts[0].trim()), parseOffset(parts, 1)));
                }
            }
        }
        if (!result.isEmpty() || plain == null || plain.isBlank()) return result;
        for (String item : plain.split(";")) {
            if (!item.isBlank()) result.add(new ThemeOffset(new Theme(item.trim()), 0));
        }
        return result;
    }

    private List<LocationOffset> parseLocations(String withOffsets, String plain) {
        String field = withOffsets != null && !withOffsets.isBlank() ? withOffsets : plain;
        List<LocationOffset> result = new ArrayList<>();
        if (field == null || field.isBlank()) return result;
        for (String item : field.split(";")) {
            String[] parts = item.split("#", -1);
            if (parts.length < 2 || parts[1].isBlank()) continue;
            Location location = new Location();
            location.setName(parts[1].trim());
            if (parts.length > 2) location.setCountryCode(blankToNull(parts[2]));
            if (parts.length > 5) location.setLat(parseDouble(parts[5]));
            if (parts.length > 6) location.setLng(parseDouble(parts[6]));
            result.add(new LocationOffset(location, parts.length > 7 ? parseOffset(parts, 7) : 0));
        }
        return result;
    }

    private List<PersonOffset> parsePersons(String withOffsets, String plain) {
        List<PersonOffset> result = new ArrayList<>();
        if (withOffsets != null && !withOffsets.isBlank()) {
            for (String item : withOffsets.split(";")) {
                String[] parts = item.split(",", -1);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    result.add(new PersonOffset(titleCase(parts[0].trim()), parseOffset(parts, 1)));
                }
            }
        }
        if (!result.isEmpty() || plain == null || plain.isBlank()) return result;
        for (String item : plain.split(";")) {
            if (!item.isBlank()) result.add(new PersonOffset(titleCase(item.trim()), 0));
        }
        return result;
    }

    private List<OrgOffset> parseOrganizations(String withOffsets, String plain) {
        List<OrgOffset> result = new ArrayList<>();
        if (withOffsets != null && !withOffsets.isBlank()) {
            for (String item : withOffsets.split(";")) {
                String[] parts = item.split(",", -1);
                if (parts.length > 0 && !parts[0].isBlank()) {
                    result.add(new OrgOffset(titleCase(parts[0].trim()), parseOffset(parts, 1)));
                }
            }
        }
        if (!result.isEmpty() || plain == null || plain.isBlank()) return result;
        for (String item : plain.split(";")) {
            if (!item.isBlank()) result.add(new OrgOffset(titleCase(item.trim()), 0));
        }
        return result;
    }

    private static String value(String[] columns, int index) {
        return index < columns.length ? columns[index] : "";
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int parseOffset(String[] parts, int index) {
        Integer parsed = index < parts.length ? parseInteger(parts[index]) : null;
        return parsed == null ? 0 : parsed;
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            String lower = word.toLowerCase(Locale.ROOT);
            if (lower.equals("iii")) result.append("III");
            else result.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            result.append(' ');
        }
        return result.toString().trim();
    }

    record ParseResult(GkgRecord record, List<ThemeOffset> themes,
                       List<LocationOffset> locations, List<PersonOffset> persons,
                       List<OrgOffset> organizations) {}
    record ThemeOffset(Theme theme, int offset) {}
    record LocationOffset(Location location, int offset) {}
    record PersonOffset(String name, int offset) {}
    record OrgOffset(String name, int offset) {}
}
