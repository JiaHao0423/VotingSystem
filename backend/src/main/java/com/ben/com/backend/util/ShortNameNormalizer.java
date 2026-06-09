package com.ben.com.backend.util;

import java.util.regex.Pattern;

public final class ShortNameNormalizer {

	private static final Pattern RESIDENTIAL = Pattern.compile("^(\\d+)([ABab])(\\d+)$");
	private static final Pattern SHOP_S_PREFIX = Pattern.compile("^[Ss](\\d+)$");

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
		var shopS = SHOP_S_PREFIX.matcher(trimmed);
		if (shopS.matches()) {
			return "店" + shopS.group(1);
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
