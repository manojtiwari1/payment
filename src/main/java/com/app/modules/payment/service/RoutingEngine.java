package com.app.modules.payment.service;

public interface RoutingEngine {

    /**
     * Routes a PENDING payment through its configured PSPs with failover, updating state and
     * recording an attempt per try.
     */
    void route(Long paymentId);
}
