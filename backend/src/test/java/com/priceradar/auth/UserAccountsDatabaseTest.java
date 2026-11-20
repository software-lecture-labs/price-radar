package com.priceradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.priceradar.TestDatabase;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserAccountsDatabaseTest {

	// The repository only ever sees SHA-256 hashes, so any 64 lowercase hex characters will do.
	private static final String ACTIVE = "a".repeat(64);
	private static final String REVOKED = "b".repeat(64);
	private static final String EXPIRED = "c".repeat(64);
	private static final String UNKNOWN = "d".repeat(64);

	private TestDatabase database;
	private JdbcClient jdbc;
	private UserRepository users;
	private RefreshTokenRepository refreshTokens;

	@BeforeAll
	void migrate() {
		database = TestDatabase.open();
		database.flyway().migrate();
		jdbc = JdbcClient.create(database.dataSource());
		users = new UserRepository(jdbc);
		refreshTokens = new RefreshTokenRepository(jdbc);
	}

	@AfterAll
	void dropSchema() {
		if (database != null) {
			database.close();
		}
	}

	@BeforeEach
	void emptyTables() {
		jdbc.sql("TRUNCATE refresh_tokens, users RESTART IDENTITY").update();
	}

	@Test
	void createdAccountsAreUsersAndCanBeFoundByEmailOrId() {
		var created = users.create("ayse@example.com", "Ayşe Yılmaz", "+905321234567", "$2a$12$hash");

		assertThat(created.role()).isEqualTo(Role.USER);
		assertThat(created.createdAt()).isBefore(Instant.now().plusSeconds(1));
		assertThat(users.findByEmail("ayse@example.com")).contains(created);
		assertThat(users.findById(created.id())).contains(created);
		assertThat(users.findByEmail("nobody@example.com")).isEmpty();
	}

	@Test
	void anEmailCanOnlyBeRegisteredOnce() {
		users.create("ayse@example.com", "Ayşe Yılmaz", "+905321234567", "$2a$12$hash");

		assertThatThrownBy(() -> users.create("ayse@example.com", "Başka Biri", "+905339876543", "$2a$12$other"))
				.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void theSchemaRejectsUnnormalizedEmailsAndPhones() {
		assertThatThrownBy(() -> users.create("Ayse@Example.com", "Ayşe Yılmaz", "+905321234567", "$2a$12$hash"))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> users.create("ayse@example.com", "Ayşe Yılmaz", "0532 123 45 67", "$2a$12$hash"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void refreshTokensAreActiveUntilRevokedOrExpired() {
		var user = users.create("ayse@example.com", "Ayşe Yılmaz", "+905321234567", "$2a$12$hash").id();
		var future = Instant.now().plus(Duration.ofDays(30));

		refreshTokens.save(user, ACTIVE, future);
		refreshTokens.save(user, REVOKED, future);
		refreshTokens.revoke(REVOKED);
		refreshTokens.save(user, EXPIRED, future);
		jdbc.sql("UPDATE refresh_tokens SET created_at = now() - interval '2 days', expires_at = now() - interval '1 day'"
				+ " WHERE token_hash = :hash")
				.param("hash", EXPIRED)
				.update();

		assertThat(refreshTokens.findActiveUserId(ACTIVE)).contains(user);
		assertThat(refreshTokens.findActiveUserId(REVOKED)).isEmpty();
		assertThat(refreshTokens.findActiveUserId(EXPIRED)).isEmpty();
		assertThat(refreshTokens.findActiveUserId(UNKNOWN)).isEmpty();

		refreshTokens.deleteInactive(user);

		assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens").query(Long.class).single()).isOne();
		assertThat(refreshTokens.findActiveUserId(ACTIVE)).contains(user);
	}

	@Test
	void deletingAUserDeletesItsRefreshTokens() {
		var user = users.create("ayse@example.com", "Ayşe Yılmaz", "+905321234567", "$2a$12$hash").id();
		refreshTokens.save(user, ACTIVE, Instant.now().plus(Duration.ofDays(30)));

		jdbc.sql("DELETE FROM users WHERE id = :id").param("id", user).update();

		assertThat(refreshTokens.findActiveUserId(ACTIVE)).isEmpty();
	}

}
