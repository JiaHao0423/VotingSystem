package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.VoteChoice;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.VoteOptionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class ProposalResultCalculator {

	private static final double PASS_RATIO = 0.5;

	private ProposalResultCalculator() {
	}

	public static ProposalResultResponse compute(Proposal proposal, Community community, VoteRecordRepository voteRecordRepository) {
		return compute(proposal, community, voteRecordRepository, null);
	}

	public static ProposalResultResponse compute(
			Proposal proposal,
			Community community,
			VoteRecordRepository voteRecordRepository,
			Instant votedAt
	) {
		long totalVotes = voteRecordRepository.countByProposalId(proposal.getId());
		BigDecimal totalWeight = voteRecordRepository.sumWeightByProposalId(proposal.getId());

		var options = Arrays.stream(VoteChoice.values())
				.map(choice -> toOptionResult(proposal.getId(), choice, totalVotes, totalWeight, voteRecordRepository))
				.toList();

		long agreeVotes = voteRecordRepository.countByProposalIdAndChoice(proposal.getId(), VoteChoice.AGREE);
		BigDecimal agreeWeight = voteRecordRepository.sumWeightByProposalIdAndChoice(proposal.getId(), VoteChoice.AGREE);

		double agreeHouseholdRatio = totalVotes > 0 ? (double) agreeVotes / totalVotes : 0;
		double agreeWeightRatio = totalWeight.compareTo(BigDecimal.ZERO) > 0
				? agreeWeight.divide(totalWeight, 6, RoundingMode.HALF_UP).doubleValue()
				: 0;
		boolean passed = agreeHouseholdRatio > PASS_RATIO && agreeWeightRatio > PASS_RATIO;

		return new ProposalResultResponse(
				proposal.getId(),
				proposal.getProposalNumber(),
				proposal.getTitle(),
				proposal.getContent(),
				proposal.getType(),
				proposal.getStatus(),
				options,
				totalVotes,
				totalWeight,
				agreeHouseholdRatio,
				agreeWeightRatio,
				passed,
				votedAt
		);
	}

	private static VoteOptionResult toOptionResult(
			Long proposalId,
			VoteChoice choice,
			long totalVotes,
			BigDecimal totalWeight,
			VoteRecordRepository voteRecordRepository
	) {
		long votes = voteRecordRepository.countByProposalIdAndChoice(proposalId, choice);
		BigDecimal weight = voteRecordRepository.sumWeightByProposalIdAndChoice(proposalId, choice);
		double voteRatio = totalVotes > 0 ? (double) votes / totalVotes : 0;
		double weightRatio = totalWeight.compareTo(BigDecimal.ZERO) > 0
				? weight.divide(totalWeight, 6, RoundingMode.HALF_UP).doubleValue()
				: 0;

		return new VoteOptionResult(choice, label(choice), votes, weight, voteRatio, weightRatio);
	}

	public static String label(VoteChoice choice) {
		return switch (choice) {
			case AGREE -> "同意";
			case DISAGREE -> "不同意";
			case ABSTAIN -> "棄權";
		};
	}
}
