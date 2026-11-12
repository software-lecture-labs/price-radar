package com.priceradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.priceradar.common.exception.BusinessRuleException;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	private static final UserProfile AYSE = new UserProfile(42, "ayse@example.com", "Ayşe Yılmaz", "+905321234567",
			Role.USER, Instant.parse("2026-11-12T10:00:00Z"));
	private static final AuthResponse SIGNED_IN = new AuthResponse("access.jwt", Instant.parse("2026-11-12T10:15:00Z"),
			"refresh-token", Instant.parse("2026-12-12T10:00:00Z"), AYSE);

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private AuthService auth;

	@Test
	void registerCreatesTheAccountAndSignsIn() {
		given(auth.register(any())).willReturn(SIGNED_IN);

		var body = assertThat(post("/api/v1/auth/register", """
				{"fullName": "Ayşe Yılmaz", "email": "Ayse@Example.com", "phone": "0532 123 45 67",
				 "password": "correct horse"}""")).hasStatus(201).bodyJson();

		body.extractingPath("$.accessToken").asString().isEqualTo("access.jwt");
		body.extractingPath("$.refreshTokenExpiresAt").asString().isEqualTo("2026-12-12T10:00:00Z");
		body.extractingPath("$.user.email").asString().isEqualTo("ayse@example.com");
		body.extractingPath("$.user.role").asString().isEqualTo("USER");
		then(auth).should().register(
				new RegisterRequest("Ayşe Yılmaz", "Ayse@Example.com", "0532 123 45 67", "correct horse"));
	}

	@Test
	void registerReportsEachInvalidField() {
		var body = assertThat(post("/api/v1/auth/register", """
				{"fullName": " ", "email": "not-an-email", "phone": "0212 123 45 67", "password": "short"}"""))
				.hasStatus(400)
				.bodyJson();

		body.extractingPath("$.title").asString().isEqualTo("Validation failed");
		body.extractingPath("$.errors.fullName").isNotNull();
		body.extractingPath("$.errors.email").isNotNull();
		body.extractingPath("$.errors.phone").asString().isEqualTo("must be a Turkish mobile number");
		body.extractingPath("$.errors.password").isNotNull();
		verifyNoInteractions(auth);
	}

	@Test
	void registerRejectsPasswordsLongerThanBcryptReads() {
		// 40 characters, but 80 bytes in UTF-8.
		var password = "ş".repeat(40);

		assertThat(post("/api/v1/auth/register", """
				{"fullName": "Ayşe Yılmaz", "email": "ayse@example.com", "phone": "05321234567", "password": "%s"}"""
				.formatted(password)))
				.hasStatus(400)
				.bodyJson()
				.extractingPath("$.errors.password")
				.asString()
				.contains("bytes");
		verifyNoInteractions(auth);
	}

	@Test
	void registerWithATakenEmailIsAConflict() {
		given(auth.register(any())).willThrow(new BusinessRuleException("An account with this email already exists."));

		assertThat(post("/api/v1/auth/register", """
				{"fullName": "Ayşe Yılmaz", "email": "ayse@example.com", "phone": "05321234567",
				 "password": "correct horse"}""")).hasStatus(409);
	}

	@Test
	void loginReturnsTokens() {
		given(auth.login(new LoginRequest("ayse@example.com", "correct horse"))).willReturn(SIGNED_IN);

		assertThat(post("/api/v1/auth/login", """
				{"email": "ayse@example.com", "password": "correct horse"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.user.fullName")
				.asString()
				.isEqualTo("Ayşe Yılmaz");
	}

	@Test
	void wrongCredentialsAre401() {
		given(auth.login(any())).willThrow(new BadCredentialsException("Invalid email or password"));

		assertThat(post("/api/v1/auth/login", """
				{"email": "ayse@example.com", "password": "wrong"}""")).hasStatus(401);
	}

	@Test
	void loginRequiresBothFields() {
		assertThat(post("/api/v1/auth/login", """
				{"email": "", "password": ""}""")).hasStatus(400);
		verifyNoInteractions(auth);
	}

	@Test
	void refreshReturnsANewAccessToken() {
		given(auth.refresh("refresh-token"))
				.willReturn(new AccessTokenResponse("new.jwt", Instant.parse("2026-11-12T10:30:00Z")));

		assertThat(post("/api/v1/auth/refresh", """
				{"refreshToken": "refresh-token"}"""))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.accessToken")
				.asString()
				.isEqualTo("new.jwt");
	}

	@Test
	void logoutRevokesTheRefreshToken() {
		assertThat(post("/api/v1/auth/logout", """
				{"refreshToken": "refresh-token"}""")).hasStatus(204);

		then(auth).should().logout("refresh-token");
	}

	@Test
	@WithMockUser(username = "42")
	void meReturnsTheSignedInUser() {
		given(auth.currentUser("42")).willReturn(AYSE);

		var body = assertThat(mockMvc.get().uri("/api/v1/auth/me")).hasStatusOk().bodyJson();

		body.extractingPath("$.id").asNumber().isEqualTo(42);
		body.extractingPath("$.phone").asString().isEqualTo("+905321234567");
		body.doesNotHavePath("$.passwordHash");
	}

	private MockMvcTester.MockMvcRequestBuilder post(String uri, String json) {
		return mockMvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
