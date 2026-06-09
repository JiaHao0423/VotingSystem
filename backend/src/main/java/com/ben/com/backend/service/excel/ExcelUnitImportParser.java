package com.ben.com.backend.service.excel;

import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.util.OwnershipRatioParser;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcelUnitImportParser {

	private static final DataFormatter FORMATTER = new DataFormatter();

	public record ImportRow(
			int rowNumber,
			String shortName,
			String fullAddress,
			String buildingTypeRaw,
			Integer floor,
			Integer unitNo,
			Integer shopNo,
			BigDecimal area,
			BigDecimal ownershipRatio,
			String ownerName,
			String ownerPhone
	) {
	}

	private enum ImportFormat {
		LEGACY_TEMPLATE,
		COMMUNITY_ROSTER
	}

	private record ColumnMap(
			int unitCol,
			int nameCol,
			int addressCol,
			int ratioCol,
			int buildingCol,
			int floorCol,
			int unitNoCol,
			int shopCol,
			int areaCol
	) {
	}

	public List<ImportRow> parse(InputStream inputStream) throws IOException {
		try (var workbook = WorkbookFactory.create(inputStream)) {
			var sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
			if (sheet == null) {
				throw new IllegalArgumentException("Excel 檔案沒有工作表");
			}

			var headerRowIndex = sheet.getFirstRowNum();
			var format = detectFormat(sheet.getRow(headerRowIndex));
			var columns = format == ImportFormat.COMMUNITY_ROSTER
					? mapCommunityRosterColumns(sheet.getRow(headerRowIndex))
					: mapLegacyColumns(sheet.getRow(headerRowIndex));

			var rows = new ArrayList<ImportRow>();
			for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
				var row = sheet.getRow(i);
				if (row == null || isBlankDataRow(row, format, columns)) {
					continue;
				}
				rows.add(format == ImportFormat.COMMUNITY_ROSTER
						? toCommunityRosterRow(row, columns)
						: toLegacyRow(row, columns));
			}
			return rows;
		}
	}

	private ImportFormat detectFormat(Row headerRow) {
		if (headerRow == null) {
			return ImportFormat.LEGACY_TEMPLATE;
		}
		var hasUnit = false;
		var hasPersonName = false;
		var hasSerial = false;
		var hasDetailAddress = false;
		var hasRatio = false;
		var hasBuilding = false;
		var hasArea = false;
		for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
			var text = cell(headerRow, c).toLowerCase();
			if (text.contains("序號")) {
				hasSerial = true;
			}
			if (text.contains("戶別")) {
				hasUnit = true;
			}
			if (text.contains("姓名")) {
				hasPersonName = true;
			}
			if (text.contains("詳細門牌") || text.contains("詳細地址") || text.contains("門牌號碼")
					|| (text.contains("門牌") && !text.contains("區分"))) {
				hasDetailAddress = true;
			}
			if (text.contains("區分所有權") || text.contains("區權")) {
				hasRatio = true;
			}
			if (text.contains("棟別")) {
				hasBuilding = true;
			}
			if (text.contains("坪數")) {
				hasArea = true;
			}
		}
		if (hasUnit && hasPersonName && hasRatio && !hasBuilding && !hasArea
				&& (hasDetailAddress || hasSerial)) {
			return ImportFormat.COMMUNITY_ROSTER;
		}
		return ImportFormat.LEGACY_TEMPLATE;
	}

	private ColumnMap mapCommunityRosterColumns(Row headerRow) {
		var columns = new ColumnMap(
				findColumn(headerRow, "戶別"),
				findColumn(headerRow, "姓名", "所有權人"),
				findColumn(headerRow, "詳細門牌", "門牌號碼", "完整門牌", "詳細地址", "門牌"),
				findColumn(headerRow, "區分所有權人比例", "區分所有權", "區權比"),
				-1,
				-1,
				-1,
				-1,
				-1
		);
		validateCommunityRosterColumns(columns);
		return columns;
	}

	private void validateCommunityRosterColumns(ColumnMap columns) {
		if (columns.unitCol() < 0 || columns.nameCol() < 0 || columns.addressCol() < 0 || columns.ratioCol() < 0) {
			throw new IllegalArgumentException("無法辨識名冊欄位，請確認包含：戶別、姓名、門牌（或詳細門牌）、區分所有權人比例");
		}
	}

	private ColumnMap mapLegacyColumns(Row headerRow) {
		if (headerRow == null || !isLegacyHeaderRow(headerRow)) {
			return new ColumnMap(0, 8, 1, 7, 2, 3, 4, 5, 6);
		}
		return new ColumnMap(
				findColumn(headerRow, "戶別簡稱", "戶別", "簡稱"),
				findColumn(headerRow, "所有權人姓名", "姓名", "所有權人"),
				findColumn(headerRow, "完整門牌", "門牌", "地址"),
				findColumn(headerRow, "區分所有權比例", "區分所有權", "區權"),
				findColumn(headerRow, "棟別"),
				findColumn(headerRow, "樓層"),
				findColumn(headerRow, "戶號"),
				findColumn(headerRow, "店面序號"),
				findColumn(headerRow, "坪數")
		);
	}

	private int findColumn(Row headerRow, String... keywords) {
		if (headerRow == null) {
			return -1;
		}
		for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
			var text = cell(headerRow, c).toLowerCase();
			for (var keyword : keywords) {
				if (!keyword.isBlank() && text.contains(keyword.toLowerCase())) {
					return c;
				}
			}
		}
		return -1;
	}

	private boolean isLegacyHeaderRow(Row row) {
		var first = cell(row, 0).toLowerCase();
		return first.contains("戶別") || first.contains("簡稱") || first.equals("shortname");
	}

	private boolean isBlankDataRow(Row row, ImportFormat format, ColumnMap columns) {
		if (format == ImportFormat.COMMUNITY_ROSTER) {
			return cell(row, columns.unitCol()).isBlank() && cell(row, columns.nameCol()).isBlank();
		}
		for (int c = 0; c <= 9; c++) {
			if (!cell(row, c).isBlank()) {
				return false;
			}
		}
		return true;
	}

	private ImportRow toCommunityRosterRow(Row row, ColumnMap columns) {
		var ratioRaw = cell(row, columns.ratioCol());
		var parsedRatio = OwnershipRatioParser.parse(ratioRaw);
		return new ImportRow(
				row.getRowNum() + 1,
				cell(row, columns.unitCol()),
				cell(row, columns.addressCol()),
				"",
				null,
				null,
				null,
				parsedRatio.area(),
				parsedRatio.ownershipPercent(),
				cell(row, columns.nameCol()),
				""
		);
	}

	private ImportRow toLegacyRow(Row row, ColumnMap columns) {
		var useFixedLayout = columns.unitCol() == 0 && columns.addressCol() == 1 && columns.ratioCol() == 7;
		return new ImportRow(
				row.getRowNum() + 1,
				cell(row, columns.unitCol()),
				cell(row, columns.addressCol()),
				columnCell(row, columns.buildingCol(), useFixedLayout ? 2 : -1),
				parseInteger(columnCell(row, columns.floorCol(), useFixedLayout ? 3 : -1)),
				parseInteger(columnCell(row, columns.unitNoCol(), useFixedLayout ? 4 : -1)),
				parseInteger(columnCell(row, columns.shopCol(), useFixedLayout ? 5 : -1)),
				parseDecimal(columnCell(row, columns.areaCol(), useFixedLayout ? 6 : -1)),
				parseRatioCell(columnCell(row, columns.ratioCol(), useFixedLayout ? 7 : -1)),
				cell(row, columns.nameCol() >= 0 ? columns.nameCol() : 8),
				cell(row, 9)
		);
	}

	private String columnCell(Row row, int mappedCol, int fixedCol) {
		var col = mappedCol >= 0 ? mappedCol : fixedCol;
		return col >= 0 ? cell(row, col) : "";
	}

	private BigDecimal parseRatioCell(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		if (value.contains("/")) {
			return OwnershipRatioParser.parse(value).ownershipPercent();
		}
		return parseDecimal(value.replace("%", ""));
	}

	private String cell(Row row, int index) {
		if (index < 0) {
			return "";
		}
		var cell = row.getCell(index);
		if (cell == null) {
			return "";
		}
		return FORMATTER.formatCellValue(cell).trim();
	}

	private Integer parseInteger(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(value.replace(",", "")).intValue();
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("無法解析整數欄位：" + value);
		}
	}

	private BigDecimal parseDecimal(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return new BigDecimal(value.replace(",", "").replace("%", "").trim());
	}

	public static BuildingType parseBuildingType(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		var normalized = raw.trim().toUpperCase();
		return switch (normalized) {
			case "A", "A棟", "A栋" -> BuildingType.A;
			case "B", "B棟", "B栋" -> BuildingType.B;
			case "SHOP", "店", "店面", "S" -> BuildingType.SHOP;
			default -> throw new IllegalArgumentException("無法辨識棟別：" + raw);
		};
	}
}
