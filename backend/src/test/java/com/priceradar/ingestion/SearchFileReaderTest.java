package com.priceradar.ingestion;

import static com.priceradar.ingestion.SearchFileFixtures.hepsiburada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchFileReaderTest {

	@TempDir
	Path repositoryRoot;

	private SearchFileFixtures fixtures;

	@BeforeEach
	void setUp() {
		fixtures = new SearchFileFixtures(repositoryRoot);
	}

	@Test
	void keepsExactDecimalsTextAndProvenanceAndLeavesUnknownValuesEmpty() throws Exception {
		var path = fixtures.write(Marketplace.HEPSIBURADA, "unicode", List.of("""
				{"sku":"000123-ABC","title":"Şarjlı öğütücü – çelik","url":"https://www.hepsiburada.com/x",
				 "price":10001999.99,"original_price":10002000.01,"brand":"Örnek",
				 "rating":4.4689922480620154,"rating_count":17,"unmapped":{"label":"İstanbul"}}"""));

		var file = fixtures.read(path);

		assertThat(file.path()).isEqualTo("data/reefapi/raw/2026-10-22/hepsiburada/search/unicode.json");
		assertThat(file.marketplace()).isEqualTo(Marketplace.HEPSIBURADA);
		assertThat(file.sha256()).isEqualTo(sha256(Files.readAllBytes(path)));
		assertThat(file.fetchedAt()).isEqualTo(OffsetDateTime.parse(SearchFileFixtures.FETCHED_AT));

		var item = file.items().getFirst();
		assertThat(item.externalId()).isEqualTo("000123-ABC");
		assertThat(item.listing().title()).isEqualTo("Şarjlı öğütücü – çelik");
		assertThat(item.listing().brand()).isEqualTo("Örnek");
		assertThat(item.offer().price()).isEqualTo(new BigDecimal("10001999.99"));
		assertThat(item.offer().listPrice()).isEqualTo(new BigDecimal("10002000.01"));
		assertThat(item.offer().rating()).isEqualTo(new BigDecimal("4.4689922480620154"));
		assertThat(item.offer().reviewCount()).isEqualTo(17);
		assertThat(item.offer().currency()).isNull();
		assertThat(item.offer().inStock()).isNull();
		assertThat(item.offer().sellerName()).isNull();
		assertThat(item.rawJson()).contains("İstanbul");
	}

	@Test
	void n11ResultsAreUnderProductsAndTheSellerIsText() {
		var file = fixtures.read(fixtures.write(Marketplace.N11, "sellers", List.of("""
				{"product_id":39734945,"title":"Tıraş makinesi","url":"https://www.n11.com/a",
				 "price":499.90,"currency":"TRY","seller":"Mağaza A","in_stock":true,"rating_count":12}""")));

		var item = file.items().getFirst();
		assertThat(item.externalId()).isEqualTo("39734945");
		assertThat(item.offer().sellerName()).isEqualTo("Mağaza A");
		assertThat(item.offer().inStock()).isTrue();
		assertThat(item.offer().currency()).isEqualTo("TRY");
		assertThat(item.offer().reviewCount()).isEqualTo(12);
	}

	@Test
	void trendyolUsesTheNumericPriceAndHasNoCurrency() {
		var file = fixtures.read(fixtures.write(Marketplace.TRENDYOL, "price-value", List.of("""
				{"content_id":123,"product_group_id":456,"title":"Kahve","url":"https://www.trendyol.com/k",
				 "price":"1.234,56 TL","price_value":1234.56,"list_price_value":1500.00,
				 "merchant_id":789,"free_cargo":true,"comment_count":23}""")));

		var item = file.items().getFirst();
		assertThat(item.externalId()).isEqualTo("123");
		assertThat(item.listing().externalGroupId()).isEqualTo("456");
		assertThat(item.offer().price()).isEqualTo(new BigDecimal("1234.56"));
		assertThat(item.offer().listPrice()).isEqualByComparingTo("1500");
		assertThat(item.offer().sellerExternalId()).isEqualTo("789");
		assertThat(item.offer().freeShipping()).isTrue();
		assertThat(item.offer().reviewCount()).isEqualTo(23);
		assertThat(item.offer().currency()).isNull();
	}

	@Test
	void teknosaReadsTheSellerObjectBarcodeAndCategoryPath() {
		var file = fixtures.read(fixtures.write(Marketplace.TEKNOSA, "seller-object", List.of("""
				{"product_id":"00042","title":"Telefon","url":"https://www.teknosa.com/t","price":100.10,
				 "was_price":120.00,"member_price":99.90,"currency":"TRY","barcode":"0009876543210","mpn":"AB-42",
				 "category_path":["Elektronik","Telefon"],"seller":{"shop_id":7,"name":"Teknosa","is_teknosa":true},
				 "in_stock":true,"free_shipping":false,"sponsored":true,"review_count":4}""")));

		var item = file.items().getFirst();
		assertThat(item.externalId()).isEqualTo("00042");
		assertThat(item.listing().barcode()).isEqualTo("0009876543210");
		assertThat(item.listing().mpn()).isEqualTo("AB-42");
		assertThat(item.listing().categoryPath()).containsExactly("Elektronik", "Telefon");
		assertThat(item.offer().sellerExternalId()).isEqualTo("7");
		assertThat(item.offer().sellerName()).isEqualTo("Teknosa");
		assertThat(item.offer().sellerIsStore()).isTrue();
		assertThat(item.offer().memberPrice()).isEqualByComparingTo("99.90");
		assertThat(item.offer().freeShipping()).isFalse();
		assertThat(item.offer().sponsored()).isTrue();
	}

	@Test
	void rejectsValuesTheDatabaseWouldRejectOrChange() {
		var invalidItems = List.of(
				hepsiburada("B", "-0.01"),
				hepsiburada("B", "1.001"),
				"""
				{"title":"Kimliksiz","url":"https://www.hepsiburada.com/b","price":1}""",
				"""
				{"sku":"B","title":"Adres","url":"ftp://example.com/b","price":1}""",
				"""
				{"sku":"B","title":"Boş\\u0000karakter","url":"https://www.hepsiburada.com/b","price":1}""");

		for (var invalid : invalidItems) {
			var path = fixtures.write(Marketplace.HEPSIBURADA, "invalid", List.of(hepsiburada("A", "10.00"), invalid));
			assertThatThrownBy(() -> fixtures.read(path)).as(invalid).isInstanceOf(InvalidSearchFileException.class);
		}
	}

	@Test
	void rejectsBrokenDocumentsAndFilesInTheWrongPlace() throws Exception {
		var broken = fixtures.writeRaw("hepsiburada", "broken", "{broken");
		var wrongStore = fixtures.writeRaw("n11", "wrong-store", """
				{"fetchedAt":"%s","store":"hepsiburada","action":"search","httpStatus":200,
				 "response":{"ok":true,"meta":{"record_count":0},"data":{"products":[]}}}"""
				.formatted(SearchFileFixtures.FETCHED_AT));
		var wrongCount = fixtures.writeRaw("hepsiburada", "wrong-count", """
				{"fetchedAt":"%s","store":"hepsiburada","action":"search","httpStatus":200,
				 "response":{"ok":true,"meta":{"record_count":5},"data":{"results":[]}}}"""
				.formatted(SearchFileFixtures.FETCHED_AT));
		var unknownFolder = fixtures.writeRaw("amazon", "unknown", "{}");
		var outside = Files.writeString(repositoryRoot.resolve("outside.json"), "{}");

		for (var path : List.of(broken, wrongStore, wrongCount, unknownFolder, outside)) {
			assertThatThrownBy(() -> fixtures.read(path)).as(path.toString())
					.isInstanceOf(InvalidSearchFileException.class);
		}
	}

	@Test
	void rejectsAFileLinkToAResponseOutsideTheRawDirectory() throws Exception {
		var original = fixtures.write(Marketplace.HEPSIBURADA, "external", List.of(hepsiburada("A", "10.00")));
		var outside = Files.move(original, repositoryRoot.resolve("external.json"));
		createSymbolicLink(original, outside);

		assertThatThrownBy(() -> fixtures.read(original))
				.isInstanceOf(InvalidSearchFileException.class)
				.hasMessageContaining("not a file under");
	}

	@Test
	void rejectsADirectoryLinkToResponsesOutsideTheRawDirectory() throws Exception {
		var original = fixtures.write(Marketplace.HEPSIBURADA, "external-directory",
				List.of(hepsiburada("A", "10.00")));
		var searchDirectory = original.getParent();
		var outside = Files.move(searchDirectory, repositoryRoot.resolve("external-search"));
		createSymbolicLink(searchDirectory, outside);

		assertThatThrownBy(() -> fixtures.read(original))
				.isInstanceOf(InvalidSearchFileException.class)
				.hasMessageContaining("not a file under");
	}

	@Test
	void readsALinkWithinTheRawDirectoryAndKeepsItsOriginalProvenance() throws Exception {
		var original = fixtures.write(Marketplace.HEPSIBURADA, "original", List.of(hepsiburada("A", "10.00")));
		var link = original.resolveSibling("linked.json");
		createSymbolicLink(link, original);

		var file = fixtures.read(link);

		assertThat(file.path()).isEqualTo("data/reefapi/raw/2026-10-22/hepsiburada/search/linked.json");
		assertThat(file.sha256()).isEqualTo(sha256(Files.readAllBytes(original)));
		assertThat(file.items().getFirst().externalId()).isEqualTo("A");
	}

	private static void createSymbolicLink(Path link, Path target) throws IOException {
		try {
			Files.createSymbolicLink(link, target);
		}
		catch (FileSystemException | UnsupportedOperationException ex) {
			assumeTrue(!System.getProperty("os.name").startsWith("Windows"),
					"Symbolic link creation is unavailable on this Windows host: " + ex.getMessage());
			throw ex;
		}
	}

	private static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

}
