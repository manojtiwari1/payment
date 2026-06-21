package com.app.modules.payment.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.modules.payment.entity.Payment;
import com.app.modules.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces the allowed payment state transitions. Used by routing, webhooks, and reconciliation
 * so that out-of-order or illegal updates (e.g. {@code SUCCESS → PENDING}) are rejected.
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PENDING));
        ALLOWED.put(PaymentStatus.PENDING, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED));
        ALLOWED.put(PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.REFUNDED));
        ALLOWED.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        return from != null && ALLOWED.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class)).contains(to);
    }

    /**
     * Validates and applies a transition on the given payment, throwing
     * {@link ResponseCode#INVALID_STATE_TRANSITION} if the move is not allowed. The caller persists.
     */
    public void transition(Payment payment, PaymentStatus target) {
        PaymentStatus current = payment.getStatus();
        if (!canTransition(current, target)) {
            throw new ApplicationException(ResponseCode.INVALID_STATE_TRANSITION,
                    "Illegal payment state transition: " + current + " -> " + target);
        }
        payment.setStatus(target);
    }
}
