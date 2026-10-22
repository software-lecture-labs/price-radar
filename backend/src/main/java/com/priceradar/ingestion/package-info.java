/**
 * Merchant integrations: scheduled collection from merchant APIs and product
 * feeds, normalization and validation. Writes listings through {@code merchant}
 * and price observations through {@code pricing}; owns no prices itself.
 *
 * <p>Planned; no code yet.
 */
@ApplicationModule(displayName = "Ingestion", allowedDependencies = { "merchant", "pricing" })
package com.priceradar.ingestion;

import org.springframework.modulith.ApplicationModule;
