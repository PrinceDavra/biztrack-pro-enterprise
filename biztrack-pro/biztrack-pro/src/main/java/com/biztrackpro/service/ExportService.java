package com.biztrackpro.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.biztrackpro.dto.CityDTO;
import com.biztrackpro.dto.KpiDTO;
import com.biztrackpro.dto.MonthlyPnlDTO;
import com.biztrackpro.entity.AdCampaign;
import com.biztrackpro.entity.BusinessProfile;
import com.biztrackpro.entity.Expense;
import com.biztrackpro.entity.Order;
import com.biztrackpro.entity.ProductCost;
import com.biztrackpro.repository.AdCampaignRepository;
import com.biztrackpro.repository.ExpenseRepository;
import com.biztrackpro.repository.OrderRepository;
import com.biztrackpro.repository.ProductCostRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.DateUtil;
import com.biztrackpro.util.Display;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * CA-ready CSV exports. Every file is UTF-8 with a leading BOM (Excel), a metadata
 * header row (business | FY | generated | GSTIN), DD/MM/YYYY dates, and TOTAL rows
 * where applicable. Numeric cells are plain (unsymboled) so spreadsheets treat them
 * as numbers.
 */
@ApplicationScoped
public class ExportService {

    private static final String[] MONTHS =
            {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    @Inject
    private OrderRepository orderRepo;

    @Inject
    private ExpenseRepository expenseRepo;

    @Inject
    private AdCampaignRepository adRepo;

    @Inject
    private ProductCostRepository costRepo;

    @Inject
    private FinancialService financialService;

    @Inject
    private ProfileService profileService;

    // ---------------------------------------------------------------------
    // public entry points (one per endpoint)
    // ---------------------------------------------------------------------

    public ExportFile pnl(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Profit & Loss Statement");
            bodyPnl(p, tenantId);
        });
        return file(pr, "P&L", content);
    }

    public ExportFile sales(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Sales Ledger");
            bodySales(p, tenantId);
        });
        return file(pr, "Sales_Ledger", content);
    }

    public ExportFile expenses(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Expense Ledger");
            bodyExpenses(p, tenantId);
        });
        return file(pr, "Expense_Ledger", content);
    }

    public ExportFile ads(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Ad Report");
            bodyAds(p, tenantId);
        });
        return file(pr, "Ad_Report", content);
    }

    public ExportFile full(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Complete Books");
            p.println();
            p.printRecord("=== PROFIT & LOSS STATEMENT ===");
            bodyPnl(p, tenantId);
            p.println();
            p.printRecord("=== SALES LEDGER ===");
            bodySales(p, tenantId);
            p.println();
            p.printRecord("=== EXPENSE LEDGER ===");
            bodyExpenses(p, tenantId);
            p.println();
            p.printRecord("=== AD REPORT ===");
            bodyAds(p, tenantId);
        });
        return file(pr, "Complete_Books", content);
    }

    public ExportFile summary(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Summary Report");
            bodySummary(p, tenantId);
        });
        return file(pr, "Summary", content);
    }

    public ExportFile backup(Long tenantId) {
        BusinessProfile pr = profileService.getEntity(tenantId);
        String content = render(p -> {
            writeMeta(p, pr, "Full Data Backup");
            bodyBackup(p, tenantId);
        });
        return file(pr, "Backup", content);
    }

    // ---------------------------------------------------------------------
    // report bodies
    // ---------------------------------------------------------------------

    private void bodyPnl(CSVPrinter p, Long tenantId) throws IOException {
        List<MonthlyPnlDTO> rows = financialService.getPnl(tenantId);

        BigDecimal[] rev = zeros();
        BigDecimal[] cogs = zeros();
        BigDecimal[] adv = zeros();
        BigDecimal[] other = zeros();
        for (MonthlyPnlDTO r : rows) {
            int m = DateUtil.monthNumberFromKey(r.monthKey);
            if (m < 1 || m > 12) {
                continue;
            }
            rev[m] = rev[m].add(r.revenue);
            cogs[m] = cogs[m].add(r.cogs);
            adv[m] = adv[m].add(r.adSpend);
            other[m] = other[m].add(r.otherExpenses);
        }
        BigDecimal[] gp = zeros();
        BigDecimal[] opex = zeros();
        BigDecimal[] net = zeros();
        List<Integer> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            gp[m] = rev[m].subtract(cogs[m]);
            opex[m] = adv[m].add(other[m]);
            net[m] = gp[m].subtract(opex[m]);
            if (rev[m].signum() != 0 || cogs[m].signum() != 0 || adv[m].signum() != 0 || other[m].signum() != 0) {
                months.add(m);
            }
        }

        List<String> header = new ArrayList<>();
        header.add("Metric");
        for (int m : months) {
            header.add(MONTHS[m]);
        }
        header.add("TOTAL");
        p.printRecord(header);

        metricRow(p, "Gross Revenue", months, rev);
        metricRow(p, "COGS", months, cogs);
        metricRow(p, "Gross Profit", months, gp);
        metricRow(p, "Advertising Expense", months, adv);
        metricRow(p, "Other Operating Expenses", months, other);
        metricRow(p, "Total OpEx", months, opex);
        metricRow(p, "NET PROFIT", months, net);

        // Net Margin % row (percent, not a sum)
        List<String> marginRow = new ArrayList<>();
        marginRow.add("Net Margin %");
        BigDecimal totRev = BigDecimal.ZERO;
        BigDecimal totNet = BigDecimal.ZERO;
        for (int m : months) {
            marginRow.add(pct(BigDecimalUtil.percent(net[m], rev[m])));
            totRev = totRev.add(rev[m]);
            totNet = totNet.add(net[m]);
        }
        marginRow.add(pct(BigDecimalUtil.percent(totNet, totRev)));
        p.printRecord(marginRow);
    }

    private void bodySales(CSVPrinter p, Long tenantId) throws IOException {
        List<Order> orders = orderRepo.findByTenant(tenantId);
        p.printRecord("Date", "Order ID", "Product", "Qty", "Unit Price", "Revenue", "COGS/unit",
                "Total COGS", "Gross Profit", "Refund", "Net Revenue", "City", "State", "Status");

        long totQty = 0;
        BigDecimal totRevenue = BigDecimal.ZERO;
        BigDecimal totCogs = BigDecimal.ZERO;
        BigDecimal totGross = BigDecimal.ZERO;
        BigDecimal totRefund = BigDecimal.ZERO;
        BigDecimal totNet = BigDecimal.ZERO;

        for (Order o : orders) {
            int qty = o.getQty() != null ? o.getQty() : 0;
            BigDecimal revenue = BigDecimalUtil.nz(o.getRevenue());
            BigDecimal cogsTotal = FinancialService.cogsOf(o);
            BigDecimal gross = revenue.subtract(cogsTotal);
            BigDecimal refund = BigDecimalUtil.nz(o.getRefund());
            BigDecimal net = revenue.subtract(refund);

            p.printRecord(DateUtil.format(o.getDate()), o.getOrderId(), o.getProduct(), qty,
                    n(o.getUnitPrice()), n(revenue), n(o.getCogsPerUnit()), n(cogsTotal), n(gross),
                    n(refund), n(net), nsafe(o.getShippingCity()), nsafe(o.getShippingProvince()), nsafe(o.getStatus()));

            totQty += qty;
            totRevenue = totRevenue.add(revenue);
            totCogs = totCogs.add(cogsTotal);
            totGross = totGross.add(gross);
            totRefund = totRefund.add(refund);
            totNet = totNet.add(net);
        }
        p.printRecord("TOTAL", "", "", totQty, "", n(totRevenue), "", n(totCogs), n(totGross),
                n(totRefund), n(totNet), "", "", "");
    }

    private void bodyExpenses(CSVPrinter p, Long tenantId) throws IOException {
        List<Expense> expenses = expenseRepo.findByTenant(tenantId);
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        List<LedgerRow> rows = new ArrayList<>();
        for (Expense e : expenses) {
            rows.add(new LedgerRow(e.getDate(), nsafe(e.getDescription()),
                    e.getCategory() != null ? e.getCategory() : "Other",
                    BigDecimalUtil.nz(e.getAmount()), nsafe(e.getPaymentMethod())));
        }
        for (AdCampaign c : ads) {
            rows.add(new LedgerRow(c.getDate(), "Ad: " + nsafe(c.getName()) + " | " + nsafe(c.getPlatform()),
                    "Advertising", BigDecimalUtil.nz(c.getSpend()), nsafe(c.getPlatform())));
        }
        rows.sort(Comparator.comparing((LedgerRow r) -> r.date,
                Comparator.nullsLast(Comparator.naturalOrder())));

        p.printRecord("Date", "Description", "Category", "Amount", "Payment Method");
        BigDecimal total = BigDecimal.ZERO;
        for (LedgerRow r : rows) {
            p.printRecord(DateUtil.format(r.date), r.description, r.category, n(r.amount), r.paymentMethod);
            total = total.add(r.amount);
        }
        p.printRecord("TOTAL", "", "", n(total), "");
    }

    private void bodyAds(CSVPrinter p, Long tenantId) throws IOException {
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);
        p.printRecord("Date", "Campaign", "Platform", "Spend", "Revenue Attributed", "ROAS",
                "Clicks", "Conversions", "CPC", "CPL");

        BigDecimal totSpend = BigDecimal.ZERO;
        BigDecimal totRevenue = BigDecimal.ZERO;
        long totClicks = 0;
        long totConversions = 0;

        for (AdCampaign c : ads) {
            BigDecimal spend = BigDecimalUtil.nz(c.getSpend());
            BigDecimal revenue = BigDecimalUtil.nz(c.getRevenue());
            int clicks = c.getClicks() != null ? c.getClicks() : 0;
            int conversions = c.getConversions() != null ? c.getConversions() : 0;
            BigDecimal roas = c.getRoas() != null ? c.getRoas() : BigDecimalUtil.divide(revenue, spend, 2);
            BigDecimal cpc = clicks > 0 ? BigDecimalUtil.divide(spend, BigDecimal.valueOf(clicks), 2) : BigDecimal.ZERO;
            BigDecimal cpl = conversions > 0 ? BigDecimalUtil.divide(spend, BigDecimal.valueOf(conversions), 2) : BigDecimal.ZERO;

            p.printRecord(DateUtil.format(c.getDate()), nsafe(c.getName()), nsafe(c.getPlatform()),
                    n(spend), n(revenue), roas.setScale(2, BigDecimalUtil.RM).toPlainString(),
                    clicks, conversions, n(cpc), n(cpl));

            totSpend = totSpend.add(spend);
            totRevenue = totRevenue.add(revenue);
            totClicks += clicks;
            totConversions += conversions;
        }
        BigDecimal totRoas = BigDecimalUtil.divide(totRevenue, totSpend, 2);
        p.printRecord("TOTAL", "", "", n(totSpend), n(totRevenue), totRoas.toPlainString(),
                totClicks, totConversions, "", "");
    }

    private void bodySummary(CSVPrinter p, Long tenantId) throws IOException {
        KpiDTO k = financialService.getKpis(tenantId);

        p.printRecord("Metric", "Value");
        p.printRecord("Total Revenue", n(k.totalRevenue));
        p.printRecord("COGS", n(k.cogs));
        p.printRecord("Gross Profit", n(k.grossProfit));
        p.printRecord("Gross Margin %", pct(k.grossMargin));
        p.printRecord("Ad Spend", n(k.adSpend));
        p.printRecord("Other Operating Expenses", n(k.otherExpenses));
        p.printRecord("Net Profit", n(k.netProfit));
        p.printRecord("Net Margin %", pct(k.netMargin));
        p.printRecord("Total Orders", k.totalOrders);
        p.printRecord("Units Sold", k.unitsSold);
        p.printRecord("Average Order Value", n(k.aov));
        p.printRecord("ROAS", k.roas.toPlainString());

        // Top 5 products by revenue
        Map<String, BigDecimal> byProduct = new LinkedHashMap<>();
        for (Order o : orderRepo.findByTenant(tenantId)) {
            String prod = o.getProduct() != null && !o.getProduct().isBlank() ? o.getProduct() : "(unnamed)";
            byProduct.merge(prod, BigDecimalUtil.nz(o.getRevenue()), BigDecimal::add);
        }
        List<Map.Entry<String, BigDecimal>> products = new ArrayList<>(byProduct.entrySet());
        products.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());
        p.println();
        p.printRecord("Top 5 Products by Revenue");
        p.printRecord("Product", "Revenue");
        for (int i = 0; i < Math.min(5, products.size()); i++) {
            p.printRecord(products.get(i).getKey(), n(products.get(i).getValue()));
        }

        // Top 5 cities by revenue
        List<CityDTO> cities = financialService.getCities(tenantId, "revenue");
        p.println();
        p.printRecord("Top 5 Cities by Revenue");
        p.printRecord("City", "Revenue", "Orders");
        for (int i = 0; i < Math.min(5, cities.size()); i++) {
            CityDTO c = cities.get(i);
            p.printRecord(c.city, n(c.revenue), c.orders);
        }
    }

    private void bodyBackup(CSVPrinter p, Long tenantId) throws IOException {
        p.println();
        p.printRecord("=== ORDERS ===");
        p.printRecord("id", "order_id", "date", "product", "sku", "qty", "unit_price", "cogs_per_unit",
                "revenue", "refund", "profit", "status", "shipping_city", "shipping_province",
                "source", "free_items", "item_count");
        for (Order o : orderRepo.findByTenant(tenantId)) {
            p.printRecord(o.getId(), o.getOrderId(), DateUtil.format(o.getDate()), nsafe(o.getProduct()),
                    nsafe(o.getSku()), o.getQty(), n(o.getUnitPrice()), n(o.getCogsPerUnit()), n(o.getRevenue()),
                    n(o.getRefund()), n(o.getProfit()), nsafe(o.getStatus()), nsafe(o.getShippingCity()),
                    nsafe(o.getShippingProvince()), nsafe(o.getSource()), o.getFreeItems(), o.getItemCount());
        }

        p.println();
        p.printRecord("=== EXPENSES ===");
        p.printRecord("id", "date", "description", "amount", "category", "payment_method", "txn_id", "source");
        for (Expense e : expenseRepo.findByTenant(tenantId)) {
            p.printRecord(e.getId(), DateUtil.format(e.getDate()), nsafe(e.getDescription()), n(e.getAmount()),
                    nsafe(e.getCategory()), nsafe(e.getPaymentMethod()), nsafe(e.getTxnId()), nsafe(e.getSource()));
        }

        p.println();
        p.printRecord("=== AD_CAMPAIGNS ===");
        p.printRecord("id", "date", "name", "platform", "spend", "revenue", "roas", "clicks", "conversions",
                "cpc", "cpl", "impressions", "reach", "cpm", "delivery_status", "source");
        for (AdCampaign c : adRepo.findByTenant(tenantId)) {
            p.printRecord(c.getId(), DateUtil.format(c.getDate()), nsafe(c.getName()), nsafe(c.getPlatform()),
                    n(c.getSpend()), n(c.getRevenue()), c.getRoas() != null ? c.getRoas().toPlainString() : "",
                    c.getClicks(), c.getConversions(), c.getCpc() != null ? c.getCpc().toPlainString() : "",
                    c.getCpl() != null ? c.getCpl().toPlainString() : "", c.getImpressions(), c.getReach(),
                    c.getCpm() != null ? c.getCpm().toPlainString() : "", nsafe(c.getDeliveryStatus()), nsafe(c.getSource()));
        }

        p.println();
        p.printRecord("=== PRODUCT_COSTS ===");
        p.printRecord("id", "product_name", "sku", "cost", "notes");
        for (ProductCost c : costRepo.findByTenant(tenantId)) {
            p.printRecord(c.getId(), nsafe(c.getProductName()), nsafe(c.getSku()), n(c.getCost()), nsafe(c.getNotes()));
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void metricRow(CSVPrinter p, String label, List<Integer> months, BigDecimal[] values) throws IOException {
        List<String> row = new ArrayList<>();
        row.add(label);
        BigDecimal total = BigDecimal.ZERO;
        for (int m : months) {
            row.add(n(values[m]));
            total = total.add(values[m]);
        }
        row.add(n(total));
        p.printRecord(row);
    }

    private void writeMeta(CSVPrinter p, BusinessProfile pr, String reportType) throws IOException {
        String biz = safe(pr.getBusinessName(), "BizTrack Pro");
        String fy = safe(pr.getFinancialYear(), Display.currentFinancialYear());
        String gstin = pr.getGstin() != null ? pr.getGstin() : "";
        String today = LocalDate.now().format(DateUtil.OUTPUT);
        p.printRecord(biz + " | FY " + fy + " | Generated: " + today + " | GSTIN: " + gstin + " | " + reportType);
    }

    private ExportFile file(BusinessProfile pr, String reportType, String content) {
        String biz = sanitize(safe(pr.getBusinessName(), "BizTrackPro"));
        String fy = sanitize(safe(pr.getFinancialYear(), Display.currentFinancialYear()));
        return new ExportFile(biz + "_" + reportType + "_" + fy + ".csv", content);
    }

    private String render(CsvBody body) {
        StringWriter sw = new StringWriter();
        try (CSVPrinter p = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            body.write(p);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build CSV export", e);
        }
        return "﻿" + sw;   // UTF-8 BOM for Excel
    }

    private static String n(BigDecimal v) {
        return Display.plain(v);
    }

    private static String pct(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, BigDecimalUtil.RM).toPlainString();
    }

    private static String nsafe(String s) {
        return s == null ? "" : s;
    }

    private static String safe(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] a = new BigDecimal[13];
        for (int i = 0; i < 13; i++) {
            a[i] = BigDecimal.ZERO;
        }
        return a;
    }

    @FunctionalInterface
    private interface CsvBody {
        void write(CSVPrinter p) throws IOException;
    }

    private static final class LedgerRow {
        final LocalDate date;
        final String description;
        final String category;
        final BigDecimal amount;
        final String paymentMethod;

        LedgerRow(LocalDate date, String description, String category, BigDecimal amount, String paymentMethod) {
            this.date = date;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
        }
    }

    /** Filename + content for a rendered CSV export. */
    public static final class ExportFile {
        public final String filename;
        public final String content;

        public ExportFile(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }
}
