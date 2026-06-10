package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.enums.AdminRole;
import com.ben.com.backend.domain.enums.MeetingStatus;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ForbiddenException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.exception.UnauthorizedException;
import com.ben.com.backend.repository.AdminUserRepository;
import com.ben.com.backend.repository.CommunityRepository;
import com.ben.com.backend.repository.MeetingRepository;
import com.ben.com.backend.web.dto.AdminAccountResponse;
import com.ben.com.backend.web.dto.AdminMeResponse;
import com.ben.com.backend.web.dto.CommunityResponse;
import com.ben.com.backend.web.dto.CreateAdminAccountRequest;
import com.ben.com.backend.web.dto.CreateCommunityRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminAccountService {

	private final AdminUserRepository adminUserRepository;
	private final CommunityRepository communityRepository;
	private final MeetingRepository meetingRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminAccountService(
			AdminUserRepository adminUserRepository,
			CommunityRepository communityRepository,
			MeetingRepository meetingRepository,
			PasswordEncoder passwordEncoder
	) {
		this.adminUserRepository = adminUserRepository;
		this.communityRepository = communityRepository;
		this.meetingRepository = meetingRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public AdminUser getCurrentAdmin() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new UnauthorizedException("尚未登入管理後台");
		}
		return adminUserRepository.findByUsernameWithCommunity(authentication.getName())
				.orElseThrow(() -> new UnauthorizedException("管理員帳號不存在"));
	}

	@Transactional(readOnly = true)
	public AdminMeResponse me() {
		return AdminMeResponse.from(getCurrentAdmin());
	}

	/** 驗證目前管理員可存取指定社區；超級管理員可存取所有社區 */
	@Transactional(readOnly = true)
	public void assertCommunityAccess(Long communityId) {
		var admin = getCurrentAdmin();
		if (admin.getRole() == AdminRole.SUPER_ADMIN) {
			return;
		}
		var ownCommunity = admin.getCommunity();
		if (ownCommunity == null || !ownCommunity.getId().equals(communityId)) {
			throw new ForbiddenException("您沒有權限管理此社區");
		}
	}

	// ---- 超級管理員操作 ----

	@Transactional(readOnly = true)
	public List<CommunityResponse> listCommunities() {
		return communityRepository.findAll().stream()
				.map(CommunityResponse::from)
				.toList();
	}

	public CommunityResponse createCommunity(CreateCommunityRequest request) {
		var name = request.getName().trim();
		if (communityRepository.findByName(name).isPresent()) {
			throw new ConflictException("社區名稱已存在：" + name);
		}
		var community = communityRepository.save(new Community(
				name,
				request.getTotalHouseholds(),
				request.getTotalArea(),
				request.getAddress()
		));

		var meetingName = request.getMeetingName() != null && !request.getMeetingName().isBlank()
				? request.getMeetingName().trim()
				: name + " 區分所有權人會議";
		var meeting = new Meeting(community, meetingName, LocalDate.now());
		meeting.setStatus(MeetingStatus.DRAFT);
		meetingRepository.save(meeting);

		return CommunityResponse.from(community);
	}

	@Transactional(readOnly = true)
	public List<AdminAccountResponse> listAccounts() {
		return adminUserRepository.findAllWithCommunity().stream()
				.map(AdminAccountResponse::from)
				.toList();
	}

	public AdminAccountResponse createAccount(CreateAdminAccountRequest request) {
		var username = request.getUsername().trim();
		if (adminUserRepository.existsByUsername(username)) {
			throw new ConflictException("帳號已存在：" + username);
		}

		Community community = null;
		if (request.getRole() == AdminRole.COMMUNITY_ADMIN) {
			if (request.getCommunityId() == null) {
				throw new IllegalArgumentException("社區管理員必須指定社區");
			}
			community = communityRepository.findById(request.getCommunityId())
					.orElseThrow(() -> new ResourceNotFoundException("找不到社區：" + request.getCommunityId()));
		}

		var admin = new AdminUser(
				username,
				passwordEncoder.encode(request.getPassword()),
				request.getDisplayName() != null && !request.getDisplayName().isBlank()
						? request.getDisplayName().trim()
						: null,
				request.getRole(),
				community
		);
		adminUserRepository.save(admin);
		return AdminAccountResponse.from(admin);
	}

	public void deleteAccount(Long id) {
		var admin = adminUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到管理員帳號：" + id));
		var current = getCurrentAdmin();
		if (admin.getId().equals(current.getId())) {
			throw new ConflictException("無法刪除自己的帳號");
		}
		if (admin.getRole() == AdminRole.SUPER_ADMIN
				&& adminUserRepository.countByRole(AdminRole.SUPER_ADMIN) <= 1) {
			throw new ConflictException("至少需保留一位超級管理員");
		}
		adminUserRepository.delete(admin);
	}

	public void resetPassword(Long id, String newPassword) {
		var admin = adminUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到管理員帳號：" + id));
		admin.setPasswordHash(passwordEncoder.encode(newPassword));
	}
}
