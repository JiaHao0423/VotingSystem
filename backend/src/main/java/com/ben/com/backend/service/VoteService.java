package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.VoteRecord;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.security.VoterPrincipal;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.SubmitVoteRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoteService {

	private final VoteRecordRepository voteRecordRepository;
	private final OwnerRepository ownerRepository;
	private final ProposalService proposalService;

	public VoteService(
			VoteRecordRepository voteRecordRepository,
			OwnerRepository ownerRepository,
			ProposalService proposalService
	) {
		this.voteRecordRepository = voteRecordRepository;
		this.ownerRepository = ownerRepository;
		this.proposalService = proposalService;
	}

	public ProposalResultResponse submitVote(Long proposalId, VoterPrincipal voter, SubmitVoteRequest request) {
		var proposal = proposalService.findProposalInCommunity(voter.communityId(), proposalId);
		if (proposal.getStatus() != ProposalStatus.ACTIVE) {
			throw new ConflictException("此提案目前不在投票時間內");
		}
		if (!proposal.isVisible()) {
			throw new ResourceNotFoundException("找不到提案：" + proposalId);
		}
		if (voteRecordRepository.existsByProposalIdAndOwnerId(proposalId, voter.ownerId())) {
			throw new ConflictException("您已對此提案投票，無法重複投票");
		}

		var owner = ownerRepository.findByIdWithUnit(voter.ownerId())
				.orElseThrow(() -> new ResourceNotFoundException("找不到所有權人：" + voter.ownerId()));
		var voteWeight = owner.getUnit().getArea() != null ? owner.getUnit().getArea() : BigDecimal.ZERO;

		var record = new VoteRecord(proposal, owner, request.getChoice(), voteWeight);
		voteRecordRepository.save(record);

		var community = proposal.getMeeting().getCommunity();
		return ProposalResultCalculator.compute(proposal, community, voteRecordRepository, record.getVotedAt());
	}

	@Transactional(readOnly = true)
	public ProposalResultResponse getResultForVoter(Long proposalId, VoterPrincipal voter) {
		var proposal = proposalService.findProposalInCommunity(voter.communityId(), proposalId);
		if (!proposal.isVisible() || proposal.getStatus() == ProposalStatus.DRAFT) {
			throw new ResourceNotFoundException("找不到提案：" + proposalId);
		}
		var community = proposal.getMeeting().getCommunity();
		var votedAt = voteRecordRepository.findByProposalIdAndOwnerId(proposalId, voter.ownerId())
				.map(VoteRecord::getVotedAt)
				.orElse(null);
		return ProposalResultCalculator.compute(proposal, community, voteRecordRepository, votedAt);
	}
}
