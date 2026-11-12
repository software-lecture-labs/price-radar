package com.priceradar.auth;

public enum Role {

	USER,
	ADMIN;

	/** Spring Security's {@code hasRole("ADMIN")} checks for the {@code ROLE_} prefix. */
	public String authority() {
		return "ROLE_" + name();
	}

}
