package com.ben.com.backend.service;

import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.ImportDuplicatePolicy;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.service.excel.ExcelUnitImportParser;
import com.ben.com.backend.util.ShortNameNormalizer;
import com.ben.com.backend.util.UnitShortNameParser;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.CreateUnitRequest;
import com.ben.com.backend.web.dto.UnitImportResultResponse;
import com.ben.com.backend.web.dto.UnitImportRowResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UnitImportService {

	private final ExcelUnitImportParser parser;
	private final UnitService unitService;
	private final OwnerService ownerService;
	private final CommunityService communityService;
	private final UnitRepository unitRepository;

	public UnitImportService(
			ExcelUnitImportParser parser,
			UnitService unitService,
			OwnerService ownerService,
			CommunityService communityService,
			UnitRepository unitRepository
	) {
		this.parser = parser;
		this.unitService = unitService;
		this.ownerService = ownerService;
		this.communityService = communityService;
		this.unitRepository = unitRepository;
	}

	public UnitImportResultResponse importExcel(
			Long communityId,
			MultipartFile file,
			boolean dryRun,
			boolean createOwners,
			ImportDuplicatePolicy duplicatePolicy
	) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("請上傳 Excel 檔案");
		}

		communityService.getById(communityId);
		var parsedRows = parser.parse(file.getInputStream());
		var policy = duplicatePolicy != null ? duplicatePolicy : ImportDuplicatePolicy.SKIP;

		var results = new ArrayList<UnitImportRowResult>();
		int createdUnits = 0;
		int updatedUnits = 0;
		int skippedUnits = 0;
		int createdOwners = 0;
		int errorCount = 0;

		for (var row : parsedRows) {
			try {
				var resolved = resolveRow(row);
				var shortName = resolved.shortName();
				var existingUnit = unitRepository.findByCommunityIdAndShortName(communityId, shortName);

				if (existingUnit.isPresent()) {
					if (policy == ImportDuplicatePolicy.SKIP) {
						skippedUnits++;
						results.add(new UnitImportRowResult(row.rowNumber(), shortName, "SKIPPED", "戶別已存在，保留原資料"));
						continue;
					}

					if (!dryRun) {
						unitService.applyImportUpdate(existingUnit.get(), toCreateUnitRequest(resolved));
						if (createOwners && hasOwnerName(row.ownerName())) {
							ownerService.upsertOwnerForImport(
									communityId,
									shortName,
									row.ownerName(),
									row.ownerPhone()
							);
							createdOwners++;
						}
					} else if (createOwners && hasOwnerName(row.ownerName())) {
						createdOwners++;
					}

					updatedUnits++;
					var message = dryRun ? "預覽：將以 Excel 取代既有戶別" : "已以 Excel 取代既有戶別";
					if (createOwners && hasOwnerName(row.ownerName())) {
						message += dryRun ? "與所有權人" : "及所有權人";
					}
					results.add(new UnitImportRowResult(row.rowNumber(), shortName, "UPDATED", message));
					continue;
				}

				if (!dryRun) {
					var request = toCreateUnitRequest(resolved);
					var created = unitService.create(communityId, request);
					createdUnits++;

					if (createOwners && hasOwnerName(row.ownerName())) {
						createOwnerForUnit(communityId, created.id(), row.ownerName(), row.ownerPhone());
						createdOwners++;
					}
				} else {
					createdUnits++;
					if (createOwners && hasOwnerName(row.ownerName())) {
						createdOwners++;
					}
				}

				var message = dryRun ? "預覽：將新增戶別" : "已新增戶別";
				if (createOwners && hasOwnerName(row.ownerName())) {
					message += dryRun ? "與所有權人" : "及所有權人";
				}
				results.add(new UnitImportRowResult(row.rowNumber(), shortName, "CREATED", message));
			} catch (Exception ex) {
				errorCount++;
				results.add(new UnitImportRowResult(
						row.rowNumber(),
						ShortNameNormalizer.normalize(row.shortName()),
						"ERROR",
						ex.getMessage()
				));
			}
		}

		return new UnitImportResultResponse(
				parsedRows.size(),
				createdUnits,
				updatedUnits,
				skippedUnits,
				createdOwners,
				errorCount,
				dryRun,
				results
		);
	}

	private void createOwnerForUnit(Long communityId, Long unitId, String ownerName, String ownerPhone) {
		var request = new CreateOwnerRequest();
		request.setUnitId(unitId);
		request.setName(ownerName.trim());
		request.setPhone(ownerPhone);
		ownerService.create(communityId, request);
	}

	private boolean hasOwnerName(String ownerName) {
		return ownerName != null && !ownerName.isBlank();
	}

	private CreateUnitRequest toCreateUnitRequest(ResolvedRow resolved) {
		var request = new CreateUnitRequest();
		request.setShortName(resolved.shortName());
		request.setFullAddress(resolved.fullAddress());
		request.setBuildingType(resolved.buildingType());
		request.setFloor(resolved.floor());
		request.setUnitNo(resolved.unitNo());
		request.setShopNo(resolved.shopNo());
		request.setArea(resolved.area());
		request.setOwnershipRatio(resolved.ownershipRatio());
		return request;
	}

	private ResolvedRow resolveRow(ExcelUnitImportParser.ImportRow row) {
		if (row.shortName() == null || row.shortName().isBlank()) {
			throw new IllegalArgumentException("戶別簡稱不可空白");
		}
		if (row.fullAddress() == null || row.fullAddress().isBlank()) {
			throw new IllegalArgumentException("完整門牌不可空白");
		}
		if (row.area() == null || row.area().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("坪數必須大於 0");
		}
		if (row.ownershipRatio() == null || row.ownershipRatio().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("區分所有權比例必須大於 0");
		}

		var shortName = ShortNameNormalizer.normalize(row.shortName());
		var parsed = UnitShortNameParser.parse(shortName)
				.orElseThrow(() -> new IllegalArgumentException("無法解析戶別簡稱：" + row.shortName()));

		var buildingType = row.buildingTypeRaw() != null && !row.buildingTypeRaw().isBlank()
				? ExcelUnitImportParser.parseBuildingType(row.buildingTypeRaw())
				: parsed.buildingType();

		if (buildingType != parsed.buildingType()) {
			throw new IllegalArgumentException("棟別與戶別簡稱不一致");
		}

		Integer floor = coalesce(row.floor(), parsed.floor());
		Integer unitNo = coalesce(row.unitNo(), parsed.unitNo());
		Integer shopNo = coalesce(row.shopNo(), parsed.shopNo());

		if (buildingType == BuildingType.SHOP) {
			if (shopNo == null) {
				throw new IllegalArgumentException("店面戶別必須提供店面序號");
			}
		} else if (floor == null || unitNo == null) {
			throw new IllegalArgumentException("住宅戶別必須提供樓層與戶號");
		}

		return new ResolvedRow(
				shortName,
				row.fullAddress().trim(),
				buildingType,
				floor,
				unitNo,
				shopNo,
				row.area(),
				row.ownershipRatio()
		);
	}

	private Integer coalesce(Integer preferred, Integer fallback) {
		return preferred != null ? preferred : fallback;
	}

	private record ResolvedRow(
			String shortName,
			String fullAddress,
			BuildingType buildingType,
			Integer floor,
			Integer unitNo,
			Integer shopNo,
			BigDecimal area,
			BigDecimal ownershipRatio
	) {
	}
}
