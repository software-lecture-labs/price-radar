package com.priceradar.security;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.priceradar.config.RateLimitProperties;

/**
 * Allows each key at most {@code maxAttempts} attempts in a fixed window that starts with the key's first attempt.
 *
 * <p>Counters are kept in memory: they reset when the application restarts, and with more than one backend
 * instance each instance would count on its own. That is enough for a single backend; running several would need a
 * shared store for the counters.</p>
 */
@Component
public class RateLimiter {

	private final int maxAttempts;
	private final Duration window;
	private final InstantSource clock;
	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
	private final AtomicReference<Instant> nextCleanup;

	@Autowired
	public RateLimiter(RateLimitProperties properties) {
		this(properties, InstantSource.system());
	}

	RateLimiter(RateLimitProperties properties, InstantSource clock) {
		this.maxAttempts = properties.maxAttempts();
		this.window = properties.window();
		this.clock = clock;
		this.nextCleanup = new AtomicReference<>(clock.instant().plus(window));
	}

	/**
	 * Counts an attempt for the key.
	 *
	 * @throws RateLimitExceededException if the key has used all its attempts in the current window
	 */
	public void acquire(String key) {
		var now = clock.instant();
		removeEndedWindows(now);
		var current = windows.compute(key, (k, existing) -> existing == null || existing.hasEnded(now)
				? new Window(now.plus(window), 1)
				: existing.withAnotherAttempt(maxAttempts));
		if (current.attempts() > maxAttempts) {
			throw new RateLimitExceededException(Duration.between(now, current.endsAt()));
		}
	}

	int trackedKeys() {
		return windows.size();
	}

	/** Runs at most once per window, so memory holds only recent keys and no request pays for a full scan. */
	private void removeEndedWindows(Instant now) {
		var due = nextCleanup.get();
		if (!now.isBefore(due) && nextCleanup.compareAndSet(due, now.plus(window))) {
			windows.values().removeIf(counted -> counted.hasEnded(now));
		}
	}

	private record Window(Instant endsAt, int attempts) {

		boolean hasEnded(Instant now) {
			return !now.isBefore(endsAt);
		}

		/** Stops one past the limit, so a long stream of refused attempts cannot overflow the counter. */
		Window withAnotherAttempt(int maxAttempts) {
			return new Window(endsAt, Math.min(attempts + 1, maxAttempts + 1));
		}

	}

}
