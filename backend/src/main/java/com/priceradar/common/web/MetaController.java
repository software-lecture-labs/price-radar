package com.priceradar.common.web;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Minimal public endpoint used to confirm the API is wired up end to end.
 * Deeper status (database, Redis, Elasticsearch, RabbitMQ) lives in Actuator.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Meta", description = "Service metadata")
public class MetaController {

	private final String applicationName;

	public MetaController(@Value("${spring.application.name}") String applicationName) {
		this.applicationName = applicationName;
	}

	@GetMapping("/ping")
	@Operation(summary = "Liveness probe for clients")
	public PingResponse ping() {
		return new PingResponse(applicationName, "v1", Instant.now());
	}

	public record PingResponse(String service, String apiVersion, Instant timestamp) {
	}

}
