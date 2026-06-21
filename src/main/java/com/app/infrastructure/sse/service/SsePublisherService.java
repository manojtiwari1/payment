package com.app.infrastructure.sse.service;

import com.app.infrastructure.sse.model.SseEvent;

public interface SsePublisherService {

    String publish(String userSub, String eventName, String data);

    String publish(SseEvent event);
}
