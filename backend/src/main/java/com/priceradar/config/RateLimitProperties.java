package com.priceradar.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Attempt limit for the sign-in endpoints (register, login, refresh), counted separately for each endpoint and
 * client IP.
 *
 * @param maxAttempts attempts allowed in one window
 * @param window how long a window lasts, counted from its first attempt
 */
@Validated
@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(@Positive int maxAttempts, @NotNull Duration window) {
}
