package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public class QrAuthRequest {

	@NotBlank(message = "缺少 QR token")
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token != null ? token.trim() : null;
	}
}
