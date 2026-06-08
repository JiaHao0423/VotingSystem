package com.ben.com.backend.web.dto;

public record OwnerCreatedResponse(
		OwnerResponse owner,
		String authCode,
		String qrToken,
		String qrUrl,
		String message
) {
}
