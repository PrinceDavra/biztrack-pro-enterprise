package com.biztrackpro.dto;

import java.math.BigDecimal;

/**
 * Container for all inbound JSON request bodies (auth + manual entry forms).
 * Grouped in one file since each is a small, field-only DTO deserialized by Jackson.
 */
public final class Requests {

    private Requests() {
    }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    /** Manual single-sale entry. revenue is optional; if absent it is derived from unitPrice * qty. */
    public static class SaleRequest {
        public String orderId;
        public String date;             // yyyy-MM-dd
        public String product;
        public String sku;
        public Integer qty;
        public BigDecimal unitPrice;
        public BigDecimal cogsPerUnit;
        public BigDecimal revenue;
        public BigDecimal refund;
        public String status;
        public String shippingCity;
        public String shippingProvince;
    }

    public static class ExpenseRequest {
        public String date;             // yyyy-MM-dd
        public String description;
        public BigDecimal amount;
        public String category;
        public String paymentMethod;
        public String txnId;
    }

    public static class AdRequest {
        public String date;             // yyyy-MM-dd
        public String name;
        public String platform;
        public BigDecimal spend;
        public BigDecimal revenue;
        public BigDecimal roas;
        public Integer clicks;
        public Integer conversions;
        public Integer impressions;
        public Integer reach;
        public String optimisedFor;
        public String deliveryStatus;
    }

    public static class CostRequest {
        public String productName;
        public String sku;
        public BigDecimal cost;
        public String notes;
    }
}
