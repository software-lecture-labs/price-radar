package com.priceradar;

import javax.sql.DataSource;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfiguration {

	@Bean(destroyMethod = "close")
	TestDatabase testDatabase() {
		return TestDatabase.open();
	}

	@Bean(destroyMethod = "")
	DataSource dataSource(TestDatabase database) {
		return database.dataSource();
	}

	@Bean
	FlywayConfigurationCustomizer testSchema(TestDatabase database) {
		return configuration -> configuration
				.defaultSchema(database.schema())
				.schemas(database.schema())
				.createSchemas(false)
				.cleanDisabled(true);
	}

}
