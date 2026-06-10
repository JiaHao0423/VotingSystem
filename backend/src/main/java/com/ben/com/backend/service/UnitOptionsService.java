package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.web.dto.BuildingOption;
import com.ben.com.backend.web.dto.FloorOption;
import com.ben.com.backend.web.dto.UnitOptionItem;
import com.ben.com.backend.web.dto.UnitOptionsResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnitOptionsService {

	private final UnitRepository unitRepository;
	private final OwnerRepository ownerRepository;
	private final CommunityService communityService;

	public UnitOptionsService(
			UnitRepository unitRepository,
			OwnerRepository ownerRepository,
			CommunityService communityService
	) {
		this.unitRepository = unitRepository;
		this.ownerRepository = ownerRepository;
		this.communityService = communityService;
	}

	public UnitOptionsResponse getOptions(Long communityId) {
		var community = communityService.getById(communityId);
		var units = unitRepository.findByCommunityIdOrderByBuildingTypeAscFloorAscUnitNoAscShopNoAsc(community.getId());
		var assignedUnitIds = new HashSet<>(
				ownerRepository.findByCommunityIdWithUnit(community.getId()).stream()
						.map(owner -> owner.getUnit().getId())
						.toList()
		);

		var buildings = new ArrayList<BuildingOption>();
		buildings.add(buildResidentialOptions(BuildingType.A, "A 棟", units, assignedUnitIds));
		buildings.add(buildResidentialOptions(BuildingType.B, "B 棟", units, assignedUnitIds));
		buildings.add(buildShopOptions(units, assignedUnitIds));

		return new UnitOptionsResponse(community.getId(), community.getName(), buildings);
	}

	private BuildingOption buildResidentialOptions(
			BuildingType buildingType,
			String label,
			List<Unit> units,
			HashSet<Long> assignedUnitIds
	) {
		var floorMap = new TreeMap<Integer, List<UnitOptionItem>>();
		units.stream()
				.filter(unit -> unit.getBuildingType() == buildingType)
				.forEach(unit -> floorMap
						.computeIfAbsent(unit.getFloor(), ignored -> new ArrayList<>())
						.add(toOptionItem(unit, assignedUnitIds)));

		var floors = floorMap.entrySet().stream()
				.map(entry -> new FloorOption(entry.getKey(), List.copyOf(entry.getValue())))
				.toList();

		return new BuildingOption(buildingType, label, floors, List.of());
	}

	private BuildingOption buildShopOptions(List<Unit> units, HashSet<Long> assignedUnitIds) {
		var shopUnits = units.stream()
				.filter(unit -> unit.getBuildingType() == BuildingType.SHOP)
				.sorted(Comparator.comparing(Unit::getShopNo, Comparator.nullsLast(Integer::compareTo)))
				.map(unit -> toOptionItem(unit, assignedUnitIds))
				.toList();

		return new BuildingOption(BuildingType.SHOP, "店面", List.of(), shopUnits);
	}

	private UnitOptionItem toOptionItem(Unit unit, HashSet<Long> assignedUnitIds) {
		return new UnitOptionItem(
				unit.getId(),
				unit.getShortName(),
				unit.getBuildingType() == BuildingType.SHOP ? unit.getShopNo() : unit.getUnitNo(),
				assignedUnitIds.contains(unit.getId())
		);
	}
}
