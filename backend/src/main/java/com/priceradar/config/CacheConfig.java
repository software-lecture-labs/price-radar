package com.priceradar.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis-backed caching: JSON values, string keys, a shared key prefix and a
 * default TTL. Per-cache TTLs can be added later with a
 * {@code RedisCacheManagerBuilderCustomizer}.
 *
 * <p>{@code @EnableCaching} lives here rather than on the application class so
 * that web-slice tests, which do not auto-configure a {@code CacheManager}, are
 * not forced to provide one.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	@Bean
	RedisCacheConfiguration defaultCacheConfiguration() {
		// Default typing is required so cached values deserialize back into their
		// concrete type; the validator keeps that restricted to our own classes.
		var typeValidator = BasicPolymorphicTypeValidator.builder()
				.allowIfSubType("com.priceradar.")
				.allowIfSubType("java.util.")
				.allowIfSubType("java.time.")
				.allowIfSubType("java.math.")
				.build();

		var valueSerializer = GenericJacksonJsonRedisSerializer.builder()
				.enableDefaultTyping(typeValidator)
				.build();

		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(DEFAULT_TTL)
				.prefixCacheNameWith("price-radar:")
				.disableCachingNullValues()
				.serializeKeysWith(SerializationPair.fromSerializer(StringRedisSerializer.UTF_8))
				.serializeValuesWith(SerializationPair.fromSerializer(valueSerializer));
	}

}
