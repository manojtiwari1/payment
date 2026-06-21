package com.app.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Decouples publishers (e.g. the auth service) from the Spring eventing API and
 * gives a single seam to later swap in an external broker (Kafka/Redis Streams)
 * without touching call sites.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(Object event) {
        delegate.publishEvent(event);
    }
}
