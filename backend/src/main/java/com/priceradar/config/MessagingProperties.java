package com.priceradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Binds {@code app.messaging.*}: the exchange the application publishes to and
 * the dead-letter pair that parks undeliverable messages.
 */
@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
		@NotBlank String exchange,
		@NotBlank String deadLetterExchange,
		@NotBlank String deadLetterQueue) {
}
