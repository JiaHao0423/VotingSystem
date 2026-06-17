package com.ben.com.backend.config;

import com.ben.com.backend.service.ProposalLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProposalStatusScheduler {

	private final ProposalLifecycleService proposalLifecycleService;

	public ProposalStatusScheduler(ProposalLifecycleService proposalLifecycleService) {
		this.proposalLifecycleService = proposalLifecycleService;
	}

	@Scheduled(fixedRate = 60_000)
	public void endExpiredProposals() {
		proposalLifecycleService.syncAllExpiredProposals();
	}
}
