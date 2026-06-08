package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import java.math.BigDecimal;
import java.time.Instant;

public record UnitResponse(
		Long id,
		Long communityId,
		String shortName,
		String fullAddress,
		BuildingType buildingType,
		Integer floor,
		Integer unitNo,
		Integer shopNo,
		BigDecimal area,
		BigDecimal ownershipRatio,
		boolean hasOwner,
		Instant createdAt
) {

	public static UnitResponse from(Unit unit, boolean hasOwner) {
		return new UnitResponse(
				unit.getId(),
				unit.getCommunity().getId(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				unit.getFloor(),
				unit.getUnitNo(),
				unit.getShopNo(),
				unit.getArea(),
				unit.getOwnershipRatio(),
				hasOwner,
				unit.getCreatedAt()
		);
	}
}
