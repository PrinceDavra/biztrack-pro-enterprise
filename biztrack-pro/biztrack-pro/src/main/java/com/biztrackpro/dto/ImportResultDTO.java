package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * Result of any CSV import. Individual importers populate the subset of fields
 * relevant to them; the rest stay at their zero defaults.
 */
public class ImportResultDTO {

    public int imported = 0;
    public int duplicates = 0;
    public int freeItemsSkipped = 0;
    public int skipped = 0;          // rows dropped by filter rules (e.g. non-success shipping rows)
    public int updated = 0;          // product costs updated in place
    public int costsApplied = 0;     // orders whose COGS was (re)matched
    public BigDecimal totalRevenue = BigDecimal.ZERO;
    public String message = "";
}
