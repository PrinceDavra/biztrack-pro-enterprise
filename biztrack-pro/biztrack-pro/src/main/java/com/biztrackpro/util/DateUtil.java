package com.biztrackpro.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

/**
 * Date parsing/formatting for the multiple third-party CSV formats and the
 * DD/MM/YYYY output used in every CA export.
 */
public final class DateUtil {

    /** CA/report output format. */
    public static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter SHOPIFY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ENGLISH);

    private static final DateTimeFormatter SHIPPING =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("dd/MM/yyyy hh:mm a")
                    .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    private DateUtil() {
    }

    /** Shopify "Created at" e.g. "2024-06-01 12:30:00 +0530". */
    public static LocalDate parseShopifyDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return OffsetDateTime.parse(s, SHOPIFY).toLocalDate();
        } catch (Exception ignored) {
            // fall through to more forgiving parses
        }
        try {
            if (s.length() >= 10) {
                return LocalDate.parse(s.substring(0, 10), ISO);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Shipping recharge "DD/MM/YYYY HH:MM am/pm" e.g. "01/06/2024 03:45 pm". */
    public static LocalDate parseShippingDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDate.parse(s, SHIPPING);
        } catch (Exception ignored) {
        }
        try {
            // date-only fallback: first token
            String datePart = s.split("\\s+")[0];
            return LocalDate.parse(datePart, DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH));
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Manual entry / API "yyyy-MM-dd". Falls back to dd/MM/yyyy. */
    public static LocalDate parseIsoDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDate.parse(s, ISO);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(s, OUTPUT);
        } catch (Exception ignored) {
        }
        // last resort: parse the leading yyyy-MM-dd of an ISO datetime
        try {
            if (s.length() >= 10) {
                return LocalDate.parse(s.substring(0, 10), ISO);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(OUTPUT);
    }

    /** Sortable month key, e.g. "2024-06". */
    public static String monthKey(LocalDate date) {
        return date == null ? "" : String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    }

    /** Human month label, e.g. "Jun 2024". */
    public static String monthLabel(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
    }

    /** 1-based month number (Jan=1) from a "yyyy-MM" key. */
    public static int monthNumberFromKey(String monthKey) {
        if (monthKey == null || !monthKey.contains("-")) {
            return 0;
        }
        return Integer.parseInt(monthKey.substring(monthKey.indexOf('-') + 1));
    }
}
