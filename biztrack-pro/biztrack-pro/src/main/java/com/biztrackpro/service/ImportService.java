package com.biztrackpro.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biztrackpro.dto.ImportResultDTO;
import com.biztrackpro.entity.AdCampaign;
import com.biztrackpro.entity.Expense;
import com.biztrackpro.entity.Order;
import com.biztrackpro.entity.ProductCost;
import com.biztrackpro.repository.AdCampaignRepository;
import com.biztrackpro.repository.ExpenseRepository;
import com.biztrackpro.repository.OrderRepository;
import com.biztrackpro.repository.ProductCostRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.CsvParserUtil;
import com.biztrackpro.util.CsvParserUtil.ParsedCsv;
import com.biztrackpro.util.DateUtil;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * All four CSV importers. Each follows its exact source-format rules, deduplicates
 * against previously imported data, and persists in a single transaction.
 */
@ApplicationScoped
public class ImportService {

    @Inject
    private OrderRepository orderRepo;

    @Inject
    private AdCampaignRepository adRepo;

    @Inject
    private ExpenseRepository expenseRepo;

    @Inject
    private ProductCostRepository costRepo;

    @Inject
    private CostService costService;

    @Inject
    private EntityManager em;

    // ---------------------------------------------------------------------
    // SHOPIFY
    // ---------------------------------------------------------------------

    public ImportResultDTO importShopify(Long tenantId, InputStream in) {
        ParsedCsv csv = CsvParserUtil.parse(in);

        // Group every line-item row under its Order ID ("Name"), preserving file order.
        Map<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        for (Map<String, String> row : csv.getRows()) {
            String name = CsvParserUtil.get(row, "Name").trim();
            if (name.isEmpty()) {
                continue;
            }
            groups.computeIfAbsent(name, k -> new ArrayList<>()).add(row);
        }

        int imported = 0;
        int duplicates = 0;
        int freeItemsSkipped = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<Order> toSave = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, String>>> entry : groups.entrySet()) {
            String orderId = entry.getKey();
            if (orderRepo.existsByOrderId(orderId, tenantId)) {
                duplicates++;
                continue;
            }
            Order o = buildShopifyOrder(tenantId, orderId, entry.getValue());
            toSave.add(o);
            imported++;
            freeItemsSkipped += o.getFreeItems() != null ? o.getFreeItems() : 0;
            totalRevenue = totalRevenue.add(BigDecimalUtil.nz(o.getRevenue()));
        }

        Tx.run(em, () -> toSave.forEach(orderRepo::save));

        ImportResultDTO r = new ImportResultDTO();
        r.imported = imported;
        r.duplicates = duplicates;
        r.freeItemsSkipped = freeItemsSkipped;
        r.totalRevenue = BigDecimalUtil.money(totalRevenue);
        r.message = imported + " order(s) imported, " + duplicates + " duplicate(s) skipped";
        return r;
    }

    private Order buildShopifyOrder(Long tenantId, String orderId, List<Map<String, String>> rows) {
        Order o = new Order();
        o.setTenantId(tenantId);
        o.setOrderId(orderId);
        o.setSource("shopify");

        LocalDate date = null;
        String status = "";
        String city = "";
        String province = "";
        BigDecimal revenue = null;   // Total, read ONCE from the first row that carries it
        BigDecimal refund = BigDecimal.ZERO;

        List<String> paidNames = new ArrayList<>();
        int paidQty = 0;
        int freeQty = 0;
        int paidRows = 0;
        BigDecimal firstPaidPrice = null;
        String firstPaidSku = null;

        for (Map<String, String> row : rows) {
            if (date == null) {
                LocalDate d = DateUtil.parseShopifyDate(CsvParserUtil.get(row, "Created at"));
                if (d != null) {
                    date = d;
                }
            }
            // Revenue: Total from the first row that has a value; never summed across line items.
            if (revenue == null) {
                String total = CsvParserUtil.get(row, "Total");
                if (total != null && !total.isBlank()) {
                    revenue = BigDecimalUtil.parseMoney(total);
                }
            }
            if (refund.signum() == 0) {
                String r = CsvParserUtil.get(row, "Refunded Amount");
                if (r != null && !r.isBlank()) {
                    refund = BigDecimalUtil.parseMoney(r);
                }
            }
            if (status.isEmpty()) {
                status = CsvParserUtil.get(row, "Financial Status");
            }
            // Shipping fields come from the first line-item row; never overwrite with an empty value.
            if (city.isEmpty()) {
                city = CsvParserUtil.get(row, "Shipping City");
            }
            if (province.isEmpty()) {
                province = CsvParserUtil.getAny(row, "Shipping Province Name", "Shipping Province");
            }

            int q = BigDecimalUtil.parseInt(CsvParserUtil.get(row, "Lineitem quantity"));
            BigDecimal price = BigDecimalUtil.parseMoney(CsvParserUtil.get(row, "Lineitem price"));
            String lineName = CsvParserUtil.get(row, "Lineitem name");
            String lineSku = CsvParserUtil.get(row, "Lineitem sku");

            if (price.signum() == 0) {
                // Free item: excluded from revenue, counted separately.
                freeQty += Math.max(q, 0);
            } else {
                paidRows++;
                paidQty += Math.max(q, 0);
                if (!lineName.isEmpty()) {
                    paidNames.add(lineName);
                }
                if (firstPaidPrice == null) {
                    firstPaidPrice = price;
                    firstPaidSku = lineSku;
                }
            }
        }

        o.setDate(date);
        o.setStatus(status);
        o.setShippingCity(city.isEmpty() ? null : city);
        o.setShippingProvince(province.isEmpty() ? null : province);
        o.setRevenue(BigDecimalUtil.money(revenue));      // money() treats null as ZERO
        o.setRefund(BigDecimalUtil.money(refund));
        o.setProduct(String.join(", ", paidNames));
        o.setQty(paidQty);
        o.setFreeItems(freeQty);
        o.setItemCount(Math.max(paidRows, 0));
        o.setUnitPrice(BigDecimalUtil.money(firstPaidPrice));
        o.setSku(firstPaidSku);
        o.setCogsPerUnit(BigDecimal.ZERO);
        // At import COGS is unknown, so profit = revenue - refund.
        o.setProfit(BigDecimalUtil.money(o.getRevenue().subtract(o.getRefund())));
        return o;
    }

    // ---------------------------------------------------------------------
    // META ADS
    // ---------------------------------------------------------------------

    public ImportResultDTO importMeta(Long tenantId, InputStream in) {
        ParsedCsv csv = CsvParserUtil.parse(in);

        int imported = 0;
        int duplicates = 0;
        int skipped = 0;
        List<AdCampaign> toSave = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Map<String, String> row : csv.getRows()) {
            String name = CsvParserUtil.getAny(row, "Campaign name", "Campaign Name").trim();
            BigDecimal spend = BigDecimalUtil.parseMoney(
                    CsvParserUtil.getAny(row, "Amount spent (INR)", "Amount Spent (INR)", "Amount spent"));

            if (name.equalsIgnoreCase("Total") || name.equalsIgnoreCase("Totals")) {
                skipped++;
                continue;
            }
            if (spend.signum() == 0 && name.isEmpty()) {
                skipped++;
                continue;
            }

            LocalDate date = DateUtil.parseIsoDate(
                    CsvParserUtil.getAny(row, "Reporting starts", "Reporting Starts", "Day", "Date"));
            String key = name.toLowerCase() + "|" + (date != null ? date.toString() : "");
            if (seen.contains(key)) {
                duplicates++;
                continue;
            }
            if (date != null && adRepo.existsByNameAndDate(name, date, tenantId)) {
                duplicates++;
                continue;
            }

            String roasIndicator = CsvParserUtil.getAny(row, "Result ROAS indicator", "Results ROAS indicator");
            String resultIndicator = CsvParserUtil.getAny(row, "Result indicator", "Results indicator");
            boolean isPurchase = roasIndicator != null && roasIndicator.toLowerCase().contains("fb_pixel_purchase");
            BigDecimal roas = BigDecimalUtil.parseMoneyOrNull(
                    CsvParserUtil.getAny(row, "Results ROAS", "Result ROAS", "Purchase ROAS (return on ad spend)"));

            AdCampaign c = new AdCampaign();
            c.setTenantId(tenantId);
            c.setName(name);
            c.setPlatform("Meta");
            c.setSource("meta");
            c.setDate(date);
            c.setSpend(BigDecimalUtil.money(spend));
            c.setImpressions(BigDecimalUtil.parseInt(CsvParserUtil.getAny(row, "Impressions")));
            c.setClicks(BigDecimalUtil.parseInt(CsvParserUtil.getAny(row, "Clicks (all)", "Clicks")));
            c.setReach(BigDecimalUtil.parseInt(CsvParserUtil.getAny(row, "Reach")));
            c.setConversions(BigDecimalUtil.parseInt(CsvParserUtil.getAny(row, "Results")));
            c.setRoas(roas);
            c.setRoasAvailable(isPurchase && roas != null);

            // Revenue only when the result is a purchase AND a ROAS figure exists.
            BigDecimal revenue = (isPurchase && roas != null) ? BigDecimalUtil.multiply(roas, spend) : BigDecimal.ZERO;
            c.setRevenue(BigDecimalUtil.money(revenue));

            c.setCpc(BigDecimalUtil.parseMoneyOrNull(CsvParserUtil.getAny(row, "CPC (all) (INR)", "CPC (all)", "CPC")));
            c.setCpm(BigDecimalUtil.parseMoneyOrNull(CsvParserUtil.getAny(row,
                    "CPM (cost per 1,000 impressions) (INR)", "CPM (cost per 1,000 impressions)", "CPM")));
            int conv = c.getConversions() != null ? c.getConversions() : 0;
            c.setCpl(conv > 0 ? BigDecimalUtil.divide(spend, BigDecimal.valueOf(conv), 2) : null);
            c.setDeliveryStatus(CsvParserUtil.getAny(row, "Campaign delivery", "Delivery"));
            c.setOptimisedFor(resultIndicator);

            toSave.add(c);
            seen.add(key);
            imported++;
        }

        Tx.run(em, () -> toSave.forEach(adRepo::save));

        ImportResultDTO r = new ImportResultDTO();
        r.imported = imported;
        r.duplicates = duplicates;
        r.skipped = skipped;
        r.message = imported + " campaign(s) imported";
        return r;
    }

    // ---------------------------------------------------------------------
    // SHIPPING RECHARGE
    // ---------------------------------------------------------------------

    public ImportResultDTO importShipping(Long tenantId, InputStream in) {
        ParsedCsv csv = CsvParserUtil.parse(in);

        int imported = 0;
        int duplicates = 0;
        int skipped = 0;
        List<Expense> toSave = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Map<String, String> row : csv.getRows()) {
            String status = CsvParserUtil.getAny(row, "Status");
            if (!"success".equalsIgnoreCase(status.trim())) {
                skipped++;
                continue;
            }
            String gateway = CsvParserUtil.getAny(row, "Gateway");
            if ("manual".equalsIgnoreCase(gateway.trim())) {
                // Manual gateway rows are credits/refunds, not expenses.
                skipped++;
                continue;
            }

            String txnId = CsvParserUtil.getAny(row, "Transaction ID", "Transaction Id", "Txn ID").trim();
            if (!txnId.isEmpty()) {
                if (seen.contains(txnId)) {
                    duplicates++;
                    continue;
                }
                if (expenseRepo.existsByTxnId(txnId, tenantId)) {
                    duplicates++;
                    continue;
                }
            }

            Expense e = new Expense();
            e.setTenantId(tenantId);
            e.setDate(DateUtil.parseShippingDate(CsvParserUtil.getAny(row, "Date")));
            e.setDescription(CsvParserUtil.getAny(row, "Description"));
            e.setAmount(BigDecimalUtil.money(BigDecimalUtil.parseMoney(CsvParserUtil.getAny(row, "Amount"))));
            e.setCategory("Shipping");
            e.setPaymentMethod(gateway);
            e.setTxnId(txnId.isEmpty() ? null : txnId);
            e.setSource("shipping");

            toSave.add(e);
            if (!txnId.isEmpty()) {
                seen.add(txnId);
            }
            imported++;
        }

        Tx.run(em, () -> toSave.forEach(expenseRepo::save));

        ImportResultDTO r = new ImportResultDTO();
        r.imported = imported;
        r.duplicates = duplicates;
        r.skipped = skipped;
        r.message = imported + " shipping charge(s) imported";
        return r;
    }

    // ---------------------------------------------------------------------
    // PRODUCT COSTS
    // ---------------------------------------------------------------------

    public ImportResultDTO importCosts(Long tenantId, InputStream in) {
        ParsedCsv csv = CsvParserUtil.parse(in);
        List<String> headers = csv.getHeaders();

        String prodHeader = CsvParserUtil.findHeaderContaining(headers, "product", "name", "item");
        String skuHeader = CsvParserUtil.findHeaderContaining(headers, "sku", "code");
        String costHeader = CsvParserUtil.findHeaderContaining(headers, "cost", "price", "cogs", "rate");

        if (prodHeader == null || costHeader == null) {
            throw new IllegalArgumentException(
                    "Could not find product-name and cost columns in the CSV headers");
        }

        final int[] counts = {0, 0};   // [inserted, updated]

        Tx.run(em, () -> {
            List<ProductCost> existing = costRepo.findByTenant(tenantId);
            Map<String, ProductCost> byName = new LinkedHashMap<>();
            for (ProductCost pc : existing) {
                if (pc.getProductName() != null) {
                    byName.put(pc.getProductName().trim().toLowerCase(), pc);
                }
            }
            for (Map<String, String> row : csv.getRows()) {
                String pname = CsvParserUtil.get(row, prodHeader).trim();
                String costRaw = CsvParserUtil.get(row, costHeader);
                if (pname.isEmpty() || costRaw == null || costRaw.isBlank()) {
                    continue;
                }
                String sku = skuHeader != null ? CsvParserUtil.get(row, skuHeader) : null;
                BigDecimal cost = BigDecimalUtil.money(BigDecimalUtil.parseMoney(costRaw));

                String nameKey = pname.toLowerCase();
                ProductCost pc = byName.get(nameKey);
                if (pc == null) {
                    pc = new ProductCost();
                    pc.setTenantId(tenantId);
                    pc.setProductName(pname);
                    counts[0]++;
                } else {
                    counts[1]++;
                }
                pc.setSku(sku);
                pc.setCost(cost);
                costRepo.save(pc);
                byName.put(nameKey, pc);
            }
        });

        // Auto-apply the refreshed catalogue to every existing order.
        int applied = costService.applyAllCosts(tenantId);

        ImportResultDTO r = new ImportResultDTO();
        r.imported = counts[0];
        r.updated = counts[1];
        r.costsApplied = applied;
        r.message = counts[0] + " cost(s) added, " + counts[1] + " updated, applied to " + applied + " order(s)";
        return r;
    }
}
