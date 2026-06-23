package com.ben.com.backend.web.dto;

import java.math.BigDecimal;

public record VoteOptionResult(
		String choiceKey,
		String label,
		long votes,
		BigDecimal weight,
		double voteRatio,
		double weightRatio
) {
}
