package com.priceradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Binds {@code app.search.*}. Keeps Elasticsearch index naming in one place so
 * indices can be namespaced per environment.
 */
@Validated
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(@NotBlank String indexPrefix) {

	/** Index name for a logical document type, e.g. {@code price-radar-products}. */
	public String indexFor(String documentType) {
		return indexPrefix + "-" + documentType;
	}

}
