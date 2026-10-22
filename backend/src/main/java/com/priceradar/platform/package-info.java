/**
 * Infrastructure wiring that no domain module owns: the Redis cache manager,
 * Kafka topics and listener error handling, and the OpenAPI description.
 *
 * <p>Depends on no other module.
 */
@ApplicationModule(displayName = "Platform", allowedDependencies = {})
package com.priceradar.platform;

import org.springframework.modulith.ApplicationModule;
