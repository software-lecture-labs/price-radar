package com.priceradar.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Pattern(regexp = PhoneNumbers.TURKISH_MOBILE, message = "must be a Turkish mobile number") String phone,
		@NotBlank @Size(min = 8, max = 72) @MaxUtf8Bytes(72) String password) {

	@Override
	public String toString() {
		return "RegisterRequest[email=%s]".formatted(email);
	}

}
