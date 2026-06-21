package com.app.modules.payment.service;

import com.app.modules.payment.dto.PspChargeRequest;
import com.app.modules.payment.dto.PspResult;
import com.app.modules.payment.enums.Psp;

/**
 * Abstraction over the payment providers. Implemented by a simulator for the assignment; a real
 * implementation would issue HTTP calls per provider.
 */
public interface PspGateway {

    PspResult charge(Psp psp, PspChargeRequest request);
}
