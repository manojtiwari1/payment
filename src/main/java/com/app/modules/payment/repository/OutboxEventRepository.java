package com.app.modules.payment.repository;

import com.app.modules.payment.entity.OutboxEvent;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Oldest unpublished events first, capped — the relay batch. */
    List<OutboxEvent> findByPublishedFalseOrderByIdAsc(Limit limit);
}
