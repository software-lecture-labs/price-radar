package com.priceradar;

import org.springframework.boot.SpringApplication;

public class TestPriceRadarBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(PriceRadarBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
