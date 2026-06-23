package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.model.VoteOptionItem;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteOptionRequest {

	@NotBlank
	private String label;

	private String description;

	private boolean passOption;

	public void setLabel(String label) {
		this.label = label != null ? label.trim() : null;
	}

	public VoteOptionItem toItem(int sortOrder) {
		return new VoteOptionItem(null, label, description, sortOrder, passOption);
	}
}
