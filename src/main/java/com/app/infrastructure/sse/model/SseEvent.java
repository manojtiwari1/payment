package com.app.infrastructure.sse.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SseEvent {

    /** Redis stream entry ID — populated after XADD. */
    private String eventId;

    /** Keycloak subject (UUID) of the target user. */
    private String userSub;

    /** SSE named-event type, e.g. "NOTIFICATION", "ORDER_UPDATED". */
    private String eventName;

    /** Arbitrary JSON payload (serialised as a String in the stream). */
    private String data;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
