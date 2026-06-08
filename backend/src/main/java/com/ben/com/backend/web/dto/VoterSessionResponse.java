package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.security.VoterPrincipal;
import java.math.BigDecimal;

public record VoterSessionResponse(
		Long ownerId,
		Long unitId,
		Long communityId,
		String unitShortName,
		String fullAddress,
		BuildingType buildingType,
		String name,
		BigDecimal area,
		BigDecimal ownershipRatio,
		boolean attended,
		String message
) {

	public static VoterSessionResponse from(VoterPrincipal principal, String message) {
		return new VoterSessionResponse(
				principal.ownerId(),
				principal.unitId(),
				principal.communityId(),
				principal.unitShortName(),
				principal.fullAddress(),
				principal.buildingType(),
				principal.name(),
				principal.area(),
				principal.ownershipRatio(),
				principal.attended(),
				message
		);
	}
}
