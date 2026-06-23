package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.ProposalType;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.security.VoterPrincipal;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.SubmitVoteRequest;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.UpdateOwnerRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnerServiceTest {

	@Autowired
	private OwnerService ownerService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UnitRepository unitRepository;

	@Autowired
	private OwnerRepository ownerRepository;

	@Autowired
	private VoteRecordRepository voteRecordRepository;

	@Autowired
	private ProposalService proposalService;

	@Autowired
	private VoteService voteService;

	@Test
	void createOwnerGeneratesAuthCodeAndQrToken() {
		var community = communityService.getDefaultCommunity();
		var unit = unitRepository.save(new Unit(
				community,
				"TEST1",
				"測試地址",
				BuildingType.SHOP,
				null,
				null,
				99,
				null,
				null
		));

		var request = new CreateOwnerRequest();
		request.setUnitId(unit.getId());
		request.setName("測試住戶");
		request.setPhone("0912345678");

		var response = ownerService.create(community.getId(), request);

		assertThat(response.authCode()).hasSize(6);
		assertThat(response.qrToken()).isNotBlank();
		assertThat(response.qrUrl()).contains("/vote?t=");
		assertThat(ownerRepository.findById(response.owner().id())).isPresent();
	}

	@Test
	void createOwnerCreatesUnitWhenMissing() {
		var community = communityService.getDefaultCommunity();

		var request = new CreateOwnerRequest();
		request.setUnitShortName("12B9");
		request.setName("王大明");
		request.setPhone("0909090909");
		request.setFullAddress("測試門牌 12F-9");
		request.setArea(new BigDecimal("28.50"));
		request.setOwnershipRatio(new BigDecimal("0.0150"));

		var response = ownerService.create(community.getId(), request);

		assertThat(response.owner().unitShortName()).isEqualTo("12B9");
		assertThat(response.owner().name()).isEqualTo("王大明");
		assertThat(response.owner().fullAddress()).isEqualTo("測試門牌 12F-9");
		assertThat(unitRepository.existsByCommunityIdAndShortName(community.getId(), "12B9")).isTrue();
	}

	@Test
	void updateOwnerUpdatesOwnerAndUnitFields() {
		var community = communityService.getDefaultCommunity();
		var unit = unitRepository.save(new Unit(
				community,
				"TEST2",
				"舊地址",
				BuildingType.A,
				4,
				7,
				null,
				new BigDecimal("25.50"),
				new BigDecimal("0.0123")
		));

		var createRequest = new CreateOwnerRequest();
		createRequest.setUnitId(unit.getId());
		createRequest.setName("王小明");
		createRequest.setPhone("0911111111");
		var created = ownerService.create(community.getId(), createRequest);

		var updateRequest = new UpdateOwnerRequest();
		updateRequest.setName("王大明");
		updateRequest.setPhone("0922222222");
		updateRequest.setAttended(true);
		updateRequest.setUnitShortName("4A7");
		updateRequest.setFullAddress("新地址 4F-7");
		updateRequest.setBuildingType(BuildingType.A);
		updateRequest.setFloor(4);
		updateRequest.setUnitNo(7);
		updateRequest.setArea(new BigDecimal("26.00"));
		updateRequest.setOwnershipRatio(new BigDecimal("0.0130"));

		var updated = ownerService.update(community.getId(), created.owner().id(), updateRequest);

		assertThat(updated.name()).isEqualTo("王大明");
		assertThat(updated.phone()).isEqualTo("0922222222");
		assertThat(updated.attended()).isTrue();
		assertThat(updated.unitShortName()).isEqualTo("4A7");
		assertThat(updated.fullAddress()).isEqualTo("新地址 4F-7");
		assertThat(updated.area()).isEqualByComparingTo("26.00");
		assertThat(updated.ownershipRatio()).isEqualByComparingTo("0.0130");
	}

	@Test
	void deleteManyRemovesSelectedOwners() {
		var community = communityService.getDefaultCommunity();
		var unit1 = unitRepository.save(new Unit(
				community,
				"DEL1",
				"刪除測試1",
				BuildingType.SHOP,
				null,
				null,
				91,
				null,
				null
		));
		var unit2 = unitRepository.save(new Unit(
				community,
				"DEL2",
				"刪除測試2",
				BuildingType.SHOP,
				null,
				null,
				92,
				null,
				null
		));

		var owner1 = ownerService.create(community.getId(), createOwnerRequest(unit1.getId(), "甲"));
		var owner2 = ownerService.create(community.getId(), createOwnerRequest(unit2.getId(), "乙"));

		var deleted = ownerService.deleteMany(
				community.getId(),
				java.util.List.of(owner1.owner().id(), owner2.owner().id())
		);

		assertThat(deleted).isEqualTo(2);
		assertThat(ownerRepository.findById(owner1.owner().id())).isEmpty();
		assertThat(ownerRepository.findById(owner2.owner().id())).isEmpty();
		assertThat(unitRepository.findById(unit1.getId())).isPresent();
		assertThat(unitRepository.findById(unit2.getId())).isPresent();
	}

	@Test
	void deleteRemovesOwnerEvenWhenVoteRecordsExist() {
		var community = communityService.getDefaultCommunity();
		var unit = unitRepository.save(new Unit(
				community,
				"VOTE1",
				"投票刪除測試",
				BuildingType.A,
				1,
				1,
				null,
				new BigDecimal("20.00"),
				new BigDecimal("0.01")
		));
		var created = ownerService.create(community.getId(), createOwnerRequest(unit.getId(), "有投票者"));

		var proposalRequest = new CreateProposalRequest();
		proposalRequest.setProposalNumber("刪除測試");
		proposalRequest.setTitle("刪除測試議案");
		proposalRequest.setContent("測試內容");
		proposalRequest.setType(ProposalType.GENERAL);
		proposalRequest.setVisible(true);
		var proposal = proposalService.create(community.getId(), proposalRequest);
		proposalService.start(community.getId(), proposal.id());

		var voter = new VoterPrincipal(
				created.owner().id(),
				unit.getId(),
				community.getId(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				created.owner().name(),
				unit.getArea(),
				unit.getOwnershipRatio(),
				true
		);
		var voteRequest = new SubmitVoteRequest();
		voteRequest.setChoiceKey("AGREE");
		voteService.submitVote(proposal.id(), voter, voteRequest);

		assertThat(voteRecordRepository.existsByProposalIdAndOwnerId(proposal.id(), created.owner().id())).isTrue();

		ownerService.delete(community.getId(), created.owner().id());

		assertThat(ownerRepository.findById(created.owner().id())).isEmpty();
		assertThat(voteRecordRepository.existsByProposalIdAndOwnerId(proposal.id(), created.owner().id())).isFalse();
	}

	private CreateOwnerRequest createOwnerRequest(Long unitId, String name) {
		var request = new CreateOwnerRequest();
		request.setUnitId(unitId);
		request.setName(name);
		return request;
	}
}
