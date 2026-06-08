package com.ben.com.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortNameNormalizerTest {

	@Test
	void normalizesResidentialShortName() {
		assertThat(ShortNameNormalizer.normalize(" 4a7 ")).isEqualTo("4A7");
		assertThat(ShortNameNormalizer.normalize("5b2")).isEqualTo("5B2");
	}

	@Test
	void keepsShopShortName() {
		assertThat(ShortNameNormalizer.normalize(" 店1 ")).isEqualTo("店1");
	}
}
