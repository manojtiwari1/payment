package com.app.common.exception;

import com.app.common.enums.ResponseCode;
import com.app.common.exception.ResourceNotFoundException;
import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.naming.SizeLimitExceededException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Centralised exception handler for all controllers.
 *
 * <p>Key fixes vs. original:
 * <ul>
 *   <li>Imports {@code org.springframework.security.access.AccessDeniedException}
 *       instead of {@code java.nio.file.AccessDeniedException} — the original
 *       import caused Spring Security 403 exceptions to fall through to the
 *       catch-all 500 handler.</li>
 *   <li>Maps {@code AccessDeniedException} to HTTP 403, not 400.</li>
 *   <li>Removed all commented-out handlers.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends BaseResponse {

    @Value("${spring.servlet.multipart.max-file-size:15MB}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:15MB}")
    private String maxRequestSize;

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleThrowable(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Throwable ex) throws IOException {
        log.error("Unhandled error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (isSseRequest(request)) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("event: error\n");
            response.getWriter().write("data: {\"status\":500,\"message\":\"Internal Server Error\"}\n\n");
            response.getWriter().flush();
            return null;
        }

        return error(Collections.singletonList("An unexpected error occurred. Please try again later."));
    }

    /**
     * Handles Spring Security's {@link AccessDeniedException}.
     *
     * <p>Previous code imported {@code java.nio.file.AccessDeniedException} which
     * is a completely different class — Spring Security 403 responses were never
     * caught here and fell through to the 500 handler.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return error(ResponseCode.ACCESS_DENIED, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return error(ResponseCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Response> handleApplicationException(ApplicationException ex) {
        log.warn("Application exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return error(ex.getErrorCode(), ex.getFields());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        log.warn("Validation failed: {}", errors);
        return error(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return error(ResponseCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Response> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return error(ResponseCode.INVALID_PARAMETER, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return error(ResponseCode.DUPLICATE, "A record with the same value already exists.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Response> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMethod());
        return error(ResponseCode.BAD_REQUEST, "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': value={}", ex.getName(), ex.getValue());
        return error(ResponseCode.BAD_REQUEST, "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Response> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return error(ResponseCode.FILE_SIZE_EXCEED, maxFileSize);
    }

    @ExceptionHandler(SizeLimitExceededException.class)
    public ResponseEntity<Response> handleSizeLimit(SizeLimitExceededException ex) {
        log.warn("Size limit exceeded: {}", ex.getMessage());
        return error(ResponseCode.FILE_SIZE_EXCEED, maxRequestSize);
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
