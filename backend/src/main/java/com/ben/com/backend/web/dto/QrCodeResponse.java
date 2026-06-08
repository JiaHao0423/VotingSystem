package com.ben.com.backend.web.dto;

public record QrCodeResponse(
		Long ownerId,
		String unitShortName,
		String ownerName,
		String qrToken,
		String qrUrl
) {
}
