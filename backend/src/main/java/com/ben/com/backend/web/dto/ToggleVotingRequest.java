package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleVotingRequest(@NotNull Boolean active) {
}
