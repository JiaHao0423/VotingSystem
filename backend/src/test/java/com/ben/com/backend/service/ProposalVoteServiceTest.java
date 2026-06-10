package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.ProposalType;
import com.ben.com.backend.domain.enums.VoteChoice;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.security.VoterPrincipal;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.SubmitVoteRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProposalVoteServiceTest {

	@Autowired
	private ProposalService proposalService;

	@Autowired
	private VoteService voteService;

	@Autowired
	private OwnerService ownerService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UnitRepository unitRepository;

	private VoterPrincipal voter;
	private Long proposalId;

	@BeforeEach
	void setUp() {
		var community = communityService.getDefaultCommunity();
		var unit = unitRepository.save(new Unit(
				community,
				"3A1",
				"測試地址",
				BuildingType.A,
				3,
				1,
				null,
				new BigDecimal("150.0"),
				new BigDecimal("0.8")
		));

		var ownerRequest = new CreateOwnerRequest();
		ownerRequest.setUnitId(unit.getId());
		ownerRequest.setName("投票測試");
		ownerRequest.setPhone("0912000111");
		var owner = ownerService.create(community.getId(), ownerRequest);
		voter = new VoterPrincipal(
				owner.owner().id(),
				unit.getId(),
				community.getId(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				owner.owner().name(),
				unit.getArea(),
				unit.getOwnershipRatio(),
				true
		);

		var createRequest = new CreateProposalRequest();
		createRequest.setProposalNumber("第一案");
		createRequest.setTitle("測試提案");
		createRequest.setContent("提案內容");
		createRequest.setType(ProposalType.GENERAL);
		createRequest.setVisible(true);
		proposalId = proposalService.create(community.getId(), createRequest).id();
		proposalService.start(community.getId(), proposalId);
	}

	@Test
	void submitVoteRecordsChoiceAndPreventsDuplicate() {
		var request = new SubmitVoteRequest();
		request.setChoice(VoteChoice.AGREE);

		var result = voteService.submitVote(proposalId, voter, request);

		assertThat(result.totalVotedHouseholds()).isEqualTo(1);
		assertThat(result.options()).anyMatch(option -> option.choice() == VoteChoice.AGREE && option.votes() == 1);
		assertThat(result.passed()).isTrue();

		assertThatThrownBy(() -> voteService.submitVote(proposalId, voter, request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("重複投票");
	}

	@Test
	void cannotVoteWhenProposalNotActive() {
		proposalService.stop(voter.communityId(), proposalId);

		var request = new SubmitVoteRequest();
		request.setChoice(VoteChoice.DISAGREE);

		assertThatThrownBy(() -> voteService.submitVote(proposalId, voter, request))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("不在投票時間");
	}

	@Test
	void voterSeesHasVotedFlagAfterVoting() {
		var request = new SubmitVoteRequest();
		request.setChoice(VoteChoice.ABSTAIN);
		voteService.submitVote(proposalId, voter, request);

		var list = proposalService.listForVoter(voter.communityId(), voter.ownerId());
		assertThat(list).anyMatch(proposal -> proposal.id().equals(proposalId) && proposal.hasVoted());
	}
}
