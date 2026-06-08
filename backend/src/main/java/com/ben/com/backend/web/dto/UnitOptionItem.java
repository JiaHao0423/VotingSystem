package com.ben.com.backend.web.dto;

public record UnitOptionItem(
		Long id,
		String shortName,
		Integer unitNo,
		boolean hasOwner
) {
}
