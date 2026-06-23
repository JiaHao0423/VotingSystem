package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.model.VoteOptionItem;

public record VoteOptionResponse(
		String key,
		String label,
		String description,
		int sortOrder,
		boolean passOption
) {

	public static VoteOptionResponse from(VoteOptionItem item) {
		return new VoteOptionResponse(
				item.key(),
				item.label(),
				item.description(),
				item.sortOrder(),
				item.passOption()
		);
	}
}
