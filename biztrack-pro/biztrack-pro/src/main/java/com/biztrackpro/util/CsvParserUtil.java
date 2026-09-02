package com.biztrackpro.util;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

/**
 * Thin wrapper over Apache Commons CSV. Reads an upload into a header list plus a
 * list of row maps, tolerating BOMs, duplicate/blank headers, and messy quoting.
 * All column access is case-insensitive and null-safe.
 */
public final class CsvParserUtil {

    private CsvParserUtil() {
    }

    public static final class ParsedCsv {
        private final List<String> headers;
        private final List<Map<String, String>> rows;

        public ParsedCsv(List<String> headers, List<Map<String, String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Map<String, String>> getRows() {
            return rows;
        }
    }

    public static ParsedCsv parse(InputStream in) {
        try {
            byte[] bytes = in.readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (!content.isEmpty() && content.charAt(0) == '﻿') {
                content = content.substring(1); // strip UTF-8 BOM
            }

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .setAllowMissingColumnNames(true)
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                    .build();

            try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
                List<String> headers = parser.getHeaderNames();
                List<Map<String, String>> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String header : headers) {
                        if (header == null || header.isEmpty()) {
                            continue;
                        }
                        String value = record.isSet(header) ? record.get(header) : "";
                        row.put(header, value == null ? "" : value.trim());
                    }
                    rows.add(row);
                }
                return new ParsedCsv(headers, rows);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse CSV file: " + e.getMessage(), e);
        }
    }

    /** Case-insensitive lookup of an exact column name. */
    public static String get(Map<String, String> row, String columnName) {
        if (row == null || columnName == null) {
            return "";
        }
        String direct = row.get(columnName);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(columnName)) {
                return e.getValue() == null ? "" : e.getValue();
            }
        }
        return "";
    }

    /** First non-empty value among candidate column names. */
    public static String getAny(Map<String, String> row, String... candidates) {
        for (String c : candidates) {
            String v = get(row, c);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return "";
    }

    /** Finds a header whose name contains ANY of the keywords (case-insensitive). */
    public static String findHeaderContaining(List<String> headers, String... keywords) {
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            if (header == null) {
                continue;
            }
            String lower = header.toLowerCase();
            for (String kw : keywords) {
                if (lower.contains(kw.toLowerCase())) {
                    return header;
                }
            }
        }
        return null;
    }
}
