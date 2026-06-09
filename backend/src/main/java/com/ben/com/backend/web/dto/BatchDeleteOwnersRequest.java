package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchDeleteOwnersRequest(
		@NotEmpty(message = "請選擇至少一位所有權人")
		List<Long> ownerIds
) {
}
