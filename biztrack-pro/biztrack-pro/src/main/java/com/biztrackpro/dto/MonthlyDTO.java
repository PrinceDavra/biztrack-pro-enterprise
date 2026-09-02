package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * One data point for the dashboard "Revenue vs Profit" chart.
 * Only months that actually contain sales are emitted.
 */
public class MonthlyDTO {

    public String monthKey;   // sortable, e.g. "2024-06"
    public String label;      // human, e.g. "Jun 2024"
    public BigDecimal revenue = BigDecimal.ZERO;
    public BigDecimal profit = BigDecimal.ZERO;

    public MonthlyDTO() {
    }

    public MonthlyDTO(String monthKey, String label, BigDecimal revenue, BigDecimal profit) {
        this.monthKey = monthKey;
        this.label = label;
        this.revenue = revenue;
        this.profit = profit;
    }
}
