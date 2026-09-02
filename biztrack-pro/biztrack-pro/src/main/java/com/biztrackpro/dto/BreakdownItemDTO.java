package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * One slice of the expense pie chart (a category, COGS, or Ad Spend).
 */
public class BreakdownItemDTO {

    public String label;
    public BigDecimal amount = BigDecimal.ZERO;

    public BreakdownItemDTO() {
    }

    public BreakdownItemDTO(String label, BigDecimal amount) {
        this.label = label;
        this.amount = amount;
    }
}
