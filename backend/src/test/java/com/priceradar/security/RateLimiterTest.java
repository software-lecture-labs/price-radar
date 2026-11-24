package com.priceradar.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.priceradar.config.RateLimitProperties;

class RateLimiterTest {

	private static final Duration WINDOW = Duration.ofMinutes(15);

	private Instant now = Instant.parse("2026-11-24T10:00:00Z");
	private final RateLimiter limiter = new RateLimiter(new RateLimitProperties(3, WINDOW), () -> now);

	@Test
	void allowsTheMaximumThenRefuses() {
		useUpAttempts("login:203.0.113.7");

		assertThatThrownBy(() -> limiter.acquire("login:203.0.113.7"))
				.isInstanceOf(RateLimitExceededException.class);
	}

	@Test
	void aRefusalSaysWhenTheWindowEnds() {
		useUpAttempts("login:203.0.113.7");
		now = now.plus(Duration.ofMinutes(5));

		assertThatThrownBy(() -> limiter.acquire("login:203.0.113.7"))
				.isInstanceOfSatisfying(RateLimitExceededException.class,
						ex -> assertThat(ex.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(10).toSeconds()));
	}

	@Test
	void retryAfterIsRoundedUpToWholeSeconds() {
		assertThat(new RateLimitExceededException(Duration.ofMillis(1500)).retryAfterSeconds()).isEqualTo(2);
		assertThat(new RateLimitExceededException(Duration.ZERO).retryAfterSeconds()).isEqualTo(1);
	}

	@Test
	void aNewWindowStartsOnceTheOldOneEnds() {
		useUpAttempts("login:203.0.113.7");
		now = now.plus(WINDOW);

		assertThatCode(() -> limiter.acquire("login:203.0.113.7")).doesNotThrowAnyException();
	}

	@Test
	void keysAreCountedSeparately() {
		useUpAttempts("login:203.0.113.7");

		assertThatCode(() -> limiter.acquire("login:198.51.100.1")).doesNotThrowAnyException();
		assertThatCode(() -> limiter.acquire("refresh:203.0.113.7")).doesNotThrowAnyException();
	}

	@Test
	void refusedAttemptsDoNotExtendTheWindow() {
		useUpAttempts("login:203.0.113.7");
		for (int i = 0; i < 100; i++) {
			assertThatThrownBy(() -> limiter.acquire("login:203.0.113.7"))
					.isInstanceOf(RateLimitExceededException.class);
		}
		now = now.plus(WINDOW);

		assertThatCode(() -> limiter.acquire("login:203.0.113.7")).doesNotThrowAnyException();
	}

	@Test
	void endedWindowsAreForgotten() {
		for (int i = 0; i < 50; i++) {
			limiter.acquire("login:10.0.0." + i);
		}
		assertThat(limiter.trackedKeys()).isEqualTo(50);

		now = now.plus(WINDOW);
		limiter.acquire("login:203.0.113.7");

		assertThat(limiter.trackedKeys()).isEqualTo(1);
	}

	private void useUpAttempts(String key) {
		for (int i = 0; i < 3; i++) {
			limiter.acquire(key);
		}
	}

}
