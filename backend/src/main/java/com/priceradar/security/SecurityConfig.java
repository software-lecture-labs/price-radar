package com.priceradar.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.priceradar.config.SecurityProperties;

/**
 * Stateless, token-authenticated API.
 *
 * <p>Only infrastructure endpoints and the API documentation are open. Add
 * application-specific rules above {@code anyRequest()} as endpoints are built.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
			CorsConfigurationSource corsConfigurationSource) throws Exception {

		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Infrastructure
						.requestMatchers("/actuator/health/**", "/actuator/info", "/api/v1/ping").permitAll()
						.requestMatchers("/actuator/**").hasRole("ADMIN")
						// API documentation
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
		var cors = properties.cors();
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(cors.allowedOrigins());
		configuration.setAllowedMethods(cors.allowedMethods());
		configuration.setAllowedHeaders(cors.allowedHeaders());
		configuration.setAllowCredentials(cors.allowCredentials());
		configuration.setMaxAge(cors.maxAge());
		configuration.setExposedHeaders(List.of("X-Total-Count"));

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
		return new JwtAuthenticationFilter(jwtService);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

}
