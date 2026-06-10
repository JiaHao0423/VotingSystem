package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAdminAccountRequest {

	@NotBlank(message = "請輸入帳號")
	@Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$", message = "帳號格式：3-50 字元，限英數字與 _ . -")
	private String username;

	private String displayName;

	/** 社區管理員必填；超級管理員忽略此欄位 */
	private Long communityId;
}
