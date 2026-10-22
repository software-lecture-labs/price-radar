/**
 * Service metadata endpoints ({@code /api/v1/ping}, {@code /api/v1/hello})
 * used to verify the deployment end to end. Holds no business logic.
 */
@ApplicationModule(displayName = "System", allowedDependencies = {})
package com.priceradar.system;

import org.springframework.modulith.ApplicationModule;
