package com.ben.com.backend.web.controller;

import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.service.UnitService;
import com.ben.com.backend.web.dto.CreateUnitRequest;
import com.ben.com.backend.web.dto.UnitResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/communities/{communityId}/units")
public class AdminUnitController {

	private final UnitService unitService;

	public AdminUnitController(UnitService unitService) {
		this.unitService = unitService;
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
}
