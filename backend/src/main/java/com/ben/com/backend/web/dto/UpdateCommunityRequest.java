package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCommunityRequest {

	@NotBlank(message = "請輸入社區名稱")
	private String name;

	@Min(value = 1, message = "總戶數必須大於 0")
	private int totalHouseholds;

	private BigDecimal totalArea;

	private String address;
}
