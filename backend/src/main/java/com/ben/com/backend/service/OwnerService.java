package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.web.dto.AuthCodeRegeneratedResponse;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.OwnerCreatedResponse;
import com.ben.com.backend.web.dto.OwnerResponse;
import com.ben.com.backend.web.dto.QrCodeResponse;
import com.ben.com.backend.web.dto.UpdateOwnerRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerService {

	private final OwnerRepository ownerRepository;
	private final UnitService unitService;
	private final AuthCodeService authCodeService;
	private final QrTokenService qrTokenService;
	private final String voterBaseUrl;

	public OwnerService(
			OwnerRepository ownerRepository,
			UnitService unitService,
			AuthCodeService authCodeService,
			QrTokenService qrTokenService,
			@Value("${app.voter-base-url:http://localhost:5173}") String voterBaseUrl
	) {
		this.ownerRepository = ownerRepository;
		this.unitService = unitService;
		this.authCodeService = authCodeService;
		this.qrTokenService = qrTokenService;
		this.voterBaseUrl = voterBaseUrl;
	}

	@Transactional(readOnly = true)
	public List<OwnerResponse> list(Long communityId) {
		return ownerRepository.findByCommunityIdWithUnit(communityId).stream()
				.map(OwnerResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public OwnerResponse getById(Long id) {
		return OwnerResponse.from(findOwner(id));
	}

	public OwnerCreatedResponse create(Long communityId, CreateOwnerRequest request) {
		var unit = unitService.findUnit(request.getUnitId());
		if (!unit.getCommunity().getId().equals(communityId)) {
			throw new ResourceNotFoundException("戶別不屬於此社區");
		}
		if (ownerRepository.existsByUnitId(unit.getId())) {
			throw new ConflictException("此戶別已有所有權人：" + unit.getShortName());
		}

		var plainCode = authCodeService.generatePlainCode();
		var qrToken = qrTokenService.generateToken();
		var owner = new Owner(
				unit,
				request.getName(),
				normalizePhone(request.getPhone()),
				authCodeService.hashCode(plainCode),
				qrToken
		);
		ownerRepository.save(owner);

		return new OwnerCreatedResponse(
				OwnerResponse.from(owner),
				plainCode,
				qrToken,
				qrTokenService.buildAuthUrl(voterBaseUrl, qrToken),
				"驗證碼僅顯示一次，請於報到時交付住戶並妥善保存"
		);
	}

	public OwnerResponse update(Long id, UpdateOwnerRequest request) {
		var owner = findOwner(id);
		owner.setName(request.getName());
		owner.setPhone(normalizePhone(request.getPhone()));
		return OwnerResponse.from(owner);
	}

	public void delete(Long id) {
		var owner = findOwner(id);
		ownerRepository.delete(owner);
	}

	public AuthCodeRegeneratedResponse regenerateAuthCode(Long id) {
		var owner = findOwner(id);
		var plainCode = authCodeService.generatePlainCode();
		owner.setAuthCodeHash(authCodeService.hashCode(plainCode));
		return new AuthCodeRegeneratedResponse(
				owner.getId(),
				owner.getUnit().getShortName(),
				plainCode,
				"舊驗證碼已失效，新驗證碼僅顯示一次"
		);
	}

	public QrCodeResponse getQrCode(Long id) {
		var owner = findOwner(id);
		return new QrCodeResponse(
				owner.getId(),
				owner.getUnit().getShortName(),
				owner.getName(),
				owner.getQrToken(),
				qrTokenService.buildAuthUrl(voterBaseUrl, owner.getQrToken())
		);
	}

	public QrCodeResponse regenerateQrToken(Long id) {
		var owner = findOwner(id);
		var newToken = qrTokenService.generateToken();
		owner.setQrToken(newToken);
		return new QrCodeResponse(
				owner.getId(),
				owner.getUnit().getShortName(),
				owner.getName(),
				newToken,
				qrTokenService.buildAuthUrl(voterBaseUrl, newToken)
		);
	}

	Owner findOwner(Long id) {
		return ownerRepository.findByIdWithUnit(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到所有權人：" + id));
	}

	private String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return null;
		}
		return phone.replace("-", "");
	}
}
