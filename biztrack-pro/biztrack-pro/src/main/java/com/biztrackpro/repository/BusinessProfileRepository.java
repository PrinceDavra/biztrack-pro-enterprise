package com.biztrackpro.repository;

import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.BusinessProfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class BusinessProfileRepository {

    @Inject
    private EntityManager em;

    public BusinessProfile save(BusinessProfile profile) {
        if (profile.getId() == null) {
            em.persist(profile);
            return profile;
        }
        return em.merge(profile);
    }

    public Optional<BusinessProfile> findByTenant(Long tenantId) {
        List<BusinessProfile> results = em.createQuery(
                        "SELECT p FROM BusinessProfile p WHERE p.tenantId = :t", BusinessProfile.class)
                .setParameter("t", tenantId)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
