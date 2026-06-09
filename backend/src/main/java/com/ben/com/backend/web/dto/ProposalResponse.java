package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.enums.ProposalType;
import java.time.Instant;

public record ProposalResponse(
		Long id,
		Long meetingId,
		String proposalNumber,
		String title,
		String content,
		ProposalType type,
		ProposalStatus status,
		Instant startTime,
		Instant endTime,
		boolean visible,
		int sortOrder,
		boolean hasVoted,
		Instant createdAt
) {

	public static ProposalResponse from(Proposal proposal, boolean hasVoted) {
		return new ProposalResponse(
				proposal.getId(),
				proposal.getMeeting().getId(),
				proposal.getProposalNumber(),
				proposal.getTitle(),
				proposal.getContent(),
				proposal.getType(),
				proposal.getStatus(),
				proposal.getStartTime(),
				proposal.getEndTime(),
				proposal.isVisible(),
				proposal.getSortOrder(),
				hasVoted,
				proposal.getCreatedAt()
		);
	}
}
