package com.priceradar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	OpenAPI priceRadarOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Price Radar API")
						.description("Product, offer and price-history API for the Price Radar comparison platform.")
						.version("v1")
						.contact(new Contact().name("Price Radar"))
						.license(new License().name("Proprietary")))
				.components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Access token obtained from POST /api/v1/auth/login")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
	}

}
