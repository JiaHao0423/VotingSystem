package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.entity.Community;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProposalResultCalculatorTest {

	@Test
	void resolveCommunityWeightPrefersUnitsSum() {
		var community = new Community("測試社區", 100, new BigDecimal("5000"), "地址");

		assertThat(ProposalResultCalculator.resolveCommunityWeight(community, new BigDecimal("3200.5")))
				.isEqualByComparingTo("3200.5");
	}

	@Test
	void resolveCommunityWeightFallsBackToCommunityTotalArea() {
		var community = new Community("測試社區", 100, new BigDecimal("5000"), "地址");

		assertThat(ProposalResultCalculator.resolveCommunityWeight(community, BigDecimal.ZERO))
				.isEqualByComparingTo("5000");
	}
}
