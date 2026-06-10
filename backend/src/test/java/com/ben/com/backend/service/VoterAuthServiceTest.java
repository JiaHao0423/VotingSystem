package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.exception.UnauthorizedException;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.web.dto.QrAuthRequest;
import com.ben.com.backend.web.dto.VerifyAuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoterAuthServiceTest {

	@Autowired
	private VoterAuthService voterAuthService;

	@Autowired
	private OwnerService ownerService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UnitRepository unitRepository;

	private Long communityId;
	private String plainAuthCode;
	private String qrToken;
	private String unitShortName;

	@BeforeEach
	void setUp() {
		var community = communityService.getDefaultCommunity();
		communityId = community.getId();
		var unit = unitRepository.save(new Unit(
				community,
				"VTEST",
				"測試門牌",
				BuildingType.SHOP,
				null,
				null,
				88,
				null,
				null
		));
		unitShortName = unit.getShortName();

		var request = new com.ben.com.backend.web.dto.CreateOwnerRequest();
		request.setUnitId(unit.getId());
		request.setName("測試住戶");
		request.setPhone("0912345678");

		var created = ownerService.create(community.getId(), request);
		plainAuthCode = created.authCode();
		qrToken = created.qrToken();
	}

	@Test
	void verifyByUnitAndCodeEstablishesSessionAndMarksAttended() {
		var request = new VerifyAuthRequest();
		request.setCommunityId(communityId);
		request.setUnitShortName(unitShortName);
		request.setAuthCode(plainAuthCode);

		var response = voterAuthService.verifyByUnitAndCode(request);

		assertThat(response.unitShortName()).isEqualTo(unitShortName);
		assertThat(response.attended()).isTrue();
		assertThat(voterAuthService.currentSession().ownerId()).isEqualTo(response.ownerId());
	}

	@Test
	void verifyByQrTokenEstablishesSession() {
		var request = new QrAuthRequest();
		request.setToken(qrToken);

		var response = voterAuthService.verifyByQrToken(request);

		assertThat(response.unitShortName()).isEqualTo(unitShortName);
		assertThat(voterAuthService.currentSession().name()).isEqualTo("測試住戶");
	}

	@Test
	void verifyWithWrongCodeFails() {
		var request = new VerifyAuthRequest();
		request.setCommunityId(communityId);
		request.setUnitShortName(unitShortName);
		request.setAuthCode("WRONG1");

		assertThatThrownBy(() -> voterAuthService.verifyByUnitAndCode(request))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessageContaining("戶別或驗證碼錯誤");
	}

	@Test
	void verifyWithoutOwnerFails() {
		var community = communityService.getDefaultCommunity();
		var emptyUnit = unitRepository.save(new Unit(
				community,
				"EMPTY1",
				"無人戶別",
				BuildingType.SHOP,
				null,
				null,
				77,
				null,
				null
		));

		var request = new VerifyAuthRequest();
		request.setCommunityId(communityId);
		request.setUnitShortName(emptyUnit.getShortName());
		request.setAuthCode("ABC123");

		assertThatThrownBy(() -> voterAuthService.verifyByUnitAndCode(request))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessageContaining("尚未登記所有權人");
	}
}
