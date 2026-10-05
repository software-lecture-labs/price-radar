package com.priceradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PriceRadarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceRadarBackendApplication.class, args);
	}

}
