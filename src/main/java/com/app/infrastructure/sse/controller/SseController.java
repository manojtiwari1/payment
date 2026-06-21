package com.app.infrastructure.sse.controller;

import com.app.common.constants.SseConstant;
import com.app.infrastructure.security.userdetails.UserPrincipal;
import com.app.infrastructure.sse.registory.SseSessionRegistry;
import com.app.infrastructure.sse.service.SseReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Manoj Tiwari
 * @since 2024-06-15
 * @implNote Controller for handling Server-Sent Events (SSE) subscriptions.
 */

@Slf4j
@RestController
@RequestMapping("/api/auth/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseSessionRegistry sseSessionRegistry;

    private final SseReplayService sseReplayService;


    @GetMapping(value = "/subscribe",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
            ){

        String userSub = String.valueOf(principal.getId());
        log.info("SSE subscribe: user='{}', lastEventId='{}'",  userSub, lastEventId);
        SseEmitter emitter = new SseEmitter(SseConstant.SSE_TIMEOUT_MS);

        sseSessionRegistry.register(userSub, emitter);

        if(null != lastEventId && !lastEventId.isBlank()){
            sseReplayService.replay(userSub, lastEventId, emitter);
        }

        return emitter;

    }
}
