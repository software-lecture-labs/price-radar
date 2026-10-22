package com.priceradar.ingestion;

import static com.priceradar.ingestion.SearchFileFixtures.hepsiburada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.priceradar.ingestion.ImportResult.Status;

@Testcontainers
class SearchFileImporterTest {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse("postgres:18.6-alpine"))
			.withDatabaseName("marketplace_import_test")
			.withUsername("price_radar")
			.withPassword("price_radar");

	private static DriverManagerDataSource dataSource;
	private static JdbcClient jdbc;

	@TempDir
	Path repositoryRoot;

	private SearchFileFixtures fixtures;
	private SearchFileImporter importer;

	@BeforeAll
	static void migrate() {
		Flyway.configure()
				.dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
				.locations("classpath:db/migration")
				.load()
				.migrate();
		dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
				POSTGRES.getPassword());
		jdbc = JdbcClient.create(dataSource);
	}

	@BeforeEach
	void setUp() {
		jdbc.sql("TRUNCATE product_observations, marketplace_products, source_files, stores RESTART IDENTITY").update();
		fixtures = new SearchFileFixtures(repositoryRoot);
		importer = new SearchFileImporter(new MarketplaceImportRepository(jdbc),
				new DataSourceTransactionManager(dataSource));
	}

	@AfterEach
	void dropFailureTrigger() {
		jdbc.sql("DROP TRIGGER IF EXISTS fail_second_row ON product_observations").update();
		jdbc.sql("DROP FUNCTION IF EXISTS fail_second_row()").update();
	}

	@Test
	void importsTheFileWithItsProvenanceAndExactValues() {
		var file = read(Marketplace.TEKNOSA, "phone", """
				{"product_id":"00042","title":"Telefon","url":"https://www.teknosa.com/t","price":100.10,
				 "was_price":120.00,"currency":"TRY","category_path":["Elektronik","Telefon"],
				 "seller":{"shop_id":7,"name":"Teknosa","is_teknosa":true},"in_stock":true}""");

		var result = importer.importFile(file);

		assertThat(result).isEqualTo(new ImportResult(file.path(), Status.IMPORTED, 1, 1));
		assertCounts(1, 1, 1, 1);
		assertThat(text("SELECT code || ':' || name || ':' || base_url FROM stores"))
				.isEqualTo("teknosa:Teknosa:https://www.teknosa.com");
		assertThat(text("SELECT external_id || ':' || title || ':' || array_to_string(category_path, '>') "
				+ "FROM marketplace_products")).isEqualTo("00042:Telefon:Elektronik>Telefon");
		assertThat(text("SELECT price || ':' || list_price || ':' || currency || ':' || seller_name || ':' "
				+ "|| seller_is_store || ':' || in_stock || ':' || (member_price IS NULL) FROM product_observations"))
				.isEqualTo("100.10:120.00:TRY:Teknosa:true:true:true");
		assertThat(text("SELECT source_path || ':' || sha256 || ':' || result_count FROM source_files"))
				.isEqualTo(file.path() + ":" + file.sha256() + ":1");
		assertThat(text("SELECT (observed_at = fetched_at)::text FROM product_observations o "
				+ "JOIN source_files s ON s.id = o.source_file_id")).isEqualTo("true");
	}

	@Test
	void importingTheSameBytesAgainChangesNothingEvenAtAnotherPath() {
		var first = read(Marketplace.HEPSIBURADA, "first", hepsiburada("A", "19.95"));
		var copy = read(Marketplace.HEPSIBURADA, "copy", hepsiburada("A", "19.95"));
		importer.importFile(first);

		assertThat(importer.importFile(first).status()).isEqualTo(Status.ALREADY_IMPORTED);
		assertThat(importer.importFile(copy).status()).isEqualTo(Status.ALREADY_IMPORTED);
		assertCounts(1, 1, 1, 1);
	}

	@Test
	void aChangedFileAtAnImportedPathIsRefusedAndExistingDataStays() {
		importer.importFile(read(Marketplace.HEPSIBURADA, "same", hepsiburada("A", "19.95")));
		var changed = read(Marketplace.HEPSIBURADA, "same", hepsiburada("B", "99.99"));

		assertThatThrownBy(() -> importer.importFile(changed)).isInstanceOf(SourceConflictException.class);
		assertCounts(1, 1, 1, 1);
		assertThat(text("SELECT external_id || ':' || price FROM marketplace_products p "
				+ "JOIN product_observations o ON o.marketplace_product_id = p.id")).isEqualTo("A:19.95");
	}

	@Test
	void aFailureOnAnyRowLeavesNothingFromThatFile() {
		jdbc.sql("""
				CREATE FUNCTION fail_second_row() RETURNS trigger LANGUAGE plpgsql AS $$
				BEGIN IF NEW.row_index = 1 THEN RAISE EXCEPTION 'synthetic failure'; END IF; RETURN NEW; END $$
				""").update();
		jdbc.sql("CREATE TRIGGER fail_second_row BEFORE INSERT ON product_observations "
				+ "FOR EACH ROW EXECUTE FUNCTION fail_second_row()").update();
		var file = read(Marketplace.HEPSIBURADA, "rollback", hepsiburada("A", "10.00"), hepsiburada("B", "20.00"));

		assertThatThrownBy(() -> importer.importFile(file)).hasMessageContaining("synthetic failure");
		assertCounts(0, 0, 0, 0);
	}

	@Test
	void aDryRunWritesNothingAndCountsOnlyUnknownProducts() {
		importer.importFile(read(Marketplace.HEPSIBURADA, "known", hepsiburada("A", "10.00")));
		var next = read(Marketplace.HEPSIBURADA, "next", hepsiburada("A", "9.00"), hepsiburada("B", "5.00"));

		var check = importer.checkFile(next);

		assertThat(check).isEqualTo(new ImportResult(next.path(), Status.WOULD_IMPORT, 2, 1));
		assertCounts(1, 1, 1, 1);
	}

	@Test
	void aRepeatedProductIsStoredOnceWithEveryOfferAndKeepsItsFirstListing() {
		var file = read(Marketplace.N11, "sellers", """
				{"product_id":39734945,"title":"Tıraş makinesi","url":"https://www.n11.com/a",
				 "price":499.90,"currency":"TRY","seller":"Mağaza A","in_stock":true}""", """
				{"product_id":39734945,"title":"Tıraş makinesi (yeni ad)","url":"https://www.n11.com/b",
				 "price":510.25,"currency":"TRY","seller":"Mağaza B","in_stock":false}""");

		var result = importer.importFile(file);

		assertThat(result.newProducts()).isEqualTo(1);
		assertCounts(1, 1, 1, 2);
		assertThat(text("SELECT title FROM marketplace_products")).isEqualTo("Tıraş makinesi");
		assertThat(text("SELECT string_agg(row_index || ':' || seller_name || ':' || price, ',' ORDER BY row_index) "
				+ "FROM product_observations")).isEqualTo("0:Mağaza A:499.90,1:Mağaza B:510.25");
	}

	private SearchFile read(Marketplace marketplace, String name, String... items) {
		return fixtures.read(fixtures.write(marketplace, name, List.of(items)));
	}

	private void assertCounts(long stores, long sources, long products, long observations) {
		assertThat(count("stores")).as("stores").isEqualTo(stores);
		assertThat(count("source_files")).as("source_files").isEqualTo(sources);
		assertThat(count("marketplace_products")).as("marketplace_products").isEqualTo(products);
		assertThat(count("product_observations")).as("product_observations").isEqualTo(observations);
	}

	private static long count(String table) {
		return jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single();
	}

	private static String text(String sql) {
		return jdbc.sql(sql).query(String.class).single();
	}

}
