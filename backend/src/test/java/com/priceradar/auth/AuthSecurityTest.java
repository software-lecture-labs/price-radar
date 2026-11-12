package com.priceradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.priceradar.config.SecurityProperties;
import com.priceradar.security.JwtService;
import com.priceradar.security.SecurityConfig;

/** The real filter chain: which auth endpoints are public and what an anonymous caller gets. */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtService.class, AuthSecurityTest.Properties.class })
class AuthSecurityTest {

	@TestConfiguration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SecurityProperties.class)
	static class Properties {
	}

	@Autowired
	private MockMvcTester mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private AuthService auth;

	@Test
	void signInEndpointsArePublic() {
		given(auth.login(any())).willReturn(new AuthResponse("a", Instant.EPOCH, "r", Instant.EPOCH,
				new UserProfile(1, "a@example.com", "A", "+905321234567", Role.USER, Instant.EPOCH)));

		assertThat(mockMvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "a@example.com", "password": "correct horse"}"""))
				.hasStatusOk();
		assertThat(mockMvc.post().uri("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken": "r"}"""))
				.hasStatus(204);
	}

	@Test
	void meWithoutATokenIs401Not403() {
		assertThat(mockMvc.get().uri("/api/v1/auth/me")).hasStatus(401);
	}

	@Test
	void meWithAnInvalidTokenIs401() {
		assertThat(mockMvc.get().uri("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.hasStatus(401);
	}

	@Test
	void meWithAValidTokenReachesTheController() {
		given(auth.currentUser("7")).willReturn(
				new UserProfile(7, "a@example.com", "A", "+905321234567", Role.USER, Instant.EPOCH));
		var token = jwtService.issueAccessToken("7", List.of(Role.USER.authority()));

		assertThat(mockMvc.get().uri("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.id")
				.asNumber()
				.isEqualTo(7);
	}

}
