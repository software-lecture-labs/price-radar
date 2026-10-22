package com.priceradar.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.priceradar.TestDatabase;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MarketplaceDatabaseTest {

	private TestDatabase database;
	JdbcClient jdbc;

	@BeforeAll
	void migrate() {
		database = TestDatabase.open();
		database.flyway().migrate();
		jdbc = JdbcClient.create(database.dataSource());
	}

	@AfterAll
	void dropSchema() {
		if (database != null) {
			database.close();
		}
	}

	@BeforeEach
	void emptyTables() {
		jdbc.sql("TRUNCATE product_observations, marketplace_products, source_files, stores RESTART IDENTITY")
				.update();
	}

	record Source(long id, long store, Instant fetchedAt) {
	}

	long store(String code, String name) {
		return jdbc.sql("INSERT INTO stores (code, name, base_url) VALUES (:code, :name, :url) RETURNING id")
				.param("code", code)
				.param("name", name)
				.param("url", "https://www." + code + ".com")
				.query(Long.class)
				.single();
	}

	long product(long store, String externalId, String title, String brand) {
		return product(store, externalId, title, brand, null, null);
	}

	long product(long store, String externalId, String title, String brand, String category,
			String imageUrl) {

		return jdbc.sql("""
				INSERT INTO marketplace_products (store_id, external_id, title, brand, category, image_url)
				VALUES (:store, :externalId, :title, :brand, :category, :imageUrl) RETURNING id""")
				.param("store", store)
				.param("externalId", externalId)
				.param("title", title)
				.param("brand", brand)
				.param("category", category)
				.param("imageUrl", imageUrl)
				.query(Long.class)
				.single();
	}

	Source source(long store, Instant fetchedAt) {
		var id = jdbc.sql("""
				INSERT INTO source_files (store_id, source_path, sha256, action, fetched_at, result_count, raw_document)
				VALUES (:store, :path, :sha, 'search', :fetchedAt, 0, '{}'::jsonb) RETURNING id""")
				.param("store", store)
				.param("path", "data/" + UUID.randomUUID() + ".json")
				.param("sha", (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", ""))
				.param("fetchedAt", fetchedAt.atOffset(ZoneOffset.UTC))
				.query(Long.class)
				.single();
		return new Source(id, store, fetchedAt);
	}

	void observe(Source source, long product, int rowIndex, String price, String listPrice) {
		jdbc.sql("""
				INSERT INTO product_observations (store_id, marketplace_product_id, source_file_id, row_index,
					observed_at, source_url, price, list_price, raw_item)
				VALUES (:store, :product, :source, :row, :observedAt, 'https://example.com/p', :price, :listPrice,
					'{}'::jsonb)""")
				.param("store", source.store())
				.param("product", product)
				.param("source", source.id())
				.param("row", rowIndex)
				.param("observedAt", source.fetchedAt().atOffset(ZoneOffset.UTC))
				.param("price", new BigDecimal(price))
				.param("listPrice", listPrice == null ? null : new BigDecimal(listPrice))
				.update();
	}

}
