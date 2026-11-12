package com.priceradar.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record ProductSummary(
		long id,
		String title,
		String brand,
		String category,
		String imageUrl,
		String barcode,
		StoreRef store,
		BigDecimal price,
		BigDecimal listPrice,
		Integer discountPercent,
		String currency,
		Boolean inStock,
		String url,
		Instant observedAt,
		int offerCount) {

	static Integer discountPercent(BigDecimal price, BigDecimal listPrice) {
		if (price == null || listPrice == null || listPrice.compareTo(price) <= 0) {
			return null;
		}
		return listPrice.subtract(price)
				.multiply(BigDecimal.valueOf(100))
				.divide(listPrice, 0, RoundingMode.DOWN)
				.intValueExact();
	}

}
