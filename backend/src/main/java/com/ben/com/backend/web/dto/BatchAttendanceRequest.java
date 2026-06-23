package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchAttendanceRequest(
		@NotEmpty List<Long> ownerIds,
		@NotNull Boolean attended
) {
}
