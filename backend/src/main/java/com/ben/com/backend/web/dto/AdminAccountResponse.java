package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.enums.AdminRole;
import java.time.Instant;

public record AdminAccountResponse(
		Long id,
		String username,
		String displayName,
		AdminRole role,
		Long communityId,
		String communityName,
		Instant createdAt
) {

	public static AdminAccountResponse from(AdminUser admin) {
		return new AdminAccountResponse(
				admin.getId(),
				admin.getUsername(),
				admin.getDisplayName(),
				admin.getRole(),
				admin.getCommunity() != null ? admin.getCommunity().getId() : null,
				admin.getCommunity() != null ? admin.getCommunity().getName() : null,
				admin.getCreatedAt()
		);
	}
}
