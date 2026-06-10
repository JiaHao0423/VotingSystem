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
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.ProposalRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.web.dto.AdminAccountResponse;
import com.ben.com.backend.web.dto.AdminMeResponse;
import com.ben.com.backend.web.dto.CommunityResponse;
import com.ben.com.backend.web.dto.CreateAdminAccountRequest;
import com.ben.com.backend.web.dto.CreateCommunityRequest;
import com.ben.com.backend.web.dto.UpdateAdminAccountRequest;
import com.ben.com.backend.web.dto.UpdateCommunityRequest;
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
	private final UnitRepository unitRepository;
	private final OwnerRepository ownerRepository;
	private final ProposalRepository proposalRepository;
	private final VoteRecordRepository voteRecordRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminAccountService(
			AdminUserRepository adminUserRepository,
			CommunityRepository communityRepository,
			MeetingRepository meetingRepository,
			UnitRepository unitRepository,
			OwnerRepository ownerRepository,
			ProposalRepository proposalRepository,
			VoteRecordRepository voteRecordRepository,
			PasswordEncoder passwordEncoder
	) {
		this.adminUserRepository = adminUserRepository;
		this.communityRepository = communityRepository;
		this.meetingRepository = meetingRepository;
		this.unitRepository = unitRepository;
		this.ownerRepository = ownerRepository;
		this.proposalRepository = proposalRepository;
		this.voteRecordRepository = voteRecordRepository;
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

	public CommunityResponse updateCommunity(Long id, UpdateCommunityRequest request) {
		var community = communityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到社區：" + id));

		var name = request.getName().trim();
		var duplicated = communityRepository.findByName(name)
				.filter(other -> !other.getId().equals(id))
				.isPresent();
		if (duplicated) {
			throw new ConflictException("社區名稱已存在：" + name);
		}

		community.setName(name);
		community.setTotalHouseholds(request.getTotalHouseholds());
		community.setTotalArea(request.getTotalArea());
		community.setAddress(request.getAddress());
		return CommunityResponse.from(community);
	}

	/** 刪除社區及其所有資料（投票紀錄、所有權人、戶別、提案、會議、社區管理帳號） */
	public void deleteCommunity(Long id) {
		if (!communityRepository.existsById(id)) {
			throw new ResourceNotFoundException("找不到社區：" + id);
		}
		voteRecordRepository.deleteByOwner_Unit_Community_Id(id);
		ownerRepository.deleteByUnit_Community_Id(id);
		proposalRepository.deleteByMeeting_Community_Id(id);
		unitRepository.deleteByCommunityId(id);
		meetingRepository.deleteByCommunityId(id);
		adminUserRepository.deleteByCommunity_Id(id);
		communityRepository.deleteById(id);
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

	public AdminAccountResponse updateAccount(Long id, UpdateAdminAccountRequest request) {
		var admin = adminUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到管理員帳號：" + id));

		var username = request.getUsername().trim();
		if (!username.equals(admin.getUsername()) && adminUserRepository.existsByUsername(username)) {
			throw new ConflictException("帳號已存在：" + username);
		}
		admin.setUsername(username);
		admin.setDisplayName(
				request.getDisplayName() != null && !request.getDisplayName().isBlank()
						? request.getDisplayName().trim()
						: null
		);

		if (admin.getRole() == AdminRole.COMMUNITY_ADMIN) {
			if (request.getCommunityId() == null) {
				throw new IllegalArgumentException("社區管理員必須指定社區");
			}
			if (admin.getCommunity() == null
					|| !admin.getCommunity().getId().equals(request.getCommunityId())) {
				var community = communityRepository.findById(request.getCommunityId())
						.orElseThrow(() -> new ResourceNotFoundException("找不到社區：" + request.getCommunityId()));
				admin.setCommunity(community);
			}
		}
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
