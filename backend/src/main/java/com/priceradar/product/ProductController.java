package com.priceradar.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.priceradar.common.exception.ResourceNotFoundException;
import com.priceradar.common.web.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Marketplace products and their observed prices")
public class ProductController {

	private final ProductQueries products;

	public ProductController(ProductQueries products) {
		this.products = products;
	}

	@GetMapping
	@Operation(summary = "Search products by title or brand")
	public PageResponse<ProductSummary> search(
			@RequestParam(name = "q", required = false) @Size(max = 100) String query,
			@RequestParam(required = false) @Pattern(regexp = "[a-z0-9][a-z0-9_-]*") String store,
			@RequestParam(defaultValue = "newest") @Pattern(regexp = ProductSort.PARAM_PATTERN) String sort,
			@RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
			@RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {

		var trimmed = query == null || query.isBlank() ? null : query.strip();
		return products.search(trimmed, store, ProductSort.fromParam(sort), page, size);
	}

	@GetMapping("/deals")
	@Operation(summary = "Products priced furthest below the store's own list price")
	public List<ProductSummary> deals(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
		return products.deals(limit);
	}

	@GetMapping("/{id}")
	@Operation(summary = "One product with its current offers and price history")
	public ProductDetail detail(@PathVariable long id) {
		return products.findDetail(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
	}

}
