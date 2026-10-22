/**
 * Decides which canonical product a merchant listing is. Uses GTIN, merchant
 * SKU and variant attributes (colour, capacity); a name-only match is never
 * confirmed automatically and goes to review instead.
 *
 * <p>Planned; no code yet.
 */
@ApplicationModule(displayName = "Matching", allowedDependencies = { "catalog", "merchant" })
package com.priceradar.matching;

import org.springframework.modulith.ApplicationModule;
