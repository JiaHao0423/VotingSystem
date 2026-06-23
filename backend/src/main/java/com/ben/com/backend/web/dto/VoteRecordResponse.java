package com.ben.com.backend.web.dto;

import java.time.Instant;

public record VoteRecordResponse(
		Long ownerId,
		String ownerName,
		String unitShortName,
		String choiceKey,
		String choiceLabel,
		Instant votedAt
) {
}
