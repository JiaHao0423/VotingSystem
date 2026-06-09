package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.BuildingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class UpdateOwnerRequest {

	@NotBlank
	private String name;

	@Pattern(regexp = "^$|^09\\d{8}$|^09\\d{2}-\\d{3}-\\d{3}$", message = "手機格式不正確")
	private String phone;

	private boolean attended;

	@NotBlank
	private String unitShortName;

	@NotBlank
	private String fullAddress;

	@NotNull
	private BuildingType buildingType;

	private Integer floor;

	private Integer unitNo;

	private Integer shopNo;

	@DecimalMin(value = "0", inclusive = false, message = "坪數必須大於 0")
	private BigDecimal area;

	@DecimalMin(value = "0", inclusive = false, message = "區分所有權比例必須大於 0")
	private BigDecimal ownershipRatio;

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

	public boolean isAttended() {
		return attended;
	}

	public void setAttended(boolean attended) {
		this.attended = attended;
	}

	public String getUnitShortName() {
		return unitShortName;
	}

	public void setUnitShortName(String unitShortName) {
		this.unitShortName = unitShortName != null ? unitShortName.trim() : null;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress != null ? fullAddress.trim() : null;
	}

	public BuildingType getBuildingType() {
		return buildingType;
	}

	public void setBuildingType(BuildingType buildingType) {
		this.buildingType = buildingType;
	}

	public Integer getFloor() {
		return floor;
	}

	public void setFloor(Integer floor) {
		this.floor = floor;
	}

	public Integer getUnitNo() {
		return unitNo;
	}

	public void setUnitNo(Integer unitNo) {
		this.unitNo = unitNo;
	}

	public Integer getShopNo() {
		return shopNo;
	}

	public void setShopNo(Integer shopNo) {
		this.shopNo = shopNo;
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
