package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Owner;
import java.math.BigDecimal;

public record OwnerQrPrintItemResponse(
		Long ownerId,
		String ownerName,
		String unitShortName,
		BigDecimal ownershipRatio,
		String qrUrl
) {

	public static OwnerQrPrintItemResponse from(Owner owner, String qrUrl) {
		var unit = owner.getUnit();
		return new OwnerQrPrintItemResponse(
				owner.getId(),
				owner.getName(),
				unit.getShortName(),
				unit.getOwnershipRatio(),
				qrUrl
		);
	}
}
