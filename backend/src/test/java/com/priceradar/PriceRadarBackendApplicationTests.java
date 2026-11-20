package com.priceradar;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class PriceRadarBackendApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void postgresIsReachableAndFlywayHasRun() {
		var jdbc = JdbcClient.create(dataSource);

		var version = jdbc.sql("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")
				.query(String.class)
				.list();
		assertThat(version).contains("1");

		var extensions = jdbc.sql("SELECT extname FROM pg_extension")
				.query(String.class)
				.list();
		assertThat(extensions).contains("pg_trgm", "unaccent");
	}

}
