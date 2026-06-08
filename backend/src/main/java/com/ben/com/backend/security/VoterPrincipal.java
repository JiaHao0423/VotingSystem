package com.ben.com.backend.security;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.domain.enums.BuildingType;
import java.io.Serializable;
import java.math.BigDecimal;

public record VoterPrincipal(
		Long ownerId,
		Long unitId,
		Long communityId,
		String unitShortName,
		String fullAddress,
		BuildingType buildingType,
		String name,
		BigDecimal area,
		BigDecimal ownershipRatio,
		boolean attended
) implements Serializable {

	public static VoterPrincipal from(Owner owner) {
		var unit = owner.getUnit();
		return new VoterPrincipal(
				owner.getId(),
				unit.getId(),
				unit.getCommunity().getId(),
				unit.getShortName(),
				unit.getFullAddress(),
				unit.getBuildingType(),
				owner.getName(),
				unit.getArea(),
				unit.getOwnershipRatio(),
				owner.isAttended()
		);
	}
}
