package com.priceradar.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import com.priceradar.common.exception.BusinessRuleException;
import com.priceradar.config.SecurityProperties;
import com.priceradar.security.JwtService;

/**
 * Email and password sign-in. A session is a short-lived JWT access token plus an opaque refresh token that can be
 * revoked; only the refresh token's hash is stored.
 *
 * <p>Register and login hash or check the password with BCrypt before their transaction starts, so no database
 * connection is held while BCrypt runs. The writes that follow (account, refresh token) commit together.</p>
 *
 * <h2>Refresh tokens are not rotated</h2>
 * <p>A refresh returns a new access token and keeps the same refresh token. This is deliberate. Rotation only adds
 * protection if reuse of an old token is detected and the whole token family is then revoked, which needs family
 * tracking and a grace period for two tabs refreshing at once. That is more than this project needs for now.</p>
 * <p>The cost: a stolen refresh token stays usable until the user logs out or it expires
 * ({@code app.security.jwt.refresh-token-ttl}). What limits the damage:</p>
 * <ul>
 * <li>only a SHA-256 hash of each refresh token is stored, so a database leak gives no usable tokens;</li>
 * <li>access tokens are short-lived ({@code app.security.jwt.access-token-ttl}) and every refresh reads the role
 * again, so a role change applies at the next refresh;</li>
 * <li>refresh attempts are rate limited per client IP.</li>
 * </ul>
 * <p>Rotation should be added if refresh tokens ever end up somewhere scripts can read them, or when clients such as
 * a mobile app are added.</p>
 */
@Service
public class AuthService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int REFRESH_TOKEN_BYTES = 32;

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SecurityProperties.Jwt properties;
	private final TransactionOperations transactions;
	// Checked when the email is unknown, so a wrong email takes as long as a wrong password.
	private final String unknownUserHash;

	AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
			JwtService jwtService, SecurityProperties securityProperties, TransactionOperations transactions) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.properties = securityProperties.jwt();
		this.transactions = transactions;
		this.unknownUserHash = passwordEncoder.encode("unknown-user-" + RANDOM.nextLong());
	}

	public AuthResponse register(RegisterRequest request) {
		var passwordHash = passwordEncoder.encode(request.password());
		return transactions.execute(status -> {
			UserAccount user;
			try {
				user = users.create(normalizeEmail(request.email()), request.fullName().strip(),
						PhoneNumbers.normalize(request.phone()), passwordHash);
			}
			catch (DuplicateKeyException ex) {
				throw new BusinessRuleException("An account with this email already exists.");
			}
			return signIn(user);
		});
	}

	public AuthResponse login(LoginRequest request) {
		var user = users.findByEmail(normalizeEmail(request.email()));
		var hash = user.map(UserAccount::passwordHash).orElse(unknownUserHash);
		var passwordMatches = passwordEncoder.matches(request.password(), hash);
		if (user.isEmpty() || !passwordMatches) {
			throw new BadCredentialsException("Invalid email or password");
		}
		return transactions.execute(status -> signIn(user.get()));
	}

	/** Issues a new access token; the role is read again, so a role change applies from the next refresh. */
	@Transactional(readOnly = true)
	public AccessTokenResponse refresh(String refreshToken) {
		var user = refreshTokens.findActiveUserId(hash(refreshToken))
				.flatMap(users::findById)
				.orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
		var accessToken = accessToken(user);
		return new AccessTokenResponse(accessToken.value(), accessToken.expiresAt());
	}

	public void logout(String refreshToken) {
		refreshTokens.revoke(hash(refreshToken));
	}

	/** @param subject the access token's subject, which is the user id */
	@Transactional(readOnly = true)
	public UserProfile currentUser(String subject) {
		long id;
		try {
			id = Long.parseLong(subject);
		}
		catch (NumberFormatException ex) {
			throw new BadCredentialsException("Token subject is not a user id");
		}
		return users.findById(id)
				.map(UserAccount::profile)
				.orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
	}

	private AuthResponse signIn(UserAccount user) {
		var accessToken = accessToken(user);
		var refreshToken = newRefreshToken();
		var refreshTokenExpiresAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(properties.refreshTokenTtl());
		refreshTokens.deleteInactive(user.id());
		refreshTokens.save(user.id(), hash(refreshToken), refreshTokenExpiresAt);
		return new AuthResponse(accessToken.value(), accessToken.expiresAt(), refreshToken, refreshTokenExpiresAt,
				user.profile());
	}

	private JwtService.AccessToken accessToken(UserAccount user) {
		return jwtService.issue(String.valueOf(user.id()), List.of(user.role().authority()));
	}

	static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private static String newRefreshToken() {
		var bytes = new byte[REFRESH_TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hash(String token) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required by every Java platform", ex);
		}
	}

}
