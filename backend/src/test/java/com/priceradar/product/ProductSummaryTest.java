package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductSummaryTest {

	@Test
	void discountIsRoundedDownToWholePercent() {
		assertThat(ProductSummary.discountPercent(new BigDecimal("11479"), new BigDecimal("13999"))).isEqualTo(18);
	}

	@Test
	void noDiscountWithoutAHigherListPrice() {
		assertThat(ProductSummary.discountPercent(new BigDecimal("100"), null)).isNull();
		assertThat(ProductSummary.discountPercent(new BigDecimal("100"), new BigDecimal("100"))).isNull();
		assertThat(ProductSummary.discountPercent(new BigDecimal("100"), new BigDecimal("90"))).isNull();
	}

	@Test
	void likeWildcardsAreEscaped() {
		assertThat(ProductQueries.escapeLike("100%_off\\")).isEqualTo("100\\%\\_off\\\\");
	}

}
