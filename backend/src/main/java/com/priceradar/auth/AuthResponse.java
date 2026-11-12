package com.priceradar.auth;

import java.time.Instant;

public record AuthResponse(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
		Instant refreshTokenExpiresAt, UserProfile user) {

	@Override
	public String toString() {
		return "AuthResponse[user=%s]".formatted(user);
	}

}
