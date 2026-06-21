package com.app.common.exception;

import com.app.common.enums.ResponseCode;
import lombok.Getter;

import java.io.Serial;


/**
 * @author Manoj Tiwari
 * @since 04-04-2026
 * @implSpec This is a custom exception class that extends RuntimeException.
 * It is designed to handle application-specific exceptions and provide additional information such as an
 * error code, fields related to the error, and the original exception if applicable. The class includes
 * constructors for creating exceptions with a specific response code and message, as well as a constructor
 * for wrapping an existing exception.
 */

@Getter
public class ApplicationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7365547609387611425L;

    private final ResponseCode errorCode;

    private String[] fields;

    private Throwable exception;

    public ApplicationException(final ResponseCode code, final String message, final String... fields) {
        super(message);
        this.errorCode = code;
        if (fields.length == 0) {
            this.fields = new String[]{message};
        } else {
            this.fields = fields;

        }
    }

    public ApplicationException(Throwable exception) {
        super(exception.getLocalizedMessage());
        this.errorCode = ResponseCode.INTERNAL_ERROR;
        this.exception = exception;
    }

}
