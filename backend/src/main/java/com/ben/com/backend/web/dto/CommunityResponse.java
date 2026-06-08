package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Community;
import java.math.BigDecimal;
import java.time.Instant;

public record CommunityResponse(
		Long id,
		String name,
		int totalHouseholds,
		BigDecimal totalArea,
		String address,
		Instant createdAt
) {

	public static CommunityResponse from(Community community) {
		return new CommunityResponse(
				community.getId(),
				community.getName(),
				community.getTotalHouseholds(),
				community.getTotalArea(),
				community.getAddress(),
				community.getCreatedAt()
		);
	}
}
