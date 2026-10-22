package com.priceradar.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.priceradar.product.CatalogQueries.BrandSummary;
import com.priceradar.product.CatalogQueries.CategorySummary;
import com.priceradar.product.CatalogQueries.StoreSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalogue", description = "Stores, categories and brands of the imported products")
public class CatalogController {

	private final CatalogQueries catalog;

	public CatalogController(CatalogQueries catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/stores")
	@Operation(summary = "Stores whose prices are tracked")
	public List<StoreSummary> stores() {
		return catalog.stores();
	}

	@GetMapping("/categories")
	@Operation(summary = "Categories with the most products")
	public List<CategorySummary> categories(@RequestParam(defaultValue = "12") @Min(1) @Max(50) int limit) {
		return catalog.topCategories(limit);
	}

	@GetMapping("/brands")
	@Operation(summary = "Brands with the most products")
	public List<BrandSummary> brands(@RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit) {
		return catalog.topBrands(limit);
	}

}
