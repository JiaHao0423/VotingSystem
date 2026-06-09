package com.ben.com.backend.web.dto;

import java.util.List;

public record AdminProposalResultResponse(
		ProposalResultResponse summary,
		List<VoteRecordResponse> voters
) {
}
