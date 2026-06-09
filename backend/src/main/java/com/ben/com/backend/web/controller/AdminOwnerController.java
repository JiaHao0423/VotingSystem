package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.OwnerService;
import com.ben.com.backend.web.dto.AuthCodeRegeneratedResponse;
import com.ben.com.backend.web.dto.BatchDeleteOwnersRequest;
import com.ben.com.backend.web.dto.BatchDeleteOwnersResponse;
import com.ben.com.backend.web.dto.CreateOwnerRequest;
import com.ben.com.backend.web.dto.OwnerCreatedResponse;
import com.ben.com.backend.web.dto.OwnerQrPrintItemResponse;
import com.ben.com.backend.web.dto.OwnerResponse;
import com.ben.com.backend.web.dto.QrCodeResponse;
import com.ben.com.backend.web.dto.UpdateOwnerRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/communities/{communityId}/owners")
public class AdminOwnerController {

	private final OwnerService ownerService;

	public AdminOwnerController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@GetMapping
	public List<OwnerResponse> list(@PathVariable Long communityId) {
		return ownerService.list(communityId);
	}

	@GetMapping("/qr/print-all")
	public List<OwnerQrPrintItemResponse> listQrPrintItems(@PathVariable Long communityId) {
		return ownerService.listQrPrintItems(communityId);
	}

	@GetMapping("/{ownerId}")
	public OwnerResponse get(@PathVariable Long communityId, @PathVariable Long ownerId) {
		return ownerService.getById(ownerId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OwnerCreatedResponse create(
			@PathVariable Long communityId,
			@Valid @RequestBody CreateOwnerRequest request
	) {
		return ownerService.create(communityId, request);
	}

	@PutMapping("/{ownerId}")
	public OwnerResponse update(
			@PathVariable Long communityId,
			@PathVariable Long ownerId,
			@Valid @RequestBody UpdateOwnerRequest request
	) {
		return ownerService.update(communityId, ownerId, request);
	}

	@PostMapping("/batch-delete")
	public BatchDeleteOwnersResponse batchDelete(
			@PathVariable Long communityId,
			@Valid @RequestBody BatchDeleteOwnersRequest request
	) {
		var deleted = ownerService.deleteMany(communityId, request.ownerIds());
		return new BatchDeleteOwnersResponse(deleted);
	}

	@DeleteMapping("/{ownerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long communityId, @PathVariable Long ownerId) {
		ownerService.delete(communityId, ownerId);
	}

	@PostMapping("/{ownerId}/regenerate-code")
	public AuthCodeRegeneratedResponse regenerateCode(
			@PathVariable Long communityId,
			@PathVariable Long ownerId
	) {
		return ownerService.regenerateAuthCode(ownerId);
	}

	@GetMapping("/{ownerId}/qr")
	public QrCodeResponse getQrCode(@PathVariable Long communityId, @PathVariable Long ownerId) {
		return ownerService.getQrCode(ownerId);
	}

	@PostMapping("/{ownerId}/regenerate-qr")
	public QrCodeResponse regenerateQr(@PathVariable Long communityId, @PathVariable Long ownerId) {
		return ownerService.regenerateQrToken(ownerId);
	}
}
