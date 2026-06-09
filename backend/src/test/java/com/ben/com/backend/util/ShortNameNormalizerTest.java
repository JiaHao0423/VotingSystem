package com.ben.com.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShortNameNormalizerTest {

	@Test
	void normalizesShopSPrefixToChineseShop() {
		assertThat(ShortNameNormalizer.normalize("S1")).isEqualTo("店1");
		assertThat(ShortNameNormalizer.normalize("s2")).isEqualTo("店2");
	}
}
