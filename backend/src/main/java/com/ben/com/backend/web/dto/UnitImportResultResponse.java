package com.ben.com.backend.web.dto;

import java.util.List;

public record UnitImportResultResponse(
		int totalRows,
		int createdUnits,
		int updatedUnits,
		int skippedUnits,
		int createdOwners,
		int errorCount,
		boolean dryRun,
		List<UnitImportRowResult> rows
) {
}
