package com.priceradar.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Imports saved search files when the application starts with the
 * {@code import} profile, then stops the application.
 *
 * <p>Arguments:
 * <ul>
 * <li>{@code --source=<path>}: a file or a folder under the raw directory, relative to it;
 * may be repeated. Without it, everything under the raw directory is imported.</li>
 * <li>{@code --commit}: write to the database. Without it, nothing is written and
 * the run only reports what would happen.</li>
 * </ul>
 *
 * <p>Every file is read and validated before the first one is written, and each
 * file is its own transaction. A failed run can be repeated: files already
 * imported are skipped.
 */
@Component
@Profile("import")
class SearchImportRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SearchImportRunner.class);

	private static final String SOURCE_OPTION = "source";
	private static final String COMMIT_OPTION = "commit";

	private final IngestionProperties properties;
	private final SearchFileImporter importer;
	private final ConfigurableApplicationContext context;

	SearchImportRunner(IngestionProperties properties, SearchFileImporter importer,
			ConfigurableApplicationContext context) {
		this.properties = properties;
		this.importer = importer;
		this.context = context;
	}

	@Override
	public void run(ApplicationArguments arguments) {
		var exitCode = 0;
		try {
			importAll(arguments.getOptionValues(SOURCE_OPTION), arguments.containsOption(COMMIT_OPTION));
		}
		catch (RuntimeException ex) {
			log.error("Import stopped: {}", ex.getMessage());
			exitCode = 1;
		}
		var code = exitCode;
		System.exit(SpringApplication.exit(context, () -> code));
	}

	private void importAll(List<String> sources, boolean commit) {
		var reader = new SearchFileReader(properties.repositoryRootPath(), properties.rawDirectoryPath());
		var files = findFiles(sources == null ? List.of("") : sources).stream().map(reader::read).toList();
		log.info("{} files are valid; {}", files.size(), commit ? "importing" : "dry run, nothing is written");

		for (var file : files) {
			var result = commit ? importer.importFile(file) : importer.checkFile(file);
			log.info("{} {}: {} observations, {} new products", result.status(), result.path(),
					result.observations(), result.newProducts());
		}
		log.info("Done: {} files", files.size());
	}

	/**
	 * Every saved search response at or under the given sources, in a stable order.
	 * Other responses the fetch tool saves (product details) live in other folders.
	 */
	private List<Path> findFiles(List<String> sources) {
		return sources.stream()
				.map(properties.rawDirectoryPath()::resolve)
				.flatMap(SearchImportRunner::jsonFiles)
				.distinct()
				.sorted()
				.toList();
	}

	private static Stream<Path> jsonFiles(Path source) {
		if (Files.isRegularFile(source)) {
			return Stream.of(source);
		}
		try (var walk = Files.walk(source)) {
			return walk.filter(Files::isRegularFile)
					.filter(SearchImportRunner::isSearchResponse)
					.toList()
					.stream();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot list " + source, ex);
		}
	}

	private static boolean isSearchResponse(Path file) {
		var folder = file.getParent().getFileName().toString();
		return "search".equals(folder) && file.getFileName().toString().endsWith(".json");
	}

}
