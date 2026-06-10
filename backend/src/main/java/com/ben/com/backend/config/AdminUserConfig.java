package com.ben.com.backend.config;

import com.ben.com.backend.domain.enums.AdminRole;
import com.ben.com.backend.repository.AdminUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class AdminUserConfig {

	@Bean
	UserDetailsService adminUserDetailsService(AdminUserRepository adminUserRepository) {
		return username -> adminUserRepository.findByUsername(username)
				.map(admin -> User.builder()
						.username(admin.getUsername())
						.password(admin.getPasswordHash())
						.roles(admin.getRole() == AdminRole.SUPER_ADMIN
								? new String[]{"ADMIN", "SUPER_ADMIN"}
								: new String[]{"ADMIN"})
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("帳號不存在：" + username));
	}
}
