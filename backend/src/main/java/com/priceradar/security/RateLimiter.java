package com.priceradar.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * A simple in-memory, fixed-window rate limiter keyed by IP address. Limits the number of authentication
 * attempts (login, register, refresh) to prevent brute-force and credential-stuffing attacks.
 *
 * <p>The window resets every {@value #WINDOW_MINUTES} minutes and allows up to {@value #MAX_ATTEMPTS} attempts
 * per IP per window. This is intentionally simple: a single-node solution that is easy to reason about.
 * For a multi-instance deployment, replace with a Redis-backed counter or a token-bucket algorithm.</p>
 */
@Component
public class RateLimiter {

	static final int MAX_ATTEMPTS = 20;
	static final int WINDOW_MINUTES = 15;
	private static final long WINDOW_MILLIS = Duration.ofMinutes(WINDOW_MINUTES).toMillis();

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	/**
	 * Records an attempt for the given key (typically the client IP). Returns {@code true} if the attempt is
	 * allowed, {@code false} if the rate limit has been exceeded.
	 */
	public boolean tryAcquire(String key) {
		var now = System.currentTimeMillis();
		var window = windows.compute(key, (k, existing) -> {
			if (existing == null || now - existing.startedAt > WINDOW_MILLIS) {
				return new Window(now, new AtomicInteger(1));
			}
			existing.count.incrementAndGet();
			return existing;
		});
		return window.count.get() <= MAX_ATTEMPTS;
	}

	private record Window(long startedAt, AtomicInteger count) {
	}

}
