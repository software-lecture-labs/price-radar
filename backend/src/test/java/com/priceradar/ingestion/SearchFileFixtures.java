package com.priceradar.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class SearchFileFixtures {

	static final String FETCHED_AT = "2026-10-22T09:03:59.354Z";

	private final Path repositoryRoot;

	SearchFileFixtures(Path repositoryRoot) {
		this.repositoryRoot = repositoryRoot;
	}

	Path rawDirectory() {
		return repositoryRoot.resolve("data/reefapi/raw");
	}

	SearchFileReader reader() {
		return new SearchFileReader(repositoryRoot, rawDirectory());
	}

	Path write(Marketplace marketplace, String name, List<String> items) {
		var results = marketplace.resultsPointer().substring(1);
		return writeRaw(marketplace.code(), name, """
				{"fetchedAt":"%s","store":"%s","action":"search","httpStatus":200,
				 "response":{"ok":true,"meta":{"record_count":%d},"data":{"%s":[%s]}}}
				""".formatted(FETCHED_AT, marketplace.code(), items.size(), results, String.join(",", items)));
	}

	Path writeRaw(String folder, String name, String content) {
		try {
			var directory = rawDirectory().resolve("2026-10-22").resolve(folder).resolve("search");
			Files.createDirectories(directory);
			return Files.writeString(directory.resolve(name + ".json"), content, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	SearchFile read(Path file) {
		return reader().read(file);
	}

	static String hepsiburada(String sku, String price) {
		return """
				{"sku":"%s","title":"Test ürünü","url":"https://www.hepsiburada.com/%s","price":%s}"""
				.formatted(sku, sku, price);
	}

}
