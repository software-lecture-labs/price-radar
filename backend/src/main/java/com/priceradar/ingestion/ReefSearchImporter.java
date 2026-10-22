package com.priceradar.ingestion;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Validates and imports one saved Reef search response; never updates existing catalogue entries. */
public final class ReefSearchImporter {

	private static final JsonMapper JSON = JsonMapper.builder()
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
			.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
	private static final Map<String, List<String>> STORES = Map.of(
			"hepsiburada", List.of("Hepsiburada", "https://www.hepsiburada.com"),
			"n11", List.of("N11", "https://www.n11.com"),
			"teknosa", List.of("Teknosa", "https://www.teknosa.com"),
			"trendyol", List.of("Trendyol", "https://www.trendyol.com"));
	private static final List<String> CATALOG = List.of("external_group_id", "title", "brand", "category",
			"category_path", "image_url", "barcode", "mpn");
	private static final List<String> OBSERVATION = List.of("source_url", "price", "currency", "list_price",
			"member_price", "seller_external_id", "seller_name", "seller_is_store", "in_stock",
			"free_shipping", "sponsored", "rating", "rating_scale", "review_count");
	private static final BigDecimal MONEY_LIMIT = new BigDecimal("100000000000000");
	private static final long IMPORT_LOCK = 0x52454546494D5054L;

	private ReefSearchImporter() { }

	public record Source(String path, String sha256, String rawJson, String store,
			OffsetDateTime fetchedAt, List<Row> rows) {
		public Source { rows = List.copyOf(rows); }
	}

	/** Maps contain only immutable strings, decimals, booleans, integers and string lists. */
	public record Row(String externalId, Map<String, Object> catalog,
			Map<String, Object> observation, String rawJson) {
		public Row {
			catalog = immutableFields(catalog);
			observation = immutableFields(observation);
		}
	}

	public record ImportResult(String status, String path, int rows, int productsInserted,
			long sourceFileId, BigDecimal priceSum) { }

	public static Source readSource(Path repoRoot, Path file) throws IOException {
		Path root = repoRoot.toRealPath();
		Path rawRoot = root.resolve("data/reefapi/raw").toRealPath();
		Path actual = (file.isAbsolute() ? file : root.resolve(file)).toRealPath();
		if (!rawRoot.startsWith(root) || !actual.startsWith(rawRoot) || !Files.isRegularFile(actual)) {
			throw new IOException("Source must be a regular file under data/reefapi/raw");
		}
		Path relative = rawRoot.relativize(actual);
		if (relative.getNameCount() != 4 || !"search".equals(relative.getName(2).toString())
				|| !actual.getFileName().toString().endsWith(".json")) {
			throw new IOException("Expected data/reefapi/raw/<date>/<store>/search/<file>.json");
		}
		byte[] bytes = Files.readAllBytes(actual);
		String document = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
		String path = root.relativize(actual).toString().replace('\\', '/');
		return parseSource(path, hash(bytes), document, relative.getName(1).toString());
	}

	private static Source parseSource(String path, String sha256, String rawJson, String expectedStore)
			throws IOException {
		try {
			JsonNode envelope = JSON.readTree(rawJson);
			require(envelope.isObject(), "Source envelope must be an object");
			validateJsonStrings(envelope);
			String store = text(envelope, "store", true);
			require(STORES.containsKey(store) && store.equals(expectedStore), "Store does not match source path");
			require("search".equals(text(envelope, "action", true)), "Only search sources are supported");
			require(Integer.valueOf(200).equals(integer(envelope, "httpStatus", true)), "httpStatus must be 200");
			JsonNode response = envelope.path("response");
			require(response.isObject() && Boolean.TRUE.equals(bool(response, "ok")), "Response must have ok=true");
			OffsetDateTime fetchedAt = OffsetDateTime.parse(text(envelope, "fetchedAt", true));
			require(fetchedAt.getNano() % 1_000 == 0, "fetchedAt exceeds PostgreSQL microsecond precision");
			JsonNode results = response.path("data").path("n11".equals(store) ? "products" : "results");
			require(results.isArray(), "Missing search result array");
			require(Integer.valueOf(results.size()).equals(integer(response.path("meta"), "record_count", true)),
					"record_count does not match result count");
			List<Row> rows = new ArrayList<>();
			for (JsonNode item : results) {
				rows.add(parseRow(store, item));
			}
			return new Source(path, sha256, rawJson, store, fetchedAt, rows);
		}
		catch (RuntimeException ex) {
			throw new IOException("Invalid search source " + path + ": " + ex.getMessage(), ex);
		}
	}

	private static Row parseRow(String store, JsonNode item) {
		require(item.isObject(), "Every result must be an object");
		boolean hb = "hepsiburada".equals(store), n11 = "n11".equals(store);
		boolean teknosa = "teknosa".equals(store), trendyol = "trendyol".equals(store);
		String id = identifier(item, hb ? "sku" : trendyol ? "content_id" : "product_id", true);
		Map<String, Object> catalog = new LinkedHashMap<>();
		catalog.put("external_group_id", trendyol ? identifier(item, "product_group_id", false) : null);
		catalog.put("title", text(item, "title", true));
		catalog.put("brand", text(item, "brand", false));
		catalog.put("category", text(item, "category", false));
		JsonNode categoryPath = item.path("category_path");
		List<String> categories = null;
		if (!absent(categoryPath)) {
			require(categoryPath.isArray(), "category_path must be an array");
			categories = new ArrayList<>();
			for (JsonNode category : categoryPath) {
				require(category.isString(), "category_path entries must be strings");
				categories.add(category.stringValue());
			}
		}
		catalog.put("category_path", categories == null ? null : List.copyOf(categories));
		catalog.put("image_url", text(item, "image", false));
		catalog.put("barcode", identifier(item, "barcode", false));
		catalog.put("mpn", text(item, "mpn", false));
		Map<String, Object> observation = new LinkedHashMap<>();
		String url = text(item, "url", true);
		URI uri = URI.create(url);
		require(("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
				&& uri.getHost() != null && !url.chars().anyMatch(Character::isWhitespace), "Invalid product URL");
		observation.put("source_url", url);
		observation.put("price", decimal(item, trendyol ? "price_value" : "price", true, true));
		String currency = text(item, "currency", false);
		require(currency == null || currency.matches("[A-Z]{3}"), "Invalid currency code");
		observation.put("currency", currency);
		observation.put("list_price", hb ? decimal(item, "original_price", false, true)
				: teknosa ? decimal(item, "was_price", false, true)
				: trendyol ? decimal(item, "list_price_value", false, true) : null);
		observation.put("member_price", teknosa ? decimal(item, "member_price", false, true) : null);
		JsonNode seller = item.path("seller");
		if (teknosa) {
			require(absent(seller) || seller.isObject(), "Teknosa seller must be an object");
		}
		observation.put("seller_external_id", teknosa ? identifier(seller, "shop_id", false)
				: trendyol ? identifier(item, "merchant_id", false) : null);
		observation.put("seller_name", teknosa ? text(seller, "name", false) : n11 ? text(item, "seller", false) : null);
		observation.put("seller_is_store", teknosa ? bool(seller, "is_teknosa") : null);
		observation.put("in_stock", n11 || teknosa ? bool(item, "in_stock") : null);
		observation.put("free_shipping", teknosa ? bool(item, "free_shipping") : trendyol ? bool(item, "free_cargo") : null);
		observation.put("sponsored", teknosa ? bool(item, "sponsored") : null);
		BigDecimal rating = decimal(item, "rating", false, false);
		BigDecimal scale = decimal(item, "rating_scale", false, false);
		require(scale == null || (scale.signum() > 0 && scale.stripTrailingZeros().scale() <= 3
				&& scale.compareTo(new BigDecimal("100000")) < 0), "Invalid rating_scale");
		require(rating == null || scale == null || rating.compareTo(scale) <= 0, "Rating exceeds rating_scale");
		observation.put("rating", rating);
		observation.put("rating_scale", scale);
		observation.put("review_count", integer(item, teknosa ? "review_count" : trendyol ? "comment_count" : "rating_count", false));
		return new Row(id, catalog, observation, JSON.writeValueAsString(item));
	}

	/** Requires an idle connection. Dry runs perform reads only, including no identity sequence changes. */
	public static ImportResult importSource(Connection connection, Source source, boolean commit) throws SQLException {
		Objects.requireNonNull(connection);
		Objects.requireNonNull(source);
		if (!connection.getAutoCommit()) {
			throw new SQLException("Importer requires autoCommit=true and owns the complete transaction");
		}
		validateSource(source);
		BigDecimal sum = source.rows().stream().map(row -> (BigDecimal) row.observation().get("price"))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		connection.setAutoCommit(false);
		boolean committed = false;
		boolean commitAttempted = false;
		boolean restoreAutoCommit = true;
		Throwable failure = null;
		try {
			try (var statement = connection.createStatement()) {
				statement.execute("SET LOCAL lock_timeout = '5s'");
				statement.execute("SET LOCAL statement_timeout = '30s'");
			}
			try (var lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
				lock.setLong(1, IMPORT_LOCK);
				lock.execute();
			}
			ExistingSource existing = existingSource(connection, source);
			Map<String, Product> products = loadProducts(connection, source);
			if (existing != null) {
				Source storedSource = new Source(existing.path(), source.sha256(), source.rawJson(),
						source.store(), source.fetchedAt(), source.rows());
				verify(connection, storedSource, existing.id(), products);
				connection.rollback();
				return new ImportResult("ALREADY_IMPORTED", source.path(), source.rows().size(), 0, existing.id(), sum);
			}
			int missing = (int) source.rows().stream().map(Row::externalId).distinct()
					.filter(id -> !products.containsKey(id)).count();
			if (!commit) {
				connection.rollback();
				return new ImportResult("DRY_RUN", source.path(), source.rows().size(), missing, 0, sum);
			}
			long storeId = store(connection, source.store());
			long sourceId = insertSource(connection, source, storeId);
			int inserted = 0;
			for (int index = 0; index < source.rows().size(); index++) {
				Row row = source.rows().get(index);
				Product product = products.get(row.externalId());
				if (product == null) {
					product = insertProduct(connection, storeId, row);
					products.put(row.externalId(), product);
					inserted++;
				}
				insertObservation(connection, source, sourceId, storeId, product.id(), index, row);
			}
			verify(connection, source, sourceId, products);
			commitAttempted = true;
			connection.commit();
			committed = true;
			// A second independent read confirms the committed state visible to this connection.
			verify(connection, source, sourceId, products);
			connection.rollback();
			return new ImportResult("IMPORTED", source.path(), source.rows().size(), inserted, sourceId, sum);
		}
		catch (SQLException | RuntimeException ex) {
			failure = ex;
			try { connection.rollback(); }
			catch (SQLException rollback) {
				restoreAutoCommit = false;
				ex.addSuppressed(rollback);
				try { connection.close(); }
				catch (SQLException close) { ex.addSuppressed(close); }
			}
			if (commitAttempted) {
				SQLException outcome = new SQLException(committed
						? "Import committed; subsequent readback failed. Rerun the same source to verify committed data."
						: "Commit acknowledgement failed; database outcome is uncertain. Verify the same source before retrying.", ex);
				failure = outcome;
				throw outcome;
			}
			throw ex;
		}
		finally {
			if (restoreAutoCommit) {
				try { if (!connection.isClosed()) connection.setAutoCommit(true); }
				catch (SQLException reset) {
					try { connection.close(); }
					catch (SQLException close) { reset.addSuppressed(close); }
					if (failure != null) failure.addSuppressed(reset);
					else throw new SQLException(committed ? "Import committed; connection reset failed" : "Connection reset failed", reset);
				}
			}
		}
	}

	private static void validateSource(Source source) throws SQLException {
		try {
			require(source.path().matches("data/reefapi/raw/[^/]+/[^/]+/search/[^/]+\\.json"), "Invalid source path");
			require(hash(source.rawJson().getBytes(StandardCharsets.UTF_8)).equals(source.sha256()), "Source hash mismatch");
			Source parsed = parseSource(source.path(), source.sha256(), source.rawJson(), source.path().split("/")[4]);
			require(parsed.equals(source), "Source metadata or rows differ from the saved envelope");
		}
		catch (IOException | RuntimeException ex) { throw new SQLException("Invalid import source: " + ex.getMessage(), ex); }
	}

	private record ExistingSource(long id, String path) { }

	private static ExistingSource existingSource(Connection connection, Source source) throws SQLException {
		try (var statement = connection.prepareStatement("SELECT id, source_path, sha256 FROM source_files WHERE source_path = ? OR sha256 = ?")) {
			statement.setString(1, source.path());
			statement.setString(2, source.sha256());
			try (var result = statement.executeQuery()) {
				if (!result.next()) return null;
				if (!source.sha256().equals(result.getString("sha256"))) {
					throw new SQLException("Source path or SHA-256 conflicts with an existing import; review required");
				}
				ExistingSource existing = new ExistingSource(result.getLong("id"), result.getString("source_path"));
				if (result.next()) throw new SQLException("Multiple source path/hash conflicts; review required");
				return existing;
			}
		}
	}

	private record Product(long id, long storeId, Map<String, Object> catalog) { }

	private static Map<String, Product> loadProducts(Connection connection, Source source) throws SQLException {
		Map<String, Product> products = new LinkedHashMap<>();
		java.sql.Array externalIds = connection.createArrayOf("text",
				source.rows().stream().map(Row::externalId).distinct().toArray(String[]::new));
		try {
			try (var statement = connection.prepareStatement("SELECT p.* FROM marketplace_products p JOIN stores s ON s.id = p.store_id WHERE s.code = ? AND p.external_id = ANY(?::text[])")) {
				statement.setString(1, source.store());
				statement.setArray(2, externalIds);
				try (var result = statement.executeQuery()) {
					while (result.next()) {
						products.put(result.getString("external_id"), new Product(result.getLong("id"), result.getLong("store_id"), fields(result, CATALOG)));
					}
				}
			}
		}
		finally { externalIds.free(); }
		return products;
	}

	private static long store(Connection connection, String code) throws SQLException {
		try (var statement = connection.prepareStatement("INSERT INTO stores(code, name, base_url) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING")) {
			statement.setString(1, code);
			statement.setString(2, STORES.get(code).get(0));
			statement.setString(3, STORES.get(code).get(1));
			statement.executeUpdate();
		}
		try (var statement = connection.prepareStatement("SELECT id FROM stores WHERE code = ?")) {
			statement.setString(1, code);
			try (var result = statement.executeQuery()) {
				check(result.next(), "Store disappeared");
				return result.getLong(1);
			}
		}
	}

	private static long insertSource(Connection connection, Source source, long storeId) throws SQLException {
		try (var statement = connection.prepareStatement("INSERT INTO source_files(store_id, source_path, sha256, action, fetched_at, result_count, raw_document) VALUES (?, ?, ?, 'search', ?, ?, ?::jsonb) RETURNING id")) {
			statement.setLong(1, storeId);
			statement.setString(2, source.path());
			statement.setString(3, source.sha256());
			statement.setObject(4, source.fetchedAt());
			statement.setInt(5, source.rows().size());
			statement.setString(6, source.rawJson());
			try (var result = statement.executeQuery()) { result.next(); return result.getLong(1); }
		}
	}

	private static Product insertProduct(Connection connection, long storeId, Row row) throws SQLException {
		String columns = String.join(", ", CATALOG);
		try (var statement = connection.prepareStatement("INSERT INTO marketplace_products(store_id, external_id, " + columns
				+ ") VALUES (?, ?, " + placeholders(CATALOG.size()) + ") ON CONFLICT (store_id, external_id) DO NOTHING RETURNING id")) {
			statement.setLong(1, storeId);
			statement.setString(2, row.externalId());
			bindFields(connection, statement, 3, CATALOG, row.catalog());
			try (var result = statement.executeQuery()) {
				check(result.next(), "Concurrent product insertion; retry after review");
				return new Product(result.getLong(1), storeId, row.catalog());
			}
		}
	}

	private static void insertObservation(Connection connection, Source source, long sourceId, long storeId,
			long productId, int index, Row row) throws SQLException {
		try (var statement = connection.prepareStatement("INSERT INTO product_observations(store_id, marketplace_product_id, source_file_id, row_index, observed_at, "
				+ String.join(", ", OBSERVATION) + ", raw_item) VALUES (?, ?, ?, ?, ?, " + placeholders(OBSERVATION.size()) + ", ?::jsonb)")) {
			statement.setLong(1, storeId);
			statement.setLong(2, productId);
			statement.setLong(3, sourceId);
			statement.setInt(4, index);
			statement.setObject(5, source.fetchedAt());
			int next = bindFields(connection, statement, 6, OBSERVATION, row.observation());
			statement.setString(next, row.rawJson());
			check(statement.executeUpdate() == 1, "Observation was not inserted");
		}
	}

	private static void verify(Connection connection, Source source, long sourceId, Map<String, Product> products) throws SQLException {
		long storeId;
		try (var statement = connection.prepareStatement("SELECT f.*, s.code, f.raw_document = ?::jsonb AS raw_matches FROM source_files f JOIN stores s ON s.id=f.store_id WHERE f.id=?")) {
			statement.setString(1, source.rawJson());
			statement.setLong(2, sourceId);
			try (var result = statement.executeQuery()) {
				check(result.next(), "Source file missing");
				storeId = result.getLong("store_id");
				check(source.store().equals(result.getString("code")) && source.path().equals(result.getString("source_path"))
						&& source.sha256().equals(result.getString("sha256")) && "search".equals(result.getString("action"))
						&& source.rows().size() == result.getInt("result_count") && result.getBoolean("raw_matches")
						&& source.fetchedAt().toInstant().equals(result.getObject("fetched_at", OffsetDateTime.class).toInstant()), "Source verification failed");
			}
		}
		Map<String, Product> actualProducts = loadProducts(connection, source);
		check(actualProducts.keySet().equals(products.keySet()), "Product identities do not match");
		for (String id : products.keySet()) {
			Product expected = products.get(id), actual = actualProducts.get(id);
			check(expected.id() == actual.id() && actual.storeId() == storeId && sameFields(expected.catalog(), actual.catalog()), "Catalogue verification failed");
		}
		// The complete source JSONB was compared above, so its indexed row is the expected raw item.
		String resultArray = "n11".equals(source.store()) ? "products" : "results";
		try (var statement = connection.prepareStatement("SELECT o.*, o.raw_item = (f.raw_document -> 'response' -> 'data' -> '"
				+ resultArray + "' -> o.row_index) AS raw_matches FROM product_observations o JOIN source_files f ON f.id=o.source_file_id WHERE o.source_file_id=? ORDER BY o.row_index")) {
			statement.setLong(1, sourceId);
			try (var result = statement.executeQuery()) {
				int index = 0;
				while (result.next()) {
					check(index < source.rows().size(), "Extra observation at row " + index);
					check(result.getInt("row_index") == index, "Missing or out-of-order observation at row " + index);
					Row row = source.rows().get(index);
					Product product = products.get(row.externalId());
					check(product != null, "Missing product for observation");
					check(result.getLong("store_id") == storeId && result.getLong("marketplace_product_id") == product.id()
							&& result.getBoolean("raw_matches") && source.fetchedAt().toInstant().equals(result.getObject("observed_at", OffsetDateTime.class).toInstant())
							&& sameFields(row.observation(), fields(result, OBSERVATION)), "Observation verification failed at row " + index);
					index++;
				}
				check(index == source.rows().size(), "Observation count mismatch");
			}
		}
	}

	private static int bindFields(Connection connection, PreparedStatement statement, int index, List<String> columns,
			Map<String, Object> fields) throws SQLException {
		for (String column : columns) {
			Object value = fields.get(column);
			if (value instanceof List<?> list) statement.setArray(index++, connection.createArrayOf("text", list.toArray()));
			else statement.setObject(index++, value);
		}
		return index;
	}

	private static Map<String, Object> fields(ResultSet result, List<String> columns) throws SQLException {
		Map<String, Object> fields = new LinkedHashMap<>();
		for (String column : columns) {
			Object value = result.getObject(column);
			if (value instanceof java.sql.Array array) {
				try { value = List.of((Object[]) array.getArray()); }
				finally { array.free(); }
			}
			fields.put(column, value);
		}
		return fields;
	}

	private static boolean sameFields(Map<String, Object> expected, Map<String, Object> actual) {
		return expected.keySet().equals(actual.keySet()) && expected.entrySet().stream().allMatch(entry -> {
			Object value = actual.get(entry.getKey());
			return entry.getValue() instanceof BigDecimal decimal && value instanceof BigDecimal other
					? decimal.compareTo(other) == 0 : Objects.equals(entry.getValue(), value);
		});
	}

	private static Map<String, Object> immutableFields(Map<String, Object> fields) {
		Map<String, Object> copy = new LinkedHashMap<>();
		fields.forEach((key, value) -> copy.put(key, value instanceof List<?> list ? List.copyOf(list) : value));
		return Collections.unmodifiableMap(copy);
	}

	private static void validateJsonStrings(JsonNode node) {
		if (node.isString()) validateJsonString(node.stringValue());
		if (node.isObject()) node.propertyNames().forEach(ReefSearchImporter::validateJsonString);
		if (node.isContainer()) node.forEach(ReefSearchImporter::validateJsonStrings);
	}

	private static void validateJsonString(String value) {
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			require(character != 0, "JSON contains a NUL character unsupported by PostgreSQL");
			if (Character.isHighSurrogate(character)) {
				require(index + 1 < value.length() && Character.isLowSurrogate(value.charAt(++index)), "JSON contains an unpaired surrogate");
			}
			else require(!Character.isLowSurrogate(character), "JSON contains an unpaired surrogate");
		}
	}

	private static String text(JsonNode parent, String key, boolean required) {
		JsonNode value = parent.path(key);
		if (absent(value)) { require(!required, "Missing " + key); return null; }
		require(value.isString(), key + " must be a string");
		String text = value.stringValue();
		require(!required || !text.isBlank(), key + " must not be blank");
		require(text.indexOf('\0') < 0, key + " contains a NUL character");
		return text;
	}

	private static String identifier(JsonNode parent, String key, boolean required) {
		JsonNode value = parent.path(key);
		if (absent(value)) { require(!required, "Missing " + key); return null; }
		if (value.isString()) {
			String id = text(parent, key, true);
			return id;
		}
		require(value.isIntegralNumber(), key + " must be an integer or string");
		return value.bigIntegerValue().toString();
	}

	private static Integer integer(JsonNode parent, String key, boolean required) {
		JsonNode value = parent.path(key);
		if (absent(value)) { require(!required, "Missing " + key); return null; }
		require(value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= 0, key + " must be a nonnegative integer");
		return value.intValue();
	}

	private static Boolean bool(JsonNode parent, String key) {
		JsonNode value = parent.path(key);
		if (absent(value)) return null;
		require(value.isBoolean(), key + " must be a boolean");
		return value.booleanValue();
	}

	private static BigDecimal decimal(JsonNode parent, String key, boolean required, boolean money) {
		JsonNode value = parent.path(key);
		if (absent(value)) { require(!required, "Missing " + key); return null; }
		require(value.isNumber(), key + " must be numeric");
		BigDecimal number = value.decimalValue();
		require(number != null && number.signum() >= 0, key + " must be finite and nonnegative");
		if (money) require(number.stripTrailingZeros().scale() <= 2 && number.compareTo(MONEY_LIMIT) < 0,
				key + " does not fit numeric(16,2) without rounding");
		return number;
	}

	private static boolean absent(JsonNode value) { return value.isMissingNode() || value.isNull(); }
	private static String placeholders(int count) { return String.join(", ", Collections.nCopies(count, "?")); }
	private static void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
	private static void check(boolean condition, String message) throws SQLException { if (!condition) throw new SQLException(message); }
	private static String hash(byte[] bytes) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
		catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
	}
}
