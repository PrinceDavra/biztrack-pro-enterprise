package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * Dashboard headline KPIs. Every monetary field is a BigDecimal.
 * Counts (orders, units) are plain longs.
 */
public class KpiDTO {

    public BigDecimal totalRevenue = BigDecimal.ZERO;
    public BigDecimal netProfit = BigDecimal.ZERO;
    public BigDecimal netMargin = BigDecimal.ZERO;      // percentage value, e.g. 18.42
    public BigDecimal grossProfit = BigDecimal.ZERO;
    public BigDecimal grossMargin = BigDecimal.ZERO;    // percentage value
    public BigDecimal totalExpenses = BigDecimal.ZERO;  // COGS + OpEx + Ad Spend
    public BigDecimal cogs = BigDecimal.ZERO;
    public BigDecimal otherExpenses = BigDecimal.ZERO;
    public BigDecimal adSpend = BigDecimal.ZERO;
    public BigDecimal adRevenue = BigDecimal.ZERO;
    public BigDecimal roas = BigDecimal.ZERO;
    public BigDecimal refunds = BigDecimal.ZERO;
    public BigDecimal aov = BigDecimal.ZERO;
    public long unitsSold = 0;
    public long totalOrders = 0;
}
