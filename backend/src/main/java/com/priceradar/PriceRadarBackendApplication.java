package com.priceradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/**
 * Modular monolith: every direct sub-package of {@code com.priceradar} is an
 * application module, declared in its {@code package-info.java}. Modules talk
 * through their top-level package (the API) or through events; sub-packages are
 * internal. {@code ModularityTests} fails the build when a rule is broken.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(systemName = "Price Radar", sharedModules = "shared")
public class PriceRadarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceRadarBackendApplication.class, args);
	}

}
