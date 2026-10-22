package com.priceradar.ingestion;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

// String binding permits relative paths such as ".."; Spring treats Path properties as resource locations.
@Validated
@ConfigurationProperties(prefix = "app.ingestion")
public record IngestionProperties(@NotBlank String repositoryRoot, @NotBlank String rawDirectory) {

	public Path repositoryRootPath() {
		return Path.of(repositoryRoot);
	}

	public Path rawDirectoryPath() {
		return Path.of(rawDirectory);
	}

}
