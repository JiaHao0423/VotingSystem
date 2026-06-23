package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.enums.ProposalType;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.UpdateProposalRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalLifecycleServiceTest {

	@Autowired
	private ProposalService proposalService;

	@Autowired
	private CommunityService communityService;

	@Test
	void adminCanRestartVotingAfterAutomaticEnd() {
		var community = communityService.getDefaultCommunity();
		var createRequest = new CreateProposalRequest();
		createRequest.setProposalNumber("重啟案");
		createRequest.setTitle("測試重啟投票");
		createRequest.setContent("內容");
		createRequest.setType(ProposalType.GENERAL);
		createRequest.setVisible(true);
		createRequest.setEndTime(Instant.now().minus(1, ChronoUnit.MINUTES));
		var proposalId = proposalService.create(community.getId(), createRequest).id();

		proposalService.setVotingActive(community.getId(), proposalId, true);

		var afterStart = proposalService.listForAdmin(community.getId()).stream()
				.filter(proposal -> proposal.id().equals(proposalId))
				.findFirst()
				.orElseThrow();
		assertThat(afterStart.status()).isEqualTo(ProposalStatus.ACTIVE);

		proposalService.setVotingActive(community.getId(), proposalId, false);

		var afterStop = proposalService.getForAdmin(community.getId(), proposalId);
		assertThat(afterStop.status()).isEqualTo(ProposalStatus.ENDED);

		proposalService.setVotingActive(community.getId(), proposalId, true);

		var afterRestart = proposalService.listForAdmin(community.getId()).stream()
				.filter(proposal -> proposal.id().equals(proposalId))
				.findFirst()
				.orElseThrow();
		assertThat(afterRestart.status()).isEqualTo(ProposalStatus.ACTIVE);
	}

	@Test
	void activeProposalEndsAutomaticallyAfterEndTime() {
		var community = communityService.getDefaultCommunity();
		var createRequest = new CreateProposalRequest();
		createRequest.setProposalNumber("自動結束案");
		createRequest.setTitle("測試自動結束");
		createRequest.setContent("內容");
		createRequest.setType(ProposalType.GENERAL);
		createRequest.setVisible(true);
		createRequest.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
		var proposalId = proposalService.create(community.getId(), createRequest).id();

		proposalService.start(community.getId(), proposalId);

		var updateRequest = new UpdateProposalRequest();
		updateRequest.setProposalNumber("自動結束案");
		updateRequest.setTitle("測試自動結束");
		updateRequest.setContent("內容");
		updateRequest.setType(ProposalType.GENERAL);
		updateRequest.setVisible(true);
		updateRequest.setEndTime(Instant.now().minus(1, ChronoUnit.MINUTES));
		proposalService.update(community.getId(), proposalId, updateRequest);

		var listed = proposalService.listForAdmin(community.getId());
		assertThat(listed)
				.filteredOn(proposal -> proposal.id().equals(proposalId))
				.singleElement()
				.extracting("status")
				.isEqualTo(ProposalStatus.ENDED);
	}

	@Test
	void hasVotingEndedWhenEndTimeReached() {
		var proposal = new Proposal();
		proposal.setStatus(ProposalStatus.ACTIVE);
		proposal.setEndTime(Instant.parse("2026-01-01T00:00:00Z"));

		assertThat(ProposalLifecycleService.hasVotingEnded(proposal, Instant.parse("2026-01-01T00:00:01Z")))
				.isTrue();
		assertThat(ProposalLifecycleService.hasVotingEnded(proposal, Instant.parse("2025-12-31T23:59:59Z")))
				.isFalse();
	}

	@Test
	void activeProposalWithoutEndTimeStaysActive() {
		var proposal = new Proposal();
		proposal.setStatus(ProposalStatus.ACTIVE);

		assertThat(ProposalLifecycleService.hasVotingEnded(proposal, Instant.now())).isFalse();
	}
}
