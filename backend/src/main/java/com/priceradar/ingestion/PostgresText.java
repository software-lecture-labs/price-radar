package com.priceradar.ingestion;

import tools.jackson.databind.JsonNode;

// PostgreSQL text/jsonb reject NUL and unpaired UTF-16 surrogates, although JSON permits them.
final class PostgresText {

	private PostgresText() {
	}

	static void requireStorable(JsonNode node) {
		if (node.isString()) {
			requireStorable(node.stringValue());
		}
		if (node.isObject()) {
			node.propertyNames().forEach(PostgresText::requireStorable);
		}
		if (node.isContainer()) {
			node.forEach(PostgresText::requireStorable);
		}
	}

	private static void requireStorable(String text) {
		for (var index = 0; index < text.length(); index++) {
			var character = text.charAt(index);
			if (character == '\0') {
				throw new InvalidSearchFileException("contains a NUL character, which PostgreSQL cannot store");
			}
			if (Character.isHighSurrogate(character) && index + 1 < text.length()
					&& Character.isLowSurrogate(text.charAt(index + 1))) {
				index++;
			}
			else if (Character.isSurrogate(character)) {
				throw new InvalidSearchFileException("contains an unpaired surrogate, which PostgreSQL cannot store");
			}
		}
	}

}
