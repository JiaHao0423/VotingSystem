package com.ben.com.backend.service.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelUnitImportParserTest {

	private final ExcelUnitImportParser parser = new ExcelUnitImportParser();

	@Test
	void parsesCommunityRosterFormatWithoutParsingAddressAsNumber() throws Exception {
		try (var workbook = new XSSFWorkbook(); var out = new java.io.ByteArrayOutputStream()) {
			var sheet = workbook.createSheet("名冊");
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("序號");
			header.createCell(1).setCellValue("戶別");
			header.createCell(2).setCellValue("姓名");
			header.createCell(3).setCellValue("詳細門牌");
			header.createCell(4).setCellValue("區分所有權人比例");

			var row = sheet.createRow(1);
			row.createCell(0).setCellValue(1);
			row.createCell(1).setCellValue("S1");
			row.createCell(2).setCellValue("測試店東");
			row.createCell(3).setCellValue("台中市北屯區旱溪西路二段196號");
			row.createCell(4).setCellValue("94.60 / 13241.21");

			workbook.write(out);
			var rows = parser.parse(new ByteArrayInputStream(out.toByteArray()));

			assertThat(rows).hasSize(1);
			assertThat(rows.getFirst().shortName()).isEqualTo("S1");
			assertThat(rows.getFirst().fullAddress()).contains("196號");
			assertThat(rows.getFirst().ownerName()).isEqualTo("測試店東");
			assertThat(rows.getFirst().area()).isEqualByComparingTo("94.60");
			assertThat(rows.getFirst().ownershipRatio()).isEqualByComparingTo("0.71");
		}
	}
}
