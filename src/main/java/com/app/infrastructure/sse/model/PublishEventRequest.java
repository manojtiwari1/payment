package com.app.infrastructure.sse.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote For Testing Purpose
 */

@Data
public class PublishEventRequest {

    @NotBlank(message="userSub must not be blank")
    private String userSub;

    @NotBlank(message = "eventName must not be blank")
    private String eventName;

    @NotBlank(message = "data must not be blank")
    private String data;
}
