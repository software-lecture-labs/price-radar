package com.priceradar.security;

import java.time.Duration;

/** A client used all its attempts for an endpoint in the current window; answered with 429 and Retry-After. */
public class RateLimitExceededException extends RuntimeException {

	private final long retryAfterSeconds;

	public RateLimitExceededException(Duration retryAfter) {
		super("Too many attempts. Please try again later.");
		// Rounded up, so a client that waits exactly this long is not refused again.
		this.retryAfterSeconds = Math.max(1, retryAfter.plusNanos(999_999_999).toSeconds());
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}

}
