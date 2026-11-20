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

// Keep caching off the application class so MVC test slices do not need a CacheManager.
@Configuration
@EnableCaching
public class CacheConfig {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

	@Bean
	RedisCacheConfiguration defaultCacheConfiguration() {
		// Cached values need type metadata to deserialize into their concrete classes.
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
