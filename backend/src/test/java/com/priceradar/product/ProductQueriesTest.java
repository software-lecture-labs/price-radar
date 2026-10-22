package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductQueriesTest extends MarketplaceDatabaseTest {

	private static final Instant EARLIER = Instant.parse("2026-10-20T09:00:00Z");
	private static final Instant LATER = Instant.parse("2026-10-22T09:00:00Z");

	private ProductQueries queries;

	@BeforeEach
	void createQueries() {
		queries = new ProductQueries(jdbc);
	}

	@Test
	void currentPriceIsTheCheapestOfferAtTheLatestObservation() {
		var store = store("hepsiburada", "Hepsiburada");
		var phone = product(store, "HB-1", "Apple iPhone 17 Pro Max 256 GB", "Apple");
		var earlier = source(store, EARLIER);
		var later = source(store, LATER);
		observe(earlier, phone, 0, "50000.00", null);
		observe(later, phone, 0, "72999.00", "79999.00");
		observe(later, phone, 1, "71500.50", null);

		var page = queries.search(null, null, ProductSort.NEWEST, 0, 10);

		assertThat(page.totalItems()).isEqualTo(1);
		var summary = page.items().getFirst();
		assertThat(summary.price()).isEqualByComparingTo("71500.50");
		assertThat(summary.listPrice()).isNull();
		assertThat(summary.offerCount()).isEqualTo(2);
		assertThat(summary.observedAt()).isEqualTo(LATER);
		assertThat(summary.store()).isEqualTo(new StoreRef("hepsiburada", "Hepsiburada"));
	}

	@Test
	void searchMatchesTitleOrBrandAndFiltersByStore() {
		var hepsiburada = store("hepsiburada", "Hepsiburada");
		var n11 = store("n11", "N11");
		var hbSource = source(hepsiburada, LATER);
		var n11Source = source(n11, LATER);
		observe(hbSource, product(hepsiburada, "HB-1", "Galaxy S25 Ultra", "Samsung"), 0, "70000", null);
		observe(hbSource, product(hepsiburada, "HB-2", "iPad 11", "Apple"), 1, "27000", null);
		observe(n11Source, product(n11, "N-1", "Samsung Galaxy Tab S10", "Samsung"), 0, "28000", null);

		assertThat(queries.search("samsung", null, ProductSort.NEWEST, 0, 10).items())
				.extracting(ProductSummary::title)
				.containsExactly("Samsung Galaxy Tab S10", "Galaxy S25 Ultra");
		assertThat(queries.search("samsung", "n11", ProductSort.NEWEST, 0, 10).items())
				.extracting(ProductSummary::title)
				.containsExactly("Samsung Galaxy Tab S10");
		assertThat(queries.search("100%_off", null, ProductSort.NEWEST, 0, 10).totalItems()).isZero();
	}

	@Test
	void pagesAndSortsByPrice() {
		var store = store("teknosa", "Teknosa");
		var source = source(store, LATER);
		observe(source, product(store, "1", "Kulaklık", null), 0, "1500", null);
		observe(source, product(store, "2", "Telefon", null), 1, "45000", null);
		observe(source, product(store, "3", "Tablet", null), 2, "20000", null);

		var first = queries.search(null, null, ProductSort.PRICE_ASC, 0, 2);
		var second = queries.search(null, null, ProductSort.PRICE_ASC, 1, 2);

		assertThat(first.items()).extracting(ProductSummary::title).containsExactly("Kulaklık", "Tablet");
		assertThat(second.items()).extracting(ProductSummary::title).containsExactly("Telefon");
		assertThat(first.totalItems()).isEqualTo(3);
		assertThat(first.totalPages()).isEqualTo(2);
	}

	@Test
	void dealsOrderByLargestSavingAndSkipImplausibleListPrices() {
		var store = store("trendyol", "Trendyol");
		var source = source(store, LATER);
		observe(source, product(store, "1", "Kılıf", null), 0, "100", "500");
		observe(source, product(store, "2", "Telefon", null), 1, "60000", "65000");
		observe(source, product(store, "3", "Airfryer", null), 2, "7000", "8000");
		observe(source, product(store, "4", "Sahte indirim", null), 3, "10", "1000");
		observe(source, product(store, "5", "İndirimsiz", null), 4, "999", null);

		var deals = queries.deals(10);

		assertThat(deals).extracting(ProductSummary::title).containsExactly("Telefon", "Airfryer", "Kılıf");
		assertThat(deals).extracting(ProductSummary::discountPercent).containsExactly(7, 12, 80);
	}

	@Test
	void detailListsLatestOffersAndLowestPricePerObservation() {
		var store = store("n11", "N11");
		var tv = product(store, "N-55", "TCL 55T61C", "TCL");
		jdbc.sql("UPDATE marketplace_products SET mpn = 'T61C', category_path = ARRAY['Elektronik', 'TV'] "
				+ "WHERE id = :id").param("id", tv).update();
		var earlier = source(store, EARLIER);
		var later = source(store, LATER);
		observe(earlier, tv, 0, "35000", null);
		observe(earlier, tv, 1, "34000", null);
		observe(later, tv, 0, "33630", null);
		observe(later, tv, 1, "33000", "36000");

		var detail = queries.findDetail(tv).orElseThrow();

		assertThat(detail.externalId()).isEqualTo("N-55");
		assertThat(detail.mpn()).isEqualTo("T61C");
		assertThat(detail.categoryPath()).containsExactly("Elektronik", "TV");
		assertThat(detail.offers()).extracting(ProductDetail.Offer::price)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("33000"), new BigDecimal("33630"));
		assertThat(detail.priceHistory()).extracting(ProductDetail.PricePoint::observedAt)
				.containsExactly(EARLIER, LATER);
		assertThat(detail.priceHistory()).extracting(ProductDetail.PricePoint::price)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(new BigDecimal("34000"), new BigDecimal("33000"));
		assertThat(detail.product().discountPercent()).isEqualTo(8);
	}

	@Test
	void unknownProductHasNoDetail() {
		assertThat(queries.findDetail(42)).isEmpty();
	}

}
