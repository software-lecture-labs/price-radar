package com.priceradar.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.priceradar.security.RateLimiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Accounts, sign-in and sessions")
public class AuthController {

	private final AuthService auth;
	private final RateLimiter rateLimiter;

	public AuthController(AuthService auth, RateLimiter rateLimiter) {
		this.auth = auth;
		this.rateLimiter = rateLimiter;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an account and sign in")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		countAttempt("register", httpRequest);
		return auth.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Sign in with email and password")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		countAttempt("login", httpRequest);
		return auth.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Exchange a refresh token for a new access token")
	public AccessTokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
		countAttempt("refresh", httpRequest);
		return auth.refresh(request.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Revoke a refresh token")
	public void logout(@Valid @RequestBody RefreshRequest request) {
		auth.logout(request.refreshToken());
	}

	@GetMapping("/me")
	@Operation(summary = "The signed-in user")
	public UserProfile me(@CurrentSecurityContext(expression = "authentication.name") String userId) {
		return auth.currentUser(userId);
	}

	/**
	 * Each endpoint has its own limit per client IP, so refreshing a session does not use up sign-in attempts. The
	 * address comes from Tomcat, which takes X-Forwarded-For into account only when the request arrives from a trusted
	 * proxy ({@code server.forward-headers-strategy: native}); a client cannot pick its own address to get a fresh
	 * limit.
	 */
	private void countAttempt(String endpoint, HttpServletRequest request) {
		rateLimiter.acquire(endpoint + ":" + request.getRemoteAddr());
	}

}
