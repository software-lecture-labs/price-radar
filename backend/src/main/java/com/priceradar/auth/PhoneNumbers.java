package com.priceradar.auth;

/** Turkish mobile numbers, the only kind the sign-up form asks for. */
final class PhoneNumbers {

	/**
	 * 05XX XXX XX XX, 5XXXXXXXXX, +90 5XX XXX XX XX or 0090 5XX..., with optional spaces, dashes and parentheses
	 * around the operator code.
	 */
	static final String TURKISH_MOBILE = "^(?:\\+90|0090|90|0)?[\\s-]*\\(?5\\d{2}\\)?(?:[\\s-]*\\d){7}$";

	private PhoneNumbers() {
	}

	/** Returns E.164, for example {@code +905321234567}. The input must match {@link #TURKISH_MOBILE}. */
	static String normalize(String phone) {
		if (phone == null || !phone.strip().matches(TURKISH_MOBILE)) {
			throw new IllegalArgumentException("Not a Turkish mobile number");
		}
		var digits = phone.replaceAll("\\D", "");
		return "+90" + digits.substring(digits.length() - 10);
	}

}
