package com.app.modules.payment.config;

import com.app.modules.payment.enums.Psp;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable PSP routing order, bound from {@code payment.routing.*}.
 *
 * <pre>
 * payment.routing.default-providers=PSP_A,PSP_B,PSP_C
 * payment.routing.merchants.M123=PSP_A,PSP_B
 * </pre>
 */
@Getter
@ConfigurationProperties(prefix = "payment.routing")
public class RoutingProperties {

    /** Comma-separated fallback PSP order used when a merchant has no specific configuration. */
    private String defaultProviders = "PSP_A,PSP_B,PSP_C";

    /** Per-merchant comma-separated PSP order, keyed by merchant code. */
    private Map<String, String> merchants = new HashMap<>();

    public void setDefaultProviders(String defaultProviders) {
        this.defaultProviders = defaultProviders;
    }

    public void setMerchants(Map<String, String> merchants) {
        this.merchants = merchants;
    }

    /** Resolves the ordered PSP list for a merchant, falling back to the default order. */
    public List<Psp> resolve(String merchantCode) {
        String csv = lookupMerchant(merchantCode);
        if (csv == null || csv.isBlank()) {
            csv = defaultProviders;
        }
        List<Psp> order = new ArrayList<>();
        for (String token : Arrays.stream(csv.split(",")).map(String::trim).toList()) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                order.add(Psp.valueOf(token));
            } catch (IllegalArgumentException ignored) {
                // skip unknown PSP tokens
            }
        }
        return order;
    }

    /** Case-insensitive merchant lookup (relaxed-binding can alter map-key case). */
    private String lookupMerchant(String merchantCode) {
        if (merchantCode == null) {
            return null;
        }
        String exact = merchants.get(merchantCode);
        if (exact != null) {
            return exact;
        }
        return merchants.entrySet().stream()
                .filter(e -> merchantCode.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
