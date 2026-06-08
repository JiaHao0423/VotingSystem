package com.ben.com.backend.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.service.CommunityService;
import com.ben.com.backend.service.OwnerService;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoterAuthControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private OwnerService ownerService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UnitRepository unitRepository;

	private MockMvc mockMvc;
	private String plainAuthCode;
	private String unitShortName;

	@BeforeEach
	void setUp() {
		mockMvc = webAppContextSetup(webApplicationContext).build();

		var community = communityService.getDefaultCommunity();
		var unit = unitRepository.save(new Unit(
				community,
				"3A1",
				"控制器測試門牌",
				BuildingType.A,
				3,
				1,
				null,
				null,
				null
		));
		unitShortName = unit.getShortName();

		var request = new CreateOwnerRequest();
		request.setUnitId(unit.getId());
		request.setName("控制器測試");
		request.setPhone("0911222333");

		plainAuthCode = ownerService.create(community.getId(), request).authCode();
	}

	@Test
	void verifyCreatesSessionAndMeReturnsProfile() throws Exception {
		var session = new MockHttpSession();

		mockMvc.perform(post("/api/auth/verify")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "unitShortName": "%s",
								  "authCode": "%s"
								}
								""".formatted(unitShortName, plainAuthCode)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unitShortName").value(unitShortName))
				.andExpect(jsonPath("$.attended").value(true));

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("控制器測試"));
	}

	@Test
	void meWithoutSessionReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unitOptionsIncludesSeededShops() throws Exception {
		mockMvc.perform(get("/api/units/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.communityName").value("鴻邑晴川硯"))
				.andExpect(jsonPath("$.buildings[2].label").value("店面"))
				.andExpect(jsonPath("$.buildings[2].units[0].shortName").value("店1"));
	}
}
