package com.ben.com.backend.web.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.enums.AdminRole;
import com.ben.com.backend.repository.AdminUserRepository;
import com.ben.com.backend.repository.CommunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminCommunityAccessTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private CommunityRepository communityRepository;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mockMvc;
	private Long communityAId;
	private Long communityBId;

	@BeforeEach
	void setUp() {
		mockMvc = webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.build();

		var communityA = communityRepository.save(new Community("測試社區A", 10, null, "A地址"));
		var communityB = communityRepository.save(new Community("測試社區B", 20, null, "B地址"));
		communityAId = communityA.getId();
		communityBId = communityB.getId();

		adminUserRepository.save(new AdminUser(
				"admin-a", passwordEncoder.encode("pass-a"), "A社區管理員",
				AdminRole.COMMUNITY_ADMIN, communityA
		));
		adminUserRepository.save(new AdminUser(
				"super", passwordEncoder.encode("super-pass"), "超管",
				AdminRole.SUPER_ADMIN, null
		));
	}

	@Test
	void communityAdminCanAccessOwnCommunity() throws Exception {
		mockMvc.perform(get("/api/admin/communities/{id}/units", communityAId)
						.with(httpBasic("admin-a", "pass-a")))
				.andExpect(status().isOk());
	}

	@Test
	void communityAdminCannotAccessOtherCommunity() throws Exception {
		mockMvc.perform(get("/api/admin/communities/{id}/units", communityBId)
						.with(httpBasic("admin-a", "pass-a")))
				.andExpect(status().isForbidden());
	}

	@Test
	void communityAdminCannotAccessSystemEndpoints() throws Exception {
		mockMvc.perform(get("/api/admin/system/communities")
						.with(httpBasic("admin-a", "pass-a")))
				.andExpect(status().isForbidden());
	}

	@Test
	void superAdminCanAccessAnyCommunityAndSystemEndpoints() throws Exception {
		mockMvc.perform(get("/api/admin/communities/{id}/units", communityAId)
						.with(httpBasic("super", "super-pass")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/communities/{id}/units", communityBId)
						.with(httpBasic("super", "super-pass")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/system/communities")
						.with(httpBasic("super", "super-pass")))
				.andExpect(status().isOk());
	}

	@Test
	void meReturnsRoleAndCommunity() throws Exception {
		mockMvc.perform(get("/api/admin/me")
						.with(httpBasic("admin-a", "pass-a")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin-a"))
				.andExpect(jsonPath("$.role").value("COMMUNITY_ADMIN"))
				.andExpect(jsonPath("$.community.name").value("測試社區A"));

		mockMvc.perform(get("/api/admin/me")
						.with(httpBasic("super", "super-pass")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
				.andExpect(jsonPath("$.community").doesNotExist());
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		mockMvc.perform(get("/api/admin/me")
						.with(httpBasic("admin-a", "wrong")))
				.andExpect(status().isUnauthorized());
	}
}
