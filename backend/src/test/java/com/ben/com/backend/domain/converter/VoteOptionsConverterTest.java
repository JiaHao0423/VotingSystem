package com.ben.com.backend.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ben.com.backend.domain.model.VoteOptionItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoteOptionsConverterTest {

	private final VoteOptionsConverter converter = new VoteOptionsConverter();

	@Test
	void convertsToValidJsonArray() {
		var json = converter.convertToDatabaseColumn(List.of(
				new VoteOptionItem("AGREE", "同意", null, 0, true),
				new VoteOptionItem("DISAGREE", "反對", null, 1, false)
		));

		assertThat(json).startsWith("[");
		assertThat(json).endsWith("]");
		assertThat(json).contains("\"key\":\"AGREE\"");
		assertThat(json).contains("\"label\":\"同意\"");
		assertThat(json).contains("\"passOption\":true");
	}

	@Test
	void roundTripsJsonArray() {
		var original = List.of(
				new VoteOptionItem("OPT_A", "同意方案A", "方案A內容", 0, true),
				new VoteOptionItem("ABSTAIN", "棄權", null, 1, false)
		);
		var json = converter.convertToDatabaseColumn(original);
		var restored = converter.convertToEntityAttribute(json);

		assertThat(restored).hasSize(2);
		assertThat(restored.getFirst().key()).isEqualTo("OPT_A");
		assertThat(restored.getFirst().label()).isEqualTo("同意方案A");
		assertThat(restored.getFirst().description()).isEqualTo("方案A內容");
		assertThat(restored.getFirst().passOption()).isTrue();
	}

	@Test
	void parsesMysqlReorderedJsonKeys() {
		var mysqlJson =
				"[{\"description\":null,\"key\":\"AGREE\",\"label\":\"同意\",\"passOption\":true,\"sortOrder\":0}]";
		var restored = converter.convertToEntityAttribute(mysqlJson);

		assertThat(restored).hasSize(1);
		assertThat(restored.getFirst().key()).isEqualTo("AGREE");
		assertThat(restored.getFirst().label()).isEqualTo("同意");
	}

	@Test
	void parsesLegacyDelimitedFormat() {
		var restored = converter.convertToEntityAttribute("AGREE|同意||1;;DISAGREE|反對||0");

		assertThat(restored).hasSizeGreaterThanOrEqualTo(2);
		assertThat(restored.stream().anyMatch(o -> "同意".equals(o.label()))).isTrue();
	}
}
