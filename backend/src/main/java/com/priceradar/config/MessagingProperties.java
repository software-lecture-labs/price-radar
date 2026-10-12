package com.priceradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Binds {@code app.messaging.*}: the topic the application publishes events to,
 * the dead-letter topic that parks records listeners could not process, and the
 * partition and replica counts both are created with.
 */
@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
		@NotBlank String eventsTopic,
		@NotBlank String deadLetterTopic,
		@Positive int partitions,
		@Positive short replicationFactor) {
}
