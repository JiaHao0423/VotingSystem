package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.domain.enums.BuildingType;
import java.math.BigDecimal;

public record QrPreviewResponse(
		String ownerName,
		String unitShortName,
		String fullAddress,
		BuildingType buildingType,
		BigDecimal area,
		BigDecimal ownershipRatio
) {

	public static QrPreviewResponse from(Owner owner) {
		var unit = owner.getUnit();
		return new QrPreviewResponse(
				owner.getName(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				unit.getArea(),
				unit.getOwnershipRatio()
		);
	}
}
