package com.app.infrastructure.sse.config;

import com.app.common.constants.SseConstant;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote This component is responsible for initializing the Redis Stream and Consumer Group
 * used for the SSE system. It runs at application startup to ensure that the necessary Redis
 * structures are in place before any events are published or consumed. The initialization logic
 * attempts to create the consumer group with the MKSTREAM option, which will create the stream if
 * it does not already exist. If the group already exists, it catches the BUSYGROUP error and logs it without
 * failing the application, allowing for idempotent initialization.
 */

@RequiredArgsConstructor
@Slf4j
@Component
public class RedisStreamInitializer {

    private final StringRedisTemplate stringRedisTemplate;


    /**
     * Ensures the Redis Stream and Consumer Group exist at startup.
     * XGROUP CREATE ... MKSTREAM creates the stream if absent.
     * If the group already exists we swallow the BUSYGROUP error.
     */
    @PostConstruct
    public void initStreamAndGroup() {

        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(SseConstant.STREAM_NAME, ReadOffset.from("0"), SseConstant.SSE_GROUP_NAME);
            log.info("Redis Stream '{}' and Consumer Group '{}' initialised.",
                    SseConstant.STREAM_NAME, SseConstant.SSE_GROUP_NAME);
        } catch (Exception e) {
            // BUSYGROUP: group already exists — safe to ignore
            if (e.getCause() != null && e.getCause().getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group '{}' already exists — skipping creation.", SseConstant.SSE_GROUP_NAME);
            } else {
                log.error("Failed to initialise Redis Stream/Group: {}", e.getMessage(), e);
                throw e;
            }
        }
    }
}
