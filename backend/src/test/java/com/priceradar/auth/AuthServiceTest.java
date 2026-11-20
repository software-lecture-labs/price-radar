package com.priceradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.priceradar.common.exception.BusinessRuleException;
import com.priceradar.config.SecurityProperties;
import com.priceradar.security.JwtService;

class AuthServiceTest {

	private static final String SECRET = "dGVzdC1vbmx5LXNpZ25pbmcta2V5LXdpdGgtZW5vdWdoLWVudHJvcHktMzJiCg==";
	private static final SecurityProperties PROPERTIES = new SecurityProperties(
			new SecurityProperties.Jwt(SECRET, "price-radar-test", Duration.ofMinutes(15), Duration.ofDays(30)),
			new SecurityProperties.Cors(List.of("http://localhost:3000"), List.of("GET"), List.of("*"), true, 3600));

	// The lowest cost keeps the tests fast; production uses 12.
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final JwtService jwtService = new JwtService(PROPERTIES);
	private final UserRepository users = mock(UserRepository.class);
	private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
	private final AuthService auth = new AuthService(users, refreshTokens, passwordEncoder, jwtService, PROPERTIES);

	@Test
	void registerNormalizesTheInputAndStoresOnlyAHash() {
		given(users.create(anyString(), anyString(), anyString(), anyString()))
				.willAnswer(call -> account(42, call.getArgument(0), call.getArgument(3), Role.USER));

		var response = auth.register(
				new RegisterRequest("  Ayşe Yılmaz ", " Ayse@Example.COM ", "0532 123 45 67", "correct horse"));

		var passwordHash = ArgumentCaptor.forClass(String.class);
		then(users).should().create(eq("ayse@example.com"), eq("Ayşe Yılmaz"), eq("+905321234567"),
				passwordHash.capture());
		assertThat(passwordHash.getValue()).isNotEqualTo("correct horse");
		assertThat(passwordEncoder.matches("correct horse", passwordHash.getValue())).isTrue();
		assertThat(response.user().email()).isEqualTo("ayse@example.com");
	}

	@Test
	void signInIssuesAnAccessTokenForTheUserAndStoresTheRefreshTokenHash() {
		given(users.create(anyString(), anyString(), anyString(), anyString()))
				.willAnswer(call -> account(42, call.getArgument(0), call.getArgument(3), Role.USER));

		var response = auth.register(
				new RegisterRequest("Ayşe Yılmaz", "ayse@example.com", "05321234567", "correct horse"));

		var claims = jwtService.parse(response.accessToken());
		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(jwtService.rolesOf(claims)).containsExactly("ROLE_USER");
		assertThat(claims.getExpiration().toInstant()).isEqualTo(response.accessTokenExpiresAt());
		assertThat(response.refreshTokenExpiresAt())
				.isBetween(Instant.now().plus(Duration.ofDays(30)).minusSeconds(5), Instant.now().plus(Duration.ofDays(30)));

		assertThat(response.refreshToken()).hasSize(43);
		then(refreshTokens).should().deleteInactive(42);
		then(refreshTokens).should().save(42, AuthService.hash(response.refreshToken()),
				response.refreshTokenExpiresAt());
	}

	@Test
	void aTakenEmailIsAConflict() {
		given(users.create(anyString(), anyString(), anyString(), anyString()))
				.willThrow(new DuplicateKeyException("users_email_key"));

		assertThatThrownBy(() -> auth.register(
				new RegisterRequest("Ayşe Yılmaz", "ayse@example.com", "05321234567", "correct horse")))
				.isInstanceOf(BusinessRuleException.class);
		then(refreshTokens).shouldHaveNoInteractions();
	}

	@Test
	void loginLooksUpTheNormalizedEmailAndChecksThePassword() {
		given(users.findByEmail("ayse@example.com"))
				.willReturn(Optional.of(account(42, "ayse@example.com", passwordEncoder.encode("correct horse"),
						Role.ADMIN)));

		var response = auth.login(new LoginRequest(" AYSE@example.com", "correct horse"));

		assertThat(jwtService.rolesOf(jwtService.parse(response.accessToken()))).containsExactly("ROLE_ADMIN");
	}

	@Test
	void wrongPasswordAndUnknownEmailFailTheSameWay() {
		given(users.findByEmail("ayse@example.com"))
				.willReturn(Optional.of(account(42, "ayse@example.com", passwordEncoder.encode("correct horse"),
						Role.USER)));
		given(users.findByEmail("nobody@example.com")).willReturn(Optional.empty());

		assertThatThrownBy(() -> auth.login(new LoginRequest("ayse@example.com", "wrong horse")))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Invalid email or password");
		assertThatThrownBy(() -> auth.login(new LoginRequest("nobody@example.com", "correct horse")))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("Invalid email or password");
		then(refreshTokens).shouldHaveNoInteractions();
	}

	@Test
	void refreshIssuesAnAccessTokenWithTheCurrentRole() {
		given(refreshTokens.findActiveUserId(AuthService.hash("refresh-token"))).willReturn(Optional.of(42L));
		given(users.findById(42)).willReturn(Optional.of(account(42, "ayse@example.com", "hash", Role.ADMIN)));

		var response = auth.refresh("refresh-token");

		var claims = jwtService.parse(response.accessToken());
		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(jwtService.rolesOf(claims)).containsExactly("ROLE_ADMIN");
	}

	@Test
	void refreshWithAnUnknownRevokedOrExpiredTokenFails() {
		given(refreshTokens.findActiveUserId(any())).willReturn(Optional.empty());

		assertThatThrownBy(() -> auth.refresh("refresh-token")).isInstanceOf(BadCredentialsException.class);
		then(users).should(never()).findById(42);
	}

	@Test
	void logoutRevokesByHash() {
		auth.logout("refresh-token");

		then(refreshTokens).should().revoke(AuthService.hash("refresh-token"));
	}

	@Test
	void currentUserRejectsSubjectsThatAreNotAnExistingUser() {
		given(users.findById(42)).willReturn(Optional.empty());

		assertThatThrownBy(() -> auth.currentUser("42")).isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> auth.currentUser("not-a-number")).isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void hashIsLowercaseHexSha256() {
		assertThat(AuthService.hash("abc"))
				.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	private static UserAccount account(long id, String email, String passwordHash, Role role) {
		return new UserAccount(id, email, "Ayşe Yılmaz", "+905321234567", passwordHash, role,
				Instant.parse("2026-11-12T10:00:00Z"));
	}

}
