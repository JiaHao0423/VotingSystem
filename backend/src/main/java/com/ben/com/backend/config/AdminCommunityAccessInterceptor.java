package com.ben.com.backend.config;

import com.ben.com.backend.service.AdminAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/** 確保管理員只能存取自己社區底下的 /api/admin/communities/{communityId}/** 資源 */
@NullMarked
@Component
public class AdminCommunityAccessInterceptor implements HandlerInterceptor {

	private final AdminAccountService adminAccountService;

	public AdminCommunityAccessInterceptor(AdminAccountService adminAccountService) {
		this.adminAccountService = adminAccountService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		@SuppressWarnings("unchecked")
		var uriVariables = (Map<String, String>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (uriVariables == null) {
			return true;
		}
		var communityId = uriVariables.get("communityId");
		if (communityId == null) {
			return true;
		}
		adminAccountService.assertCommunityAccess(Long.valueOf(communityId));
		return true;
	}
}
