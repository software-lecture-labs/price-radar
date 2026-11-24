package com.priceradar.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.priceradar.security.RateLimitExceededException;
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
		requireNotRateLimited(httpRequest);
		return auth.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Sign in with email and password")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		requireNotRateLimited(httpRequest);
		return auth.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Exchange a refresh token for a new access token")
	public AccessTokenResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
		requireNotRateLimited(httpRequest);
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

	private void requireNotRateLimited(HttpServletRequest request) {
		if (!rateLimiter.tryAcquire(clientIp(request))) {
			throw new RateLimitExceededException("Too many authentication attempts. Please try again later.");
		}
	}

	/**
	 * Returns the client IP, respecting {@code X-Forwarded-For} when behind a reverse proxy. The
	 * {@code server.forward-headers-strategy=framework} setting ensures Spring validates the header.
	 */
	private static String clientIp(HttpServletRequest request) {
		var forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].strip();
		}
		return request.getRemoteAddr();
	}

}
