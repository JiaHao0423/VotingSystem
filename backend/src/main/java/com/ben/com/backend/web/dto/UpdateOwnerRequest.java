package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateOwnerRequest {

	@NotBlank
	private String name;

	@Pattern(regexp = "^$|^09\\d{8}$|^09\\d{2}-\\d{3}-\\d{3}$", message = "手機格式不正確")
	private String phone;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name != null ? name.trim() : null;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone != null ? phone.trim() : null;
	}
}
