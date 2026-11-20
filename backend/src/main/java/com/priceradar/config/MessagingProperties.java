package com.priceradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
		@NotBlank String eventsTopic,
		@NotBlank String deadLetterTopic,
		@Positive int partitions,
		@Positive short replicationFactor) {
}
