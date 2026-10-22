/**
 * Canonical products, brands, categories and attributes: the product a shopper
 * searches for, independent of who sells it. One canonical product per variant
 * (identified by GTIN where available), never one per merchant.
 *
 * <p>Planned; no code yet.
 */
@ApplicationModule(displayName = "Catalog", allowedDependencies = {})
package com.priceradar.catalog;

import org.springframework.modulith.ApplicationModule;
