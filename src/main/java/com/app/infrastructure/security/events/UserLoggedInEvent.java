package com.app.infrastructure.security.events;

import java.time.Instant;

/**
 * Published when a user successfully authenticates.
 *
 * @param subject the user's identifier (user id as string)
 * @param email   the user's e-mail
 * @param at      the time the login occurred
 */
public record UserLoggedInEvent(String subject, String email, Instant at) {
}
