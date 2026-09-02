package com.biztrackpro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Display-only formatting helpers (never used inside financial calculations).
 * Rupee amounts use Indian digit grouping; CSV numeric cells use plain scaled numbers.
 */
public final class Display {

    private static final Locale IN = Locale.forLanguageTag("en-IN");

    private Display() {
    }

    /** "₹1,23,456" — Indian grouping, whole rupees, for advisor/insight text. */
    public static String inr(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        NumberFormat f = NumberFormat.getInstance(IN);
        f.setMaximumFractionDigits(0);
        f.setMinimumFractionDigits(0);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return "₹" + f.format(x);
    }

    /** "18.42%" */
    public static String pct(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        return x.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /** "1234.50" — plain 2dp number for CSV numeric columns (no symbol, no grouping). */
    public static String plain(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        return x.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** "x2.45" style ROAS label. */
    public static String roas(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        return x.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Current Indian financial year, e.g. "2026-27" (starts 1 April). */
    public static String currentFinancialYear() {
        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        int endYY = (startYear + 1) % 100;
        return String.format("%d-%02d", startYear, endYY);
    }
}
