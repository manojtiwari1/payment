package com.app.modules.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the payment module: enables {@code @Async} (after-commit routing) and {@code @Scheduled}
 * (reconciliation), and binds the routing/PSP-simulation configuration properties.
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({RoutingProperties.class, PspSimulationProperties.class})
public class PaymentModuleConfig {
}
