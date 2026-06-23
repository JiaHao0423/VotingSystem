package com.ben.com.backend.domain.model;

public record VoteOptionItem(
		String key,
		String label,
		String description,
		int sortOrder,
		boolean passOption
) {
}
