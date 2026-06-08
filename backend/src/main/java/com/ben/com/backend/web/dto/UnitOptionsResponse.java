package com.ben.com.backend.web.dto;

import java.util.List;

public record UnitOptionsResponse(
		Long communityId,
		String communityName,
		List<BuildingOption> buildings
) {
}
