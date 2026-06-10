package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.AdminAccountService;
import com.ben.com.backend.web.dto.AdminAccountResponse;
import com.ben.com.backend.web.dto.CommunityResponse;
import com.ben.com.backend.web.dto.CreateAdminAccountRequest;
import com.ben.com.backend.web.dto.CreateCommunityRequest;
import com.ben.com.backend.web.dto.ResetAdminPasswordRequest;
import com.ben.com.backend.web.dto.UpdateAdminAccountRequest;
import com.ben.com.backend.web.dto.UpdateCommunityRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 超級管理員專用：社區與管理帳號管理 */
@RestController
@RequestMapping("/api/admin/system")
public class SystemAdminController {

	private final AdminAccountService adminAccountService;

	public SystemAdminController(AdminAccountService adminAccountService) {
		this.adminAccountService = adminAccountService;
	}

	@GetMapping("/communities")
	public List<CommunityResponse> listCommunities() {
		return adminAccountService.listCommunities();
	}

	@PostMapping("/communities")
	@ResponseStatus(HttpStatus.CREATED)
	public CommunityResponse createCommunity(@Valid @RequestBody CreateCommunityRequest request) {
		return adminAccountService.createCommunity(request);
	}

	@PutMapping("/communities/{communityId}")
	public CommunityResponse updateCommunity(
			@PathVariable Long communityId,
			@Valid @RequestBody UpdateCommunityRequest request
	) {
		return adminAccountService.updateCommunity(communityId, request);
	}

	@DeleteMapping("/communities/{communityId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCommunity(@PathVariable Long communityId) {
		adminAccountService.deleteCommunity(communityId);
	}

	@GetMapping("/admins")
	public List<AdminAccountResponse> listAdmins() {
		return adminAccountService.listAccounts();
	}

	@PostMapping("/admins")
	@ResponseStatus(HttpStatus.CREATED)
	public AdminAccountResponse createAdmin(@Valid @RequestBody CreateAdminAccountRequest request) {
		return adminAccountService.createAccount(request);
	}

	@PutMapping("/admins/{adminId}")
	public AdminAccountResponse updateAdmin(
			@PathVariable Long adminId,
			@Valid @RequestBody UpdateAdminAccountRequest request
	) {
		return adminAccountService.updateAccount(adminId, request);
	}

	@DeleteMapping("/admins/{adminId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAdmin(@PathVariable Long adminId) {
		adminAccountService.deleteAccount(adminId);
	}

	@PutMapping("/admins/{adminId}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(
			@PathVariable Long adminId,
			@Valid @RequestBody ResetAdminPasswordRequest request
	) {
		adminAccountService.resetPassword(adminId, request.getPassword());
	}
}
