package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ProductComparisonTest {

	private static final Instant OBSERVED = Instant.parse("2026-10-22T09:00:00Z");
	private static final ProductComparison.Identity PHONE = identity(1, "Apple iPhone 15 128 GB Siyah", "Apple",
			"0195949018558", "MTUV3TU/A", "TRY");

	@Test
	void barcodeChecksItsChecksumAndPreservesEquivalentGtinRepresentations() {
		assertThat(ProductComparison.barcode("195949018558")).isEqualTo("00195949018558");
		assertThat(ProductComparison.barcode("0195949018558")).isEqualTo("00195949018558");
		assertThat(ProductComparison.barcode("195949018559")).isNull();
		assertThat(ProductComparison.barcode("19594901855X")).isNull();
		assertThat(ProductComparison.barcode("0000000000000")).isNull();
		assertThat(ProductComparison.barcode(" ")).isNull();
	}

	@Test
	void aValidExactBarcodeMatchesDifferentMarketplaceTitles() {
		var candidate = identity(2, "iPhone 15 Black 128GB Apple Türkiye", "Apple", "195949018558", null,
				"TRY");

		assertThat(ProductComparison.matchType(PHONE, candidate)).isEqualTo("barcode");
	}

	@Test
	void contradictoryBarcodesVetoEvenAnIdenticalTitleAndMpn() {
		var candidate = identity(2, PHONE.title(), "Apple", "194253324133", PHONE.mpn(), "TRY");

		assertThat(ProductComparison.matchType(PHONE, candidate)).isNull();
	}

	@Test
	void exactManufacturerPartNumberRequiresTheSameBrandAndCompatibleVariants() {
		var candidate = identity(2, "Apple 128GB Siyah Akıllı Telefon", " APPLE ", null, " mtuv3tu /a ", "TRY");

		assertThat(ProductComparison.matchType(PHONE, candidate)).isEqualTo("mpn");
		assertThat(ProductComparison.matchType(PHONE,
				identity(3, candidate.title(), "Other", null, PHONE.mpn(), "TRY"))).isNull();
		assertThat(ProductComparison.matchType(PHONE,
				identity(4, "Apple iPhone 15 256 GB Siyah", "Apple", null, PHONE.mpn(), "TRY"))).isNull();
		assertThat(ProductComparison.matchType(PHONE,
				identity(5, "Apple iPhone 15 128 GB Mavi", "Apple", null, PHONE.mpn(), "TRY"))).isNull();
		assertThat(ProductComparison.matchType(PHONE,
				identity(6, "Apple iPhone 15 128 GB Siyah Uyumlu Kılıf", "Apple", null, PHONE.mpn(), "TRY")))
				.isNull();
	}

	@Test
	void exactFullTitleFallbackPreservesModelStorageColorAndBundleIdentity() {
		assertThat(ProductComparison.matchType(PHONE,
				identity(2, " APPLE  iPhone 15 128 GB Siyah ", "Apple", null, null, "TRY")))
				.isEqualTo("exact_title");
		for (var title : List.of("Apple iPhone 15 Pro 128 GB Siyah", "Apple iPhone 15 256 GB Siyah",
				"Apple iPhone 15 128 GB Mavi", "Apple iPhone 15 128 GB Siyah 2'li Paket",
				"Apple iPhone 15 128 GB Siyah Kılıf")) {
			assertThat(ProductComparison.matchType(PHONE, identity(2, title, "Apple", null, null, "TRY")))
					.as(title).isNull();
		}
		assertThat(ProductComparison.matchType(PHONE,
				identity(2, PHONE.title(), "Apple", null, "DIFFERENT123", "TRY"))).isNull();
	}

	@Test
	void genericOrUnbrandedTitlesAreNotAnIdentity() {
		var generic = identity(1, "Samsung Telefon", "Samsung", null, null, "TRY");
		assertThat(ProductComparison.matchType(generic,
				identity(2, generic.title(), "Samsung", null, null, "TRY"))).isNull();
		assertThat(ProductComparison.matchType(PHONE,
				identity(2, PHONE.title(), null, null, null, "TRY"))).isNull();
	}

	@Test
	void fourStoresKeepUnavailableOffersEmptyAndPreserveActualSellerUrlsAndUnknownStock() {
		var offers = ProductComparison.offers(PHONE, List.of(candidate(PHONE, "hepsiburada", "50000", null,
				"https://www.hepsiburada.com/original-product")));

		assertThat(offers).extracting(offer -> offer.store().code())
				.containsExactly("hepsiburada", "n11", "teknosa", "trendyol");
		assertThat(offers.getFirst().url()).isEqualTo("https://www.hepsiburada.com/original-product");
		assertThat(offers.getFirst().matchType()).isEqualTo("listing");
		assertThat(offers.getFirst().inStock()).isNull();
		assertThat(offers.subList(1, 4)).allSatisfy(offer -> {
			assertThat(offer.productId()).isNull();
			assertThat(offer.url()).isNull();
			assertThat(offer.price()).isNull();
			assertThat(offer.matchType()).isNull();
		});
	}

	@Test
	void perStoreSelectionExcludesKnownOutOfStockThenUsesPriceAndRejectsIncomparableCurrencies() {
		var n11 = identity(2, PHONE.title(), PHONE.brand(), null, null, "TRY");
		var dollar = identity(3, PHONE.title(), PHONE.brand(), null, null, "USD");
		var unknownCurrency = identity(4, PHONE.title(), PHONE.brand(), null, null, null);
		var offers = ProductComparison.offers(PHONE, List.of(
				candidate(PHONE, "hepsiburada", "50000", null, "https://hb.example/product"),
				candidate(n11, "n11", "45000", true, "https://n11.example/seller-b"),
				candidate(n11, "n11", "40000", true, "https://n11.example/seller-a"),
				candidate(n11, "n11", "35000", false, "https://n11.example/out-of-stock"),
				candidate(n11, "n11", "30000", null, "https://n11.example/stock-unknown"),
				candidate(dollar, "teknosa", "1200", true, "https://teknosa.example/product"),
				candidate(unknownCurrency, "trendyol", "1000", true, "https://trendyol.example/product")));

		assertThat(offers.get(1).price()).isEqualByComparingTo("30000");
		assertThat(offers.get(1).url()).isEqualTo("https://n11.example/stock-unknown");
		assertThat(offers.get(1).inStock()).isNull();
		assertThat(offers.get(1).sellerName()).isEqualTo("Actual seller");
		assertThat(offers.get(1).freeShipping()).isNull();
		assertThat(offers.get(2).price()).isNull();
		assertThat(offers.get(3).price()).isNull();
	}

	private static ProductComparison.Identity identity(long id, String title, String brand, String barcode,
			String mpn, String currency) {
		return new ProductComparison.Identity(id, title, brand, barcode, mpn, currency);
	}

	private static ProductComparison.Candidate candidate(ProductComparison.Identity identity, String store,
			String price, Boolean inStock, String url) {
		return new ProductComparison.Candidate(identity, new StoreRef(store, store), new BigDecimal(price),
				null, inStock, null, url, OBSERVED, "Actual seller", 1);
	}

}
