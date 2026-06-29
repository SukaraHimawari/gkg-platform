package edu.gkg.service.parser;

import edu.gkg.model.*;

import java.util.*;

public class GkgLineParser {

    public ParseResult parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("行为空");
        }

        String[] parts = line.split("\t", -1);
        if (parts.length < 27) {
            throw new IllegalArgumentException("字段数不足，期望至少27列，实际 " + parts.length);
        }

        GkgRecord record = parseRecord(parts);

        // 列7: V2Themes
        List<Theme> themes = new ArrayList<>();
        if (parts.length > 7 && parts[7] != null && !parts[7].trim().isEmpty()) {
            String themesStr = parts[7].trim();
            for (String code : themesStr.split(";")) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    themes.add(new Theme(trimmed));
                }
            }
        }

        // 列8: V2ThemesWithOffsets（兜底）
        if (themes.isEmpty() && parts.length > 8 && parts[8] != null && !parts[8].trim().isEmpty()) {
            String themesStr = parts[8].trim();
            for (String item : themesStr.split(";")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    String[] kv = trimmed.split(",");
                    if (kv.length > 0 && !kv[0].trim().isEmpty()) {
                        themes.add(new Theme(kv[0].trim()));
                    }
                }
            }
        }

        // 从文档ID提取关键词兜底
        if (themes.isEmpty() && record.getDocumentId() != null) {
            String docId = record.getDocumentId();
            String[] segments = docId.split("/");
            for (String seg : segments) {
                if (seg.length() > 5 && !seg.matches(".*\\d.*") && !seg.contains(".")) {
                    themes.add(new Theme(seg.substring(0, Math.min(30, seg.length()))));
                    break;
                }
            }
        }

        // 列9: V2Locations, 列11: V2Persons, 列13: V2Organizations, 列22: V2Quotations
        List<Location> locations = parseLocations(parts[9]);
        List<PersonOffset> personOffsets = parsePersons(parts[11]);
        List<OrgOffset> orgOffsets = parseOrganizations(parts[13]);
        List<Quote> quotes = parseQuotes(parts.length > 22 ? parts[22] : "", record.getRecordId());

        List<Person> persons = personOffsets.stream()
                .map(po -> new Person(po.name()))
                .distinct()
                .toList();
        List<Organization> organizations = orgOffsets.stream()
                .map(oo -> new Organization(oo.name()))
                .distinct()
                .toList();

        return new ParseResult(
                record,
                themes,
                locations,
                persons,
                organizations,
                quotes,
                personOffsets,
                orgOffsets
        );
    }

    private GkgRecord parseRecord(String[] parts) {
        GkgRecord record = new GkgRecord();
        // 列0: GKGRECORDID, 列1: DATE
        record.setRecordId(parts[0].trim());
        record.setPublishDate(parts[1].trim());

        // 列2: SourceCollectionIdentifier
        if (parts.length > 2 && !parts[2].trim().isEmpty()) {
            try {
                record.setSourceCollection(Integer.parseInt(parts[2].trim()));
            } catch (NumberFormatException ignored) {}
        }

        // 列3: SourceCommonName, 列4: DocumentIdentifier
        record.setSourceCommonName(parts.length > 3 && !parts[3].trim().isEmpty() ? parts[3].trim() : null);
        record.setDocumentId(parts.length > 4 && !parts[4].trim().isEmpty() ? parts[4].trim() : null);

        // 列15: V2Tone
        parseTone(parts.length > 15 ? parts[15] : "", record);

        return record;
    }

    private void parseTone(String toneField, GkgRecord record) {
        if (toneField == null || toneField.trim().isEmpty()) return;
        String[] parts = toneField.split(",");
        if (parts.length >= 4) {
            try {
                record.setTone(Double.parseDouble(parts[0].trim()));
                record.setPositiveScore(Double.parseDouble(parts[1].trim()));
                record.setNegativeScore(Double.parseDouble(parts[2].trim()));
                record.setPolarity(Double.parseDouble(parts[3].trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private List<Location> parseLocations(String locationsField) {
        List<Location> result = new ArrayList<>();
        if (locationsField == null || locationsField.trim().isEmpty()) return result;
        for (String loc : locationsField.split(";")) {
            String trimmed = loc.trim();
            if (!trimmed.isEmpty()) {
                Location location = new Location();
                String[] parts = trimmed.split("#");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    // GKG V2Locations 格式: type#name#countryCode#...
                    // 跳过数字前缀
                    String name = parts.length > 1 ? parts[1] : parts[0];
                    location.setName(name.trim());
                    if (parts.length > 2) {
                        location.setCountryCode(parts[2].trim());
                    }
                }
                result.add(location);
            }
        }
        return result;
    }

    private List<PersonOffset> parsePersons(String personsField) {
        List<PersonOffset> result = new ArrayList<>();
        if (personsField == null || personsField.trim().isEmpty()) return result;
        for (String name : personsField.split(";")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                result.add(new PersonOffset(formatName(trimmed), 0));
            }
        }
        return result;
    }

    private List<OrgOffset> parseOrganizations(String orgsField) {
        List<OrgOffset> result = new ArrayList<>();
        if (orgsField == null || orgsField.trim().isEmpty()) return result;
        for (String name : orgsField.split(";")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                result.add(new OrgOffset(formatName(trimmed), 0));
            }
        }
        return result;
    }

    private String formatName(String name) {
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (word.length() == 2 && word.equalsIgnoreCase("jr")) {
                    sb.append("Jr ");
                } else if (word.length() == 2 && word.equalsIgnoreCase("sr")) {
                    sb.append("Sr ");
                } else if (word.length() == 3 && word.equalsIgnoreCase("iii")) {
                    sb.append("III ");
                } else {
                    sb.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
            }
        }
        return sb.toString().trim();
    }

    private List<Quote> parseQuotes(String quotesField, String recordId) {
        List<Quote> result = new ArrayList<>();
        if (quotesField == null || quotesField.trim().isEmpty()) return result;
        for (String quoteStr : quotesField.split(";")) {
            String trimmed = quoteStr.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\|", -1);
            if (parts.length >= 2) {
                Quote quote = new Quote();
                quote.setRecordId(recordId);
                quote.setVerb(parts[0].trim().isEmpty() ? null : parts[0].trim());
                quote.setContent(parts[1].trim().isEmpty() ? null : parts[1].trim());
                if (parts.length > 2) {
                    try {
                        quote.setCharOffset(Integer.parseInt(parts[2].trim()));
                    } catch (NumberFormatException ignored) {}
                }
                if (parts.length > 3) {
                    try {
                        quote.setLength(Integer.parseInt(parts[3].trim()));
                    } catch (NumberFormatException ignored) {}
                }
                result.add(quote);
            }
        }
        return result;
    }

    public record ParseResult(
            GkgRecord record,
            List<Theme> themes,
            List<Location> locations,
            List<Person> persons,
            List<Organization> organizations,
            List<Quote> quotes,
            List<PersonOffset> personOffsets,
            List<OrgOffset> orgOffsets
    ) {}

    public record PersonOffset(String name, int offset) {}
    public record OrgOffset(String name, int offset) {}
}
