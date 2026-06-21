package com.app.modules.payment.repository;

import com.app.modules.payment.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByMerchantCodeAndIdempotencyKey(String merchantCode, String idempotencyKey);

    /**
     * Race-safe insert: relies on Postgres to serialize concurrent attempts on the unique index.
     * Returns 1 if this caller won the insert, 0 if the key already existed (the caller must then
     * read the existing row and roll back its own payment insert).
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys
                (merchant_code, idempotency_key, payment_id, request_hash, created_at)
            VALUES (:merchantCode, :idempotencyKey, :paymentId, :requestHash, now())
            ON CONFLICT (merchant_code, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("merchantCode") String merchantCode,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("paymentId") Long paymentId,
                       @Param("requestHash") String requestHash);
}
