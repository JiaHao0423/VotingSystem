package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderProposalsRequest(
		@NotEmpty List<Long> orderedIds
) {
}
