package com.ben.com.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class AdminUserConfig {

	@Bean
	UserDetailsService adminUserDetailsService(
			PasswordEncoder passwordEncoder,
			@Value("${spring.security.user.name:admin}") String username,
			@Value("${spring.security.user.password:admin}") String password
	) {
		var admin = User.builder()
				.username(username)
				.password(passwordEncoder.encode(password))
				.roles("ADMIN")
				.build();
		return new InMemoryUserDetailsManager(admin);
	}
}
