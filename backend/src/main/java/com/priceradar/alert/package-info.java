/**
 * Price alerts and the notifications they trigger (e-mail, push). Reacts to
 * price-change events asynchronously so no API request waits on delivery.
 *
 * <p>Planned; no code yet.
 */
@ApplicationModule(displayName = "Alert", allowedDependencies = { "pricing", "matching", "identity" })
package com.priceradar.alert;

import org.springframework.modulith.ApplicationModule;
