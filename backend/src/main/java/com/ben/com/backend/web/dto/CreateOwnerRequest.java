package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class CreateOwnerRequest {

	private Long unitId;

	private String unitShortName;

	@NotBlank
	private String name;

	@Pattern(regexp = "^$|^09\\d{8}$|^09\\d{2}-\\d{3}-\\d{3}$", message = "手機格式不正確")
	private String phone;

	private String fullAddress;

	@DecimalMin(value = "0", inclusive = false, message = "坪數必須大於 0")
	private BigDecimal area;

	@DecimalMin(value = "0", inclusive = false, message = "區分所有權比例必須大於 0")
	private BigDecimal ownershipRatio;

	public Long getUnitId() {
		return unitId;
	}

	public void setUnitId(Long unitId) {
		this.unitId = unitId;
	}

	public String getUnitShortName() {
		return unitShortName;
	}

	public void setUnitShortName(String unitShortName) {
		this.unitShortName = unitShortName != null ? unitShortName.trim() : null;
	}

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

	public String getFullAddress() {
		return fullAddress;
	}

	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress != null ? fullAddress.trim() : null;
	}

	public BigDecimal getArea() {
		return area;
	}

	public void setArea(BigDecimal area) {
		this.area = area;
	}

	public BigDecimal getOwnershipRatio() {
		return ownershipRatio;
	}

	public void setOwnershipRatio(BigDecimal ownershipRatio) {
		this.ownershipRatio = ownershipRatio;
	}
}
