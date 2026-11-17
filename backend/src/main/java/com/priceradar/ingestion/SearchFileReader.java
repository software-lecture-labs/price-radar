package com.priceradar.ingestion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;

import com.priceradar.ingestion.Marketplace.Field;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class SearchFileReader {

	private static final JsonMapper JSON = JsonMapper.builder()
			// Preserve source prices exactly before checking database precision.
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.build();

	private static final String SEARCH_ACTION = "search";
	private static final int HTTP_OK = 200;
	// PostgreSQL timestamps have microsecond precision.
	private static final int NANOS_PER_MICRO = 1_000;

	private final Path repositoryRoot;
	private final Path rawDirectory;

	public SearchFileReader(Path repositoryRoot, Path rawDirectory) {
		this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
		this.rawDirectory = rawDirectory.toAbsolutePath().normalize();
	}

	public SearchFile read(Path file) {
		var location = locate(file);
		try {
			var bytes = Files.readAllBytes(location.file());
			return parse(location, sha256(bytes), decodeUtf8(bytes));
		}
		catch (IOException ex) {
			throw new InvalidSearchFileException(location.storedPath() + ": cannot be read", ex);
		}
		catch (InvalidSearchFileException ex) {
			throw new InvalidSearchFileException(location.storedPath() + ": " + ex.getMessage(), ex);
		}
	}

	private record Location(Path file, String storedPath, Marketplace marketplace) {
	}

	private Location locate(Path file) {
		var absolute = file.toAbsolutePath().normalize();
		if (!absolute.startsWith(rawDirectory) || !Files.isRegularFile(absolute)) {
			throw new InvalidSearchFileException(file + ": not a file under " + rawDirectory);
		}

		var relative = rawDirectory.relativize(absolute);
		if (relative.getNameCount() != 4 || !SEARCH_ACTION.equals(relative.getName(2).toString())
				|| !relative.getFileName().toString().endsWith(".json")) {
			throw new InvalidSearchFileException(file + ": expected <label>/<marketplace>/search/<name>.json");
		}

		var folder = relative.getName(1).toString();
		var marketplace = Marketplace.fromCode(folder)
				.orElseThrow(() -> new InvalidSearchFileException(file + ": unknown marketplace folder " + folder));
		var storedPath = repositoryRoot.relativize(absolute).toString().replace('\\', '/');
		try {
			var realFile = absolute.toRealPath();
			if (!realFile.startsWith(rawDirectory.toRealPath())) {
				throw new InvalidSearchFileException(file + ": not a file under " + rawDirectory);
			}
			return new Location(realFile, storedPath, marketplace);
		}
		catch (IOException ex) {
			throw new InvalidSearchFileException(storedPath + ": cannot be read", ex);
		}
	}

	private SearchFile parse(Location location, String sha256, String document) {
		var envelope = readTree(document);
		require(envelope.isObject(), "the document must be a JSON object");
		PostgresText.requireStorable(envelope);

		require(location.marketplace().code().equals(envelope.path("store").asString(null)),
				"store does not match the folder it was saved in");
		require(SEARCH_ACTION.equals(envelope.path("action").asString(null)), "only search responses are supported");
		require(envelope.path("httpStatus").asInt(0) == HTTP_OK, "httpStatus must be 200");

		var response = envelope.path("response");
		require(response.path("ok").asBoolean(false), "response.ok must be true");

		var fetchedAt = fetchedAt(envelope);
		var results = response.path("data").at(location.marketplace().resultsPointer());
		require(results.isArray(), "response.data" + location.marketplace().resultsPointer() + " must be a list");
		require(response.path("meta").path("record_count").asInt(-1) == results.size(),
				"response.meta.record_count does not match the number of results");

		var items = new ArrayList<SearchItem>();
		for (var index = 0; index < results.size(); index++) {
			try {
				items.add(item(location.marketplace(), results.get(index)));
			}
			catch (InvalidSearchFileException ex) {
				throw new InvalidSearchFileException("result " + index + ": " + ex.getMessage(), ex);
			}
		}
		return new SearchFile(location.storedPath(), sha256, document, location.marketplace(), fetchedAt, items);
	}

	private static SearchItem item(Marketplace marketplace, JsonNode node) {
		require(node.isObject(), "must be a JSON object");
		var values = new JsonValues(node, marketplace);

		var listing = new SearchItem.Listing(
				values.identifier(Field.GROUP_ID),
				values.requiredText(Field.TITLE),
				values.text(Field.BRAND),
				values.text(Field.CATEGORY),
				values.textList(Field.CATEGORY_PATH),
				values.text(Field.IMAGE_URL),
				values.identifier(Field.BARCODE),
				values.text(Field.MPN));

		var rating = values.decimal(Field.RATING);
		var ratingScale = values.ratingScale(Field.RATING_SCALE);
		require(rating == null || ratingScale == null || rating.compareTo(ratingScale) <= 0,
				"rating is higher than rating_scale");

		var offer = new SearchItem.Offer(
				values.requiredUrl(Field.URL),
				values.requiredMoney(Field.PRICE),
				values.currency(Field.CURRENCY),
				values.money(Field.LIST_PRICE),
				values.money(Field.MEMBER_PRICE),
				values.identifier(Field.SELLER_ID),
				values.text(Field.SELLER_NAME),
				values.flag(Field.SELLER_IS_STORE),
				values.flag(Field.IN_STOCK),
				values.flag(Field.FREE_SHIPPING),
				values.flag(Field.SPONSORED),
				rating,
				ratingScale,
				values.count(Field.REVIEW_COUNT));

		return new SearchItem(values.requiredIdentifier(Field.ID), listing, offer, JSON.writeValueAsString(node));
	}

	private static OffsetDateTime fetchedAt(JsonNode envelope) {
		try {
			var fetchedAt = OffsetDateTime.parse(envelope.path("fetchedAt").asString(""));
			require(fetchedAt.getNano() % NANOS_PER_MICRO == 0, "fetchedAt is more precise than PostgreSQL can store");
			return fetchedAt;
		}
		catch (DateTimeParseException ex) {
			throw new InvalidSearchFileException("fetchedAt must be an ISO-8601 timestamp", ex);
		}
	}

	private static JsonNode readTree(String document) {
		try {
			return JSON.readTree(document);
		}
		catch (JacksonException ex) {
			throw new InvalidSearchFileException("not valid JSON: " + ex.getOriginalMessage(), ex);
		}
	}

	private static String decodeUtf8(byte[] bytes) {
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
		}
		catch (CharacterCodingException ex) {
			throw new InvalidSearchFileException("not valid UTF-8", ex);
		}
	}

	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Every Java runtime provides SHA-256", ex);
		}
	}

	private static void require(boolean condition, String problem) {
		if (!condition) {
			throw new InvalidSearchFileException(problem);
		}
	}

}
