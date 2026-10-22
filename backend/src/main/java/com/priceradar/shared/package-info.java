/**
 * Shared kernel: domain exceptions every module may throw.
 *
 * <p>Declared as a shared module on {@code PriceRadarBackendApplication}, so any
 * module may depend on it without listing it. {@code shared.web} turns these
 * exceptions into RFC 9457 problem responses and is internal to this module.
 * Keep it small; anything with business meaning belongs in a domain module.
 */
@ApplicationModule(displayName = "Shared kernel", allowedDependencies = {})
package com.priceradar.shared;

import org.springframework.modulith.ApplicationModule;
