package com.ben.com.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final AdminCommunityAccessInterceptor adminCommunityAccessInterceptor;

	public WebMvcConfig(AdminCommunityAccessInterceptor adminCommunityAccessInterceptor) {
		this.adminCommunityAccessInterceptor = adminCommunityAccessInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(adminCommunityAccessInterceptor)
				.addPathPatterns("/api/admin/communities/**");
	}
}
