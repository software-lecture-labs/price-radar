/**
 * Prices, discounts and price history per merchant listing. Price observations
 * are append-only and carry amount, ISO 4217 currency and observation time.
 * Publishes an event when a listing's current price changes.
 *
 * <p>Planned; no code yet.
 */
@ApplicationModule(displayName = "Pricing", allowedDependencies = {})
package com.priceradar.pricing;

import org.springframework.modulith.ApplicationModule;
