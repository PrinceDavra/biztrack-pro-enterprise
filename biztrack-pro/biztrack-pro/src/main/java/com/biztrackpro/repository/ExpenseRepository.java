package com.biztrackpro.repository;

import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.Expense;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ExpenseRepository {

    @Inject
    private EntityManager em;

    public Expense save(Expense expense) {
        if (expense.getId() == null) {
            em.persist(expense);
            return expense;
        }
        return em.merge(expense);
    }

    public Optional<Expense> findById(Long id, Long tenantId) {
        Expense e = em.find(Expense.class, id);
        if (e == null || !e.getTenantId().equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(e);
    }

    public boolean existsByTxnId(String txnId, Long tenantId) {
        Long count = em.createQuery(
                        "SELECT COUNT(e) FROM Expense e WHERE e.txnId = :txn AND e.tenantId = :t", Long.class)
                .setParameter("txn", txnId)
                .setParameter("t", tenantId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public List<Expense> findByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT e FROM Expense e WHERE e.tenantId = :t ORDER BY e.date ASC, e.id ASC", Expense.class)
                .setParameter("t", tenantId)
                .getResultList();
    }

    public List<Expense> findByTenantPaged(Long tenantId, int offset, int limit) {
        return em.createQuery(
                        "SELECT e FROM Expense e WHERE e.tenantId = :t ORDER BY e.date DESC, e.id DESC", Expense.class)
                .setParameter("t", tenantId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT COUNT(e) FROM Expense e WHERE e.tenantId = :t", Long.class)
                .setParameter("t", tenantId)
                .getSingleResult();
    }

    public boolean deleteById(Long id, Long tenantId) {
        Optional<Expense> existing = findById(id, tenantId);
        if (existing.isEmpty()) {
            return false;
        }
        em.remove(existing.get());
        return true;
    }
}
