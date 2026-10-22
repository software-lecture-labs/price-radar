package com.priceradar.ingestion;

import java.math.BigDecimal;
import java.util.List;

/**
 * One result row of a search response: the product as the marketplace lists it,
 * and the offer seen for it. Values a marketplace did not provide are
 * {@code null}; nothing is guessed.
 */
public record SearchItem(String externalId, Listing listing, Offer offer, String rawJson) {

	/** Catalogue values, stored once per marketplace product. */
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

	/** What was offered at observation time; one row per offer, even for repeated products. */
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
