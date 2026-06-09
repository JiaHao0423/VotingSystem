package com.ben.com.backend.service;

import com.ben.com.backend.exception.UnauthorizedException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.security.VoterPrincipal;
import com.ben.com.backend.util.ShortNameNormalizer;
import com.ben.com.backend.web.dto.QrAuthRequest;
import com.ben.com.backend.web.dto.QrPreviewResponse;
import com.ben.com.backend.web.dto.VerifyAuthRequest;
import com.ben.com.backend.web.dto.VoterSessionResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VoterAuthService {

	private static final String INVALID_CREDENTIALS = "戶別或驗證碼錯誤，請確認後再試";
	private static final String INVALID_QR = "無效的 QR Code 或連結已失效";
	private static final String NO_OWNER = "此戶別尚未登記所有權人，請洽管理員";

	private final OwnerRepository ownerRepository;
	private final UnitRepository unitRepository;
	private final CommunityService communityService;
	private final AuthCodeService authCodeService;

	public VoterAuthService(
			OwnerRepository ownerRepository,
			UnitRepository unitRepository,
			CommunityService communityService,
			AuthCodeService authCodeService
	) {
		this.ownerRepository = ownerRepository;
		this.unitRepository = unitRepository;
		this.communityService = communityService;
		this.authCodeService = authCodeService;
	}

	public VoterSessionResponse verifyByUnitAndCode(VerifyAuthRequest request) {
		var community = communityService.getDefaultCommunity();
		var shortName = ShortNameNormalizer.normalize(request.getUnitShortName());
		var authCode = normalizeAuthCode(request.getAuthCode());

		if (!unitRepository.existsByCommunityIdAndShortName(community.getId(), shortName)) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}

		var owner = ownerRepository.findByCommunityIdAndUnitShortName(community.getId(), shortName)
				.orElseThrow(() -> new UnauthorizedException(NO_OWNER));

		if (!authCodeService.matches(authCode, owner.getAuthCodeHash())) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}

		return establishSession(owner, "身份驗證成功，歡迎進入投票系統");
	}

	@Transactional(readOnly = true)
	public QrPreviewResponse previewQrToken(String token) {
		var owner = ownerRepository.findByQrTokenWithUnit(token)
				.orElseThrow(() -> new UnauthorizedException(INVALID_QR));
		return QrPreviewResponse.from(owner);
	}

	public VoterSessionResponse verifyByQrToken(QrAuthRequest request) {
		var owner = ownerRepository.findByQrTokenWithUnit(request.getToken())
				.orElseThrow(() -> new UnauthorizedException(INVALID_QR));

		return establishSession(owner, "報到成功，歡迎進入投票系統");
	}

	@Transactional(readOnly = true)
	public VoterSessionResponse currentSession() {
		var principal = getCurrentPrincipal();
		return VoterSessionResponse.from(principal, "已登入");
	}

	public void logout() {
		SecurityContextHolder.clearContext();
	}

	private VoterSessionResponse establishSession(com.ben.com.backend.domain.entity.Owner owner, String message) {
		if (!owner.isAttended()) {
			owner.setAttended(true);
		}

		var principal = VoterPrincipal.from(owner);
		var authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				java.util.List.of(new SimpleGrantedAuthority("ROLE_VOTER"))
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return VoterSessionResponse.from(principal, message);
	}

	@Transactional(readOnly = true)
	public VoterPrincipal getCurrentPrincipal() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof VoterPrincipal principal)) {
			throw new UnauthorizedException("尚未登入或 session 已過期");
		}
		return principal;
	}

	private String normalizeAuthCode(String authCode) {
		if (authCode == null) {
			return "";
		}
		return authCode.trim().replace(" ", "").toUpperCase();
	}
}
