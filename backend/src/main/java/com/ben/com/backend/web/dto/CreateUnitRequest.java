package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateUnitRequest {

	@NotBlank
	private String shortName;

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

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName != null ? shortName.trim() : null;
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
