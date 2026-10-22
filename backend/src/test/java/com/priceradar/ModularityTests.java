package com.priceradar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Enforces the module rules on every build: no cycles between modules, no
 * access to another module's internal packages, and only the dependencies each
 * {@code package-info.java} allows. Needs neither Spring context nor Docker.
 */
class ModularityTests {

	private static final ApplicationModules MODULES = ApplicationModules.of(PriceRadarBackendApplication.class);

	@Test
	void modulesRespectTheirBoundaries() {
		MODULES.verify();
	}

	@Test
	void allExpectedModulesAreDetected() {
		assertThat(MODULES.stream().map(ApplicationModule::getIdentifier).map(Object::toString))
				.containsExactlyInAnyOrder("alert", "catalog", "favorites", "identity", "ingestion", "matching",
						"merchant", "platform", "pricing", "search", "shared", "system");
	}

	/** PlantUML diagrams and module canvases, written to target/spring-modulith-docs. */
	@Test
	void writeDocumentation() {
		new Documenter(MODULES)
				.writeModulesAsPlantUml()
				.writeIndividualModulesAsPlantUml()
				.writeModuleCanvases();
	}

}
