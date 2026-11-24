package com.priceradar.security;

/**
 * Thrown when a client exceeds the allowed number of authentication attempts within the rate-limit window.
 */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException(String message) {
		super(message);
	}

}
