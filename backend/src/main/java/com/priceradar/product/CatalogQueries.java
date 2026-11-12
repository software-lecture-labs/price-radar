package com.priceradar.product;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogQueries {

	private final JdbcClient jdbc;

	public CatalogQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record StoreSummary(String code, String name, String baseUrl, long productCount) {
	}

	public record CategorySummary(String name, long productCount, String imageUrl) {
	}

	public record BrandSummary(String name, long productCount) {
	}

	public List<StoreSummary> stores() {
		return jdbc.sql("""
				SELECT s.code, s.name, s.base_url, count(p.id) AS product_count
				FROM stores s
				LEFT JOIN marketplace_products p ON p.store_id = s.id
				GROUP BY s.id
				ORDER BY s.name
				""")
				.query((rs, rowNum) -> new StoreSummary(rs.getString("code"), rs.getString("name"),
						rs.getString("base_url"), rs.getLong("product_count")))
				.list();
	}

	public List<CategorySummary> topCategories(int limit) {
		return jdbc.sql("""
				SELECT category, count(*) AS product_count,
					(array_agg(image_url ORDER BY id) FILTER (WHERE image_url IS NOT NULL))[1] AS image_url
				FROM marketplace_products
				WHERE category IS NOT NULL
				GROUP BY category
				ORDER BY product_count DESC, category
				LIMIT :limit
				""")
				.param("limit", limit)
				.query((rs, rowNum) -> new CategorySummary(rs.getString("category"), rs.getLong("product_count"),
						rs.getString("image_url")))
				.list();
	}

	public List<BrandSummary> topBrands(int limit) {
		return jdbc.sql("""
				SELECT brand, count(*) AS product_count
				FROM marketplace_products
				WHERE brand IS NOT NULL
				GROUP BY brand
				ORDER BY product_count DESC, brand
				LIMIT :limit
				""")
				.param("limit", limit)
				.query((rs, rowNum) -> new BrandSummary(rs.getString("brand"), rs.getLong("product_count")))
				.list();
	}

}
