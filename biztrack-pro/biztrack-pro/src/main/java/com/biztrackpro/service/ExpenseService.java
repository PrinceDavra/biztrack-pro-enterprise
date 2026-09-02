package com.biztrackpro.service;

import java.time.LocalDate;

import com.biztrackpro.dto.PageDTO;
import com.biztrackpro.dto.Requests.ExpenseRequest;
import com.biztrackpro.entity.Expense;
import com.biztrackpro.repository.ExpenseRepository;
import com.biztrackpro.util.BigDecimalUtil;
import com.biztrackpro.util.DateUtil;
import com.biztrackpro.util.Tx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Operating-expense CRUD (list/add/delete).
 */
@ApplicationScoped
public class ExpenseService {

    @Inject
    private ExpenseRepository expenseRepo;

    @Inject
    private EntityManager em;

    public PageDTO<Expense> list(Long tenantId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 500);
        long total = expenseRepo.countByTenant(tenantId);
        return new PageDTO<>(expenseRepo.findByTenantPaged(tenantId, (p - 1) * s, s), total, p, s);
    }

    public Expense add(Long tenantId, ExpenseRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Missing expense data");
        }
        return Tx.call(em, () -> {
            Expense e = new Expense();
            e.setTenantId(tenantId);
            e.setDate(req.date != null && !req.date.isBlank() ? DateUtil.parseIsoDate(req.date) : LocalDate.now());
            e.setDescription(req.description);
            e.setAmount(BigDecimalUtil.money(req.amount));
            e.setCategory(req.category != null && !req.category.isBlank() ? req.category : "Other");
            e.setPaymentMethod(req.paymentMethod);
            e.setTxnId(req.txnId);
            e.setSource("manual");
            expenseRepo.save(e);
            return e;
        });
    }

    public boolean delete(Long tenantId, Long id) {
        return Tx.call(em, () -> expenseRepo.deleteById(id, tenantId));
    }
}
