package com.priceradar.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.priceradar.ingestion.Marketplace;

/** Conservative identity checks: a search query or a similar model name is never an identity. */
final class ProductComparison {

	private static final Pattern CAPACITY = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(TB|GB|MB)\\b");
	private static final Pattern COLOR = Pattern.compile(
			"(?U)\\b(siyah|black|beyaz|white|mavi|blue|lacivert|navy|gümüş|silver|gri|gray|grey|"
					+ "kırmızı|red|yeşil|green|pembe|pink|mor|purple|sarı|yellow|turuncu|orange|krem|cream|"
					+ "abis|burgonya|burgundy)\\b");
	private static final Pattern BUNDLE = Pattern.compile(
			"(?U)\\b(uyumlu|kılıf|kilif|case|koruyucu|yedek|paket|paketi|bundle|seti|set|adet)\\b");

	record Identity(long id, String title, String brand, String barcode, String mpn, String currency) {
	}

	record Candidate(Identity identity, StoreRef store, BigDecimal price, BigDecimal listPrice,
			Boolean inStock, Boolean freeShipping, String url, Instant observedAt, String sellerName,
			int offerCount) {
	}

	private ProductComparison() {
	}

	static List<ProductDetail.ComparisonOffer> offers(Identity selected, List<Candidate> candidates) {
		var bestByStore = new HashMap<String, ProductDetail.ComparisonOffer>();
		for (var candidate : candidates) {
			var match = matchType(selected, candidate.identity());
			if (match == null || !Objects.equals(selected.currency(), candidate.identity().currency())) {
				continue;
			}
			var offer = new ProductDetail.ComparisonOffer(candidate.store(), candidate.identity().id(),
					candidate.identity().title(), candidate.price(), candidate.listPrice(),
					candidate.identity().currency(), candidate.inStock(), candidate.freeShipping(), candidate.url(),
					candidate.observedAt(), candidate.sellerName(), candidate.offerCount(), match);
			bestByStore.merge(candidate.store().code(), offer,
					(left, right) -> OFFER_ORDER.compare(left, right) <= 0 ? left : right);
		}
		return Arrays.stream(Marketplace.values())
				.map(store -> bestByStore.getOrDefault(store.code(), new ProductDetail.ComparisonOffer(
						new StoreRef(store.code(), store.displayName()), null, null, null, null, null,
						null, null, null, null, null, 0, null)))
				.toList();
	}

	// Stock information takes precedence over price; each candidate already uses its latest observation.
	private static final Comparator<ProductDetail.ComparisonOffer> OFFER_ORDER = Comparator
			.comparingInt((ProductDetail.ComparisonOffer offer) -> Boolean.FALSE.equals(offer.inStock()) ? 1 : 0)
			.thenComparing(ProductDetail.ComparisonOffer::price)
			.thenComparing(ProductDetail.ComparisonOffer::observedAt, Comparator.reverseOrder())
			.thenComparing(ProductDetail.ComparisonOffer::productId)
			.thenComparing(ProductDetail.ComparisonOffer::url);

	static String matchType(Identity selected, Identity candidate) {
		if (selected.id() == candidate.id()) {
			return "listing";
		}
		var selectedBarcode = barcode(selected.barcode());
		var candidateBarcode = barcode(candidate.barcode());
		if (selectedBarcode != null && candidateBarcode != null) {
			// A contradictory GTIN also vetoes an otherwise identical title or MPN.
			return selectedBarcode.equals(candidateBarcode) ? "barcode" : null;
		}
		var brand = normalize(selected.brand());
		if (brand.isEmpty() || !brand.equals(normalize(candidate.brand()))) {
			return null;
		}
		var mpn = normalizeMpn(selected.mpn());
		var candidateMpn = normalizeMpn(candidate.mpn());
		if (!mpn.isEmpty() && !candidateMpn.isEmpty() && !mpn.equals(candidateMpn)) {
			return null;
		}
		if (mpn.length() >= 4 && mpn.chars().anyMatch(Character::isDigit)
				&& mpn.equals(candidateMpn)
				&& compatibleVariants(selected.title(), candidate.title())) {
			return "mpn";
		}
		var title = normalize(selected.title());
		// Do not group generic headings such as "Samsung Telefon" across unrelated listings.
		return title.length() >= 12 && title.chars().anyMatch(Character::isDigit)
				&& title.equals(normalize(candidate.title())) ? "exact_title" : null;
	}

	static String normalize(String value) {
		return value == null ? "" : value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	static String normalizeMpn(String value) {
		return normalize(value).replaceAll("\\s+", "");
	}

	/** Validate GTIN-8/12/13/14 and compare their zero-padded representation. */
	static String barcode(String value) {
		if (value == null) {
			return null;
		}
		var digits = value.replaceAll("\\s+", "");
		if (!digits.matches("(?:[0-9]{8}|[0-9]{12,14})") || digits.chars().allMatch(digit -> digit == '0')) {
			return null;
		}
		int sum = 0;
		for (int i = digits.length() - 1, weight = 1; i >= 0; i--, weight = 4 - weight) {
			sum += (digits.charAt(i) - '0') * weight;
		}
		return sum % 10 == 0 ? "0".repeat(14 - digits.length()) + digits : null;
	}

	private static boolean compatibleVariants(String left, String right) {
		var leftTitle = normalize(left);
		var rightTitle = normalize(right);
		var leftCapacity = values(CAPACITY, leftTitle);
		var rightCapacity = values(CAPACITY, rightTitle);
		var leftColor = values(COLOR, leftTitle);
		var rightColor = values(COLOR, rightTitle);
		return (leftCapacity.isEmpty() || rightCapacity.isEmpty() || leftCapacity.equals(rightCapacity))
				&& (leftColor.isEmpty() || rightColor.isEmpty() || leftColor.equals(rightColor))
				&& values(BUNDLE, leftTitle).equals(values(BUNDLE, rightTitle));
	}

	private static Set<String> values(Pattern pattern, String value) {
		var values = new HashSet<String>();
		var matcher = pattern.matcher(value);
		while (matcher.find()) {
			values.add(matcher.group().replaceAll("\\s+", ""));
		}
		return values;
	}

}
