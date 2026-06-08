package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.UnitOptionsService;
import com.ben.com.backend.service.VoterAuthService;
import com.ben.com.backend.web.dto.QrAuthRequest;
import com.ben.com.backend.web.dto.UnitOptionsResponse;
import com.ben.com.backend.web.dto.VerifyAuthRequest;
import com.ben.com.backend.web.dto.VoterSessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VoterAuthController {

	private final VoterAuthService voterAuthService;
	private final UnitOptionsService unitOptionsService;

	public VoterAuthController(VoterAuthService voterAuthService, UnitOptionsService unitOptionsService) {
		this.voterAuthService = voterAuthService;
		this.unitOptionsService = unitOptionsService;
	}

	@GetMapping("/units/options")
	public UnitOptionsResponse unitOptions() {
		return unitOptionsService.getOptions();
	}

	@PostMapping("/auth/verify")
	public VoterSessionResponse verify(@Valid @RequestBody VerifyAuthRequest request) {
		return voterAuthService.verifyByUnitAndCode(request);
	}

	@PostMapping("/auth/qr")
	public VoterSessionResponse verifyQr(@Valid @RequestBody QrAuthRequest request) {
		return voterAuthService.verifyByQrToken(request);
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
