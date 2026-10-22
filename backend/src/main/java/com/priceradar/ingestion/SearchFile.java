package com.priceradar.ingestion;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A saved ReefAPI search response that passed validation.
 *
 * @param path repository-relative path, e.g. {@code data/reefapi/raw/2026-10-22/n11/search/x.json};
 *        together with {@code sha256} it identifies the file in {@code source_files}
 * @param sha256 hash of the file's bytes as saved
 * @param rawJson the complete saved document, kept for provenance
 * @param fetchedAt when the response was received; becomes every observation's time
 */
public record SearchFile(
		String path,
		String sha256,
		String rawJson,
		Marketplace marketplace,
		OffsetDateTime fetchedAt,
		List<SearchItem> items) {

	public SearchFile {
		items = List.copyOf(items);
	}

}
