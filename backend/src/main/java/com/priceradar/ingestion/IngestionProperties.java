package com.priceradar.ingestion;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Binds {@code app.ingestion.*}: where the saved ReefAPI responses are. Bound as
 * text because Spring reads a {@link Path} property as a resource location, which
 * does not allow relative paths such as {@code ..}.
 *
 * @param repositoryRoot stored source paths are relative to this directory
 * @param rawDirectory where {@code tools/reefapi/fetch.mjs} saves responses
 */
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
