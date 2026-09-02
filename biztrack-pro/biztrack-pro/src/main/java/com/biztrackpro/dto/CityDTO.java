package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * Per-city sales analytics row.
 */
public class CityDTO {

    public String city;
    public String province;
    public BigDecimal revenue = BigDecimal.ZERO;
    public long orders = 0;
    public long units = 0;
    public BigDecimal aov = BigDecimal.ZERO;
    public BigDecimal share = BigDecimal.ZERO;   // % of total city revenue

    public CityDTO() {
    }

    public CityDTO(String city) {
        this.city = city;
    }
}
