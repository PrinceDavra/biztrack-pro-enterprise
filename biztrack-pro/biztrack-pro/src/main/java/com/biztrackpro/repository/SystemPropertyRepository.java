package com.biztrackpro.repository;

import java.util.List;
import java.util.Optional;

import com.biztrackpro.entity.SystemProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class SystemPropertyRepository {

    @Inject
    private EntityManager em;

    public Optional<SystemProperty> findByKey(String key) {
        List<SystemProperty> results = em.createQuery(
                        "SELECT s FROM SystemProperty s WHERE s.propKey = :k", SystemProperty.class)
                .setParameter("k", key)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public SystemProperty save(SystemProperty property) {
        if (property.getId() == null) {
            em.persist(property);
            return property;
        }
        return em.merge(property);
    }
}
