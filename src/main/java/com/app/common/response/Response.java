package com.app.common.response;

import com.app.common.enums.ResponseCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.ObjectMapper;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class Response implements Serializable {

    @Serial
    private static final long serialVersionUID = 6090126975288080877L;

    Integer status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant date;

    Object data;

    ResponseCode code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<String> errors;

    String path;

    public Instant getDate() {
        return Instant.now();
    }

    public static Response parse(final String json) {
        return new ObjectMapper().readValue(json, Response.class);
    }
}
