package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommunityRequest {

	@NotBlank(message = "請輸入社區名稱")
	private String name;

	@Min(value = 1, message = "總戶數必須大於 0")
	private int totalHouseholds;

	private BigDecimal totalArea;

	private String address;

	/** 預設區權會場次名稱（選填，未填則自動產生） */
	private String meetingName;
}
