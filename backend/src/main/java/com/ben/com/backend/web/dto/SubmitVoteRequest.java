package com.ben.com.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SubmitVoteRequest {

	@NotBlank(message = "請選擇投票選項")
	private String choiceKey;

	public void setChoiceKey(String choiceKey) {
		this.choiceKey = choiceKey != null ? choiceKey.trim() : null;
	}
}
