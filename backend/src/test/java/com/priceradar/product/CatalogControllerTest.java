package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.priceradar.product.CatalogQueries.BrandSummary;
import com.priceradar.product.CatalogQueries.CategorySummary;
import com.priceradar.product.CatalogQueries.StoreSummary;

@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private CatalogQueries catalog;

	@Test
	void listsStores() {
		given(catalog.stores()).willReturn(List.of(new StoreSummary("n11", "N11", "https://www.n11.com", 816)));

		var body = assertThat(mockMvc.get().uri("/api/v1/stores")).hasStatusOk().bodyJson();

		body.extractingPath("$[0].code").asString().isEqualTo("n11");
		body.extractingPath("$[0].productCount").asNumber().isEqualTo(816);
	}

	@Test
	void categoriesAndBrandsUseTheirDefaultLimits() {
		given(catalog.topCategories(12)).willReturn(List.of(new CategorySummary("Tablet", 229, null)));
		given(catalog.topBrands(5)).willReturn(List.of(new BrandSummary("Apple", 456)));

		assertThat(mockMvc.get().uri("/api/v1/categories")).hasStatusOk()
				.bodyJson().extractingPath("$[0].name").asString().isEqualTo("Tablet");
		assertThat(mockMvc.get().uri("/api/v1/brands")).hasStatusOk()
				.bodyJson().extractingPath("$[0].productCount").asNumber().isEqualTo(456);
	}

	@Test
	void rejectsOutOfRangeLimits() {
		assertThat(mockMvc.get().uri("/api/v1/categories?limit=0")).hasStatus(400);
		assertThat(mockMvc.get().uri("/api/v1/brands?limit=51")).hasStatus(400);
		verifyNoInteractions(catalog);
	}

}
