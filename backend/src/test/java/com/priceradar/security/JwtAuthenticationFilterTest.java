package com.priceradar.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.priceradar.config.SecurityProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

class JwtAuthenticationFilterTest {

	private static final String SECRET = "dGVzdC1vbmx5LXNpZ25pbmcta2V5LXdpdGgtZW5vdWdoLWVudHJvcHktMzJiCg==";
	private static final String ISSUER = "price-radar-filter-test";

	private final JwtService jwtService = new JwtService(new SecurityProperties(
			new SecurityProperties.Jwt(SECRET, ISSUER, Duration.ofMinutes(5), Duration.ofDays(1)),
			new SecurityProperties.Cors(List.of("http://localhost:3000"), List.of("GET"), List.of("*"), true, 3600)));
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validTokenAuthenticatesTheRequestAndContinuesTheChain() throws Exception {
		var request = request(jwtService.issueAccessToken("42", List.of("ROLE_USER", "ROLE_ADMIN")));
		var response = new MockHttpServletResponse();
		var chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		var authentication = SecurityContextHolder.getContext().getAuthentication();
		assertThat(authentication.getName()).isEqualTo("42");
		assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_USER", "ROLE_ADMIN");
		assertThat(chain.getRequest()).isSameAs(request);
	}

	@Test
	void invalidRolesLeaveTheRequestUnauthenticatedAndContinueTheChain() throws Exception {
		var invalidRoles = List.of(
				List.of("ROLE_USER", 1),
				Arrays.asList("ROLE_USER", null),
				List.of("ROLE_USER", ""),
				List.of("ROLE_USER", " \t"));
		for (var roles : invalidRoles) {
			var token = Jwts.builder()
					.issuer(ISSUER)
					.subject("42")
					.claim("roles", roles)
					.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
					.compact();
			var request = request(token);
			var response = new MockHttpServletResponse();
			var chain = new MockFilterChain();

			filter.doFilter(request, response, chain);

			assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
			assertThat(chain.getRequest()).isSameAs(request);
			assertThat(response.getStatus()).isEqualTo(200);
		}
	}

	private static MockHttpServletRequest request(String token) {
		var request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		return request;
	}

}
