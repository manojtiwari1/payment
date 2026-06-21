package com.app.infrastructure.sse.service.impl;

import com.app.common.constants.SseConstant;
import com.app.infrastructure.sse.service.SseReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote Implementation of SseReplayService that handles replaying missed SSE events to clients
 * based on the lastEventId they provide. It reads from the Redis stream starting from the event ID
 * immediately after lastEventId and sends any relevant events to the client through the SseEmitter.
 * The incrementId method is used to calculate the next event ID to read from the stream, ensuring that the
 * replay starts from the correct position. If no events are found or if an error occurs while sending an event,
 * appropriate debug logs are recorded.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class SseReplayServiceImpl implements SseReplayService {

    private final StringRedisTemplate  stringRedisTemplate;


    @Override
    public void replay(String userSub, String lastEventId, SseEmitter emitter) {
        if(lastEventId == null || lastEventId.isEmpty()) return;

        log.debug("Replaying events for userSub={}, fromId={}", userSub, lastEventId);

        String fromId = incrementId(lastEventId);
        Range<String> range = Range.rightUnbounded(Range.Bound.inclusive(fromId));

        List<MapRecord<String, Object, Object>> records =
                stringRedisTemplate.opsForStream().range(SseConstant.STREAM_NAME, range);

        if(null == records || records.isEmpty()) {
            log.debug("No missed events for user '{}' since '{}'", userSub, lastEventId);
            return;
        }

        int replayCount = 0;
        for(MapRecord<String, Object, Object> record : records){
            Map<Object, Object> fields =record.getValue();
            String recordUserSubs = String.valueOf(fields.get("userSub"));
            if(!userSub.equals(recordUserSubs)) continue;

            String eventId = record.getId().getValue();
            String eventName =String.valueOf(fields.get("eventName"));
            String data = String.valueOf(fields.get("data"));

            try{
                emitter.send(SseEmitter.event()
                        .id(eventId)
                        .name(eventName)
                        .data(data));
                replayCount++;
            }catch (IOException e){
                log.warn("Failed to send event '{}' to '{}'", eventId, userSub);
                break;
            }
        }
        log.debug("Replayed {} events to user '{}' since '{}'", replayCount, userSub, lastEventId);
    }

    @Override
    public String incrementId(String id) {
        String[] parts = id.split("-");
        if(parts.length == 2){
            try{
                long seq = Long.parseLong(parts[1]);
                return parts[0]+ "-" + (seq + 1);
            }catch (NumberFormatException e){}
        }
        return id+ "-0";
    }
}
