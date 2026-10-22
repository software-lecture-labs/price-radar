package com.priceradar.ingestion;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.priceradar.ingestion.Marketplace.Field;

import tools.jackson.databind.JsonNode;

/**
 * Typed, validated reads of one search result item. Every method returns
 * {@code null} for a value that is absent, and throws
 * {@link InvalidSearchFileException} for a value that is present but wrong.
 */
final class JsonValues {

	/** {@code numeric(16, 2)} in the schema: 14 digits before the decimal point, 2 after. */
	private static final BigDecimal MONEY_LIMIT = new BigDecimal("1e14");
	private static final int MONEY_SCALE = 2;
	/** {@code numeric(8, 3)} for rating scales. */
	private static final BigDecimal RATING_SCALE_LIMIT = new BigDecimal("1e5");
	private static final int RATING_SCALE_SCALE = 3;
	private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

	private final JsonNode item;
	private final Marketplace marketplace;

	JsonValues(JsonNode item, Marketplace marketplace) {
		this.item = item;
		this.marketplace = marketplace;
	}

	String text(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		check(node.isString(), field, "must be text");
		return node.stringValue();
	}

	String requiredText(Field field) {
		var text = text(field);
		check(text != null && !text.isBlank(), field, "is required");
		return text;
	}

	/** Identifiers arrive as text or as whole numbers; both are kept as text, leading zeros intact. */
	String identifier(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		if (node.isIntegralNumber()) {
			return node.bigIntegerValue().toString();
		}
		check(node.isString() && !node.stringValue().isBlank(), field, "must be non-blank text or a whole number");
		return node.stringValue();
	}

	String requiredIdentifier(Field field) {
		var identifier = identifier(field);
		check(identifier != null, field, "is required");
		return identifier;
	}

	List<String> textList(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		check(node.isArray(), field, "must be a list");
		var values = new ArrayList<String>();
		for (var element : node) {
			check(element.isString(), field, "must contain only text");
			values.add(element.stringValue());
		}
		return values;
	}

	Boolean flag(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		check(node.isBoolean(), field, "must be true or false");
		return node.booleanValue();
	}

	Integer count(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		check(node.isIntegralNumber() && node.canConvertToInt() && node.intValue() >= 0, field,
				"must be a non-negative whole number");
		return node.intValue();
	}

	BigDecimal decimal(Field field) {
		var node = node(field);
		if (node == null) {
			return null;
		}
		check(node.isNumber() && node.decimalValue().signum() >= 0, field, "must be a non-negative number");
		return node.decimalValue();
	}

	/** An amount that must fit {@code numeric(16, 2)} exactly, without rounding. */
	BigDecimal money(Field field) {
		var amount = decimal(field);
		check(amount == null || fits(amount, MONEY_SCALE, MONEY_LIMIT), field, "does not fit numeric(16, 2)");
		return amount;
	}

	BigDecimal requiredMoney(Field field) {
		var amount = money(field);
		check(amount != null, field, "is required");
		return amount;
	}

	BigDecimal ratingScale(Field field) {
		var scale = decimal(field);
		check(scale == null || (scale.signum() > 0 && fits(scale, RATING_SCALE_SCALE, RATING_SCALE_LIMIT)), field,
				"must be positive and fit numeric(8, 3)");
		return scale;
	}

	String currency(Field field) {
		var code = text(field);
		check(code == null || CURRENCY_CODE.matcher(code).matches(), field, "must be an ISO 4217 code");
		return code;
	}

	/** An absolute http(s) URL; stored as given. */
	String requiredUrl(Field field) {
		var url = requiredText(field);
		try {
			var uri = new URI(url);
			check(("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null,
					field, "must be an absolute http(s) URL");
		}
		catch (URISyntaxException ex) {
			throw invalid(field, "is not a valid URL");
		}
		return url;
	}

	/** The node at the field's location, or {@code null} when it is absent, JSON null or not provided. */
	private JsonNode node(Field field) {
		var pointer = marketplace.pointerTo(field);
		if (pointer == null) {
			return null;
		}
		var node = item.at(pointer);
		return node.isMissingNode() || node.isNull() ? null : node;
	}

	private static boolean fits(BigDecimal value, int scale, BigDecimal limit) {
		return value.stripTrailingZeros().scale() <= scale && value.compareTo(limit) < 0;
	}

	private void check(boolean condition, Field field, String problem) {
		if (!condition) {
			throw invalid(field, problem);
		}
	}

	private InvalidSearchFileException invalid(Field field, String problem) {
		return new InvalidSearchFileException(
				"%s (%s) %s".formatted(field, marketplace.pointerTo(field), problem));
	}

}
