package com.priceradar.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No password rules here: a rule added later must not lock out existing accounts.
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 256) String password) {

	@Override
	public String toString() {
		return "LoginRequest[email=%s]".formatted(email);
	}

}
