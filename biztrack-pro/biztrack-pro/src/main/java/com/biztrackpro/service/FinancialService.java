package com.biztrackpro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.biztrackpro.dto.BreakdownItemDTO;
import com.biztrackpro.dto.CityDTO;
import com.biztrackpro.dto.KpiDTO;
import com.biztrackpro.dto.MonthlyDTO;
import com.biztrackpro.dto.MonthlyPnlDTO;
import com.biztrackpro.entity.AdCampaign;
import com.biztrackpro.entity.Expense;
import com.biztrackpro.entity.Order;
import com.biztrackpro.repository.AdCampaignRepository;
import com.biztrackpro.repository.ExpenseRepository;
import com.biztrackpro.repository.OrderRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.DateUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Core financial engine. Every figure is derived from the tenant's orders,
 * expenses and ad campaigns using BigDecimal math and the master formulas:
 *
 *   Net Profit  = Revenue - (COGS x Qty) - Operating Expenses - Ad Spend - Refunds
 *   Gross Profit= Revenue - (COGS x Qty)
 *   Total Exp.  = COGS + Operating Expenses + Ad Spend   (ad spend ALWAYS included)
 *   ROAS = AdRevenue / AdSpend,  AOV = Revenue / Orders
 *
 * In the monthly P&amp;L, refunds are grouped into "other operating expenses" so a
 * month's Net Profit equals the master formula exactly.
 */
@ApplicationScoped
public class FinancialService {

    @Inject
    private OrderRepository orderRepo;

    @Inject
    private ExpenseRepository expenseRepo;

    @Inject
    private AdCampaignRepository adRepo;

    // ---------------------------------------------------------------------
    // KPIs
    // ---------------------------------------------------------------------

    public KpiDTO getKpis(Long tenantId) {
        List<Order> orders = orderRepo.findByTenant(tenantId);
        List<Expense> expenses = expenseRepo.findByTenant(tenantId);
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;
        long unitsSold = 0;
        for (Order o : orders) {
            totalRevenue = totalRevenue.add(BigDecimalUtil.nz(o.getRevenue()));
            totalRefunds = totalRefunds.add(BigDecimalUtil.nz(o.getRefund()));
            cogs = cogs.add(cogsOf(o));
            unitsSold += o.getQty() != null ? o.getQty() : 0;
        }

        BigDecimal otherExpenses = sum(expenses, Expense::getAmount);
        BigDecimal adSpend = sum(ads, AdCampaign::getSpend);
        BigDecimal adRevenue = sum(ads, AdCampaign::getRevenue);

        BigDecimal grossProfit = totalRevenue.subtract(cogs);
        BigDecimal netProfit = totalRevenue.subtract(cogs).subtract(otherExpenses).subtract(adSpend).subtract(totalRefunds);
        BigDecimal totalExpenses = cogs.add(otherExpenses).add(adSpend);

        KpiDTO k = new KpiDTO();
        k.totalRevenue = BigDecimalUtil.money(totalRevenue);
        k.netProfit = BigDecimalUtil.money(netProfit);
        k.netMargin = BigDecimalUtil.percent(netProfit, totalRevenue);
        k.grossProfit = BigDecimalUtil.money(grossProfit);
        k.grossMargin = BigDecimalUtil.percent(grossProfit, totalRevenue);
        k.cogs = BigDecimalUtil.money(cogs);
        k.otherExpenses = BigDecimalUtil.money(otherExpenses);
        k.adSpend = BigDecimalUtil.money(adSpend);
        k.adRevenue = BigDecimalUtil.money(adRevenue);
        k.totalExpenses = BigDecimalUtil.money(totalExpenses);
        k.refunds = BigDecimalUtil.money(totalRefunds);
        k.roas = BigDecimalUtil.divide(adRevenue, adSpend, 2);
        k.aov = BigDecimalUtil.divide(totalRevenue, BigDecimal.valueOf(orders.size()), 2);
        k.unitsSold = unitsSold;
        k.totalOrders = orders.size();
        return k;
    }

    // ---------------------------------------------------------------------
    // MONTHLY (dashboard chart) - only months that contain sales
    // ---------------------------------------------------------------------

    public List<MonthlyDTO> getMonthly(Long tenantId) {
        List<Order> orders = orderRepo.findByTenant(tenantId);
        List<Expense> expenses = expenseRepo.findByTenant(tenantId);
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        Map<String, BigDecimal> moRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> moCogs = new LinkedHashMap<>();
        Map<String, BigDecimal> moRefund = new LinkedHashMap<>();
        Map<String, LocalDate> moDate = new LinkedHashMap<>();
        for (Order o : orders) {
            if (o.getDate() == null) {
                continue;
            }
            String key = DateUtil.monthKey(o.getDate());
            add(moRevenue, key, BigDecimalUtil.nz(o.getRevenue()));
            add(moCogs, key, cogsOf(o));
            add(moRefund, key, BigDecimalUtil.nz(o.getRefund()));
            moDate.putIfAbsent(key, o.getDate());
        }

        Map<String, BigDecimal> moExpense = monthlyTotals(expenses, Expense::getDate, Expense::getAmount);
        Map<String, BigDecimal> moAdSpend = monthlyTotals(ads, AdCampaign::getDate, AdCampaign::getSpend);

        List<String> months = new ArrayList<>(moRevenue.keySet());
        months.sort(Comparator.naturalOrder());

        List<MonthlyDTO> out = new ArrayList<>();
        for (String key : months) {
            BigDecimal revenue = moRevenue.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal profit = revenue
                    .subtract(moCogs.getOrDefault(key, BigDecimal.ZERO))
                    .subtract(moExpense.getOrDefault(key, BigDecimal.ZERO))
                    .subtract(moAdSpend.getOrDefault(key, BigDecimal.ZERO))
                    .subtract(moRefund.getOrDefault(key, BigDecimal.ZERO));
            out.add(new MonthlyDTO(key, DateUtil.monthLabel(moDate.get(key)),
                    BigDecimalUtil.money(revenue), BigDecimalUtil.money(profit)));
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // EXPENSE BREAKDOWN (dashboard pie) - categories + COGS + Ad Spend
    // ---------------------------------------------------------------------

    public List<BreakdownItemDTO> getExpenseBreakdown(Long tenantId) {
        List<Order> orders = orderRepo.findByTenant(tenantId);
        List<Expense> expenses = expenseRepo.findByTenant(tenantId);
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            String cat = e.getCategory() != null && !e.getCategory().isBlank() ? e.getCategory() : "Other";
            add(byCategory, cat, BigDecimalUtil.nz(e.getAmount()));
        }

        BigDecimal cogs = BigDecimal.ZERO;
        for (Order o : orders) {
            cogs = cogs.add(cogsOf(o));
        }
        BigDecimal adSpend = sum(ads, AdCampaign::getSpend);

        if (cogs.signum() > 0) {
            add(byCategory, "COGS", cogs);
        }
        if (adSpend.signum() > 0) {
            add(byCategory, "Ad Spend", adSpend);
        }

        List<BreakdownItemDTO> out = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : byCategory.entrySet()) {
            if (e.getValue().signum() > 0) {
                out.add(new BreakdownItemDTO(e.getKey(), BigDecimalUtil.money(e.getValue())));
            }
        }
        out.sort(Comparator.comparing((BreakdownItemDTO b) -> b.amount).reversed());
        return out;
    }

    // ---------------------------------------------------------------------
    // MONTHLY P&L (analytics) - months with any data
    // ---------------------------------------------------------------------

    public List<MonthlyPnlDTO> getPnl(Long tenantId) {
        List<Order> orders = orderRepo.findByTenant(tenantId);
        List<Expense> expenses = expenseRepo.findByTenant(tenantId);
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        Map<String, BigDecimal> moRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> moCogs = new LinkedHashMap<>();
        Map<String, BigDecimal> moRefund = new LinkedHashMap<>();
        Map<String, LocalDate> moDate = new LinkedHashMap<>();
        for (Order o : orders) {
            if (o.getDate() == null) {
                continue;
            }
            String key = DateUtil.monthKey(o.getDate());
            add(moRevenue, key, BigDecimalUtil.nz(o.getRevenue()));
            add(moCogs, key, cogsOf(o));
            add(moRefund, key, BigDecimalUtil.nz(o.getRefund()));
            moDate.putIfAbsent(key, o.getDate());
        }
        Map<String, BigDecimal> moExpense = monthlyTotals(expenses, Expense::getDate, Expense::getAmount);
        Map<String, BigDecimal> moAdSpend = monthlyTotals(ads, AdCampaign::getDate, AdCampaign::getSpend);
        captureDates(moDate, expenses, Expense::getDate);
        captureDates(moDate, ads, AdCampaign::getDate);

        TreeSet<String> months = new TreeSet<>();
        months.addAll(moRevenue.keySet());
        months.addAll(moExpense.keySet());
        months.addAll(moAdSpend.keySet());

        List<MonthlyPnlDTO> out = new ArrayList<>();
        for (String key : months) {
            BigDecimal revenue = moRevenue.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal cogs = moCogs.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal adSpend = moAdSpend.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal refunds = moRefund.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal opex = moExpense.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal otherExpenses = opex.add(refunds);
            BigDecimal grossProfit = revenue.subtract(cogs);
            BigDecimal netProfit = grossProfit.subtract(adSpend).subtract(otherExpenses);

            MonthlyPnlDTO row = new MonthlyPnlDTO();
            row.monthKey = key;
            row.label = DateUtil.monthLabel(moDate.get(key));
            row.revenue = BigDecimalUtil.money(revenue);
            row.cogs = BigDecimalUtil.money(cogs);
            row.grossProfit = BigDecimalUtil.money(grossProfit);
            row.adSpend = BigDecimalUtil.money(adSpend);
            row.otherExpenses = BigDecimalUtil.money(otherExpenses);
            row.netProfit = BigDecimalUtil.money(netProfit);
            row.margin = BigDecimalUtil.percent(netProfit, revenue);
            out.add(row);
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // CITY ANALYTICS
    // ---------------------------------------------------------------------

    public List<CityDTO> getCities(Long tenantId, String sort) {
        List<Order> orders = orderRepo.findByTenant(tenantId);

        Map<String, CityDTO> byCity = new LinkedHashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order o : orders) {
            String city = o.getShippingCity();
            if (city == null || city.isBlank()) {
                continue;
            }
            String key = city.trim();
            CityDTO c = byCity.computeIfAbsent(key, CityDTO::new);
            c.revenue = c.revenue.add(BigDecimalUtil.nz(o.getRevenue()));
            c.orders += 1;
            c.units += o.getQty() != null ? o.getQty() : 0;
            if ((c.province == null || c.province.isBlank())
                    && o.getShippingProvince() != null && !o.getShippingProvince().isBlank()) {
                c.province = o.getShippingProvince();
            }
            totalRevenue = totalRevenue.add(BigDecimalUtil.nz(o.getRevenue()));
        }

        List<CityDTO> out = new ArrayList<>(byCity.values());
        for (CityDTO c : out) {
            c.aov = BigDecimalUtil.divide(c.revenue, BigDecimal.valueOf(c.orders), 2);
            c.share = BigDecimalUtil.percent(c.revenue, totalRevenue);
            c.revenue = BigDecimalUtil.money(c.revenue);
        }

        Comparator<CityDTO> cmp;
        if ("orders".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparingLong((CityDTO c) -> c.orders).reversed();
        } else if ("aov".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing((CityDTO c) -> c.aov).reversed();
        } else {
            cmp = Comparator.comparing((CityDTO c) -> c.revenue).reversed();
        }
        out.sort(cmp);
        return out;
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /** COGS for a single order = cogsPerUnit * qty. */
    public static BigDecimal cogsOf(Order o) {
        int qty = o.getQty() != null ? o.getQty() : 0;
        return BigDecimalUtil.multiply(BigDecimalUtil.nz(o.getCogsPerUnit()), BigDecimal.valueOf(qty));
    }

    private static <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (T t : list) {
            total = total.add(BigDecimalUtil.nz(getter.apply(t)));
        }
        return total;
    }

    private static <T> Map<String, BigDecimal> monthlyTotals(List<T> list,
                                                             java.util.function.Function<T, LocalDate> dateGetter,
                                                             java.util.function.Function<T, BigDecimal> valueGetter) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (T t : list) {
            LocalDate d = dateGetter.apply(t);
            if (d == null) {
                continue;
            }
            add(map, DateUtil.monthKey(d), BigDecimalUtil.nz(valueGetter.apply(t)));
        }
        return map;
    }

    private static <T> void captureDates(Map<String, LocalDate> moDate, List<T> list,
                                         java.util.function.Function<T, LocalDate> dateGetter) {
        for (T t : list) {
            LocalDate d = dateGetter.apply(t);
            if (d != null) {
                moDate.putIfAbsent(DateUtil.monthKey(d), d);
            }
        }
    }

    private static void add(Map<String, BigDecimal> map, String key, BigDecimal value) {
        map.merge(key, value, BigDecimal::add);
    }
}
