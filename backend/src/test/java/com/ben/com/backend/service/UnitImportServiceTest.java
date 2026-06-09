package com.ben.com.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.enums.ImportDuplicatePolicy;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.service.excel.ExcelUnitImportTemplateService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnitImportServiceTest {

	@Autowired
	private UnitImportService unitImportService;

	@Autowired
	private CommunityService communityService;

	@Autowired
	private UnitRepository unitRepository;

	@Autowired
	private ExcelUnitImportTemplateService templateService;

	@Test
	void dryRunImportResidentialUnit() throws Exception {
		var community = communityService.getDefaultCommunity();
		var template = templateService.buildTemplate();
		var file = new MockMultipartFile(
				"file",
				"units.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new ByteArrayInputStream(template)
		);

		var before = unitRepository.findByCommunityIdOrderByBuildingTypeAscFloorAscUnitNoAscShopNoAsc(community.getId()).size();
		var result = unitImportService.importExcel(community.getId(), file, true, false, ImportDuplicatePolicy.SKIP);

		assertThat(result.totalRows()).isGreaterThan(0);
		assertThat(result.createdUnits()).isGreaterThan(0);
		assertThat(result.dryRun()).isTrue();
		assertThat(unitRepository.findByCommunityIdOrderByBuildingTypeAscFloorAscUnitNoAscShopNoAsc(community.getId())).hasSize(before);
	}

	@Test
	void importCommunityRosterFormatCreatesShopAndResidentialUnits() throws Exception {
		var community = communityService.getDefaultCommunity();
		var bytes = buildCommunityRosterWorkbook();
		var file = new MockMultipartFile(
				"file",
				"roster.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new ByteArrayInputStream(bytes)
		);

		var result = unitImportService.importExcel(community.getId(), file, false, true, ImportDuplicatePolicy.SKIP);

		assertThat(result.createdUnits()).isEqualTo(2);
		assertThat(result.createdOwners()).isEqualTo(2);
		assertThat(unitRepository.existsByCommunityIdAndShortName(community.getId(), "店99")).isTrue();
		assertThat(unitRepository.existsByCommunityIdAndShortName(community.getId(), "2A1")).isTrue();
	}

	@Test
	void importCreatesNewResidentialUnit() throws Exception {
		var community = communityService.getDefaultCommunity();
		var bytes = buildSingleRowWorkbook("12B9", "台中市東區測試地址12樓-9", "B", "12", "9", "", "20.5", "0.75");
		var file = new MockMultipartFile(
				"file",
				"units.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				new ByteArrayInputStream(bytes)
		);

		var result = unitImportService.importExcel(community.getId(), file, false, false, ImportDuplicatePolicy.SKIP);

		assertThat(result.createdUnits()).isEqualTo(1);
		assertThat(unitRepository.existsByCommunityIdAndShortName(community.getId(), "12B9")).isTrue();
	}

	private byte[] buildCommunityRosterWorkbook() throws Exception {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var sheet = workbook.createSheet("名冊");
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("序號");
			header.createCell(1).setCellValue("戶別");
			header.createCell(2).setCellValue("姓名");
			header.createCell(3).setCellValue("詳細門牌");
			header.createCell(4).setCellValue("區分所有權人比例");

			var shop = sheet.createRow(1);
			shop.createCell(0).setCellValue(1);
			shop.createCell(1).setCellValue("S99");
			shop.createCell(2).setCellValue("測試店東");
			shop.createCell(3).setCellValue("台中市北屯區測試路196號");
			shop.createCell(4).setCellValue("94.60 / 13241.21");

			var residential = sheet.createRow(2);
			residential.createCell(0).setCellValue(2);
			residential.createCell(1).setCellValue("2A1");
			residential.createCell(2).setCellValue("測試住戶");
			residential.createCell(3).setCellValue("台中市北屯區測試路190巷8號2F-1");
			residential.createCell(4).setCellValue("84.00 / 13241.21");

			workbook.write(out);
			return out.toByteArray();
		}
	}

	private byte[] buildSingleRowWorkbook(
			String shortName,
			String address,
			String building,
			String floor,
			String unitNo,
			String shopNo,
			String area,
			String ratio
	) throws Exception {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var sheet = workbook.createSheet("import");
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("戶別簡稱");
			header.createCell(1).setCellValue("完整門牌");
			header.createCell(2).setCellValue("棟別");
			header.createCell(3).setCellValue("樓層");
			header.createCell(4).setCellValue("戶號");
			header.createCell(5).setCellValue("店面序號");
			header.createCell(6).setCellValue("坪數");
			header.createCell(7).setCellValue("區分所有權比例(%)");
			var row = sheet.createRow(1);
			row.createCell(0).setCellValue(shortName);
			row.createCell(1).setCellValue(address);
			row.createCell(2).setCellValue(building);
			row.createCell(3).setCellValue(floor);
			row.createCell(4).setCellValue(unitNo);
			row.createCell(5).setCellValue(shopNo);
			row.createCell(6).setCellValue(area);
			row.createCell(7).setCellValue(ratio);
			workbook.write(out);
			return out.toByteArray();
		}
	}
}
