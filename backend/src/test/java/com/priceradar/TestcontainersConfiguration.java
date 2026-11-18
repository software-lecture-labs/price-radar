package com.priceradar;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real backing services for integration tests.
 *
 * <p>Image tags are pinned on purpose: {@code latest} makes a green build
 * unreproducible. The Elasticsearch tag tracks the {@code elasticsearch-java}
 * client version managed by Spring Boot.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18.6-alpine");
	static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8.10-alpine");
	static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:4.3.1");
	static final DockerImageName ELASTICSEARCH_IMAGE =
			DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.4.5");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGRES_IMAGE)
				.withDatabaseName("price_radar")
				.withUsername("price_radar")
				.withPassword("price_radar");
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);
	}

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(KAFKA_IMAGE);
	}

	@Bean
	@ServiceConnection
	@Ssl
	ElasticsearchContainer elasticsearchContainer() {
		return new ElasticsearchContainer(ELASTICSEARCH_IMAGE);
	}

}
