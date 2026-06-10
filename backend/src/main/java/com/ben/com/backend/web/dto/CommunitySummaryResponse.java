package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Community;

public record CommunitySummaryResponse(
		Long id,
		String name,
		String address
) {

	public static CommunitySummaryResponse from(Community community) {
		return new CommunitySummaryResponse(
				community.getId(),
				community.getName(),
				community.getAddress()
		);
	}
}
