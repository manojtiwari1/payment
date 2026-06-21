package com.app.infrastructure.sse.controller;

import com.app.infrastructure.sse.model.PublishEventRequest;
import com.app.infrastructure.sse.service.SsePublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Manoj Tiwari
 * @since 2024-06-15
 * @implSpec Controller for publishing SSE events to connected clients.  Provides two endpoints:
 */


@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventPublisherController {

    private final SsePublisherService ssePublisherService;


    @PostMapping("/publish")
    public ResponseEntity<Map<String, String>> publish(@Valid @RequestBody PublishEventRequest request){
        String eventId = ssePublisherService.publish(
                request.getUserSub(),
                request.getEventName(),
                request.getData()
        );
        log.info("Published event: userSub='{}', eventName='{}', eventId='{}'", request.getUserSub(), request.getEventName(), eventId);
        return ResponseEntity.ok(Map.of("eventId", eventId));
    }


}
