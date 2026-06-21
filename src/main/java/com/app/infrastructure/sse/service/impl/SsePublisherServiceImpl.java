package com.app.infrastructure.sse.service.impl;

import com.app.common.constants.SseConstant;
import com.app.infrastructure.sse.model.SseEvent;
import com.app.infrastructure.sse.service.SsePublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote Implementation of SsePublisherService that handles publishing SSE events to a Redis stream.
 * Each event is stored as a MapRecord in the Redis stream with fields for userSub, eventName, data,
 * and timestamp. After publishing, the stream is trimmed to maintain a maximum length defined by
 * SseConstant.SSE_MAX_STREAM_LENGTH.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SsePublisherServiceImpl implements SsePublisherService {

    private final StringRedisTemplate redisTemplate;


    @Override
    public String publish(String userSub, String eventName, String data) {
        Map<String, String> fieldsMap = Map.of(
                "userSub", userSub,
                "eventName", eventName,
                "data", data,
                "timestamp", Instant.now().toString()
        );

        MapRecord<String, String, String> record =
                MapRecord.create(SseConstant.STREAM_NAME, fieldsMap);

        RecordId recordId = redisTemplate.opsForStream().add(record);

        if(null == recordId){
            throw new IllegalStateException("XADD returned null — Redis write failed");
        }
        String eventId = recordId.getValue();
        log.debug("SSE publish eventId={} and eventName {} and userSub {}", eventId, eventName, userSub);

        redisTemplate.opsForStream().trim(SseConstant.STREAM_NAME,
                SseConstant.SSE_MAX_STREAM_LENGTH, true);

        return eventId;

    }


    @Override
    public String publish(SseEvent event) {
        return publish(event.getUserSub(), event.getEventName(), event.getData());
    }
}
