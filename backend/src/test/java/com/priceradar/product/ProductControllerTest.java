package com.priceradar.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.priceradar.common.web.PageResponse;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

	private static final ProductSummary PHONE = new ProductSummary(7, "Apple iPhone 17 Pro Max 256 GB", "Apple",
			"Cep Telefonu", "https://cdn.example.com/p.jpg", null, new StoreRef("hepsiburada", "Hepsiburada"),
			new BigDecimal("115199.00"), new BigDecimal("129999.00"), 11, "TRY", null,
			"https://www.hepsiburada.com/p", Instant.parse("2026-10-22T08:47:04Z"), 1);

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private ProductQueries products;

	@Test
	void searchTrimsTheQueryAndReturnsAPage() {
		given(products.search("iphone", "hepsiburada", ProductSort.PRICE_ASC, 1, 12))
				.willReturn(PageResponse.of(List.of(PHONE), 1, 12, 13));

		var body = assertThat(mockMvc.get()
				.uri("/api/v1/products?q= iphone &store=hepsiburada&sort=price_asc&page=1&size=12"))
				.hasStatusOk()
				.bodyJson();

		body.extractingPath("$.items[0].title").asString().isEqualTo("Apple iPhone 17 Pro Max 256 GB");
		body.extractingPath("$.items[0].store.name").asString().isEqualTo("Hepsiburada");
		body.extractingPath("$.items[0].price").asNumber()
				.satisfies(price -> assertThat(price.doubleValue()).isEqualTo(115199.0));
		body.extractingPath("$.items[0].observedAt").asString().isEqualTo("2026-10-22T08:47:04Z");
		body.extractingPath("$.totalItems").asNumber().isEqualTo(13);
		body.extractingPath("$.totalPages").asNumber().isEqualTo(2);
	}

	@Test
	void blankQueryMeansNoFilter() {
		given(products.search(null, null, ProductSort.NEWEST, 0, 24))
				.willReturn(PageResponse.of(List.of(), 0, 24, 0));

		assertThat(mockMvc.get().uri("/api/v1/products?q=%20")).hasStatusOk();
	}

	@Test
	void rejectsOutOfRangeAndUnknownParameters() {
		assertThat(mockMvc.get().uri("/api/v1/products?size=101")).hasStatus(400);
		assertThat(mockMvc.get().uri("/api/v1/products?sort=cheapest")).hasStatus(400);
		assertThat(mockMvc.get().uri("/api/v1/products/deals?limit=0")).hasStatus(400);
		verifyNoInteractions(products);
	}

	@Test
	void dealsReturnsTheList() {
		given(products.deals(5)).willReturn(List.of(PHONE));

		assertThat(mockMvc.get().uri("/api/v1/products/deals")).hasStatusOk()
				.bodyJson()
				.extractingPath("$[0].discountPercent")
				.asNumber()
				.isEqualTo(11);
	}

	@Test
	void unknownProductIsAProblem404() {
		given(products.findDetail(99)).willReturn(Optional.empty());

		assertThat(mockMvc.get().uri("/api/v1/products/99")).hasStatus(404)
				.bodyJson()
				.extractingPath("$.title")
				.asString()
				.isEqualTo("Resource not found");
	}

}
