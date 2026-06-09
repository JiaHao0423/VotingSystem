package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.domain.enums.BuildingType;

public record QrCodeResponse(
		Long ownerId,
		String unitShortName,
		String ownerName,
		String fullAddress,
		BuildingType buildingType,
		String qrToken,
		String qrUrl
) {

	public static QrCodeResponse from(Owner owner, String qrUrl) {
		var unit = owner.getUnit();
		return new QrCodeResponse(
				owner.getId(),
				unit.getShortName(),
				owner.getName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				owner.getQrToken(),
				qrUrl
		);
	}
}
