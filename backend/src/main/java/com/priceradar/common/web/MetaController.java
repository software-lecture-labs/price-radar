package com.priceradar.common.web;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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

	@GetMapping("/hello")
	@Operation(summary = "Greeting used to verify the API from a browser")
	public HelloResponse hello() {
		return new HelloResponse("Hello World", applicationName);
	}

	public record PingResponse(String service, String apiVersion, Instant timestamp) {
	}

	public record HelloResponse(String message, String service) {
	}

}
