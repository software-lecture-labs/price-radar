package com.priceradar.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

	private final RateLimiter limiter = new RateLimiter();

	@Test
	void allowsUpToMaxAttempts() {
		for (int i = 0; i < RateLimiter.MAX_ATTEMPTS; i++) {
			assertThat(limiter.tryAcquire("192.168.1.1")).isTrue();
		}
		assertThat(limiter.tryAcquire("192.168.1.1")).isFalse();
	}

	@Test
	void differentKeysAreIndependent() {
		for (int i = 0; i < RateLimiter.MAX_ATTEMPTS; i++) {
			limiter.tryAcquire("192.168.1.1");
		}
		assertThat(limiter.tryAcquire("192.168.1.1")).isFalse();
		assertThat(limiter.tryAcquire("10.0.0.1")).isTrue();
	}

}
