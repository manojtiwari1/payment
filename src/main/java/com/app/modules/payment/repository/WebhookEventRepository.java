package com.app.modules.payment.repository;

import com.app.modules.payment.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    /**
     * Race-safe insert keyed by {@code event_id}. Returns 1 if newly inserted, 0 if the event was
     * already recorded (duplicate delivery → ignore).
     */
    @Modifying
    @Query(value = """
            INSERT INTO webhook_events
                (event_id, psp, payment_id, payload, processed, created_at)
            VALUES (:eventId, :psp, :paymentId, :payload, false, now())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId,
                       @Param("psp") String psp,
                       @Param("paymentId") Long paymentId,
                       @Param("payload") String payload);

    @Modifying
    @Query("UPDATE WebhookEvent w SET w.processed = true WHERE w.eventId = :eventId")
    void markProcessed(@Param("eventId") String eventId);
}
