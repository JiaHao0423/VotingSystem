package com.ben.com.backend.util;

import com.ben.com.backend.domain.enums.BuildingType;
import java.util.Optional;
import java.util.regex.Pattern;

public final class UnitShortNameParser {

	private static final Pattern RESIDENTIAL = Pattern.compile("^(\\d+)([AB])(\\d+)$");
	private static final Pattern SHOP = Pattern.compile("^店(\\d+)$");

	private UnitShortNameParser() {
	}

	public record ParsedUnit(
			String shortName,
			BuildingType buildingType,
			Integer floor,
			Integer unitNo,
			Integer shopNo
	) {
	}

	public static Optional<ParsedUnit> parse(String rawShortName) {
		var shortName = ShortNameNormalizer.normalize(rawShortName);
		if (shortName == null || shortName.isBlank()) {
			return Optional.empty();
		}

		var shopMatcher = SHOP.matcher(shortName);
		if (shopMatcher.matches()) {
			return Optional.of(new ParsedUnit(
					shortName,
					BuildingType.SHOP,
					null,
					null,
					Integer.parseInt(shopMatcher.group(1))
			));
		}

		var residentialMatcher = RESIDENTIAL.matcher(shortName);
		if (residentialMatcher.matches()) {
			var building = BuildingType.valueOf(residentialMatcher.group(2));
			return Optional.of(new ParsedUnit(
					shortName,
					building,
					Integer.parseInt(residentialMatcher.group(1)),
					Integer.parseInt(residentialMatcher.group(3)),
					null
			));
		}

		return Optional.empty();
	}
}
