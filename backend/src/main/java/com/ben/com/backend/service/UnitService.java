package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.util.ShortNameNormalizer;
import com.ben.com.backend.web.dto.CreateUnitRequest;
import com.ben.com.backend.web.dto.UnitResponse;
import com.ben.com.backend.web.dto.UpdateOwnerRequest;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UnitService {

	private final UnitRepository unitRepository;
	private final OwnerRepository ownerRepository;
	private final CommunityService communityService;

	public UnitService(
			UnitRepository unitRepository,
			OwnerRepository ownerRepository,
			CommunityService communityService
	) {
		this.unitRepository = unitRepository;
		this.ownerRepository = ownerRepository;
		this.communityService = communityService;
	}

	@Transactional(readOnly = true)
	public List<UnitResponse> list(Long communityId, BuildingType buildingType, boolean unassignedOnly) {
		communityService.getById(communityId);
		List<Unit> units;
		if (unassignedOnly) {
			units = unitRepository.findUnassignedByCommunityId(communityId);
		} else if (buildingType != null) {
			units = unitRepository.findByCommunityIdAndBuildingTypeOrderByFloorAscUnitNoAsc(communityId, buildingType);
		} else {
			units = unitRepository.findByCommunityIdOrderByBuildingTypeAscFloorAscUnitNoAscShopNoAsc(communityId);
		}

		var assignedUnitIds = new HashSet<>(
				ownerRepository.findByCommunityIdWithUnit(communityId).stream()
						.map(owner -> owner.getUnit().getId())
						.toList()
		);

		return units.stream()
				.map(unit -> UnitResponse.from(unit, assignedUnitIds.contains(unit.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public UnitResponse getById(Long id) {
		var unit = findUnit(id);
		var hasOwner = ownerRepository.existsByUnitId(unit.getId());
		return UnitResponse.from(unit, hasOwner);
	}

	public UnitResponse create(Long communityId, CreateUnitRequest request) {
		var community = communityService.getById(communityId);
		validateUnitFields(request);

		var shortName = ShortNameNormalizer.normalize(request.getShortName());
		if (unitRepository.existsByCommunityIdAndShortName(communityId, shortName)) {
			throw new ConflictException("簡稱已存在：" + shortName);
		}

		var unit = new Unit(
				community,
				shortName,
				request.getFullAddress(),
				request.getBuildingType(),
				request.getFloor(),
				request.getUnitNo(),
				request.getShopNo(),
				request.getArea(),
				request.getOwnershipRatio()
		);
		unitRepository.save(unit);
		return UnitResponse.from(unit, false);
	}

	public void delete(Long id) {
		var unit = findUnit(id);
		if (ownerRepository.existsByUnitId(unit.getId())) {
			throw new ConflictException("此戶別已有所有權人，無法刪除");
		}
		unitRepository.delete(unit);
	}

	Unit findUnit(Long id) {
		return unitRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到戶別：" + id));
	}

	public Unit findByShortName(Long communityId, String shortName) {
		return findByShortNameOptional(communityId, shortName)
				.orElseThrow(() -> new ResourceNotFoundException("找不到戶別：" + ShortNameNormalizer.normalize(shortName)));
	}

	public java.util.Optional<Unit> findByShortNameOptional(Long communityId, String shortName) {
		var normalized = ShortNameNormalizer.normalize(shortName);
		return unitRepository.findByCommunityIdAndShortName(communityId, normalized);
	}

	public void applyImportUpdate(Unit unit, CreateUnitRequest request) {
		validateUnitFields(request);
		unit.setFullAddress(request.getFullAddress());
		unit.setBuildingType(request.getBuildingType());
		if (request.getBuildingType() == BuildingType.SHOP) {
			unit.setFloor(null);
			unit.setUnitNo(null);
			unit.setShopNo(request.getShopNo());
		} else {
			unit.setFloor(request.getFloor());
			unit.setUnitNo(request.getUnitNo());
			unit.setShopNo(null);
		}
		unit.setArea(request.getArea());
		unit.setOwnershipRatio(request.getOwnershipRatio());
	}

	public void updateFromOwnerRequest(Long communityId, Unit unit, UpdateOwnerRequest request) {
		validateUnitFields(request.getBuildingType(), request.getFloor(), request.getUnitNo(), request.getShopNo());

		var shortName = ShortNameNormalizer.normalize(request.getUnitShortName());
		if (!shortName.equals(unit.getShortName())
				&& unitRepository.existsByCommunityIdAndShortName(communityId, shortName)) {
			throw new ConflictException("簡稱已存在：" + shortName);
		}

		unit.setShortName(shortName);
		unit.setFullAddress(request.getFullAddress());
		unit.setBuildingType(request.getBuildingType());
		if (request.getBuildingType() == BuildingType.SHOP) {
			unit.setFloor(null);
			unit.setUnitNo(null);
			unit.setShopNo(request.getShopNo());
		} else {
			unit.setFloor(request.getFloor());
			unit.setUnitNo(request.getUnitNo());
			unit.setShopNo(null);
		}
		unit.setArea(request.getArea());
		unit.setOwnershipRatio(request.getOwnershipRatio());
	}

	private void validateUnitFields(CreateUnitRequest request) {
		validateUnitFields(request.getBuildingType(), request.getFloor(), request.getUnitNo(), request.getShopNo());
	}

	private void validateUnitFields(BuildingType buildingType, Integer floor, Integer unitNo, Integer shopNo) {
		if (buildingType == BuildingType.SHOP) {
			if (shopNo == null) {
				throw new IllegalArgumentException("店面戶別必須提供 shopNo");
			}
			return;
		}
		if (floor == null || unitNo == null) {
			throw new IllegalArgumentException("住宅戶別必須提供 floor 與 unitNo");
		}
	}
}
