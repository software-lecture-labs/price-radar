package com.priceradar.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Web slice only: no database, cache, broker or security filters involved.
 */
@WebMvcTest(MetaController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.application.name=price-radar-backend")
class MetaControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@Test
	void pingReportsServiceIdentity() {
		var body = assertThat(mockMvc.get().uri("/api/v1/ping")).hasStatusOk().bodyJson();

		body.extractingPath("$.service").asString().isEqualTo("price-radar-backend");
		body.extractingPath("$.apiVersion").asString().isEqualTo("v1");
		body.extractingPath("$.timestamp").asString().isNotBlank();
	}

}
