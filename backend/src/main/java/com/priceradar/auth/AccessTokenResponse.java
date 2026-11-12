package com.priceradar.auth;

import java.time.Instant;

public record AccessTokenResponse(String accessToken, Instant accessTokenExpiresAt) {

	@Override
	public String toString() {
		return "AccessTokenResponse[accessTokenExpiresAt=%s]".formatted(accessTokenExpiresAt);
	}

}
