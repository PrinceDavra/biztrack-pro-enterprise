package com.biztrackpro.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.biztrackpro.dto.PageDTO;
import com.biztrackpro.dto.Requests.AdRequest;
import com.biztrackpro.entity.AdCampaign;
import com.biztrackpro.repository.AdCampaignRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.DateUtil;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Ad-campaign CRUD (list/add/delete). Derived metrics (revenue, ROAS, CPC, CPL,
 * CPM) are computed with BigDecimal when not supplied.
 */
@ApplicationScoped
public class AdService {

    @Inject
    private AdCampaignRepository adRepo;

    @Inject
    private EntityManager em;

    public PageDTO<AdCampaign> list(Long tenantId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 500);
        long total = adRepo.countByTenant(tenantId);
        return new PageDTO<>(adRepo.findByTenantPaged(tenantId, (p - 1) * s, s), total, p, s);
    }

    public AdCampaign add(Long tenantId, AdRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Missing campaign data");
        }
        return Tx.call(em, () -> {
            AdCampaign c = new AdCampaign();
            c.setTenantId(tenantId);
            c.setDate(req.date != null && !req.date.isBlank() ? DateUtil.parseIsoDate(req.date) : LocalDate.now());
            c.setName(req.name != null ? req.name : "Untitled campaign");
            c.setPlatform(req.platform != null && !req.platform.isBlank() ? req.platform : "Meta");

            BigDecimal spend = BigDecimalUtil.money(req.spend);
            c.setSpend(spend);

            int clicks = req.clicks != null ? req.clicks : 0;
            int conversions = req.conversions != null ? req.conversions : 0;
            c.setClicks(clicks);
            c.setConversions(conversions);
            c.setImpressions(req.impressions != null ? req.impressions : 0);
            c.setReach(req.reach != null ? req.reach : 0);

            BigDecimal roas = req.roas;
            BigDecimal revenue = req.revenue;
            if (revenue == null && roas != null) {
                revenue = BigDecimalUtil.multiply(roas, spend);
            }
            if (revenue == null) {
                revenue = BigDecimal.ZERO;
            }
            c.setRevenue(BigDecimalUtil.money(revenue));

            if (roas == null && BigDecimalUtil.isPositive(spend) && BigDecimalUtil.isPositive(revenue)) {
                roas = BigDecimalUtil.ratio(revenue, spend);
            }
            c.setRoas(roas);
            c.setRoasAvailable(roas != null);

            c.setCpc(clicks > 0 ? BigDecimalUtil.divide(spend, BigDecimal.valueOf(clicks), 2) : null);
            c.setCpl(conversions > 0 ? BigDecimalUtil.divide(spend, BigDecimal.valueOf(conversions), 2) : null);
            int impressions = c.getImpressions();
            c.setCpm(impressions > 0
                    ? BigDecimalUtil.divide(BigDecimalUtil.multiply(spend, BigDecimal.valueOf(1000)), BigDecimal.valueOf(impressions), 2)
                    : null);

            c.setOptimisedFor(req.optimisedFor);
            c.setDeliveryStatus(req.deliveryStatus != null && !req.deliveryStatus.isBlank() ? req.deliveryStatus : "active");
            c.setSource("manual");

            adRepo.save(c);
            return c;
        });
    }

    public boolean delete(Long tenantId, Long id) {
        return Tx.call(em, () -> adRepo.deleteById(id, tenantId));
    }
}
