package com.priceradar.product;

import java.util.Locale;

public enum ProductSort {

	NEWEST("p.id DESC"),
	PRICE_ASC("cur.price ASC, p.id DESC"),
	PRICE_DESC("cur.price DESC, p.id DESC");

	static final String PARAM_PATTERN = "newest|price_asc|price_desc";

	final String orderBy;

	ProductSort(String orderBy) {
		this.orderBy = orderBy;
	}

	static ProductSort fromParam(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}

}
