package com.biztrackpro.repository;

import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.ProductCost;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ProductCostRepository {

    @Inject
    private EntityManager em;

    public ProductCost save(ProductCost cost) {
        if (cost.getId() == null) {
            em.persist(cost);
            return cost;
        }
        return em.merge(cost);
    }

    public Optional<ProductCost> findById(Long id, Long tenantId) {
        ProductCost c = em.find(ProductCost.class, id);
        if (c == null || !c.getTenantId().equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(c);
    }

    public List<ProductCost> findByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT c FROM ProductCost c WHERE c.tenantId = :t ORDER BY c.productName ASC",
                        ProductCost.class)
                .setParameter("t", tenantId)
                .getResultList();
    }

    public Optional<ProductCost> findByProductNameIgnoreCase(String productName, Long tenantId) {
        List<ProductCost> results = em.createQuery(
                        "SELECT c FROM ProductCost c WHERE c.tenantId = :t AND LOWER(c.productName) = LOWER(:name)",
                        ProductCost.class)
                .setParameter("t", tenantId)
                .setParameter("name", productName)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public boolean deleteById(Long id, Long tenantId) {
        Optional<ProductCost> existing = findById(id, tenantId);
        if (existing.isEmpty()) {
            return false;
        }
        em.remove(existing.get());
        return true;
    }
}
