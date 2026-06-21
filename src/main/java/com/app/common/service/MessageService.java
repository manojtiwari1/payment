package com.app.common.service;

import com.app.common.enums.ResponseCode;

public interface MessageService {

    /**
     * This method is responsible for get the message from the message bundle using
     * provided code.
     *
     * @param code
     * @param params
     * @return String
     */
    String getMessage(final ResponseCode code, final String... params);
}
