package com.ben.com.backend.service.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelUnitImportTemplateService {

	private static final String[] HEADERS = {
			"戶別簡稱",
			"完整門牌",
			"棟別",
			"樓層",
			"戶號",
			"店面序號",
			"坪數",
			"區分所有權比例(%)",
			"所有權人姓名",
			"手機"
	};

	private static final String[][] EXAMPLES = {
			{"4A7", "台中市東區旱溪西路二段190巷8號4樓-7", "A", "4", "7", "", "25.50", "0.82", "王大明", "0912345678"},
			{"5B2", "台中市東區旱溪西路二段190巷10號5樓-2", "B", "5", "2", "", "28.10", "0.91", "", ""},
			{"店1", "台中市東區旱溪西路二段196號", "SHOP", "", "", "1", "30.00", "1.20", "陳店東", ""},
	};

	public byte[] buildTemplate() throws IOException {
		try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
			var sheet = workbook.createSheet("戶別匯入");
			var headerStyle = workbook.createCellStyle();
			var font = workbook.createFont();
			font.setBold(true);
			headerStyle.setFont(font);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			var headerRow = sheet.createRow(0);
			for (int i = 0; i < HEADERS.length; i++) {
				var cell = headerRow.createCell(i);
				cell.setCellValue(HEADERS[i]);
				cell.setCellStyle(headerStyle);
				sheet.setColumnWidth(i, 18 * 256);
			}

			for (int r = 0; r < EXAMPLES.length; r++) {
				var row = sheet.createRow(r + 1);
				for (int c = 0; c < EXAMPLES[r].length; c++) {
					row.createCell(c).setCellValue(EXAMPLES[r][c]);
				}
			}

			workbook.write(out);
			return out.toByteArray();
		}
	}
}
