package com.biztrackpro.repository;

import java.util.Optional;

import com.biztrackpro.entity.Tenant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

@ApplicationScoped
public class TenantRepository {

    @Inject
    private EntityManager em;

    public Tenant save(Tenant tenant) {
        if (tenant.getId() == null) {
            em.persist(tenant);
            return tenant;
        }
        return em.merge(tenant);
    }

    public Optional<Tenant> findById(Long id) {
        return Optional.ofNullable(em.find(Tenant.class, id));
    }

    public Optional<Tenant> findByEmail(String email) {
        TypedQuery<Tenant> q = em.createQuery(
                "SELECT t FROM Tenant t WHERE LOWER(t.email) = LOWER(:email)", Tenant.class);
        q.setParameter("email", email);
        try {
            return Optional.of(q.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        Long count = em.createQuery(
                        "SELECT COUNT(t) FROM Tenant t WHERE LOWER(t.email) = LOWER(:email)", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count != null && count > 0;
    }
}
