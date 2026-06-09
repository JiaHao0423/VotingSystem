package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.enums.ProposalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class UpdateProposalRequest {

	@NotBlank
	private String proposalNumber;

	@NotBlank
	private String title;

	@NotBlank
	private String content;

	@NotNull
	private ProposalType type;

	private Instant startTime;

	private Instant endTime;

	private boolean visible;

	private int sortOrder;

	public String getProposalNumber() {
		return proposalNumber;
	}

	public void setProposalNumber(String proposalNumber) {
		this.proposalNumber = proposalNumber != null ? proposalNumber.trim() : null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title != null ? title.trim() : null;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public ProposalType getType() {
		return type;
	}

	public void setType(ProposalType type) {
		this.type = type;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
