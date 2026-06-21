package com.app.common.service;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ApplicationException;
import com.app.common.response.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ResponseService {

    /**
     * Build and return success response with the provided message
     *
     * @param code
     * @param args
     * @return
     */
    ResponseEntity<Response> success(final ResponseCode code, final String... args);

    /**
     * Build and return error response
     *
     * @param code
     * @return
     */
    ResponseEntity<Response> error(final ResponseCode code, String... args);

    /**
     * Build and return error response using {@link ApplicationException}
     *
     * @param applicationException
     * @return
     */
    ResponseEntity<Response> error(final ApplicationException applicationException);

    /**
     * Build and return error response using errors.
     *
     * @param errors
     * @return
     */
    ResponseEntity<Response> error(final List<String> errors);

    /**
     * Build and return success response with the data
     *
     * @param entity
     * @return
     */
    ResponseEntity<Response> data(final Object entity);

    ResponseEntity<Response> data(final Page<?> page);
}
