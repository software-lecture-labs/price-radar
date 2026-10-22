package com.priceradar;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Test-only listener that lets integration tests prove the event publication
 * registry works end to end. Imported by {@link AbstractIntegrationTest} rather
 * than by a single test class, so that every integration test keeps sharing
 * one Spring context and one set of containers.
 */
@TestComponent
class SmokeTestEventListener {

	@ApplicationModuleListener
	public void on(SmokeTestEvent event) {
		// Nothing to do: returning normally is what completes the publication.
	}

	record SmokeTestEvent(String id) {
	}

}
