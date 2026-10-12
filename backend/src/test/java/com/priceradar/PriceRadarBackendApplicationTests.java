package com.priceradar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import com.priceradar.config.MessagingProperties;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Smoke test for the installed stack: the context starts and every backing
 * service — PostgreSQL, Redis, Elasticsearch, Kafka — actually answers.
 */
class PriceRadarBackendApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	@Autowired
	private KafkaAdmin kafkaAdmin;

	@Autowired
	private KafkaTemplate<Object, Object> kafkaTemplate;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private MessagingProperties messagingProperties;

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

	@Test
	void redisIsReachable() throws Exception {
		try (var connection = redisConnectionFactory.getConnection()) {
			assertThat(connection.ping()).isEqualTo("PONG");
		}
	}

	@Test
	void elasticsearchIsReachable() throws Exception {
		assertThat(elasticsearchClient.ping().value()).isTrue();
	}

	@Test
	void kafkaTopicsAreCreated() {
		var topics = kafkaAdmin.describeTopics(messagingProperties.eventsTopic(),
				messagingProperties.deadLetterTopic());

		assertThat(topics).containsOnlyKeys(messagingProperties.eventsTopic(), messagingProperties.deadLetterTopic());
		assertThat(topics.get(messagingProperties.eventsTopic()).partitions())
				.hasSize(messagingProperties.partitions());
	}

	@Test
	void kafkaAcceptsJsonEvents() throws Exception {
		var result = kafkaTemplate.send(messagingProperties.eventsTopic(), "smoke-test", Map.of("ok", true))
				.get(30, TimeUnit.SECONDS);

		assertThat(result.getRecordMetadata().offset()).isNotNegative();
	}

	@Test
	void redisCacheManagerIsConfigured() {
		assertThat(cacheManager).isNotNull();
		assertThat(cacheManager.getCache("smoke-test")).isNotNull();
	}

}
