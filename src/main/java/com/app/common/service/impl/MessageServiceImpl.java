package com.app.common.service.impl;

import com.app.common.enums.ResponseCode;
import com.app.common.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * @author Manoj Tiwari
 * @since 08-04-2026
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSource messageSource;

    @Override
    public String getMessage(final ResponseCode code, final String[] args) {
        log.info("code name {}", code.name());

        return messageSource.getMessage(
                code.name(),
                args,
                "An unknown error has occurred!",
                Locale.getDefault()
        );
    }
}
