package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.priceradar.product.CatalogQueries.BrandSummary;
import com.priceradar.product.CatalogQueries.CategorySummary;
import com.priceradar.product.CatalogQueries.StoreSummary;

class CatalogQueriesTest extends MarketplaceDatabaseTest {

	private CatalogQueries catalog;

	@BeforeEach
	void createQueries() {
		catalog = new CatalogQueries(jdbc);
	}

	@Test
	void storesAreListedByNameWithTheirProductCount() {
		var teknosa = store("teknosa", "Teknosa");
		store("n11", "N11");
		product(teknosa, "1", "Telefon", null);
		product(teknosa, "2", "Tablet", null);

		assertThat(catalog.stores()).containsExactly(
				new StoreSummary("n11", "N11", "https://www.n11.com", 0),
				new StoreSummary("teknosa", "Teknosa", "https://www.teknosa.com", 2));
	}

	@Test
	void categoriesWithMostProductsComeFirstWithTheFirstAvailablePhoto() {
		var store = store("teknosa", "Teknosa");
		product(store, "1", "Tablet A", null, "Tablet", null);
		product(store, "2", "Tablet B", null, "Tablet", "https://cdn.example.com/b.jpg");
		product(store, "3", "Tablet C", null, "Tablet", "https://cdn.example.com/c.jpg");
		product(store, "4", "Telefon", null, "Cep Telefonu", null);
		product(store, "5", "Kategorisiz", null, null, null);

		assertThat(catalog.topCategories(10)).containsExactly(
				new CategorySummary("Tablet", 3, "https://cdn.example.com/b.jpg"),
				new CategorySummary("Cep Telefonu", 1, null));
		assertThat(catalog.topCategories(1)).hasSize(1);
	}

	@Test
	void brandsWithMostProductsComeFirst() {
		var store = store("n11", "N11");
		product(store, "1", "iPhone", "Apple");
		product(store, "2", "iPad", "Apple");
		product(store, "3", "Galaxy", "Samsung");
		product(store, "4", "Markasız", null);

		assertThat(catalog.topBrands(5)).containsExactly(
				new BrandSummary("Apple", 2),
				new BrandSummary("Samsung", 1));
	}

}
