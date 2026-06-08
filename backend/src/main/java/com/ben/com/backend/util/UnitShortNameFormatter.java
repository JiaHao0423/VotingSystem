package com.ben.com.backend.util;

import com.ben.com.backend.domain.enums.BuildingType;

public final class UnitShortNameFormatter {

	private UnitShortNameFormatter() {
	}

	public static String formatResidential(int floor, BuildingType buildingType, int unitNo) {
		if (buildingType != BuildingType.A && buildingType != BuildingType.B) {
			throw new IllegalArgumentException("住宅戶別棟別必須為 A 或 B");
		}
		return floor + buildingType.name() + unitNo;
	}

	public static String formatShop(int shopNo) {
		return "店" + shopNo;
	}

	public static String formatFullAddress(BuildingType buildingType, int floor, int unitNo) {
		var laneNo = buildingType == BuildingType.A ? "8" : "10";
		return "台中市東區旱溪西路二段190巷" + laneNo + "號" + floor + "樓-" + unitNo;
	}
}
