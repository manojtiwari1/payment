package com.app.infrastructure.lock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over PostgreSQL transaction-scoped advisory locks.
 *
 * <p>Transaction-scoped ({@code pg_advisory_xact_lock}) so the lock auto-releases at commit/rollback
 * — no leak across a pooled connection. <strong>Must be called inside an active transaction.</strong>
 */
@Service
public class AdvisoryLockService {

    @PersistenceContext
    private EntityManager entityManager;

    /** Blocks until the advisory lock for {@code key} is held (released at transaction end). */
    public void lock(long key) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
    }

    /**
     * Tries to acquire the lock without blocking.
     *
     * @return {@code true} if acquired; {@code false} if another transaction holds it.
     */
    public boolean tryLock(long key) {
        Object result = entityManager.createNativeQuery("SELECT pg_try_advisory_xact_lock(:key)")
                .setParameter("key", key)
                .getSingleResult();
        return Boolean.TRUE.equals(result);
    }
}
