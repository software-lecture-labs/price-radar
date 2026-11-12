package com.priceradar.auth;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Stores SHA-256 hashes of refresh tokens, never the tokens themselves. */
@Repository
class RefreshTokenRepository {

	private final JdbcClient jdbc;

	RefreshTokenRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	void save(long userId, String tokenHash, Instant expiresAt) {
		jdbc.sql("""
				INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
				VALUES (:userId, :tokenHash, :expiresAt)""")
				.param("userId", userId)
				.param("tokenHash", tokenHash)
				.param("expiresAt", expiresAt.atOffset(ZoneOffset.UTC))
				.update();
	}

	/** The owner of a token that is neither revoked nor expired. */
	Optional<Long> findActiveUserId(String tokenHash) {
		return jdbc.sql("""
				SELECT user_id FROM refresh_tokens
				WHERE token_hash = :tokenHash AND revoked_at IS NULL AND expires_at > now()""")
				.param("tokenHash", tokenHash)
				.query(Long.class)
				.optional();
	}

	void revoke(String tokenHash) {
		jdbc.sql("UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = :tokenHash AND revoked_at IS NULL")
				.param("tokenHash", tokenHash)
				.update();
	}

	/** Keeps the table from growing with every sign-in. */
	void deleteInactive(long userId) {
		jdbc.sql("""
				DELETE FROM refresh_tokens
				WHERE user_id = :userId AND (revoked_at IS NOT NULL OR expires_at <= now())""")
				.param("userId", userId)
				.update();
	}

}
