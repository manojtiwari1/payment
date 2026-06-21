package com.app.infrastructure.sse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseReplayService {

    void replay(String userSub, String lastEventId, SseEmitter emitter);

    String incrementId(String id);

}
