package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ThresholdBase;
import com.ben.com.backend.domain.model.VoteOptionItem;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.util.VoteOptionDefaults;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.VoteOptionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;

public final class ProposalResultCalculator {

	private ProposalResultCalculator() {
	}

	public static ProposalResultResponse compute(
			Proposal proposal,
			Community community,
			BigDecimal totalCommunityArea,
			long attendedHouseholds,
			BigDecimal attendedWeight,
			VoteRecordRepository voteRecordRepository
	) {
		return compute(
				proposal,
				community,
				totalCommunityArea,
				attendedHouseholds,
				attendedWeight,
				voteRecordRepository,
				null
		);
	}

	public static ProposalResultResponse compute(
			Proposal proposal,
			Community community,
			BigDecimal totalCommunityArea,
			long attendedHouseholds,
			BigDecimal attendedWeight,
			VoteRecordRepository voteRecordRepository,
			Instant votedAt
	) {
		var voteOptions = VoteOptionDefaults.normalize(proposal.getVoteOptions());
		var aggregates = new HashMap<String, long[]>();
		var weightByKey = new HashMap<String, BigDecimal>();

		for (Object[] row : voteRecordRepository.aggregateByProposalId(proposal.getId())) {
			var key = (String) row[0];
			aggregates.put(key, new long[] {toLong(row[1])});
			weightByKey.put(key, (BigDecimal) row[2]);
		}

		long totalVotes = voteRecordRepository.countByProposalId(proposal.getId());
		BigDecimal totalVotedWeight = voteRecordRepository.sumWeightByProposalId(proposal.getId());

		var options = voteOptions.stream()
				.map(option -> toOptionResult(option, aggregates, weightByKey, totalVotes, totalVotedWeight))
				.toList();

		Set<String> passKeys = VoteOptionDefaults.passKeys(voteOptions);
		long agreeVotes = 0;
		BigDecimal agreeWeight = BigDecimal.ZERO;
		if (!passKeys.isEmpty()) {
			agreeVotes = voteRecordRepository.countPassVotes(proposal.getId(), passKeys);
			agreeWeight = voteRecordRepository.sumPassVoteWeight(proposal.getId(), passKeys);
		}

		BigDecimal communityWeight = resolveCommunityWeight(community, totalCommunityArea);
		int thresholdHouseholds = proposal.getThresholdBase() == ThresholdBase.ATTENDED
				? (int) attendedHouseholds
				: community.getTotalHouseholds();
		BigDecimal thresholdWeight = proposal.getThresholdBase() == ThresholdBase.ATTENDED
				? attendedWeight
				: communityWeight;

		double agreeHouseholdRatio = thresholdHouseholds > 0
				? (double) agreeVotes / thresholdHouseholds
				: 0;
		double agreeWeightRatio = thresholdWeight.compareTo(BigDecimal.ZERO) > 0
				? agreeWeight.divide(thresholdWeight, 6, RoundingMode.HALF_UP).doubleValue()
				: 0;

		double passRatio = (double) proposal.getPassThresholdNumerator() / proposal.getPassThresholdDenominator();
		boolean passed = agreeHouseholdRatio >= passRatio && agreeWeightRatio >= passRatio;

		return new ProposalResultResponse(
				proposal.getId(),
				proposal.getProposalNumber(),
				proposal.getTitle(),
				proposal.getContent(),
				proposal.getType(),
				proposal.getStatus(),
				options,
				totalVotes,
				totalVotedWeight,
				community.getTotalHouseholds(),
				communityWeight,
				thresholdHouseholds,
				thresholdWeight,
				proposal.getPassThresholdNumerator(),
				proposal.getPassThresholdDenominator(),
				proposal.getThresholdBase(),
				agreeHouseholdRatio,
				agreeWeightRatio,
				passed,
				votedAt
		);
	}

	static BigDecimal resolveCommunityWeight(Community community, BigDecimal unitsAreaSum) {
		if (unitsAreaSum.compareTo(BigDecimal.ZERO) > 0) {
			return unitsAreaSum;
		}
		return community.getTotalArea() != null ? community.getTotalArea() : BigDecimal.ZERO;
	}

	private static VoteOptionResult toOptionResult(
			VoteOptionItem option,
			HashMap<String, long[]> aggregates,
			HashMap<String, BigDecimal> weightByKey,
			long totalVotes,
			BigDecimal totalWeight
	) {
		long votes = aggregates.containsKey(option.key()) ? aggregates.get(option.key())[0] : 0;
		BigDecimal weight = weightByKey.getOrDefault(option.key(), BigDecimal.ZERO);
		double voteRatio = totalVotes > 0 ? (double) votes / totalVotes : 0;
		double weightRatio = totalWeight.compareTo(BigDecimal.ZERO) > 0
				? weight.divide(totalWeight, 6, RoundingMode.HALF_UP).doubleValue()
				: 0;

		return new VoteOptionResult(option.key(), option.label(), votes, weight, voteRatio, weightRatio);
	}

	private static long toLong(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(value.toString());
	}
}
