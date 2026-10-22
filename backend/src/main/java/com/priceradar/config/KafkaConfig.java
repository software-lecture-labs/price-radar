package com.priceradar.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Base Kafka topology: one topic for application events, plus a dead-letter
 * topic so listeners added later have somewhere to park a poison record
 * instead of blocking their partition forever.
 *
 * <p>Spring Boot wires the {@link CommonErrorHandler} below into the
 * auto-configured listener container factory, so every {@code @KafkaListener}
 * gets retry with back-off and dead-lettering without extra setup.
 */
@Configuration
public class KafkaConfig {

	@Bean
	NewTopic eventsTopic(MessagingProperties properties) {
		return TopicBuilder.name(properties.eventsTopic())
				.partitions(properties.partitions())
				.replicas(properties.replicationFactor())
				.build();
	}

	@Bean
	NewTopic deadLetterTopic(MessagingProperties properties) {
		return TopicBuilder.name(properties.deadLetterTopic())
				.partitions(properties.partitions())
				.replicas(properties.replicationFactor())
				.build();
	}

	@Bean
	CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
			ProducerFactory<Object, Object> producerFactory, MessagingProperties properties) {
		// A record that failed deserialization reaches the recoverer as raw bytes;
		// re-serializing those as JSON would corrupt them, so they get their own template.
		var templates = new LinkedHashMap<Class<?>, KafkaOperations<?, ?>>();
		templates.put(byte[].class, new KafkaTemplate<>(producerFactory,
				Map.of(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class)));
		templates.put(Object.class, kafkaTemplate);

		// A negative partition lets Kafka pick one; the source topic travels in the
		// kafka_dlt-original-topic header.
		var recoverer = new DeadLetterPublishingRecoverer(templates,
				(record, ex) -> new TopicPartition(properties.deadLetterTopic(), -1));

		var backOff = new ExponentialBackOffWithMaxRetries(3);
		backOff.setInitialInterval(2_000L);
		backOff.setMultiplier(2.0);
		return new DefaultErrorHandler(recoverer, backOff);
	}

}
