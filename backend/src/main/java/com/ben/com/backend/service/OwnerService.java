package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Owner;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.util.ShortNameNormalizer;
import com.ben.com.backend.util.UnitShortNameParser;
import com.ben.com.backend.web.dto.AuthCodeRegeneratedResponse;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.CreateUnitRequest;
import com.ben.com.backend.web.dto.OwnerCreatedResponse;
import com.ben.com.backend.web.dto.OwnerQrPrintItemResponse;
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
	private final VoteRecordRepository voteRecordRepository;
	private final UnitService unitService;
	private final AuthCodeService authCodeService;
	private final QrTokenService qrTokenService;
	private final String voterBaseUrl;

	public OwnerService(
			OwnerRepository ownerRepository,
			VoteRecordRepository voteRecordRepository,
			UnitService unitService,
			AuthCodeService authCodeService,
			QrTokenService qrTokenService,
			@Value("${app.voter-base-url:http://localhost:5173}") String voterBaseUrl
	) {
		this.ownerRepository = ownerRepository;
		this.voteRecordRepository = voteRecordRepository;
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
	public List<OwnerQrPrintItemResponse> listQrPrintItems(Long communityId) {
		return ownerRepository.findByCommunityIdWithUnit(communityId).stream()
				.map(owner -> OwnerQrPrintItemResponse.from(
						owner,
						qrTokenService.buildAuthUrl(voterBaseUrl, owner.getQrToken())
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public OwnerResponse getById(Long communityId, Long id) {
		return OwnerResponse.from(findOwnerInCommunity(communityId, id));
	}

	public OwnerCreatedResponse create(Long communityId, CreateOwnerRequest request) {
		var unit = resolveOrCreateUnit(communityId, request);
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
				"永久 QR Code 已建立，請列印供住戶報到掃描登入"
		);
	}

	public OwnerResponse update(Long communityId, Long id, UpdateOwnerRequest request) {
		var owner = findOwnerInCommunity(communityId, id);
		owner.setName(request.getName());
		owner.setPhone(normalizePhone(request.getPhone()));
		owner.setAttended(request.isAttended());
		unitService.updateFromOwnerRequest(communityId, owner.getUnit(), request);
		return OwnerResponse.from(owner);
	}

	public void delete(Long communityId, Long id) {
		removeOwners(communityId, List.of(id));
	}

	public int deleteMany(Long communityId, List<Long> ids) {
		return removeOwners(communityId, ids);
	}

	private int removeOwners(Long communityId, List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return 0;
		}
		var distinctIds = ids.stream().distinct().toList();
		var owners = ownerRepository.findAllById(distinctIds);
		if (owners.size() != distinctIds.size()) {
			throw new ResourceNotFoundException("部分所有權人不存在");
		}
		for (var owner : owners) {
			if (!owner.getUnit().getCommunity().getId().equals(communityId)) {
				throw new ResourceNotFoundException("所有權人不屬於此社區");
			}
		}
		voteRecordRepository.deleteByOwner_IdIn(distinctIds);
		ownerRepository.deleteAll(owners);
		return owners.size();
	}

	public void upsertOwnerForImport(
			Long communityId,
			String unitShortName,
			String ownerName,
			String ownerPhone
	) {
		if (ownerName == null || ownerName.isBlank()) {
			return;
		}
		var normalized = ShortNameNormalizer.normalize(unitShortName);
		var existing = ownerRepository.findByCommunityIdAndUnitShortName(communityId, normalized);
		if (existing.isPresent()) {
			var owner = existing.get();
			owner.setName(ownerName.trim());
			owner.setPhone(normalizePhone(ownerPhone));
			return;
		}
		var unit = unitService.findByShortName(communityId, normalized);
		var request = new CreateOwnerRequest();
		request.setUnitId(unit.getId());
		request.setName(ownerName.trim());
		request.setPhone(ownerPhone);
		create(communityId, request);
	}

	public AuthCodeRegeneratedResponse regenerateAuthCode(Long communityId, Long id) {
		var owner = findOwnerInCommunity(communityId, id);
		var plainCode = authCodeService.generatePlainCode();
		owner.setAuthCodeHash(authCodeService.hashCode(plainCode));
		return new AuthCodeRegeneratedResponse(
				owner.getId(),
				owner.getUnit().getShortName(),
				plainCode,
				"舊驗證碼已失效，新驗證碼僅顯示一次"
		);
	}

	public QrCodeResponse getQrCode(Long communityId, Long id) {
		var owner = findOwnerInCommunity(communityId, id);
		return QrCodeResponse.from(owner, qrTokenService.buildAuthUrl(voterBaseUrl, owner.getQrToken()));
	}

	public QrCodeResponse regenerateQrToken(Long communityId, Long id) {
		var owner = findOwnerInCommunity(communityId, id);
		var newToken = qrTokenService.generateToken();
		owner.setQrToken(newToken);
		return QrCodeResponse.from(owner, qrTokenService.buildAuthUrl(voterBaseUrl, newToken));
	}

	Owner findOwner(Long id) {
		return ownerRepository.findByIdWithUnit(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到所有權人：" + id));
	}

	Owner findOwnerInCommunity(Long communityId, Long id) {
		var owner = findOwner(id);
		if (!owner.getUnit().getCommunity().getId().equals(communityId)) {
			throw new ResourceNotFoundException("所有權人不屬於此社區");
		}
		return owner;
	}

	private com.ben.com.backend.domain.entity.Unit resolveOrCreateUnit(
			Long communityId,
			CreateOwnerRequest request
	) {
		if (request.getUnitId() != null) {
			return unitService.findUnit(request.getUnitId());
		}
		if (request.getUnitShortName() == null || request.getUnitShortName().isBlank()) {
			throw new IllegalArgumentException("請提供戶別編號或戶別簡稱");
		}

		var normalized = ShortNameNormalizer.normalize(request.getUnitShortName());
		return unitService.findByShortNameOptional(communityId, normalized)
				.orElseGet(() -> createUnitForOwner(communityId, request, normalized));
	}

	private com.ben.com.backend.domain.entity.Unit createUnitForOwner(
			Long communityId,
			CreateOwnerRequest request,
			String normalizedShortName
	) {
		if (request.getFullAddress() == null || request.getFullAddress().isBlank()) {
			throw new IllegalArgumentException("戶別尚未建立，請填寫完整門牌");
		}
		if (request.getArea() == null || request.getArea().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("戶別尚未建立，請填寫坪數");
		}
		if (request.getOwnershipRatio() == null
				|| request.getOwnershipRatio().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("戶別尚未建立，請填寫區分所有權比例");
		}

		var parsed = UnitShortNameParser.parse(normalizedShortName)
				.orElseThrow(() -> new IllegalArgumentException("無法解析戶別簡稱：" + request.getUnitShortName()));

		var unitRequest = new CreateUnitRequest();
		unitRequest.setShortName(parsed.shortName());
		unitRequest.setFullAddress(request.getFullAddress());
		unitRequest.setBuildingType(parsed.buildingType());
		unitRequest.setFloor(parsed.floor());
		unitRequest.setUnitNo(parsed.unitNo());
		unitRequest.setShopNo(parsed.shopNo());
		unitRequest.setArea(request.getArea());
		unitRequest.setOwnershipRatio(request.getOwnershipRatio());

		var created = unitService.create(communityId, unitRequest);
		return unitService.findUnit(created.id());
	}

	private String normalizePhone(String phone) {
		if (phone == null || phone.isBlank()) {
			return null;
		}
		return phone.replace("-", "");
	}
}
