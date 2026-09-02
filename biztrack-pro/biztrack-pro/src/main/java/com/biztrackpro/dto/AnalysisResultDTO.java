package com.biztrackpro.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured output of the rule-based AI Advisor. Five insight sections plus a
 * compact data summary the UI shows above them. Every insight string references
 * real monetary values (e.g. "your AOV of ₹269").
 */
public class AnalysisResultDTO {

    public DataSummary summary = new DataSummary();
    public List<String> keyInsights = new ArrayList<>();
    public List<String> actionItems = new ArrayList<>();
    public List<String> redFlags = new ArrayList<>();
    public List<String> cityOpportunity = new ArrayList<>();
    public List<String> adStrategy = new ArrayList<>();

    public static class DataSummary {
        public BigDecimal totalRevenue = BigDecimal.ZERO;
        public BigDecimal netProfit = BigDecimal.ZERO;
        public BigDecimal netMargin = BigDecimal.ZERO;
        public BigDecimal aov = BigDecimal.ZERO;
        public BigDecimal adSpend = BigDecimal.ZERO;
        public BigDecimal roas = BigDecimal.ZERO;
        public long totalOrders = 0;
        public long unitsSold = 0;
        public String bestMonth = "";
        public String topCity = "";
    }
}
