package com.app.infrastructure.sse.service;

public interface SseDeliveryService {


    public boolean deliverToUser(String userSub, String eventId, String eventName, String data);

    public void sendHeartbeat(String userSub);
}
