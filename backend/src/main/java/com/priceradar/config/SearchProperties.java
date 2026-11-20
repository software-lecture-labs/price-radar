package com.priceradar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(@NotBlank String indexPrefix) {

	public String indexFor(String documentType) {
		return indexPrefix + "-" + documentType;
	}

}
