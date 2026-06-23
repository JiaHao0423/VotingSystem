package com.ben.com.backend.domain.converter;

import com.ben.com.backend.domain.model.VoteOptionItem;
import com.ben.com.backend.util.VoteOptionDefaults;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter
public class VoteOptionsConverter implements AttributeConverter<List<VoteOptionItem>, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final TypeReference<List<VoteOptionItem>> TYPE = new TypeReference<>() {};
	private static final String RECORD_SEP = ";;";

	@Override
	public String convertToDatabaseColumn(List<VoteOptionItem> attribute) {
		var options = attribute == null || attribute.isEmpty()
				? VoteOptionDefaults.standard()
				: VoteOptionDefaults.normalize(attribute);
		try {
			return MAPPER.writeValueAsString(options);
		} catch (Exception ex) {
			throw new IllegalStateException("無法序列化投票選項", ex);
		}
	}

	@Override
	public List<VoteOptionItem> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return new ArrayList<>(VoteOptionDefaults.standard());
		}
		var trimmed = dbData.trim();
		if (trimmed.startsWith("[")) {
			return parseJsonArray(trimmed);
		}
		return parseLegacyDelimited(trimmed);
	}

	private static List<VoteOptionItem> parseJsonArray(String json) {
		try {
			var parsed = MAPPER.readValue(json, TYPE);
			if (parsed == null || parsed.isEmpty()) {
				return new ArrayList<>(VoteOptionDefaults.standard());
			}
			return new ArrayList<>(VoteOptionDefaults.normalize(parsed));
		} catch (Exception ex) {
			return new ArrayList<>(VoteOptionDefaults.standard());
		}
	}

	private static List<VoteOptionItem> parseLegacyDelimited(String dbData) {
		var items = new ArrayList<VoteOptionItem>();
		var records = dbData.split(RECORD_SEP, -1);
		for (int i = 0; i < records.length; i++) {
			var fields = records[i].split("\\|", -1);
			if (fields.length < 2) {
				continue;
			}
			var key = unescapeLegacy(fields[0]);
			var label = unescapeLegacy(fields[1]);
			var description = fields.length > 2 ? unescapeLegacy(fields[2]) : null;
			var passOption = fields.length > 3 && "1".equals(fields[3]);
			items.add(new VoteOptionItem(key, label, description, i, passOption));
		}
		if (items.isEmpty()) {
			return new ArrayList<>(VoteOptionDefaults.standard());
		}
		return new ArrayList<>(VoteOptionDefaults.normalize(items));
	}

	private static String unescapeLegacy(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}
		var builder = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\\' && i + 1 < value.length()) {
				builder.append(value.charAt(++i));
			} else {
				builder.append(c);
			}
		}
		var result = builder.toString();
		return result.isEmpty() ? null : result;
	}
}
