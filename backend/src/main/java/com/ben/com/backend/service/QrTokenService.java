package com.ben.com.backend.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class QrTokenService {

	private static final int TOKEN_BYTES = 32;
	private final SecureRandom secureRandom = new SecureRandom();

	public String generateToken() {
		var bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String buildAuthUrl(String baseUrl, String token) {
		var normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return normalized + "/vote?t=" + token;
	}
}
