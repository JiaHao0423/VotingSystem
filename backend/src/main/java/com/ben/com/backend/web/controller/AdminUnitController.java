package com.ben.com.backend.web.controller;

import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.ImportDuplicatePolicy;
import com.ben.com.backend.service.UnitImportService;
import com.ben.com.backend.service.UnitService;
import com.ben.com.backend.service.excel.ExcelUnitImportTemplateService;
import com.ben.com.backend.web.dto.CreateUnitRequest;
import com.ben.com.backend.web.dto.UnitImportResultResponse;
import com.ben.com.backend.web.dto.UnitResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/communities/{communityId}/units")
public class AdminUnitController {

	private final UnitService unitService;
	private final UnitImportService unitImportService;
	private final ExcelUnitImportTemplateService templateService;

	public AdminUnitController(
			UnitService unitService,
			UnitImportService unitImportService,
			ExcelUnitImportTemplateService templateService
	) {
		this.unitService = unitService;
		this.unitImportService = unitImportService;
		this.templateService = templateService;
	}

	@GetMapping
	public List<UnitResponse> list(
			@PathVariable Long communityId,
			@RequestParam(required = false) BuildingType buildingType,
			@RequestParam(defaultValue = "false") boolean unassignedOnly
	) {
		return unitService.list(communityId, buildingType, unassignedOnly);
	}

	@GetMapping("/{unitId}")
	public UnitResponse get(@PathVariable Long communityId, @PathVariable Long unitId) {
		return unitService.getById(unitId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UnitResponse create(@PathVariable Long communityId, @Valid @RequestBody CreateUnitRequest request) {
		return unitService.create(communityId, request);
	}

	@DeleteMapping("/{unitId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long communityId, @PathVariable Long unitId) {
		unitService.delete(unitId);
	}

	@GetMapping("/import/template")
	public ResponseEntity<byte[]> downloadImportTemplate() throws IOException {
		var bytes = templateService.buildTemplate();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"unit-import-template.xlsx\"")
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(bytes);
	}

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UnitImportResultResponse importUnits(
			@PathVariable Long communityId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "false") boolean dryRun,
			@RequestParam(defaultValue = "true") boolean createOwners,
			@RequestParam(defaultValue = "SKIP") ImportDuplicatePolicy duplicatePolicy
	) throws IOException {
		return unitImportService.importExcel(communityId, file, dryRun, createOwners, duplicatePolicy);
	}
}
