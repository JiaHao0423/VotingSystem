package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.repository.ProposalRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalLifecycleService {

	private final ProposalRepository proposalRepository;

	public ProposalLifecycleService(ProposalRepository proposalRepository) {
		this.proposalRepository = proposalRepository;
	}

	@Transactional
	public void syncExpiredProposals(Long communityId) {
		proposalRepository.endActiveProposalsPastEndTime(communityId, Instant.now());
	}

	@Transactional
	public void syncAllExpiredProposals() {
		proposalRepository.endAllActiveProposalsPastEndTime(Instant.now());
	}

	static boolean hasVotingEnded(Proposal proposal, Instant now) {
		return proposal.getStatus() == ProposalStatus.ACTIVE
				&& proposal.getEndTime() != null
				&& !now.isBefore(proposal.getEndTime());
	}
}
