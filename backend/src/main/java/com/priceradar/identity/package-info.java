/**
 * Authentication and authorization: JWT issuing and verification and the
 * stateless security filter chain.
 *
 * <p>{@link com.priceradar.identity.JwtService} is the module API. The filter
 * chain in {@code identity.internal} is not visible to other modules. Login and
 * refresh endpoints are planned here.
 */
@ApplicationModule(displayName = "Identity", allowedDependencies = {})
package com.priceradar.identity;

import org.springframework.modulith.ApplicationModule;
