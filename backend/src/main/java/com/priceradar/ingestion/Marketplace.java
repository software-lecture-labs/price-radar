package com.priceradar.ingestion;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum Marketplace {

	HEPSIBURADA("hepsiburada", "Hepsiburada", "https://www.hepsiburada.com", "/results", Map.of(
			Field.ID, "/sku",
			Field.LIST_PRICE, "/original_price",
			Field.REVIEW_COUNT, "/rating_count")),

	N11("n11", "N11", "https://www.n11.com", "/products", Map.of(
			Field.ID, "/product_id",
			Field.SELLER_NAME, "/seller",
			Field.IN_STOCK, "/in_stock",
			Field.REVIEW_COUNT, "/rating_count")),

	TEKNOSA("teknosa", "Teknosa", "https://www.teknosa.com", "/results", Map.of(
			Field.ID, "/product_id",
			Field.LIST_PRICE, "/was_price",
			Field.MEMBER_PRICE, "/member_price",
			Field.SELLER_ID, "/seller/shop_id",
			Field.SELLER_NAME, "/seller/name",
			Field.SELLER_IS_STORE, "/seller/is_teknosa",
			Field.IN_STOCK, "/in_stock",
			Field.FREE_SHIPPING, "/free_shipping",
			Field.SPONSORED, "/sponsored",
			Field.REVIEW_COUNT, "/review_count")),

	TRENDYOL("trendyol", "Trendyol", "https://www.trendyol.com", "/results", Map.of(
			Field.ID, "/content_id",
			Field.GROUP_ID, "/product_group_id",
			// "price" is display text such as "1.234,56 TL"; the number is in price_value.
			Field.PRICE, "/price_value",
			Field.LIST_PRICE, "/list_price_value",
			Field.SELLER_ID, "/merchant_id",
			Field.FREE_SHIPPING, "/free_cargo",
			Field.REVIEW_COUNT, "/comment_count"));

	public enum Field {

		ID(null),
		GROUP_ID(null),
		TITLE("/title"),
		BRAND("/brand"),
		CATEGORY("/category"),
		CATEGORY_PATH("/category_path"),
		IMAGE_URL("/image"),
		BARCODE("/barcode"),
		MPN("/mpn"),
		URL("/url"),
		PRICE("/price"),
		LIST_PRICE(null),
		MEMBER_PRICE(null),
		CURRENCY("/currency"),
		SELLER_ID(null),
		SELLER_NAME(null),
		SELLER_IS_STORE(null),
		IN_STOCK(null),
		FREE_SHIPPING(null),
		SPONSORED(null),
		RATING("/rating"),
		RATING_SCALE("/rating_scale"),
		REVIEW_COUNT(null);

		private final String defaultPointer;

		Field(String defaultPointer) {
			this.defaultPointer = defaultPointer;
		}

		public String defaultPointer() {
			return defaultPointer;
		}

	}

	private final String code;
	private final String displayName;
	private final String baseUrl;
	private final String resultsPointer;
	private final Map<Field, String> pointerOverrides;

	Marketplace(String code, String displayName, String baseUrl, String resultsPointer,
			Map<Field, String> pointerOverrides) {
		this.code = code;
		this.displayName = displayName;
		this.baseUrl = baseUrl;
		this.resultsPointer = resultsPointer;
		this.pointerOverrides = pointerOverrides;
	}

	public static Optional<Marketplace> fromCode(String code) {
		return Arrays.stream(values()).filter(marketplace -> marketplace.code.equals(code)).findFirst();
	}

	public String code() {
		return code;
	}

	public String displayName() {
		return displayName;
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String resultsPointer() {
		return resultsPointer;
	}

	public String pointerTo(Field field) {
		return pointerOverrides.getOrDefault(field, field.defaultPointer());
	}

}
