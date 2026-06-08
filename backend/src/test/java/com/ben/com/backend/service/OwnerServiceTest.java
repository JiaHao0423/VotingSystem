package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.service.CommunityService;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
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
		assertThat(response.qrUrl()).contains("/auth?t=");
		assertThat(ownerRepository.findById(response.owner().id())).isPresent();
	}
}
