package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyAuthRequest {

	@NotBlank(message = "請選擇戶別")
	private String unitShortName;

	@NotBlank(message = "請輸入驗證碼")
	private String authCode;

	public String getUnitShortName() {
		return unitShortName;
	}

	public void setUnitShortName(String unitShortName) {
		this.unitShortName = unitShortName;
	}

	public String getAuthCode() {
		return authCode;
	}

	public void setAuthCode(String authCode) {
		this.authCode = authCode != null ? authCode.trim().replace(" ", "") : null;
	}
}
