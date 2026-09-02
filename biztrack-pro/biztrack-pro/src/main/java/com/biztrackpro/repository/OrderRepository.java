package com.biztrackpro.repository;

import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.Order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class OrderRepository {

    @Inject
    private EntityManager em;

    public Order save(Order order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }

    public Optional<Order> findById(Long id, Long tenantId) {
        Order o = em.find(Order.class, id);
        if (o == null || !o.getTenantId().equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(o);
    }

    public boolean existsByOrderId(String orderId, Long tenantId) {
        Long count = em.createQuery(
                        "SELECT COUNT(o) FROM Order o WHERE o.orderId = :oid AND o.tenantId = :t", Long.class)
                .setParameter("oid", orderId)
                .setParameter("t", tenantId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public List<Order> findByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT o FROM Order o WHERE o.tenantId = :t ORDER BY o.date ASC, o.id ASC", Order.class)
                .setParameter("t", tenantId)
                .getResultList();
    }

    public List<Order> findByTenantPaged(Long tenantId, int offset, int limit) {
        return em.createQuery(
                        "SELECT o FROM Order o WHERE o.tenantId = :t ORDER BY o.date DESC, o.id DESC", Order.class)
                .setParameter("t", tenantId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT COUNT(o) FROM Order o WHERE o.tenantId = :t", Long.class)
                .setParameter("t", tenantId)
                .getSingleResult();
    }

    public boolean deleteById(Long id, Long tenantId) {
        Optional<Order> existing = findById(id, tenantId);
        if (existing.isEmpty()) {
            return false;
        }
        em.remove(existing.get());
        return true;
    }
}
