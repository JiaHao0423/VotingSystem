package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.enums.ProposalType;
import com.ben.com.backend.domain.enums.ThresholdBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProposalResultResponse(
		Long id,
		String proposalNumber,
		String title,
		String content,
		ProposalType type,
		ProposalStatus status,
		List<VoteOptionResult> options,
		long totalVotedHouseholds,
		BigDecimal totalVotedWeight,
		int totalCommunityHouseholds,
		BigDecimal totalCommunityWeight,
		int thresholdHouseholds,
		BigDecimal thresholdWeight,
		int passThresholdNumerator,
		int passThresholdDenominator,
		ThresholdBase thresholdBase,
		double agreeHouseholdRatio,
		double agreeWeightRatio,
		boolean passed,
		Instant votedAt
) {
}
