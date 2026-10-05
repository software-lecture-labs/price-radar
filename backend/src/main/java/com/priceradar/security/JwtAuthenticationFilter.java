package com.priceradar.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Populates the {@link SecurityContextHolder} from a {@code Bearer} token.
 *
 * <p>Registered as a bean by {@link SecurityConfig} rather than annotated as a
 * component: as a {@code Filter} it would otherwise be picked up by web-slice
 * tests that do not load the security collaborators it needs.
 *
 * <p>An invalid token is treated as "no authentication" rather than an error:
 * the authorization rules then decide whether the endpoint was public anyway,
 * and protected endpoints fall through to the entry point as a 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		var token = bearerToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				var claims = jwtService.parse(token);
				var authorities = jwtService.rolesOf(claims).stream()
						.map(SimpleGrantedAuthority::new)
						.toList();
				var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (JwtException | IllegalArgumentException ex) {
				logger.debug("Rejected bearer token: " + ex.getMessage());
				SecurityContextHolder.clearContext();
			}
		}

		chain.doFilter(request, response);
	}

	private static String bearerToken(HttpServletRequest request) {
		var header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			return null;
		}
		var token = header.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? null : token;
	}

}
