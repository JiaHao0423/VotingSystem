package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.VoteChoice;
import java.math.BigDecimal;

public record VoteOptionResult(
		VoteChoice choice,
		String label,
		long votes,
		BigDecimal weight,
		double voteRatio,
		double weightRatio
) {
}
