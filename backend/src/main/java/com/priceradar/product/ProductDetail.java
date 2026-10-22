package com.priceradar.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetail(
		ProductSummary product,
		String externalId,
		String externalGroupId,
		String mpn,
		List<String> categoryPath,
		List<Offer> offers,
		List<ComparisonOffer> comparisonOffers,
		List<PricePoint> priceHistory) {

	public record ComparisonOffer(
			StoreRef store,
			Long productId,
			String title,
			BigDecimal price,
			BigDecimal listPrice,
			String currency,
			Boolean inStock,
			Boolean freeShipping,
			String url,
			Instant observedAt,
			String sellerName,
			int offerCount,
			String matchType) {
	}

	public record Offer(
			String sellerName,
			BigDecimal price,
			BigDecimal listPrice,
			String currency,
			Boolean inStock,
			Boolean freeShipping,
			String url,
			Instant observedAt) {
	}

	public record PricePoint(Instant observedAt, BigDecimal price) {
	}

}
