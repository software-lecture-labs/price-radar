package com.priceradar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.transaction.support.TransactionTemplate;

import com.priceradar.SmokeTestEventListener.SmokeTestEvent;
import com.priceradar.platform.MessagingProperties;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

/**
 * Smoke test for the installed stack: the context starts and every backing
 * service — PostgreSQL, Redis, Elasticsearch, Kafka — actually answers, and the
 * Spring Modulith event publication registry round-trips through PostgreSQL.
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

	@Autowired
	private ApplicationEventPublisher events;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private CompletedEventPublications completedEventPublications;

	@Test
	void postgresIsReachableAndFlywayHasRun() {
		var jdbc = JdbcClient.create(dataSource);

		var version = jdbc.sql("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")
				.query(String.class)
				.list();
		assertThat(version).contains("1", "2");

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

	@Test
	void eventPublicationRegistryRecordsAndCompletesPublications() {
		var event = new SmokeTestEvent(UUID.randomUUID().toString());

		transactionTemplate.executeWithoutResult(status -> events.publishEvent(event));

		// The listener runs asynchronously after commit; the registry marks the
		// publication complete once it returns.
		await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(completedEventPublications.findAll())
				.extracting(EventPublication::getEvent)
				.contains(event));

		completedEventPublications.deletePublications(publication -> event.equals(publication.getEvent()));
	}

}
