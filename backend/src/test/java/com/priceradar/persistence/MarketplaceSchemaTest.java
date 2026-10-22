package com.priceradar.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.priceradar.TestDatabase;

@Tag("integration")
class MarketplaceSchemaTest {

	private static TestDatabase database;
	private static Flyway flyway;

	private Connection connection;

	@BeforeAll
	static void migrateSchema() {
		database = TestDatabase.open();
		flyway = database.flyway();
		assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
	}

	@AfterAll
	static void dropSchema() {
		if (database != null) {
			database.close();
		}
	}

	@BeforeEach
	void openTransaction() throws SQLException {
		connection = database.dataSource().getConnection();
		connection.setAutoCommit(false);
	}

	@AfterEach
	void rollbackTransaction() throws SQLException {
		if (connection != null) {
			try {
				connection.rollback();
			}
			finally {
				connection.close();
			}
		}
	}

	@Test
	void migrationsCreateEmptyTablesAndCanBeRunAgain() throws SQLException {
		assertThat(flyway.migrate().migrationsExecuted).isZero();
		for (var table : new String[] { "stores", "source_files", "marketplace_products", "product_observations" }) {
			assertThat(queryLong("SELECT count(*) FROM " + table)).as(table).isZero();
		}
	}

	@Test
	void productIdsAreUniqueWithinEachStoreAndPreserveLeadingZeroes() throws SQLException {
		var firstStore = insertStore("first_store");
		var secondStore = insertStore("second_store");
		insertProduct(firstStore, "000123-ABC");
		insertProduct(secondStore, "000123-ABC");

		assertThat(queryLong("SELECT count(*) FROM marketplace_products WHERE external_id = ?", "000123-ABC"))
				.isEqualTo(2);
		assertSqlState("23505", () -> insertProduct(firstStore, "000123-ABC"));
	}

	@Test
	void sourcePathsAndHashesEachPreventReimportingAFile() throws SQLException {
		var store = insertStore("first_store");
		insertSource(store, "data/first.json", "a".repeat(64));

		assertSqlState("23505", () -> insertSource(store, "data/first.json", "b".repeat(64)));
		assertSqlState("23505", () -> insertSource(store, "data/second.json", "a".repeat(64)));
	}

	@Test
	void absentCurrencyAndStockStayUnknown() throws SQLException {
		var fixture = insertFixture("first_store", "a");
		insertObservation(fixture.store(), fixture.product(), fixture.source(), 0, "499.90", null, "seller-a");

		try (var statement = connection.createStatement();
				var result = statement.executeQuery("SELECT currency, in_stock FROM product_observations")) {
			assertThat(result.next()).isTrue();
			assertThat(result.getObject("currency")).isNull();
			assertThat(result.getObject("in_stock")).isNull();
		}
	}

	@Test
	void repeatedProductsWithDifferentSellersKeepBothPricesAndSourcePositions() throws SQLException {
		var fixture = insertFixture("first_store", "a");
		insertObservation(fixture.store(), fixture.product(), fixture.source(), 0, "499.90", "TRY", "seller-a");
		insertObservation(fixture.store(), fixture.product(), fixture.source(), 1, "510.25", "TRY", "seller-b");

		try (var statement = connection.createStatement();
				var result = statement.executeQuery(
						"SELECT row_index, seller_external_id, price FROM product_observations ORDER BY row_index")) {
			assertThat(result.next()).isTrue();
			assertThat(result.getInt("row_index")).isZero();
			assertThat(result.getString("seller_external_id")).isEqualTo("seller-a");
			assertThat(result.getBigDecimal("price")).isEqualByComparingTo("499.90");
			assertThat(result.next()).isTrue();
			assertThat(result.getInt("row_index")).isEqualTo(1);
			assertThat(result.getString("seller_external_id")).isEqualTo("seller-b");
			assertThat(result.getBigDecimal("price")).isEqualByComparingTo("510.25");
			assertThat(result.next()).isFalse();
		}

		assertSqlState("23505", () -> insertObservation(
				fixture.store(), fixture.product(), fixture.source(), 0, "520.00", "TRY", "seller-c"));
	}

	@Test
	void rejectsNegativeNonfiniteAndMissingPrices() throws SQLException {
		var fixture = insertFixture("first_store", "a");
		assertSqlState("23514", () -> insertObservation(
				fixture.store(), fixture.product(), fixture.source(), 0, "-0.01", "TRY", "seller-a"));
		assertSqlState("23514", () -> insertObservation(
				fixture.store(), fixture.product(), fixture.source(), 0, "NaN", "TRY", "seller-a"));
		for (var price : new String[] { "Infinity", "-Infinity" }) {
			// PostgreSQL rejects infinity during numeric(16, 2) conversion, before CHECK constraints.
			assertSqlState("22003", () -> insertObservation(
					fixture.store(), fixture.product(), fixture.source(), 0, price, "TRY", "seller-a"));
		}
		assertSqlState("23502", () -> insertObservation(
				fixture.store(), fixture.product(), fixture.source(), 0, null, "TRY", "seller-a"));
	}

	@Test
	void rejectsMalformedCurrencyCodes() throws SQLException {
		var fixture = insertFixture("first_store", "a");
		for (var currency : new String[] { "try", "TL", "TRY ", "123", "" }) {
			assertSqlState("23514", () -> insertObservation(
					fixture.store(), fixture.product(), fixture.source(), 0, "100.00", currency, "seller-a"));
		}
	}

	@Test
	void rejectsProductOrSourceFromAnotherStore() throws SQLException {
		var first = insertFixture("first_store", "a");
		var second = insertFixture("second_store", "b");

		assertSqlState("23503", () -> insertObservation(
				first.store(), first.product(), second.source(), 0, "100.00", "TRY", "seller-a"));
		assertSqlState("23503", () -> insertObservation(
				first.store(), second.product(), first.source(), 0, "100.00", "TRY", "seller-a"));
	}

	@Test
	void decimalPricesAndRatingsRoundTripExactly() throws SQLException {
		var fixture = insertFixture("first_store", "a");
		var observation = insertObservation(
				fixture.store(), fixture.product(), fixture.source(), 0, "10001999.99", "TRY", "seller-a");
		try (var statement = connection.prepareStatement("""
				UPDATE product_observations
				SET list_price = ?, member_price = ?, rating = ?, rating_scale = ?
				WHERE id = ?
				RETURNING price, list_price, member_price, rating, rating_scale
				""")) {
			statement.setBigDecimal(1, new BigDecimal("10002000.01"));
			statement.setBigDecimal(2, new BigDecimal("9999999.95"));
			statement.setBigDecimal(3, new BigDecimal("4.4689922480620154"));
			statement.setBigDecimal(4, new BigDecimal("5.000"));
			statement.setLong(5, observation);
			try (var result = statement.executeQuery()) {
				assertThat(result.next()).isTrue();
				assertThat(result.getBigDecimal("price")).isEqualTo(new BigDecimal("10001999.99"));
				assertThat(result.getBigDecimal("list_price")).isEqualTo(new BigDecimal("10002000.01"));
				assertThat(result.getBigDecimal("member_price")).isEqualTo(new BigDecimal("9999999.95"));
				assertThat(result.getBigDecimal("rating")).isEqualTo(new BigDecimal("4.4689922480620154"));
				assertThat(result.getBigDecimal("rating_scale")).isEqualTo(new BigDecimal("5.000"));
			}
		}
	}

	private Fixture insertFixture(String code, String hashCharacter) throws SQLException {
		var store = insertStore(code);
		return new Fixture(store, insertProduct(store, "000123-ABC"),
				insertSource(store, "data/" + code + ".json", hashCharacter.repeat(64)));
	}

	private long insertStore(String code) throws SQLException {
		return queryLong("INSERT INTO stores (code, name, base_url) VALUES (?, ?, ?) RETURNING id",
				code, "Test store", "https://example.test");
	}

	private long insertProduct(long store, String externalId) throws SQLException {
		return queryLong("""
				INSERT INTO marketplace_products (store_id, external_id, title)
				VALUES (?, ?, 'Test product') RETURNING id
				""", store, externalId);
	}

	private long insertSource(long store, String path, String hash) throws SQLException {
		return queryLong("""
				INSERT INTO source_files (store_id, source_path, sha256, action, fetched_at, result_count, raw_document)
				VALUES (?, ?, ?, 'search', '2026-10-22T12:00:00Z', 2, '{"results": []}'::jsonb) RETURNING id
				""", store, path, hash);
	}

	private long insertObservation(long store, long product, long source, int rowIndex,
			String price, String currency, String seller) throws SQLException {
		return queryLong("""
				INSERT INTO product_observations (store_id, marketplace_product_id, source_file_id, row_index,
				    observed_at, source_url, price, currency, seller_external_id, raw_item)
				VALUES (?, ?, ?, ?, '2026-10-22T12:00:00Z', 'https://example.test/product/123',
				    CAST(? AS numeric), ?, ?, '{}'::jsonb) RETURNING id
				""", store, product, source, rowIndex, price, currency, seller);
	}

	private long queryLong(String sql, Object... parameters) throws SQLException {
		try (var statement = connection.prepareStatement(sql)) {
			for (int index = 0; index < parameters.length; index++) {
				statement.setObject(index + 1, parameters[index]);
			}
			try (var result = statement.executeQuery()) {
				assertThat(result.next()).isTrue();
				return result.getLong(1);
			}
		}
	}

	private void assertSqlState(String expectedState, SqlOperation operation) throws SQLException {
		var savepoint = connection.setSavepoint();
		try {
			assertThatThrownBy(operation::execute).isInstanceOfSatisfying(SQLException.class,
					exception -> assertThat(exception.getSQLState()).isEqualTo(expectedState));
		}
		finally {
			connection.rollback(savepoint);
			connection.releaseSavepoint(savepoint);
		}
	}

	@FunctionalInterface
	private interface SqlOperation {
		void execute() throws SQLException;
	}

	private record Fixture(long store, long product, long source) {
	}

}
