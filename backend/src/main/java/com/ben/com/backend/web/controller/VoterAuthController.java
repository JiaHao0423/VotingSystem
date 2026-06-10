package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.UnitOptionsService;
import com.ben.com.backend.service.VoterAuthService;
import com.ben.com.backend.web.dto.QrAuthRequest;
import com.ben.com.backend.web.dto.QrPreviewResponse;
import com.ben.com.backend.web.dto.UnitOptionsResponse;
import com.ben.com.backend.web.dto.VerifyAuthRequest;
import com.ben.com.backend.web.dto.VoterSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VoterAuthController {

	private final VoterAuthService voterAuthService;
	private final UnitOptionsService unitOptionsService;
	private final SecurityContextRepository securityContextRepository;

	public VoterAuthController(
			VoterAuthService voterAuthService,
			UnitOptionsService unitOptionsService,
			SecurityContextRepository securityContextRepository
	) {
		this.voterAuthService = voterAuthService;
		this.unitOptionsService = unitOptionsService;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/units/options")
	public UnitOptionsResponse unitOptions(@RequestParam Long communityId) {
		return unitOptionsService.getOptions(communityId);
	}

	@PostMapping("/auth/verify")
	public VoterSessionResponse verify(
			@Valid @RequestBody VerifyAuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		var response = voterAuthService.verifyByUnitAndCode(request);
		persistVoterSession(httpRequest, httpResponse);
		return response;
	}

	@GetMapping("/auth/qr/preview")
	public QrPreviewResponse previewQr(@RequestParam String token) {
		return voterAuthService.previewQrToken(token);
	}

	@PostMapping("/auth/qr")
	public VoterSessionResponse verifyQr(
			@Valid @RequestBody QrAuthRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		var response = voterAuthService.verifyByQrToken(request);
		persistVoterSession(httpRequest, httpResponse);
		return response;
	}

	private void persistVoterSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		httpRequest.getSession(true).setAttribute("VOTER_AUTHENTICATED", Boolean.TRUE);
		securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);
	}

	@GetMapping("/auth/me")
	public VoterSessionResponse me() {
		return voterAuthService.currentSession();
	}

	@PostMapping("/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		voterAuthService.logout();
		new SecurityContextLogoutHandler().logout(request, response, null);
	}
}
