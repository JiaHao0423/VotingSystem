package com.ben.com.backend.web.dto;

public record UnitImportRowResult(
		int rowNumber,
		String shortName,
		String status,
		String message
) {
}
