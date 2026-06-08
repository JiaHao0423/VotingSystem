package com.ben.com.backend.web.dto;

public record AuthCodeRegeneratedResponse(
		Long ownerId,
		String unitShortName,
		String authCode,
		String message
) {
}
