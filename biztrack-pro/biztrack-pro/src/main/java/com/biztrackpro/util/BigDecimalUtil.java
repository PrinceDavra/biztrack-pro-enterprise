package com.biztrackpro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central helpers for money math. Every monetary value in BizTrack Pro is a
 * BigDecimal — never double/float. Money is kept at scale 2, ratios at scale 4,
 * percentages at scale 2, all HALF_UP.
 */
public final class BigDecimalUtil {

    public static final int MONEY_SCALE = 2;
    public static final int RATIO_SCALE = 4;
    public static final int PERCENT_SCALE = 2;
    public static final RoundingMode RM = RoundingMode.HALF_UP;
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private BigDecimalUtil() {
    }

    /** Null-safe: returns ZERO when the value is null. */
    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal money(BigDecimal v) {
        return nz(v).setScale(MONEY_SCALE, RM);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nz(a).add(nz(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return nz(a).subtract(nz(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return nz(a).multiply(nz(b));
    }

    /** Safe divide: returns ZERO when the divisor is null or zero. */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        BigDecimal d = nz(denominator);
        if (d.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nz(numerator).divide(d, scale, RM);
    }

    public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        return divide(numerator, denominator, RATIO_SCALE);
    }

    /** (part / whole) * 100, safe against a zero whole. */
    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        BigDecimal w = nz(whole);
        if (w.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return nz(part).multiply(HUNDRED).divide(w, PERCENT_SCALE, RM);
    }

    public static boolean isPositive(BigDecimal v) {
        return nz(v).signum() > 0;
    }

    /** Parses money from arbitrary CSV text: strips the rupee symbol, commas and spaces. */
    public static BigDecimal parseMoney(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String cleaned = raw.replace("₹", "")   // ₹
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace("INR", "")
                .replace(",", "")
                .replace("\"", "")
                .trim();
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** Parses money but returns null when the field is blank (used where "blank" is meaningful, e.g. ROAS). */
    public static BigDecimal parseMoneyOrNull(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String cleaned = raw.replace("₹", "")
                .replace(",", "")
                .replace("\"", "")
                .trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int parseInt(String raw) {
        if (raw == null) {
            return 0;
        }
        String cleaned = raw.replace(",", "").replace("\"", "").trim();
        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            // tolerate values like "12.0"
            return new BigDecimal(cleaned).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
