package com.app.infrastructure.sse.service.impl;

import com.app.infrastructure.sse.registory.SseSessionRegistry;
import com.app.infrastructure.sse.service.SseDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote Implementation of SseDeliveryService that handles the actual
 * sending of SSE events to connected clients.
 */



@Service
@Slf4j
@RequiredArgsConstructor
public class SseDeliveryServiceImpl implements SseDeliveryService {

    private final SseSessionRegistry sseSessionRegistry;

    @Override
    public boolean deliverToUser(String userSub, String eventId, String eventName, String data) {

        List<SseEmitter> emitters = sseSessionRegistry.getEmitters(userSub);
        if(emitters.isEmpty()) return false;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        boolean delivered = false;
        for(SseEmitter emitter: emitters){
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .id(eventId)
                    .name(eventName)
                    .data(data);
            try{
            emitter.send(eventBuilder);
            delivered = true;
            }catch (Exception e){
                log.debug("Emitter dead for user '{}': {} — marking for removal", userSub, e.getMessage());
                deadEmitters.add(emitter);}
        }
        // Clean up dead emitters
        deadEmitters.forEach(dEmit -> sseSessionRegistry.remove(userSub, dEmit));
        return delivered;
    }

    @Override
    public void sendHeartbeat(String userSub) {
        List<SseEmitter> emitterList = sseSessionRegistry.getEmitters(userSub);
        if(emitterList.isEmpty()) return;
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for(SseEmitter emitter: emitterList){
            try{
                emitter.send(SseEmitter.event().name("heartbeat"));
            }catch (Exception e){
                log.debug("Emitter dead for user '{}': {} — marking for removal", userSub, e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        deadEmitters.forEach(dEmit -> sseSessionRegistry.remove(userSub, dEmit));


    }
}
