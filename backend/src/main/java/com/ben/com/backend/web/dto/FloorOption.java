package com.ben.com.backend.web.dto;

import java.util.List;

public record FloorOption(
		int floor,
		List<UnitOptionItem> units
) {
}
