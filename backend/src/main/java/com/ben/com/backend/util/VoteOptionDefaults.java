package com.ben.com.backend.util;

import com.ben.com.backend.domain.model.VoteOptionItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class VoteOptionDefaults {

	private VoteOptionDefaults() {
	}

	public static List<VoteOptionItem> standard() {
		return List.of(
				new VoteOptionItem("AGREE", "同意", null, 0, true),
				new VoteOptionItem("DISAGREE", "反對", null, 1, false),
				new VoteOptionItem("ABSTAIN", "棄權", null, 2, false)
		);
	}

	public static List<VoteOptionItem> normalize(List<VoteOptionItem> options) {
		if (options == null || options.isEmpty()) {
			return new ArrayList<>(standard());
		}
		var normalized = new ArrayList<VoteOptionItem>();
		var seenKeys = new java.util.HashSet<String>();
		int order = 0;
		for (var option : options.stream().sorted(Comparator.comparingInt(VoteOptionItem::sortOrder)).toList()) {
			if (option.label() == null || option.label().isBlank()) {
				continue;
			}
			var key = normalizeKey(option.key(), option.label(), seenKeys);
			seenKeys.add(key);
			normalized.add(new VoteOptionItem(
					key,
					option.label().trim(),
					option.description() != null ? option.description().trim() : null,
					order++,
					option.passOption()
			));
		}
		if (normalized.isEmpty()) {
			return new ArrayList<>(standard());
		}
		return normalized;
	}

	private static String normalizeKey(String key, String label, Set<String> seenKeys) {
		var candidate = key != null && !key.isBlank()
				? key.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_")
				: "OPT_" + label.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
		if (candidate.isBlank()) {
			candidate = "OPTION";
		}
		var unique = candidate;
		int suffix = 1;
		while (seenKeys.contains(unique)) {
			unique = candidate + "_" + suffix++;
		}
		return unique;
	}

	public static String labelFor(List<VoteOptionItem> options, String choiceKey) {
		return options.stream()
				.filter(option -> option.key().equals(choiceKey))
				.map(VoteOptionItem::label)
				.findFirst()
				.orElse(choiceKey);
	}

	public static Set<String> passKeys(List<VoteOptionItem> options) {
		return options.stream()
				.filter(VoteOptionItem::passOption)
				.map(VoteOptionItem::key)
				.collect(Collectors.toSet());
	}
}
