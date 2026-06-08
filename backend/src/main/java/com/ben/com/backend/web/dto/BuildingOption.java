package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.BuildingType;
import java.util.List;

public record BuildingOption(
		BuildingType buildingType,
		String label,
		List<FloorOption> floors,
		List<UnitOptionItem> units
) {
}
