package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.VoteChoice;
import java.time.Instant;

public record VoteRecordResponse(
		Long ownerId,
		String ownerName,
		String unitShortName,
		VoteChoice choice,
		String choiceLabel,
		Instant votedAt
) {
}
