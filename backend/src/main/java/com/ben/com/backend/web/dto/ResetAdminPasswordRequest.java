package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetAdminPasswordRequest {

	@NotBlank(message = "請輸入新密碼")
	@Size(min = 6, max = 100, message = "密碼長度需為 6-100 字元")
	private String password;
}
