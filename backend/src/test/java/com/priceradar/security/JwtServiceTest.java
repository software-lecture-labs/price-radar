package com.priceradar.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.priceradar.config.SecurityProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

	private static final String SECRET = "dGVzdC1vbmx5LXNpZ25pbmcta2V5LXdpdGgtZW5vdWdoLWVudHJvcHktMzJiCg==";

	private final JwtService jwtService = new JwtService(properties("price-radar-test", Duration.ofMinutes(5)));

	@Test
	void issuedTokenRoundTripsSubjectAndRoles() {
		var token = jwtService.issueAccessToken("42", List.of("ROLE_USER", "ROLE_ADMIN"));

		var claims = jwtService.parse(token);

		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(claims.getIssuer()).isEqualTo("price-radar-test");
		assertThat(jwtService.rolesOf(claims)).containsExactly("ROLE_USER", "ROLE_ADMIN");
	}

	@Test
	void rejectsTokenSignedWithAnotherKey() {
		var foreignSecret = "YW5vdGhlci1rZXktdGhhdC1pcy1hbHNvLWxvbmctZW5vdWdoLTMyYnl0ZXMK";
		var foreignService = new JwtService(new SecurityProperties(
				new SecurityProperties.Jwt(foreignSecret, "price-radar-test", Duration.ofMinutes(5),
						Duration.ofDays(1)),
				cors()));

		var token = foreignService.issueAccessToken("42", List.of("ROLE_USER"));

		assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsTokenFromAnotherIssuer() {
		var otherIssuer = new JwtService(properties("somebody-else", Duration.ofMinutes(5)));

		var token = otherIssuer.issueAccessToken("42", List.of("ROLE_USER"));

		assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsExpiredToken() {
		var expiring = new JwtService(properties("price-radar-test", Duration.ofSeconds(-60)));

		var token = expiring.issueAccessToken("42", List.of("ROLE_USER"));

		assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rolesClaimIsEmptyWhenEmpty() {
		var token = jwtService.issueAccessToken("42", List.of());

		assertThat(jwtService.rolesOf(jwtService.parse(token))).isEmpty();
	}

	@Test
	void rolesClaimIsEmptyWhenAbsentOrNotACollection() {
		var claims = mock(Claims.class);
		assertThat(jwtService.rolesOf(claims)).isEmpty();

		for (var roles : List.of("ROLE_USER", 1, Map.of("role", "ROLE_USER"))) {
			given(claims.get("roles")).willReturn(roles);
			assertThat(jwtService.rolesOf(claims)).isEmpty();
		}
	}

	@Test
	void rejectsNonStringNullAndBlankRoles() {
		var claims = mock(Claims.class);
		for (var role : Arrays.asList(1, true, Map.of("role", "ROLE_ADMIN"), null, "", " \t")) {
			given(claims.get("roles")).willReturn(Arrays.asList("ROLE_USER", role));

			assertThatThrownBy(() -> jwtService.rolesOf(claims))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("JWT roles must contain only non-blank strings");
		}
	}

	private static SecurityProperties properties(String issuer, Duration accessTokenTtl) {
		return new SecurityProperties(
				new SecurityProperties.Jwt(SECRET, issuer, accessTokenTtl, Duration.ofDays(30)),
				cors());
	}

	private static SecurityProperties.Cors cors() {
		return new SecurityProperties.Cors(List.of("http://localhost:3000"), List.of("GET"), List.of("*"), true, 3600);
	}

}
