package com.biztrackpro.service;

import java.math.BigDecimal;
import java.util.List;

import com.biztrackpro.dto.Requests.CostRequest;
import com.biztrackpro.entity.Order;
import com.biztrackpro.entity.ProductCost;
import com.biztrackpro.repository.OrderRepository;
import com.biztrackpro.repository.ProductCostRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Product-cost catalogue plus the COGS-matching engine.
 *
 * applyAllCosts matches each order to a product cost by priority:
 *   1. SKU exact (case-insensitive)
 *   2. Product name exact (case-insensitive)
 *   3. Product name LIKE (either contains the other, case-insensitive)
 * On a match it sets cogsPerUnit and recomputes profit = revenue - cogs*qty - refund.
 */
@ApplicationScoped
public class CostService {

    @Inject
    private ProductCostRepository costRepo;

    @Inject
    private OrderRepository orderRepo;

    @Inject
    private EntityManager em;

    public List<ProductCost> list(Long tenantId) {
        return costRepo.findByTenant(tenantId);
    }

    /** Upsert by product name (case-insensitive): update if the name already exists, else insert. */
    public ProductCost add(Long tenantId, CostRequest req) {
        if (req == null || req.productName == null || req.productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        return Tx.call(em, () -> {
            ProductCost pc = costRepo.findByProductNameIgnoreCase(req.productName.trim(), tenantId)
                    .orElseGet(ProductCost::new);
            pc.setTenantId(tenantId);
            pc.setProductName(req.productName.trim());
            pc.setSku(req.sku);
            pc.setCost(BigDecimalUtil.money(req.cost));
            pc.setNotes(req.notes);
            return costRepo.save(pc);
        });
    }

    public boolean delete(Long tenantId, Long id) {
        return Tx.call(em, () -> costRepo.deleteById(id, tenantId));
    }

    /** Applies the whole cost catalogue to every order. Returns the number of orders matched. */
    public int applyAllCosts(Long tenantId) {
        return Tx.call(em, () -> {
            List<ProductCost> costs = costRepo.findByTenant(tenantId);
            List<Order> orders = orderRepo.findByTenant(tenantId);
            int applied = 0;
            for (Order o : orders) {
                ProductCost match = matchCost(o, costs);
                if (match == null) {
                    continue;
                }
                BigDecimal cost = BigDecimalUtil.nz(match.getCost());
                o.setCogsPerUnit(cost);
                int qty = o.getQty() != null ? o.getQty() : 0;
                BigDecimal cogsTotal = BigDecimalUtil.multiply(cost, BigDecimal.valueOf(qty));
                BigDecimal profit = BigDecimalUtil.nz(o.getRevenue()).subtract(cogsTotal).subtract(BigDecimalUtil.nz(o.getRefund()));
                o.setProfit(BigDecimalUtil.money(profit));
                orderRepo.save(o);
                applied++;
            }
            return applied;
        });
    }

    private ProductCost matchCost(Order o, List<ProductCost> costs) {
        String orderSku = lower(o.getSku());
        String orderProduct = lower(o.getProduct());

        // 1. SKU exact
        if (!orderSku.isEmpty()) {
            for (ProductCost c : costs) {
                String csku = lower(c.getSku());
                if (!csku.isEmpty() && csku.equals(orderSku)) {
                    return c;
                }
            }
        }
        // 2. Product name exact
        if (!orderProduct.isEmpty()) {
            for (ProductCost c : costs) {
                String cname = lower(c.getProductName());
                if (!cname.isEmpty() && cname.equals(orderProduct)) {
                    return c;
                }
            }
        }
        // 3. Product name LIKE (either direction)
        if (!orderProduct.isEmpty()) {
            for (ProductCost c : costs) {
                String cname = lower(c.getProductName());
                if (!cname.isEmpty() && (orderProduct.contains(cname) || cname.contains(orderProduct))) {
                    return c;
                }
            }
        }
        return null;
    }

    private static String lower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
