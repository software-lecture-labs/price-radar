package com.priceradar.auth;

import java.time.Instant;

/** A stored account. Holds the password hash, so it never leaves the auth package; see {@link UserProfile}. */
record UserAccount(long id, String email, String fullName, String phone, String passwordHash, Role role,
		Instant createdAt) {

	UserProfile profile() {
		return new UserProfile(id, email, fullName, phone, role, createdAt);
	}

}
