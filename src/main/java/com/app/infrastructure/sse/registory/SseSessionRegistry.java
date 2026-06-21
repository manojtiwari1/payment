package com.app.infrastructure.sse.registory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote This class manages SSE sessions for users. It allows registering new SseEmitters for a user,
 * removing them when they complete or error out, and retrieving active emitters for a user.
 * It uses thread-safe collections to handle concurrent access in a multi-threaded environment.
 * The registry also provides utility methods to get the count of active connections for a user and to
 * list all connected users on this instance.
 */

@Slf4j
@Component
public class SseSessionRegistry {

    private final Map<String, List<SseEmitter>> sessions = new ConcurrentHashMap<>();

    public SseEmitter register(String userSub, SseEmitter emitter){
        sessions.computeIfAbsent(userSub, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("Registered emitter {} for user sub {}", emitter, userSub);
        Runnable cleanup = ()-> remove(userSub, emitter);
        emitter.onCompletion(cleanup);
        emitter.onError(ex ->{
            log.debug("Emitter {} for user sub {} failed", emitter, userSub, ex);
            cleanup.run();
        });
        return emitter;
    }

    public void remove(String userSub, SseEmitter emitter){
        List<SseEmitter> list = sessions.get(userSub);
        if(list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) {
            sessions.remove(userSub);
        }
        log.debug("Removed for user {}  Remaining: {}", emitter, countFor(userSub));
    }


    public int countFor(String userSub){
        List<SseEmitter> list = sessions.get(userSub);
        return list == null ? 0 : list.size();
    }


    /**
     * Return a snapshot of all emitters for the given user.
     * Returns an empty list if the user has no active connections on this instance.
     */
    public List<SseEmitter> getEmitters(String userSub) {
        List<SseEmitter> list = sessions.get(userSub);
        return list == null ? Collections.emptyList() : List.copyOf(list);
    }


    /**
     * Return all userSubs that currently have at least one active emitter on this instance.
     */
    public Set<String> connectedUsers() {
        return Collections.unmodifiableSet(sessions.keySet());
    }



    public int totalConnections() {
        return sessions.values().stream().mapToInt(List::size).sum();
    }


}
