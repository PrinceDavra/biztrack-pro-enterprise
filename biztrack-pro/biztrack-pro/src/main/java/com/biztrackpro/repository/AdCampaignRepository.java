package com.biztrackpro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.AdCampaign;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class AdCampaignRepository {

    @Inject
    private EntityManager em;

    public AdCampaign save(AdCampaign campaign) {
        if (campaign.getId() == null) {
            em.persist(campaign);
            return campaign;
        }
        return em.merge(campaign);
    }

    public Optional<AdCampaign> findById(Long id, Long tenantId) {
        AdCampaign c = em.find(AdCampaign.class, id);
        if (c == null || !c.getTenantId().equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(c);
    }

    public boolean existsByNameAndDate(String name, LocalDate date, Long tenantId) {
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM AdCampaign c WHERE c.name = :n AND c.date = :d AND c.tenantId = :t",
                        Long.class)
                .setParameter("n", name)
                .setParameter("d", date)
                .setParameter("t", tenantId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public List<AdCampaign> findByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT c FROM AdCampaign c WHERE c.tenantId = :t ORDER BY c.date ASC, c.id ASC",
                        AdCampaign.class)
                .setParameter("t", tenantId)
                .getResultList();
    }

    public List<AdCampaign> findByTenantPaged(Long tenantId, int offset, int limit) {
        return em.createQuery(
                        "SELECT c FROM AdCampaign c WHERE c.tenantId = :t ORDER BY c.date DESC, c.id DESC",
                        AdCampaign.class)
                .setParameter("t", tenantId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countByTenant(Long tenantId) {
        return em.createQuery(
                        "SELECT COUNT(c) FROM AdCampaign c WHERE c.tenantId = :t", Long.class)
                .setParameter("t", tenantId)
                .getSingleResult();
    }

    public boolean deleteById(Long id, Long tenantId) {
        Optional<AdCampaign> existing = findById(id, tenantId);
        if (existing.isEmpty()) {
            return false;
        }
        em.remove(existing.get());
        return true;
    }
}
