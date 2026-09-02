package com.biztrackpro.config;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Owns the single {@link EntityManagerFactory} and produces a request-scoped
 * {@link EntityManager} that repositories and services share within one HTTP request.
 *
 * Connection settings default to persistence.xml but are overridden here from
 * environment variables (DB_URL, DB_USER, DB_PASSWORD) so the same WAR runs in
 * Docker, staging and production without rebuilding.
 */
@ApplicationScoped
public class EntityManagerProducer {

    private static final String PU_NAME = "biztrackPU";

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        Map<String, String> overrides = new HashMap<>();
        putIfPresent(overrides, "hibernate.connection.url", System.getenv("DB_URL"));
        putIfPresent(overrides, "hibernate.connection.username", System.getenv("DB_USER"));
        putIfPresent(overrides, "hibernate.connection.password", System.getenv("DB_PASSWORD"));
        String ddl = System.getenv("HIBERNATE_DDL_AUTO");
        putIfPresent(overrides, "hibernate.hbm2ddl.auto", ddl);

        this.emf = Persistence.createEntityManagerFactory(PU_NAME, overrides);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    @Produces
    @RequestScoped
    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public void closeEntityManager(@Disposes EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @PreDestroy
    public void destroy() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
