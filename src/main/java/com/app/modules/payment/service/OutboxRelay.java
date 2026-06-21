package com.app.modules.payment.service;

public interface OutboxRelay {

    /**
     * Publishes a batch of unpublished outbox events to Kafka and marks the delivered ones.
     *
     * @return number of events successfully published in this pass.
     */
    int publishPending();
}
