package com.biztrackpro.util;

import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

/**
 * Small helper for RESOURCE_LOCAL transactions. Services wrap all writes in
 * {@code Tx.run(em, () -> {...})} so commit/rollback handling lives in one place.
 */
public final class Tx {

    private Tx() {
    }

    public static <T> T call(EntityManager em, Supplier<T> work) {
        EntityTransaction tx = em.getTransaction();
        boolean startedHere = false;
        try {
            if (!tx.isActive()) {
                tx.begin();
                startedHere = true;
            }
            T result = work.get();
            if (startedHere) {
                tx.commit();
            }
            return result;
        } catch (RuntimeException ex) {
            if (startedHere && tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public static void run(EntityManager em, Runnable work) {
        call(em, () -> {
            work.run();
            return null;
        });
    }
}
