package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminAccountRequest {

	@NotBlank(message = "請輸入帳號")
	@Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$", message = "帳號格式：3-50 字元，限英數字與 _ . -")
	private String username;

	@NotBlank(message = "請輸入密碼")
	@Size(min = 6, max = 100, message = "密碼長度需為 6-100 字元")
	private String password;

	private String displayName;

	@NotNull(message = "請選擇角色")
	private AdminRole role;

	/** COMMUNITY_ADMIN 必填 */
	private Long communityId;
}
