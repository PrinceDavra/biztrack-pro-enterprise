package com.biztrackpro.service;

import java.math.BigDecimal;

import com.biztrackpro.dto.PageDTO;
import com.biztrackpro.dto.Requests.SaleRequest;
import com.biztrackpro.entity.Order;
import com.biztrackpro.repository.OrderRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.DateUtil;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Orders CRUD (list/add/delete). Profit is recomputed from the master formula
 * on every write: profit = revenue - (cogsPerUnit * qty) - refund.
 */
@ApplicationScoped
public class SalesService {

    @Inject
    private OrderRepository orderRepo;

    @Inject
    private EntityManager em;

    public PageDTO<Order> list(Long tenantId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 500);
        long total = orderRepo.countByTenant(tenantId);
        return new PageDTO<>(orderRepo.findByTenantPaged(tenantId, (p - 1) * s, s), total, p, s);
    }

    public Order add(Long tenantId, SaleRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Missing sale data");
        }
        return Tx.call(em, () -> {
            Order o = new Order();
            o.setTenantId(tenantId);

            String orderId = (req.orderId != null && !req.orderId.isBlank())
                    ? req.orderId.trim()
                    : "M-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
            if (orderRepo.existsByOrderId(orderId, tenantId)) {
                throw new IllegalArgumentException("Order ID '" + orderId + "' already exists");
            }
            o.setOrderId(orderId);

            o.setDate(req.date != null && !req.date.isBlank() ? DateUtil.parseIsoDate(req.date) : java.time.LocalDate.now());
            o.setProduct(req.product != null ? req.product : "");
            o.setSku(req.sku);

            int qty = req.qty != null && req.qty > 0 ? req.qty : 1;
            o.setQty(qty);
            o.setItemCount(1);
            o.setFreeItems(0);

            BigDecimal unitPrice = BigDecimalUtil.money(req.unitPrice);
            o.setUnitPrice(unitPrice);

            BigDecimal cogs = BigDecimalUtil.money(req.cogsPerUnit);
            o.setCogsPerUnit(cogs);

            BigDecimal revenue = req.revenue != null
                    ? BigDecimalUtil.money(req.revenue)
                    : BigDecimalUtil.money(BigDecimalUtil.multiply(unitPrice, BigDecimal.valueOf(qty)));
            o.setRevenue(revenue);

            BigDecimal refund = BigDecimalUtil.money(req.refund);
            o.setRefund(refund);

            o.setStatus(req.status != null && !req.status.isBlank() ? req.status : "paid");
            o.setShippingCity(req.shippingCity);
            o.setShippingProvince(req.shippingProvince);
            o.setSource("manual");

            BigDecimal cogsTotal = BigDecimalUtil.multiply(cogs, BigDecimal.valueOf(qty));
            o.setProfit(BigDecimalUtil.money(revenue.subtract(cogsTotal).subtract(refund)));

            orderRepo.save(o);
            return o;
        });
    }

    public boolean delete(Long tenantId, Long id) {
        return Tx.call(em, () -> orderRepo.deleteById(id, tenantId));
    }
}
