package com.ben.com.backend.service;

import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthCodeService {

	private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int CODE_LENGTH = 6;

	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthCodeService(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public String generatePlainCode() {
		var code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(ALPHANUM.charAt(secureRandom.nextInt(ALPHANUM.length())));
		}
		return code.toString();
	}

	public String hashCode(String plainCode) {
		return passwordEncoder.encode(plainCode);
	}

	public boolean matches(String plainCode, String hash) {
		return passwordEncoder.matches(plainCode, hash);
	}
}
