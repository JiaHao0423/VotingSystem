package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class VerifyAuthRequest {

	@Setter
  @NotNull(message = "請選擇社區")
	private Long communityId;

	@Setter
  @NotBlank(message = "請選擇戶別")
	private String unitShortName;

	@NotBlank(message = "請輸入驗證碼")
	private String authCode;

  public void setAuthCode(String authCode) {
		this.authCode = authCode != null ? authCode.trim().replace(" ", "") : null;
	}
}
