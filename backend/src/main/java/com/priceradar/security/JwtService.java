package com.priceradar.security;

import java.time.Instant;
import java.util.ArrayList;
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

@Service
public class JwtService {

	private static final String CLAIM_ROLES = "roles";

	private final SecretKey signingKey;
	private final SecurityProperties.Jwt properties;

	public JwtService(SecurityProperties securityProperties) {
		this.properties = securityProperties.jwt();
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.properties.secret()));
	}

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

	public Claims parse(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.requireIssuer(properties.issuer())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public List<String> rolesOf(Claims claims) {
		var roles = claims.get(CLAIM_ROLES);
		if (!(roles instanceof Collection<?> collection)) {
			return List.of();
		}
		var authorities = new ArrayList<String>(collection.size());
		for (var role : collection) {
			if (!(role instanceof String authority) || authority.isBlank()) {
				throw new IllegalArgumentException("JWT roles must contain only non-blank strings");
			}
			authorities.add(authority);
		}
		return List.copyOf(authorities);
	}

}
