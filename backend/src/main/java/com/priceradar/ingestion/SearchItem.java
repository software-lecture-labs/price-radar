package com.priceradar.ingestion;

import java.math.BigDecimal;
import java.util.List;

public record SearchItem(String externalId, Listing listing, Offer offer, String rawJson) {

	public record Listing(
			String externalGroupId,
			String title,
			String brand,
			String category,
			List<String> categoryPath,
			String imageUrl,
			String barcode,
			String mpn) {

		public Listing {
			categoryPath = categoryPath == null ? null : List.copyOf(categoryPath);
		}

	}

	public record Offer(
			String url,
			BigDecimal price,
			String currency,
			BigDecimal listPrice,
			BigDecimal memberPrice,
			String sellerExternalId,
			String sellerName,
			Boolean sellerIsStore,
			Boolean inStock,
			Boolean freeShipping,
			Boolean sponsored,
			BigDecimal rating,
			BigDecimal ratingScale,
			Integer reviewCount) {
	}

}
