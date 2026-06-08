package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.domain.enums.BuildingType;
import java.time.Instant;

public record OwnerResponse(
		Long id,
		Long unitId,
		String unitShortName,
		String fullAddress,
		BuildingType buildingType,
		String name,
		String phone,
		boolean attended,
		Instant createdAt
) {

	public static OwnerResponse from(Owner owner) {
		var unit = owner.getUnit();
		return new OwnerResponse(
				owner.getId(),
				unit.getId(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				owner.getName(),
				owner.getPhone(),
				owner.isAttended(),
				owner.getCreatedAt()
		);
	}
}
