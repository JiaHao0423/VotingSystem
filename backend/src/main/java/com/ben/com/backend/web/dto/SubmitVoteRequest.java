package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.VoteChoice;
import jakarta.validation.constraints.NotNull;

public class SubmitVoteRequest {

	@NotNull(message = "請選擇投票選項")
	private VoteChoice choice;

	public VoteChoice getChoice() {
		return choice;
	}

	public void setChoice(VoteChoice choice) {
		this.choice = choice;
	}
}
