package com.priceradar.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code app.security.*}.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(@NotNull @Valid Jwt jwt, @NotNull @Valid Cors cors) {

	public record Jwt(
			@NotBlank String secret,
			@NotBlank String issuer,
			@NotNull Duration accessTokenTtl,
			@NotNull Duration refreshTokenTtl) {
	}

	public record Cors(
			@NotEmpty List<String> allowedOrigins,
			@NotEmpty List<String> allowedMethods,
			@NotEmpty List<String> allowedHeaders,
			boolean allowCredentials,
			long maxAge) {
	}

}
