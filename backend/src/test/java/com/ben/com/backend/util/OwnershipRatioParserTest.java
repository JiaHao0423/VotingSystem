package com.ben.com.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OwnershipRatioParserTest {

	@Test
	void parsesFractionToPercentWithTwoDecimals() {
		var parsed = OwnershipRatioParser.parse("94.60 / 13241.21");

		assertThat(parsed.area()).isEqualByComparingTo("94.60");
		assertThat(parsed.ownershipPercent()).isEqualByComparingTo("0.71");
	}

	@Test
	void parsesResidentialFraction() {
		var parsed = OwnershipRatioParser.parse("84.00 / 13241.21");

		assertThat(parsed.area()).isEqualByComparingTo("84.00");
		assertThat(parsed.ownershipPercent()).isEqualByComparingTo("0.63");
	}
}
