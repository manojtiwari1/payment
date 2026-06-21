package com.app.infrastructure.sse.service.impl;

import com.app.common.constants.SseConstant;
import com.app.infrastructure.sse.service.SseDeliveryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote StreamConsumerService is responsible for consuming events from the
 * Redis Stream and delivering them to users via SSE.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class StreamConsumerService {

    private final StringRedisTemplate redisTemplate;

    private final SseDeliveryService sseDeliveryService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private ExecutorService executorService;

    @PostConstruct
    public void start(){
        isRunning.set(true);
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        executorService.submit(this::consumeLoop);
        log.info("Stream consumer started: stream='{}', group='{}', consumer='{}'",
                SseConstant.STREAM_NAME, SseConstant.SSE_GROUP_NAME, SseConstant.SSE_CONSUMER_NAME);
    }

    @SuppressWarnings("unchecked")
    private void consumeLoop() {
        Consumer consumer = Consumer.from(SseConstant.SSE_GROUP_NAME, SseConstant.SSE_CONSUMER_NAME);
        StreamReadOptions readOptions = StreamReadOptions.empty()
                .count(SseConstant.SSE_BATCH_SIZE)
                .block(Duration.ofMillis(SseConstant.SSE_BLOCK_TIMEOUT_MS));
        StreamOffset<String> offset = StreamOffset.create(SseConstant.STREAM_NAME, ReadOffset.lastConsumed());
        while(isRunning.get() && !Thread.currentThread().isInterrupted()){
//            List<MapRecord<String, Object, Object>>  records =
//                    redisTemplate.opsForStream().read(MapRecord.class, consumer, readOptions, offset);
            try{
            List<MapRecord<String, Object, Object>> records =
                    redisTemplate.opsForStream().read(consumer, readOptions, offset);

            if (records == null || records.isEmpty()) {
                continue; // Blocking read timed out — loop again
            }

            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }
            }catch (Exception e){
                if (isRunning.get()) {
                    log.error("Error in stream consumer loop: {}", e.getMessage(), e);
                    sleepQuietly(1_000); // Back-off before retrying
                }
            }

        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String eventId = record.getId().getValue();
        Map<Object, Object> fields = record.getValue();

        String userSub = safeGet(fields, "userSub");
        String eventName = safeGet(fields, "eventName");
        String data = safeGet(fields, "data");

        if (userSub == null || eventName == null || data == null) {
            log.warn("Malformed record {}: missing required fields — auto-ACKing to avoid PEL bloat", eventId);
            ack(eventId);
            return;
        }

        log.debug("Processing stream record {} — event='{}', user='{}'", eventId, eventName, userSub);

        boolean delivered = sseDeliveryService.deliverToUser(userSub, eventId, eventName, data);

        if (delivered) {
            // ACK only after confirmed delivery — guarantees at-least-once
            ack(eventId);
        } else {
            // User has no active connection on this instance.
            // Another instance may deliver it. We ACK here to avoid the PEL
            // growing unboundedly for users that simply aren't connected anywhere.
            // The PEL recovery scheduler handles truly-stuck messages.
            log.debug("User '{}' not connected on this instance — ACKing undelivered event {}", userSub, eventId);
            ack(eventId);
        }
    }


    private void ack(String eventId) {
        try {
            redisTemplate.opsForStream().acknowledge(SseConstant.STREAM_NAME, SseConstant.SSE_GROUP_NAME, eventId);
        } catch (Exception e) {
            log.error("Failed to ACK event {}: {}", eventId, e.getMessage());
        }
    }

    private String safeGet(Map<Object, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @PreDestroy
    private void stop(){
        isRunning.set(false);
        if(null != executorService){
            executorService.shutdown();
            try{
                if(!executorService.awaitTermination(5, TimeUnit.SECONDS)){
                    executorService.shutdown();
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        log.info("Stream consumer stopped");
    }
}
