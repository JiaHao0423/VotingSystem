package com.ben.com.backend.util;

import java.util.regex.Pattern;

public final class ShortNameNormalizer {

	private static final Pattern RESIDENTIAL = Pattern.compile("^(\\d+)([ABab])(\\d+)$");

	private ShortNameNormalizer() {
	}

	public static String normalize(String input) {
		if (input == null) {
			return null;
		}
		var trimmed = input.trim().replace(" ", "");
		if (trimmed.isEmpty()) {
			return trimmed;
		}
		if (trimmed.startsWith("店")) {
			return trimmed;
		}

		var matcher = RESIDENTIAL.matcher(trimmed);
		if (matcher.matches()) {
			return matcher.group(1) + matcher.group(2).toUpperCase() + matcher.group(3);
		}
		return trimmed;
	}
}
