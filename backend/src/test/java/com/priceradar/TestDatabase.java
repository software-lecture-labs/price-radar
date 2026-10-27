package com.priceradar;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.zaxxer.hikari.HikariDataSource;

public final class TestDatabase implements AutoCloseable {

	private final JdbcClient administration;
	private final HikariDataSource dataSource;
	private final String schema;

	private TestDatabase(JdbcClient administration, HikariDataSource dataSource, String schema) {
		this.administration = administration;
		this.dataSource = dataSource;
		this.schema = schema;
	}

	public static TestDatabase open() {
		var url = requiredEnvironment("TEST_POSTGRES_URL");
		var user = requiredEnvironment("TEST_POSTGRES_USER");
		var password = requiredEnvironment("TEST_POSTGRES_PASSWORD");
		if (!url.startsWith("jdbc:postgresql:")) {
			throw new IllegalStateException("TEST_POSTGRES_URL must be a PostgreSQL JDBC URL");
		}
		if (url.toLowerCase(Locale.ROOT).contains("-pooler.")) {
			throw new IllegalStateException("TEST_POSTGRES_URL must use a direct endpoint without connection pooling");
		}

		var administration = JdbcClient.create(new DriverManagerDataSource(url, user, password));
		var extensions = administration.sql("""
				SELECT extname FROM pg_catalog.pg_extension
				WHERE extname IN ('pg_trgm', 'unaccent')
				""")
				.query(String.class)
				.list();
		if (!extensions.containsAll(List.of("pg_trgm", "unaccent"))) {
			throw new IllegalStateException(
					"Install pg_trgm and unaccent in the separate test database before running integration tests");
		}

		var schema = "price_radar_test_" + UUID.randomUUID().toString().replace("-", "");
		administration.sql("CREATE SCHEMA " + schema).update();
		try {
			var dataSource = new HikariDataSource();
			dataSource.setJdbcUrl(url);
			dataSource.setUsername(user);
			dataSource.setPassword(password);
			dataSource.setSchema(schema);
			dataSource.setMaximumPoolSize(4);
			dataSource.setMinimumIdle(0);
			dataSource.setPoolName(schema);
			return new TestDatabase(administration, dataSource, schema);
		}
		catch (RuntimeException ex) {
			try {
				administration.sql("DROP SCHEMA " + schema + " CASCADE").update();
			}
			catch (RuntimeException cleanup) {
				ex.addSuppressed(cleanup);
			}
			throw ex;
		}
	}

	public DataSource dataSource() {
		return dataSource;
	}

	public String schema() {
		return schema;
	}

	public Flyway flyway() {
		return Flyway.configure()
				.dataSource(dataSource)
				.defaultSchema(schema)
				.schemas(schema)
				.createSchemas(false)
				.cleanDisabled(true)
				.locations("classpath:db/migration")
				.load();
	}

	@Override
	public void close() {
		dataSource.close();
		administration.sql("DROP SCHEMA IF EXISTS " + schema + " CASCADE").update();
	}

	private static String requiredEnvironment(String name) {
		var value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(name + " is required for the separate test database");
		}
		return value;
	}

}
