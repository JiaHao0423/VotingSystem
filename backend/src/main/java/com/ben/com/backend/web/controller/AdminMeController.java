package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.AdminAccountService;
import com.ben.com.backend.web.dto.AdminMeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminMeController {

	private final AdminAccountService adminAccountService;

	public AdminMeController(AdminAccountService adminAccountService) {
		this.adminAccountService = adminAccountService;
	}

	@GetMapping("/me")
	public AdminMeResponse me() {
		return adminAccountService.me();
	}
}
