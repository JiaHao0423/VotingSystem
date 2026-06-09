package com.ben.com.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class OwnershipRatioParser {

	private static final Pattern FRACTION = Pattern.compile(
			"^\\s*([\\d,]+(?:\\.\\d+)?)\\s*/\\s*([\\d,]+(?:\\.\\d+)?)\\s*$"
	);

	private OwnershipRatioParser() {
	}

	public record ParsedRatio(BigDecimal area, BigDecimal ownershipPercent) {
	}

	public static ParsedRatio parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("區分所有權比例不可空白");
		}

		var fractionMatcher = FRACTION.matcher(raw.trim());
		if (fractionMatcher.matches()) {
			var numerator = parseDecimal(fractionMatcher.group(1));
			var denominator = parseDecimal(fractionMatcher.group(2));
			if (numerator.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("區分所有權比例分子必須大於 0");
			}
			if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("區分所有權比例分母必須大於 0");
			}
			var percent = numerator
					.divide(denominator, 10, RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100))
					.setScale(2, RoundingMode.HALF_UP);
			return new ParsedRatio(numerator.setScale(2, RoundingMode.HALF_UP), percent);
		}

		var percent = parseDecimal(raw.replace("%", "").trim());
		return new ParsedRatio(null, percent.setScale(2, RoundingMode.HALF_UP));
	}

	private static BigDecimal parseDecimal(String value) {
		return new BigDecimal(value.replace(",", "").trim());
	}
}
