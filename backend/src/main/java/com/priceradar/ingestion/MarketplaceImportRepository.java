package com.priceradar.ingestion;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceImportRepository {

	private final JdbcClient jdbc;

	public MarketplaceImportRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record RecordedSource(long id, String path, String sha256) {
	}

	// Serialize imports so concurrent transactions cannot both insert the same new product.
	public void lockForImport() {
		jdbc.sql("SELECT true FROM pg_advisory_xact_lock(hashtext('price-radar.marketplace-import'))")
				.query(Boolean.class)
				.single();
	}

	public List<RecordedSource> findSources(String path, String sha256) {
		return jdbc.sql("""
				SELECT id, source_path, sha256
				FROM source_files
				WHERE source_path = :path OR sha256 = :sha256
				""")
				.param("path", path)
				.param("sha256", sha256)
				.query((rs, rowNum) -> new RecordedSource(rs.getLong("id"), rs.getString("source_path"),
						rs.getString("sha256")))
				.list();
	}

	public Optional<Long> findStoreId(Marketplace marketplace) {
		return jdbc.sql("SELECT id FROM stores WHERE code = :code")
				.param("code", marketplace.code())
				.query(Long.class)
				.optional();
	}

	public long ensureStore(Marketplace marketplace) {
		jdbc.sql("""
				INSERT INTO stores (code, name, base_url)
				VALUES (:code, :name, :baseUrl)
				ON CONFLICT (code) DO NOTHING
				""")
				.param("code", marketplace.code())
				.param("name", marketplace.displayName())
				.param("baseUrl", marketplace.baseUrl())
				.update();
		return findStoreId(marketplace).orElseThrow();
	}

	public long insertSource(SearchFile file, long storeId) {
		return jdbc.sql("""
				INSERT INTO source_files (store_id, source_path, sha256, action, fetched_at, result_count, raw_document)
				VALUES (:storeId, :path, :sha256, 'search', :fetchedAt, :resultCount, CAST(:rawDocument AS jsonb))
				RETURNING id
				""")
				.param("storeId", storeId)
				.param("path", file.path())
				.param("sha256", file.sha256())
				.param("fetchedAt", file.fetchedAt())
				.param("resultCount", file.items().size())
				.param("rawDocument", file.rawJson())
				.query(Long.class)
				.single();
	}

	public Map<String, Long> findProductIds(long storeId, Collection<String> externalIds) {
		if (externalIds.isEmpty()) {
			return Map.of();
		}
		return jdbc.sql("""
				SELECT external_id, id
				FROM marketplace_products
				WHERE store_id = :storeId AND external_id IN (:externalIds)
				""")
				.param("storeId", storeId)
				.param("externalIds", externalIds)
				.query((rs, rowNum) -> Map.entry(rs.getString("external_id"), rs.getLong("id")))
				.list()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public long insertProduct(long storeId, String externalId, SearchItem.Listing listing) {
		var categoryPath = listing.categoryPath() == null ? null : listing.categoryPath().toArray(String[]::new);
		return jdbc.sql("""
				INSERT INTO marketplace_products
					(store_id, external_id, external_group_id, title, brand, category, category_path,
					image_url, barcode, mpn)
				VALUES
					(:storeId, :externalId, :externalGroupId, :title, :brand, :category, :categoryPath,
					:imageUrl, :barcode, :mpn)
				RETURNING id
				""")
				.param("storeId", storeId)
				.param("externalId", externalId)
				.param("externalGroupId", listing.externalGroupId())
				.param("title", listing.title())
				.param("brand", listing.brand())
				.param("category", listing.category())
				.param("categoryPath", categoryPath)
				.param("imageUrl", listing.imageUrl())
				.param("barcode", listing.barcode())
				.param("mpn", listing.mpn())
				.query(Long.class)
				.single();
	}

	public void insertObservation(long storeId, long productId, long sourceId, int rowIndex, OffsetDateTime observedAt,
			SearchItem item) {

		var offer = item.offer();
		jdbc.sql("""
				INSERT INTO product_observations
					(store_id, marketplace_product_id, source_file_id, row_index, observed_at, source_url,
					price, currency, list_price, member_price, seller_external_id, seller_name, seller_is_store,
					in_stock, free_shipping, sponsored, rating, rating_scale, review_count, raw_item)
				VALUES
					(:storeId, :productId, :sourceId, :rowIndex, :observedAt, :url,
					:price, :currency, :listPrice, :memberPrice, :sellerExternalId, :sellerName, :sellerIsStore,
					:inStock, :freeShipping, :sponsored, :rating, :ratingScale, :reviewCount, CAST(:rawItem AS jsonb))
				""")
				.param("storeId", storeId)
				.param("productId", productId)
				.param("sourceId", sourceId)
				.param("rowIndex", rowIndex)
				.param("observedAt", observedAt)
				.param("url", offer.url())
				.param("price", offer.price())
				.param("currency", offer.currency())
				.param("listPrice", offer.listPrice())
				.param("memberPrice", offer.memberPrice())
				.param("sellerExternalId", offer.sellerExternalId())
				.param("sellerName", offer.sellerName())
				.param("sellerIsStore", offer.sellerIsStore())
				.param("inStock", offer.inStock())
				.param("freeShipping", offer.freeShipping())
				.param("sponsored", offer.sponsored())
				.param("rating", offer.rating())
				.param("ratingScale", offer.ratingScale())
				.param("reviewCount", offer.reviewCount())
				.param("rawItem", item.rawJson())
				.update();
	}

}
