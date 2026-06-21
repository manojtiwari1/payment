package com.app.modules.payment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Programmatic Resilience4j registries (no Spring Boot starter — avoids the Boot-4 autoconfig gap).
 *
 * <ul>
 *   <li><b>Circuit breaker</b> — one instance per PSP (created lazily by name). Opens at a 50%
 *       failure rate and stays open for 30s before probing again.</li>
 *   <li><b>Rate limiter</b> — one instance per merchant: 100 requests/minute, no waiting.</li>
 * </ul>
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${payment.resilience.cb.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${payment.resilience.cb.open-state-seconds:30}") long openStateSeconds,
            @Value("${payment.resilience.cb.sliding-window-size:10}") int slidingWindowSize,
            @Value("${payment.resilience.cb.minimum-calls:5}") int minimumCalls) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(openStateSeconds))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumCalls)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(
            @Value("${payment.resilience.rate-limit.per-minute:100}") int limitForPeriod) {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO) // never block — reject immediately when over the limit
                .build();
        return RateLimiterRegistry.of(config);
    }
}
