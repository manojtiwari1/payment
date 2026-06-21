package com.app.modules.payment.config;

import com.app.modules.payment.enums.Psp;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-PSP simulated behavior, bound from {@code payment.psp.*}.
 *
 * <p>Example (application-dev.properties):
 * <pre>
 * payment.psp.providers.PSP_A.outcome=TRANSIENT_FAILURE
 * payment.psp.providers.PSP_B.outcome=SUCCESS
 * payment.psp.providers.PSP_B.latency-ms=20
 * </pre>
 */
@Getter
@ConfigurationProperties(prefix = "payment.psp")
public class PspSimulationProperties {

    /** Keyed by {@link Psp} name (e.g. "PSP_A"). Missing entries default to SUCCESS. */
    private Map<String, PspBehavior> providers = new HashMap<>();

    public void setProviders(Map<String, PspBehavior> providers) {
        this.providers = providers;
    }

    /** Case-insensitive lookup (relaxed-binding can alter map-key case); defaults to SUCCESS. */
    public PspBehavior behaviorFor(Psp psp) {
        PspBehavior exact = providers.get(psp.name());
        if (exact != null) {
            return exact;
        }
        return providers.entrySet().stream()
                .filter(e -> psp.name().equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(PspBehavior::new);
    }
}
