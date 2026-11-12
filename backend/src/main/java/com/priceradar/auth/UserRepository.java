package com.priceradar.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class UserRepository {

	private static final String COLUMNS = "id, email, full_name, phone, password_hash, role, created_at";

	private final JdbcClient jdbc;

	UserRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** @throws org.springframework.dao.DuplicateKeyException if the email is taken */
	UserAccount create(String email, String fullName, String phone, String passwordHash) {
		return jdbc.sql("""
				INSERT INTO users (email, full_name, phone, password_hash)
				VALUES (:email, :fullName, :phone, :passwordHash)
				RETURNING %s""".formatted(COLUMNS))
				.param("email", email)
				.param("fullName", fullName)
				.param("phone", phone)
				.param("passwordHash", passwordHash)
				.query(UserRepository::account)
				.single();
	}

	Optional<UserAccount> findByEmail(String email) {
		return jdbc.sql("SELECT %s FROM users WHERE email = :email".formatted(COLUMNS))
				.param("email", email)
				.query(UserRepository::account)
				.optional();
	}

	Optional<UserAccount> findById(long id) {
		return jdbc.sql("SELECT %s FROM users WHERE id = :id".formatted(COLUMNS))
				.param("id", id)
				.query(UserRepository::account)
				.optional();
	}

	private static UserAccount account(ResultSet rs, int row) throws SQLException {
		return new UserAccount(
				rs.getLong("id"),
				rs.getString("email"),
				rs.getString("full_name"),
				rs.getString("phone"),
				rs.getString("password_hash"),
				Role.valueOf(rs.getString("role")),
				rs.getObject("created_at", OffsetDateTime.class).toInstant());
	}

}
