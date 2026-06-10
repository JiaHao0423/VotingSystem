package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.enums.AdminRole;

public record AdminMeResponse(
		Long id,
		String username,
		String displayName,
		AdminRole role,
		CommunityResponse community
) {

	public static AdminMeResponse from(AdminUser admin) {
		return new AdminMeResponse(
				admin.getId(),
				admin.getUsername(),
				admin.getDisplayName(),
				admin.getRole(),
				admin.getCommunity() != null ? CommunityResponse.from(admin.getCommunity()) : null
		);
	}
}
