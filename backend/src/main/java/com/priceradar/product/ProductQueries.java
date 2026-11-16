package com.priceradar.product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.priceradar.common.web.PageResponse;

/** Current prices use the cheapest offer at the latest observation time. */
@Repository
public class ProductQueries {

	private static final String SUMMARY_SELECT = """
			SELECT p.id, p.title, p.brand, p.category, p.image_url, p.barcode,
				s.code AS store_code, s.name AS store_name,
				cur.price, cur.list_price, cur.currency, cur.in_stock, cur.source_url, cur.observed_at,
				cur.offer_count
			FROM marketplace_products p
			JOIN stores s ON s.id = p.store_id
			JOIN LATERAL (
				SELECT o.price, o.list_price, o.currency, o.in_stock, o.source_url, o.observed_at,
					count(*) OVER () AS offer_count
				FROM product_observations o
				WHERE o.marketplace_product_id = p.id
					AND o.store_id = p.store_id
					AND o.observed_at = (
						SELECT max(m.observed_at)
						FROM product_observations m
						WHERE m.marketplace_product_id = p.id AND m.store_id = p.store_id)
				ORDER BY o.price, o.id DESC
				LIMIT 1
			) cur ON true
			""";

	private static final String HAS_OBSERVATION = """
			EXISTS (SELECT 1 FROM product_observations o
				WHERE o.marketplace_product_id = p.id AND o.store_id = p.store_id)""";

	// Discounts over 90% usually indicate an inflated list price.
	private static final String PLAUSIBLE_DISCOUNT = "cur.list_price > cur.price AND cur.price >= cur.list_price * 0.1";

	private final JdbcClient jdbc;

	public ProductQueries(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public PageResponse<ProductSummary> search(String query, String storeCode, ProductSort sort, int page,
			int size) {

		var conditions = new ArrayList<String>();
		var params = new HashMap<String, Object>();
		if (query != null) {
			conditions.add("(p.title ILIKE :pattern OR p.brand ILIKE :pattern)");
			params.put("pattern", "%" + escapeLike(query) + "%");
		}
		if (storeCode != null) {
			conditions.add("s.code = :store");
			params.put("store", storeCode);
		}

		var items = jdbc.sql(SUMMARY_SELECT + where(conditions) + "ORDER BY " + sort.orderBy
				+ "\nLIMIT :limit OFFSET :offset")
				.params(params)
				.param("limit", size)
				.param("offset", (long) page * size)
				.query(ProductQueries::mapSummary)
				.list();

		conditions.add(HAS_OBSERVATION);
		long total = jdbc.sql("SELECT count(*) FROM marketplace_products p JOIN stores s ON s.id = p.store_id\n"
				+ where(conditions))
				.params(params)
				.query(Long.class)
				.single();

		return PageResponse.of(items, page, size, total);
	}

	// Rank by money saved so cheap accessories do not dominate on percentage alone.
	public List<ProductSummary> deals(int limit) {
		return jdbc.sql(SUMMARY_SELECT + "WHERE " + PLAUSIBLE_DISCOUNT
				+ "\nORDER BY cur.list_price - cur.price DESC, p.id DESC\nLIMIT :limit")
				.param("limit", limit)
				.query(ProductQueries::mapSummary)
				.list();
	}

	@Transactional(readOnly = true)
	public Optional<ProductDetail> findDetail(long id) {
		var summary = jdbc.sql(SUMMARY_SELECT + "WHERE p.id = :id")
				.param("id", id)
				.query(ProductQueries::mapSummary)
				.optional();
		if (summary.isEmpty()) {
			return Optional.empty();
		}

		var offers = jdbc.sql("""
				SELECT o.seller_name, o.price, o.list_price, o.currency, o.in_stock, o.free_shipping,
					o.source_url, o.observed_at
				FROM product_observations o
				WHERE o.marketplace_product_id = :id
					AND o.observed_at = (
						SELECT max(observed_at) FROM product_observations WHERE marketplace_product_id = :id)
				ORDER BY o.price, o.id
				""")
				.param("id", id)
				.query((rs, rowNum) -> new ProductDetail.Offer(rs.getString("seller_name"),
						rs.getBigDecimal("price"), rs.getBigDecimal("list_price"), rs.getString("currency"),
						rs.getObject("in_stock", Boolean.class), rs.getObject("free_shipping", Boolean.class),
						rs.getString("source_url"), instant(rs, "observed_at")))
				.list();

		var history = jdbc.sql("""
				SELECT observed_at, min(price) AS price
				FROM product_observations
				WHERE marketplace_product_id = :id
				GROUP BY observed_at
				ORDER BY observed_at
				""")
				.param("id", id)
				.query((rs, rowNum) -> new ProductDetail.PricePoint(instant(rs, "observed_at"),
						rs.getBigDecimal("price")))
				.list();

		return jdbc.sql("SELECT external_id, external_group_id, mpn, category_path FROM marketplace_products "
				+ "WHERE id = :id")
				.param("id", id)
				.query((rs, rowNum) -> new ProductDetail(summary.get(), rs.getString("external_id"),
						rs.getString("external_group_id"), rs.getString("mpn"), textArray(rs, "category_path"),
						offers, comparisonOffers(summary.get(), rs.getString("mpn")), history))
				.optional();
	}

	private List<ProductDetail.ComparisonOffer> comparisonOffers(ProductSummary product, String mpn) {
		var selected = new ProductComparison.Identity(product.id(), product.title(), product.brand(),
				product.barcode(), mpn, product.currency());
		// Fetch potential exact identities, then validate GTIN, brand, variants and currency in Java.
		// Marketplace group IDs and search terms are deliberately excluded: neither identifies a variant.
		var candidates = jdbc.sql("""
				SELECT p.id, p.title, p.brand, p.barcode, p.mpn, s.code AS store_code, s.name AS store_name,
					o.price, o.list_price, o.currency, o.in_stock, o.free_shipping, o.source_url,
					o.observed_at, o.seller_name, count(*) OVER (PARTITION BY p.id) AS offer_count
				FROM marketplace_products p
				JOIN stores s ON s.id = p.store_id
				JOIN product_observations o ON o.marketplace_product_id = p.id AND o.store_id = p.store_id
				WHERE o.observed_at = (
					SELECT max(latest.observed_at) FROM product_observations latest
					WHERE latest.marketplace_product_id = p.id AND latest.store_id = p.store_id)
					AND (p.id = :id
						OR ltrim(regexp_replace(p.barcode, '\\s+', '', 'g'), '0') = :barcode
						OR btrim(regexp_replace(lower(p.title), '\\s+', ' ', 'g')) = :title
						OR (:mpn <> '' AND regexp_replace(lower(p.mpn), '\\s+', '', 'g') = :mpn
							AND btrim(regexp_replace(lower(p.brand), '\\s+', ' ', 'g')) = :brand))
				ORDER BY p.id, o.id
				""")
				.param("id", product.id())
				.param("barcode", product.barcode() == null ? null
						: product.barcode().replaceAll("\\s+", "").replaceFirst("^0+", ""))
				.param("title", ProductComparison.normalize(product.title()))
				.param("mpn", ProductComparison.normalizeMpn(mpn))
				.param("brand", ProductComparison.normalize(product.brand()))
				.query((rs, rowNum) -> new ProductComparison.Candidate(
						new ProductComparison.Identity(rs.getLong("id"), rs.getString("title"), rs.getString("brand"),
								rs.getString("barcode"), rs.getString("mpn"), rs.getString("currency")),
						new StoreRef(rs.getString("store_code"), rs.getString("store_name")),
						rs.getBigDecimal("price"), rs.getBigDecimal("list_price"),
						rs.getObject("in_stock", Boolean.class), rs.getObject("free_shipping", Boolean.class),
						rs.getString("source_url"), instant(rs, "observed_at"), rs.getString("seller_name"),
						rs.getInt("offer_count")))
				.list();
		return ProductComparison.offers(selected, candidates);
	}

	private static ProductSummary mapSummary(ResultSet rs, int rowNum) throws SQLException {
		var price = rs.getBigDecimal("price");
		var listPrice = rs.getBigDecimal("list_price");
		return new ProductSummary(rs.getLong("id"), rs.getString("title"), rs.getString("brand"),
				rs.getString("category"), rs.getString("image_url"), rs.getString("barcode"),
				new StoreRef(rs.getString("store_code"), rs.getString("store_name")), price, listPrice,
				ProductSummary.discountPercent(price, listPrice), rs.getString("currency"),
				rs.getObject("in_stock", Boolean.class), rs.getString("source_url"), instant(rs, "observed_at"),
				rs.getInt("offer_count"));
	}

	private static String where(List<String> conditions) {
		return conditions.isEmpty() ? "" : "WHERE " + String.join("\nAND ", conditions) + "\n";
	}

	static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Instant instant(ResultSet rs, String column) throws SQLException {
		var value = rs.getObject(column, OffsetDateTime.class);
		return value == null ? null : value.toInstant();
	}

	private static List<String> textArray(ResultSet rs, String column) throws SQLException {
		var array = rs.getArray(column);
		return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
	}

}
