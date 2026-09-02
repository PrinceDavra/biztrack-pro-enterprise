package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * One row of the monthly Profit &amp; Loss table (Analytics -> P&amp;L).
 * Only months that contain any data (sales, expenses or ad spend) are emitted.
 */
public class MonthlyPnlDTO {

    public String monthKey;   // "2024-06"
    public String label;      // "Jun 2024"
    public BigDecimal revenue = BigDecimal.ZERO;
    public BigDecimal cogs = BigDecimal.ZERO;
    public BigDecimal grossProfit = BigDecimal.ZERO;
    public BigDecimal adSpend = BigDecimal.ZERO;
    public BigDecimal otherExpenses = BigDecimal.ZERO;   // operating expenses + refunds
    public BigDecimal netProfit = BigDecimal.ZERO;
    public BigDecimal margin = BigDecimal.ZERO;          // net margin %
}
