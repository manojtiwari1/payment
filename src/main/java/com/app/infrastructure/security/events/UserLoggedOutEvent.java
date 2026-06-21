package com.app.infrastructure.security.events;

import java.time.Instant;

/**
 * Published when a user logs out (access token blacklisted).
 *
 * @param subject the user's identifier (user id as string, or JWT subject)
 * @param at      the time the logout occurred
 */
public record UserLoggedOutEvent(String subject, Instant at) {
}
