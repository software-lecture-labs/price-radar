package com.priceradar.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ReefSearchImporterTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18.6-alpine"))
			.withDatabaseName("marketplace_import_test")
			.withUsername("price_radar")
			.withPassword("price_radar");

	@TempDir
	Path repoRoot;

	private Connection connection;

	@BeforeAll
	static void migrateSchema() {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.locations("classpath:db/migration")
				.load().migrate();
	}

	@BeforeEach
	void openConnection() throws SQLException {
		connection = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
		execute("TRUNCATE product_observations, marketplace_products, source_files, stores RESTART IDENTITY");
	}

	@AfterEach
	void closeConnection() throws SQLException {
		if (connection != null) {
			try {
				execute("DROP TRIGGER IF EXISTS reject_second_observation ON product_observations");
				execute("DROP FUNCTION IF EXISTS reject_second_observation()");
			}
			finally {
				connection.close();
			}
		}
	}

	@Test
	void dryRunValidatesRowsAndRollsBackEveryTable() throws Exception {
		var source = source("hepsiburada", "dry-run", hb("001-A", "10.25") + "," + hb("002-B", "20.30"), 2);
		var result = ReefSearchImporter.importSource(connection, source, false);

		assertThat(result.status().toString()).isEqualTo("DRY_RUN");
		assertThat(result.rows()).isEqualTo(2);
		assertThat(result.priceSum()).isEqualByComparingTo("30.55");
		assertThat(connection.getAutoCommit()).isTrue();
		assertEmptyTables();
	}

	@Test
	void importsExactDecimalsUnicodeAndOriginalSourceProvenanceWithoutInventingUnknowns() throws Exception {
		var source = source("hepsiburada", "unicode", """
				{"sku":"000123-ABC","title":"Şarjlı öğütücü – çelik","url":"https://example.test/offer/1",
				 "price":10001999.99,"original_price":10002000.01,"brand":"Örnek",
				 "rating":4.4689922480620154,"rating_count":17,"unmapped":{"label":"İstanbul"}}
				""", 1);
		var result = ReefSearchImporter.importSource(connection, source, true);

		assertThat(result.status().toString()).isEqualTo("IMPORTED");
		assertThat(result.productsInserted()).isEqualTo(1);
		assertThat(result.priceSum()).isEqualByComparingTo("10001999.99");
		assertThat(source.sha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(source.rawJson().getBytes(StandardCharsets.UTF_8))));
		try (var statement = connection.createStatement(); var row = statement.executeQuery("""
				SELECT p.external_id, p.title, p.brand, o.price, o.list_price, o.rating, o.review_count,
				       o.currency, o.in_stock, o.seller_name, o.row_index, o.observed_at,
				       o.raw_item #>> '{unmapped,label}' AS raw_label,
				       s.source_path, s.sha256, s.fetched_at, s.result_count,
				       s.raw_document #>> '{response,data,results,0,title}' AS raw_title
				FROM marketplace_products p JOIN product_observations o ON o.marketplace_product_id = p.id
				JOIN source_files s ON s.id = o.source_file_id
				""")) {
			assertThat(row.next()).isTrue();
			assertThat(row.getString("external_id")).isEqualTo("000123-ABC");
			assertThat(row.getString("title")).isEqualTo("Şarjlı öğütücü – çelik");
			assertThat(row.getString("brand")).isEqualTo("Örnek");
			assertThat(row.getBigDecimal("price")).isEqualTo(new BigDecimal("10001999.99"));
			assertThat(row.getBigDecimal("list_price")).isEqualTo(new BigDecimal("10002000.01"));
			assertThat(row.getBigDecimal("rating")).isEqualByComparingTo("4.4689922480620154");
			assertThat(row.getInt("review_count")).isEqualTo(17);
			for (var column : new String[] { "currency", "in_stock", "seller_name" }) {
				assertThat(row.getObject(column)).as(column).isNull();
			}
			assertThat(row.getInt("row_index")).isZero();
			assertThat(row.getObject("observed_at", OffsetDateTime.class).toInstant()).isEqualTo(source.fetchedAt().toInstant());
			assertThat(row.getObject("fetched_at", OffsetDateTime.class).toInstant()).isEqualTo(source.fetchedAt().toInstant());
			assertThat(row.getString("source_path")).isEqualTo("data/reefapi/raw/2026-10-22/hepsiburada/search/unicode.json");
			assertThat(row.getString("sha256")).isEqualTo(source.sha256());
			assertThat(row.getInt("result_count")).isEqualTo(1);
			assertThat(row.getString("raw_label")).isEqualTo("İstanbul");
			assertThat(row.getString("raw_title")).isEqualTo(row.getString("title"));
			assertThat(row.next()).isFalse();
		}
	}

	@Test
	void reimportingSameFileOrBytesAtAnotherPathDoesNotDuplicateObservations() throws Exception {
		var source = source("hepsiburada", "first", hb("000123", "19.95"), 1);
		var first = ReefSearchImporter.importSource(connection, source, true);
		var repeated = ReefSearchImporter.importSource(connection, source, true);
		var duplicate = source("hepsiburada", "another-path", hb("000123", "19.95"), 1);
		var sameBytes = ReefSearchImporter.importSource(connection, duplicate, true);

		assertThat(repeated.status().toString()).isEqualTo("ALREADY_IMPORTED");
		assertThat(sameBytes.status().toString()).isEqualTo("ALREADY_IMPORTED");
		assertThat(repeated.sourceFileId()).isEqualTo(first.sourceFileId());
		assertCounts(1, 1, 1, 1);
	}

	@Test
	void changedBytesAtAnImportedPathAreRejectedAndLeaveExistingDataIntact() throws Exception {
		ReefSearchImporter.importSource(connection, source("hepsiburada", "same", hb("A", "19.95"), 1), true);
		var changed = source("hepsiburada", "same", hb("B", "99.99"), 1);

		assertThatThrownBy(() -> ReefSearchImporter.importSource(connection, changed, true)).isInstanceOf(SQLException.class);
		assertCounts(1, 1, 1, 1);
		assertThat(queryText("SELECT external_id FROM marketplace_products")).isEqualTo("A");
		assertThat(queryText("SELECT price::text FROM product_observations")).isEqualTo("19.95");
	}

	@Test
	void databaseFailureOnSecondRowRollsBackTheFirstRowProductsAndProvenance() throws Exception {
		execute("""
				CREATE FUNCTION reject_second_observation() RETURNS trigger LANGUAGE plpgsql AS $$
				BEGIN IF NEW.row_index = 1 THEN RAISE EXCEPTION 'synthetic second-row failure'; END IF; RETURN NEW; END $$
				""");
		execute("CREATE TRIGGER reject_second_observation BEFORE INSERT ON product_observations "
				+ "FOR EACH ROW EXECUTE FUNCTION reject_second_observation()");
		var source = source("hepsiburada", "rollback", hb("A", "10.00") + "," + hb("B", "20.00"), 2);

		assertThatThrownBy(() -> ReefSearchImporter.importSource(connection, source, true))
				.isInstanceOf(SQLException.class).hasMessageContaining("synthetic second-row failure");
		assertThat(connection.getAutoCommit()).isTrue();
		assertEmptyTables();
	}

	@Test
	void rollbackFailureClosesConnectionWithoutEnablingAutoCommitOrPersistingPartialRows() throws Exception {
		execute("""
				CREATE FUNCTION reject_second_observation() RETURNS trigger LANGUAGE plpgsql AS $$
				BEGIN IF NEW.row_index = 1 THEN RAISE EXCEPTION 'synthetic second-row failure'; END IF; RETURN NEW; END $$
				""");
		execute("CREATE TRIGGER reject_second_observation BEFORE INSERT ON product_observations "
				+ "FOR EACH ROW EXECUTE FUNCTION reject_second_observation()");
		var source = source("hepsiburada", "failed-rollback", hb("A", "10.00") + "," + hb("B", "20.00"), 2);
		var delegate = connection;
		var rollbackFailed = new AtomicBoolean();
		var autoCommitEnabledAfterFailure = new AtomicBoolean();
		var proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				new Class<?>[] { Connection.class }, (ignored, method, arguments) -> {
					if (method.getName().equals("rollback") && method.getParameterCount() == 0) {
						rollbackFailed.set(true);
						throw new SQLException("synthetic rollback failure");
					}
					if (method.getName().equals("setAutoCommit") && Boolean.TRUE.equals(arguments[0])
							&& rollbackFailed.get()) {
						autoCommitEnabledAfterFailure.set(true);
					}
					try {
						return method.invoke(delegate, arguments);
					}
					catch (InvocationTargetException exception) {
						throw exception.getCause();
					}
				});
		try {
			assertThatThrownBy(() -> ReefSearchImporter.importSource(proxy, source, true))
					.isInstanceOf(SQLException.class).hasMessageContaining("synthetic second-row failure")
					.satisfies(exception -> assertThat(exception.getSuppressed()).extracting(Throwable::getMessage)
							.contains("synthetic rollback failure"));
			assertThat(rollbackFailed.get()).isTrue();
			assertThat(autoCommitEnabledAfterFailure.get()).isFalse();
			assertThat(delegate.isClosed()).isTrue();
			assertThat(proxy.isClosed()).isTrue();
		}
		finally {
			delegate.close();
			connection = DriverManager.getConnection(
					POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
		}
		assertEmptyTables();
	}

	@Test
	void n11ProductsArrayKeepsEachSellerOfferForTheSameProduct() throws Exception {
		var source = source("n11", "sellers", """
				{"product_id":39734945,"title":"Tıraş makinesi","url":"https://example.test/seller-a",
				 "price":499.90,"currency":"TRY","seller":"Mağaza A","in_stock":true,"rating_count":12},
				{"product_id":39734945,"title":"Tıraş makinesi","url":"https://example.test/seller-b",
				 "price":510.25,"currency":"TRY","seller":"Mağaza B","in_stock":false,"rating_count":3}
				""", 2);
		var result = ReefSearchImporter.importSource(connection, source, true);

		assertThat(result.productsInserted()).isEqualTo(1);
		assertCounts(1, 1, 1, 2);
		assertThat(queryText("SELECT external_id FROM marketplace_products")).isEqualTo("39734945");
		assertThat(queryText("SELECT string_agg(row_index || ':' || seller_name || ':' || price || ':' || in_stock, ',' ORDER BY row_index) FROM product_observations"))
				.isEqualTo("0:Mağaza A:499.90:true,1:Mağaza B:510.25:false");
	}

	@Test
	void trendyolUsesNumericPriceAndPreservesUnknownCurrency() throws Exception {
		var source = source("trendyol", "price-value", """
				{"content_id":123,"product_group_id":456,"title":"Kahve","url":"https://example.test/coffee",
				 "price":"1.234,56 TL","price_value":1234.56,"list_price_value":1500.00,
				 "merchant_id":789,"free_cargo":true,"comment_count":23}
				""", 1);
		ReefSearchImporter.importSource(connection, source, true);

		assertThat(queryText("SELECT external_id || ':' || external_group_id FROM marketplace_products")).isEqualTo("123:456");
		assertThat(queryText("SELECT price || ':' || list_price || ':' || seller_external_id || ':' || free_shipping || ':' || review_count FROM product_observations"))
				.isEqualTo("1234.56:1500.00:789:true:23");
		assertThat(queryLong("SELECT count(*) FROM product_observations WHERE currency IS NULL AND in_stock IS NULL")).isEqualTo(1);
	}

	@Test
	void teknosaMapsSellerObjectBarcodeMpnAndCategoryPath() throws Exception {
		var source = source("teknosa", "seller-object", """
				{"product_id":"00042","title":"Telefon","url":"https://example.test/phone","price":100.10,
				 "was_price":120.00,"member_price":99.90,"currency":"TRY","barcode":"0009876543210","mpn":"AB-42",
				 "category_path":["Elektronik","Telefon"],"seller":{"shop_id":7,"name":"Teknosa","is_teknosa":true},
				 "in_stock":true,"free_shipping":false,"sponsored":true,"review_count":4}
				""", 1);
		ReefSearchImporter.importSource(connection, source, true);

		assertThat(queryText("SELECT external_id || ':' || barcode || ':' || mpn || ':' || array_to_string(category_path, '>') FROM marketplace_products"))
				.isEqualTo("00042:0009876543210:AB-42:Elektronik>Telefon");
		assertThat(queryText("SELECT seller_external_id || ':' || seller_name || ':' || seller_is_store || ':' || member_price || ':' || free_shipping || ':' || sponsored FROM product_observations"))
				.isEqualTo("7:Teknosa:true:99.90:false:true");
	}

	@Test
	void malformedJsonAndInvalidLastRowsAreRejectedBeforeAnyDatabaseWrite() throws Exception {
		var file = file("hepsiburada", "invalid");
		Files.writeString(file, "{broken", StandardCharsets.UTF_8);
		assertThatThrownBy(() -> ReefSearchImporter.readSource(repoRoot, file)).isInstanceOf(IOException.class);
		for (var badRow : new String[] { hb("B", "-0.01"), hb("B", "1.001"),
				"{\"title\":\"Missing id\",\"url\":\"https://example.test/b\",\"price\":1}" }) {
			assertThatThrownBy(() -> source("hepsiburada", "invalid", hb("A", "10.00") + "," + badRow, 2))
					.isInstanceOf(IOException.class);
			assertEmptyTables();
		}
	}

	private ReefSearchImporter.Source source(String store, String name, String rows, int count) throws IOException {
		var file = file(store, name);
		Files.writeString(file, """
				{"fetchedAt":"2026-10-22T09:03:59.354Z","store":"%s","action":"search","httpStatus":200,
				 "response":{"ok":true,"meta":{"record_count":%d},"data":{"%s":[%s]}}}
				""".formatted(store, count, store.equals("n11") ? "products" : "results", rows), StandardCharsets.UTF_8);
		return ReefSearchImporter.readSource(repoRoot, file);
	}

	private Path file(String store, String name) throws IOException {
		var directory = repoRoot.resolve("data/reefapi/raw/2026-10-22/" + store + "/search");
		Files.createDirectories(directory);
		return directory.resolve(name + ".json");
	}

	private static String hb(String sku, String price) {
		return "{\"sku\":\"%s\",\"title\":\"Test product\",\"url\":\"https://example.test/%s\",\"price\":%s}"
				.formatted(sku, sku, price);
	}

	private void assertEmptyTables() throws SQLException {
		assertCounts(0, 0, 0, 0);
	}

	private void assertCounts(long stores, long sources, long products, long observations) throws SQLException {
		assertThat(queryLong("SELECT count(*) FROM stores")).isEqualTo(stores);
		assertThat(queryLong("SELECT count(*) FROM source_files")).isEqualTo(sources);
		assertThat(queryLong("SELECT count(*) FROM marketplace_products")).isEqualTo(products);
		assertThat(queryLong("SELECT count(*) FROM product_observations")).isEqualTo(observations);
	}

	private long queryLong(String sql) throws SQLException {
		return Long.parseLong(queryText(sql));
	}

	private String queryText(String sql) throws SQLException {
		try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
			assertThat(result.next()).isTrue();
			return result.getString(1);
		}
	}

	private void execute(String sql) throws SQLException {
		try (var statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

}
