package com.priceradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = { "05321234567", "0532 123 45 67", "0 (532) 123 45 67", "532-123-45-67", "5321234567",
			"+90 532 123 45 67", "+905321234567", "905321234567", "0090 532 123 4567" })
	void acceptsTurkishMobileNumbersInCommonFormats(String phone) {
		assertThat(phone).matches(PhoneNumbers.TURKISH_MOBILE);
		assertThat(PhoneNumbers.normalize(phone)).isEqualTo("+905321234567");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0212 123 45 67", "0532 123 45 6", "0532 123 45 678", "+1 532 123 45 67",
			"0532 ABC 45 67", "", "++905321234567" })
	void rejectsLandlinesForeignAndMalformedNumbers(String phone) {
		assertThat(phone).doesNotMatch(PhoneNumbers.TURKISH_MOBILE);
		assertThatThrownBy(() -> PhoneNumbers.normalize(phone)).isInstanceOf(IllegalArgumentException.class);
	}

}
