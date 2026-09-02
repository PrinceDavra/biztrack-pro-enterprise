package com.biztrackpro.service;

import java.math.BigDecimal;
import java.util.List;

import com.biztrackpro.dto.AnalysisResultDTO;
import com.biztrackpro.dto.CityDTO;
import com.biztrackpro.dto.KpiDTO;
import com.biztrackpro.dto.MonthlyDTO;
import com.biztrackpro.entity.AdCampaign;
import com.biztrackpro.repository.AdCampaignRepository;
import com.biztrackpro.repository.SystemPropertyRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.Display;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Rule-based "AI" advisor (no external API). Produces five sections — Key Insights,
 * Action Items, Red Flags, City Opportunity and Ad Strategy — with every statement
 * referencing the tenant's real numbers. Thresholds are read from system_properties
 * (with sensible defaults) so rules can be tuned without a redeploy.
 */
@ApplicationScoped
public class AnalysisService {

    @Inject
    private FinancialService financialService;

    @Inject
    private AdCampaignRepository adRepo;

    @Inject
    private SystemPropertyRepository systemPropertyRepo;

    public AnalysisResultDTO analyse(Long tenantId) {
        KpiDTO k = financialService.getKpis(tenantId);
        List<MonthlyDTO> monthly = financialService.getMonthly(tenantId);
        List<CityDTO> cities = financialService.getCities(tenantId, "revenue");
        List<AdCampaign> ads = adRepo.findByTenant(tenantId);

        // Tunable thresholds.
        BigDecimal marginLow = prop("advisor.margin.low", "20");
        BigDecimal aovLow = prop("advisor.aov.low", "300");
        BigDecimal adDangerPct = prop("advisor.adspend.danger.pct", "50");
        BigDecimal roasScale = prop("advisor.roas.scale", "3");
        BigDecimal roasReview = prop("advisor.roas.review", "1");
        long ordersLoyalty = prop("advisor.orders.loyalty", "50").longValue();
        BigDecimal seasonalityPct = prop("advisor.seasonality.pct", "50");

        BigDecimal revenue = k.totalRevenue;
        BigDecimal margin = k.netMargin;
        BigDecimal aov = k.aov;
        BigDecimal adSpend = k.adSpend;
        BigDecimal roas = k.roas;
        long orders = k.totalOrders;
        BigDecimal adSpendPct = BigDecimalUtil.percent(adSpend, revenue);

        MonthlyDTO best = null;
        MonthlyDTO worst = null;
        for (MonthlyDTO m : monthly) {
            if (best == null || m.revenue.compareTo(best.revenue) > 0) {
                best = m;
            }
            if (worst == null || m.revenue.compareTo(worst.revenue) < 0) {
                worst = m;
            }
        }

        AnalysisResultDTO result = new AnalysisResultDTO();

        // ------- data summary -------
        result.summary.totalRevenue = k.totalRevenue;
        result.summary.netProfit = k.netProfit;
        result.summary.netMargin = k.netMargin;
        result.summary.aov = k.aov;
        result.summary.adSpend = k.adSpend;
        result.summary.roas = k.roas;
        result.summary.totalOrders = k.totalOrders;
        result.summary.unitsSold = k.unitsSold;
        result.summary.bestMonth = best != null ? best.label : "";
        result.summary.topCity = !cities.isEmpty() ? cities.get(0).city : "";

        // ------- 1. KEY INSIGHTS -------
        if (best != null) {
            result.keyInsights.add("Your best month was " + best.label + " with "
                    + Display.inr(best.revenue) + " in revenue.");
        }
        if (monthly.size() >= 2) {
            MonthlyDTO last = monthly.get(monthly.size() - 1);
            MonthlyDTO prev = monthly.get(monthly.size() - 2);
            BigDecimal change = BigDecimalUtil.percent(last.revenue.subtract(prev.revenue), prev.revenue);
            if (last.revenue.compareTo(prev.revenue) >= 0) {
                result.keyInsights.add("Revenue is growing: " + prev.label + " " + Display.inr(prev.revenue)
                        + " to " + last.label + " " + Display.inr(last.revenue)
                        + " (+" + Display.pct(change.abs()) + ").");
            } else {
                result.keyInsights.add("Revenue is declining: " + prev.label + " " + Display.inr(prev.revenue)
                        + " to " + last.label + " " + Display.inr(last.revenue)
                        + " (-" + Display.pct(change.abs()) + ").");
            }
        } else {
            result.keyInsights.add("Not enough monthly history yet to establish a revenue trend.");
        }
        result.keyInsights.add("Your net margin is " + Display.pct(margin)
                + " on " + Display.inr(revenue) + " of revenue.");
        result.keyInsights.add("Your average order value (AOV) is " + Display.inr(aov)
                + " across " + orders + " order(s).");

        // ------- 2. ACTION ITEMS -------
        if (margin.signum() >= 0 && margin.compareTo(marginLow) < 0) {
            result.actionItems.add("Net margin of " + Display.pct(margin) + " is below the healthy "
                    + marginLow.stripTrailingZeros().toPlainString()
                    + "% mark — raise prices modestly or negotiate lower COGS.");
        } else if (margin.compareTo(marginLow) >= 0) {
            result.actionItems.add("Healthy " + Display.pct(margin) + " margin — reinvest 30–40% of your "
                    + Display.inr(k.netProfit) + " net profit into scaling ads.");
        }
        if (aov.compareTo(aovLow) < 0) {
            result.actionItems.add("AOV of " + Display.inr(aov)
                    + " is on the low side — add bundles or a minimum-order free-shipping threshold to lift basket size.");
        } else {
            result.actionItems.add("Strong AOV of " + Display.inr(aov)
                    + " — introduce a premium product to push order values even higher.");
        }
        if (orders > ordersLoyalty) {
            result.actionItems.add("With " + orders + " orders you have a base worth a loyalty / repeat-purchase campaign.");
        }
        if (best != null && worst != null && best.revenue.signum() > 0) {
            BigDecimal halfBest = best.revenue.multiply(seasonalityPct).divide(BigDecimalUtil.HUNDRED, 2, BigDecimalUtil.RM);
            if (worst.revenue.compareTo(halfBest) < 0) {
                result.actionItems.add("Revenue swings sharply (" + Display.inr(worst.revenue) + " in " + worst.label
                        + " vs " + Display.inr(best.revenue) + " in " + best.label
                        + ") — plan for seasonality with inventory and cash buffers.");
            }
        }

        // ------- 3. RED FLAGS -------
        boolean anyFlag = false;
        if (margin.signum() < 0) {
            result.redFlags.add("Critical: you are running at a loss (net margin " + Display.pct(margin)
                    + "). Costs currently exceed revenue — act now.");
            anyFlag = true;
        }
        if (k.cogs.signum() == 0 && orders > 0) {
            result.redFlags.add("COGS is " + Display.inr(BigDecimal.ZERO) + " across " + orders
                    + " orders — import a product cost sheet so profit is accurate.");
            anyFlag = true;
        }
        if (revenue.signum() > 0 && adSpendPct.compareTo(adDangerPct) > 0) {
            result.redFlags.add("Ad spend (" + Display.inr(adSpend) + ") is over "
                    + adDangerPct.stripTrailingZeros().toPlainString() + "% of revenue — dangerously high.");
            anyFlag = true;
        }
        if (adSpend.signum() > 0 && roas.compareTo(BigDecimal.ONE) < 0) {
            result.redFlags.add("ROAS is below 1x (" + Display.roas(roas) + "x) — you are losing money on ads.");
            anyFlag = true;
        }
        if (!anyFlag) {
            result.redFlags.add("No red flags — your fundamentals look healthy.");
        }

        // ------- 4. CITY OPPORTUNITY -------
        if (cities.isEmpty()) {
            result.cityOpportunity.add("No city data yet. Re-export your Shopify orders with the Shipping City "
                    + "column included to unlock city analytics.");
        } else {
            CityDTO topCity = cities.get(0);
            CityDTO aovCity = cities.get(0);
            for (CityDTO c : cities) {
                if (c.aov.compareTo(aovCity.aov) > 0) {
                    aovCity = c;
                }
            }
            result.cityOpportunity.add("Your top market is " + topCity.city + " with " + Display.inr(topCity.revenue)
                    + " across " + topCity.orders + " orders.");
            result.cityOpportunity.add(aovCity.city + " has your highest AOV at " + Display.inr(aovCity.aov)
                    + " — a strong candidate for premium targeting.");
        }

        // ------- 5. AD STRATEGY -------
        if (ads.isEmpty()) {
            result.adStrategy.add("No ad data yet — import your Meta Ads CSV to see ROAS and spend insights.");
        } else {
            AdCampaign bestCampaign = null;
            for (AdCampaign c : ads) {
                if (c.getRoas() == null) {
                    continue;
                }
                if (bestCampaign == null || c.getRoas().compareTo(bestCampaign.getRoas()) > 0) {
                    bestCampaign = c;
                }
            }
            String bestName = bestCampaign != null ? bestCampaign.getName() : ads.get(0).getName();

            if (roas.compareTo(roasScale) >= 0) {
                result.adStrategy.add("Your ads return " + Display.roas(roas) + "x — scale the winners, starting with \""
                        + bestName + "\".");
            } else if (roas.compareTo(roasReview) >= 0) {
                result.adStrategy.add("ROAS of " + Display.roas(roas) + "x is workable — review and improve creative on \""
                        + bestName + "\".");
            } else {
                result.adStrategy.add("ROAS of " + Display.roas(roas)
                        + "x is below break-even — pause all campaigns immediately and rework targeting.");
            }
            if (bestCampaign != null && bestCampaign.getRoas() != null) {
                result.adStrategy.add("Best performing campaign: \"" + bestName + "\" at "
                        + Display.roas(bestCampaign.getRoas()) + "x ROAS.");
            }
            result.adStrategy.add("Ad spend is " + Display.pct(adSpendPct)
                    + " of revenue (a healthy range is 15–25%).");
        }

        return result;
    }

    private BigDecimal prop(String key, String def) {
        return systemPropertyRepo.findByKey(key)
                .map(p -> {
                    try {
                        return new BigDecimal(p.getPropValue().trim());
                    } catch (Exception e) {
                        return new BigDecimal(def);
                    }
                })
                .orElse(new BigDecimal(def));
    }
}
