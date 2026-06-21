package com.app.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse {

    private Long totalElements;

    private Integer totalPages;

    private Integer size;

    private Integer number;

    private Integer numberOfElements;

    private List<?> content;
}
