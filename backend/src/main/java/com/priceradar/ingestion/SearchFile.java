package com.priceradar.ingestion;

import java.time.OffsetDateTime;
import java.util.List;

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
