package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.enums.ProposalType;
import com.ben.com.backend.domain.enums.ThresholdBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

	@Valid
	private List<VoteOptionRequest> voteOptions;

	@Min(1)
	private int passThresholdNumerator = 1;

	@Min(1)
	private int passThresholdDenominator = 2;

	private ThresholdBase thresholdBase = ThresholdBase.ATTENDED;

	private boolean allowRevote = true;

	public void setProposalNumber(String proposalNumber) {
		this.proposalNumber = proposalNumber != null ? proposalNumber.trim() : null;
	}

	public void setTitle(String title) {
		this.title = title != null ? title.trim() : null;
	}
}
