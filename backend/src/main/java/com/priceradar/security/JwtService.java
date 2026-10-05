package com.priceradar.security;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.priceradar.config.SecurityProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and verifies the HMAC-signed access tokens used by the API.
 *
 * <p>Refresh tokens are deliberately <em>not</em> JWTs: they are opaque random
 * values stored hashed in {@code refresh_tokens} so they can be revoked.
 */
@Service
public class JwtService {

	private static final String CLAIM_ROLES = "roles";

	private final SecretKey signingKey;
	private final SecurityProperties.Jwt properties;

	public JwtService(SecurityProperties securityProperties) {
		this.properties = securityProperties.jwt();
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.properties.secret()));
	}

	/** Signs an access token for the given subject and authorities. */
	public String issueAccessToken(String subject, Collection<String> roles) {
		var now = Instant.now();
		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(subject)
				.claim(CLAIM_ROLES, List.copyOf(roles))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(properties.accessTokenTtl())))
				.signWith(signingKey)
				.compact();
	}

	/**
	 * Verifies signature, issuer and expiry.
	 *
	 * @throws JwtException when the token is not a valid, unexpired token issued by us
	 */
	public Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.requireIssuer(properties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	@SuppressWarnings("unchecked")
	public List<String> rolesOf(Claims claims) {
		var roles = claims.get(CLAIM_ROLES);
		return roles instanceof Collection<?> collection ? List.copyOf((Collection<String>) collection) : List.of();
	}

}
