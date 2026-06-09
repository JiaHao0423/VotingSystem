package com.ben.com.backend.service.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

class RosterFileProbeTest {

	@Test
	void parsesActualCommunityRosterWorkbook() throws Exception {
		var parser = new ExcelUnitImportParser();
		try (InputStream in = getClass().getResourceAsStream("/community-roster.xlsx")) {
			assertThat(in).isNotNull();
			var rows = parser.parse(in);
			assertThat(rows).hasSizeGreaterThan(100);
			assertThat(rows.getFirst().shortName()).isEqualTo("S1");
			assertThat(rows.getFirst().ownerName()).isNotBlank();
			assertThat(rows.getFirst().fullAddress()).contains("196");
		}
	}
}
