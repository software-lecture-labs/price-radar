/**
 * Elasticsearch indexing and product search. The index is a read model rebuilt
 * from PostgreSQL and kept current by listening to catalog, merchant, matching
 * and pricing events; it is never the source of truth.
 *
 * <p>Planned; only {@link com.priceradar.search.SearchProperties} exists so far.
 */
@ApplicationModule(displayName = "Search", allowedDependencies = { "catalog", "merchant", "matching", "pricing" })
package com.priceradar.search;

import org.springframework.modulith.ApplicationModule;
