package com.app.infrastructure.sse.scheduler;

import com.app.common.constants.SseConstant;
import com.app.infrastructure.sse.registory.SseSessionRegistry;
import com.app.infrastructure.sse.service.SseDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote This class implements two scheduled tasks for the SSE system:
 * 1. sendHeartbeats() - runs every 30 seconds to send heartbeat events to all connected
 * users to keep their SSE connections alive and detect disconnections.
 * 2. reclaimStalePelEntries() - runs every 60 seconds to check the Redis Stream's Pending Entries List (PEL)
 * for any messages that have been pending for longer than a defined idle interval.
 * If such messages are found, it attempts to reclaim them and redeliver to the user.
 * This helps ensure that events are not lost if a consumer fails to process them in a timely manner.
 * After successful redelivery, the message is acknowledged to remove it from the PEL.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

    private final StringRedisTemplate stringRedisTemplate;

    private final SseSessionRegistry sseSessionRegistry;

    private final SseDeliveryService sseDeliveryService;

    public void sendHeartbeats(){
        int size = sseSessionRegistry.connectedUsers().size();
        if(size == 0) return;
        log.debug("Sending heartbeats to user(s)");
        sseSessionRegistry.connectedUsers().forEach(sseDeliveryService::sendHeartbeat);
    }


    // PEL recovery — every 60 seconds

    @Scheduled(fixedDelay = 60_000)
    public void reclaimStalePelEntries(){

        PendingMessagesSummary pendingMessagesSummary = stringRedisTemplate.opsForStream()
                .pending(SseConstant.STREAM_NAME, SseConstant.SSE_GROUP_NAME);

        if(null == pendingMessagesSummary ||
                pendingMessagesSummary.getTotalPendingMessages() == 0) return;

        log.debug("Reclaiming stale pel entries: totalPending={}, consumers={}",
                pendingMessagesSummary.getTotalPendingMessages(),
                SseConstant.SSE_GROUP_NAME);

        PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
                .pending(SseConstant.STREAM_NAME,
                        Consumer.from(SseConstant.SSE_GROUP_NAME, SseConstant.SSE_CONSUMER_NAME),
                        Range.unbounded(),
                        SseConstant.SSE_BATCH_SIZE);

        if(null == pendingMessages) return;

        for(PendingMessage pm: pendingMessages){
            long idleMs = pm.getElapsedTimeSinceLastDelivery().toMillis();
            if(idleMs >= SseConstant.SSE_MIN_IDLE_INTERVAL){
                reclaimAndProcess(pm.getIdAsString());
            }
        }

    }


    private void reclaimAndProcess(String entryId){

        List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream().claim(
                SseConstant.STREAM_NAME,
                SseConstant.SSE_GROUP_NAME,
                SseConstant.SSE_CONSUMER_NAME,
                Duration.ofMillis(SseConstant.SSE_MIN_IDLE_INTERVAL),
                RecordId.of(entryId)
        );

        if(null == claimed || claimed.isEmpty()) return;

        for(MapRecord<String, Object, Object> record: claimed){
            String userSub = String.valueOf(record.getValue().get("userSub"));
            String eventName = String.valueOf(record.getValue().get("eventName"));
            String data = String.valueOf(record.getValue().get("data"));
            String eventId = record.getId().getValue();

            log.info("PEL recovery redelivering event {} to user {}", eventId, userSub);
            boolean isDelivered = sseDeliveryService.deliverToUser(userSub, eventName, data, eventId);
            if(isDelivered){
                stringRedisTemplate.opsForStream().acknowledge(SseConstant.STREAM_NAME,
                        SseConstant.SSE_GROUP_NAME, eventId);
            }
        }

    }




}
