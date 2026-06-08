package com.ben.com.backend.web.dto;

import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.enums.MeetingStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MeetingResponse(
		Long id,
		Long communityId,
		String name,
		LocalDate meetingDate,
		MeetingStatus status,
		Instant createdAt
) {

	public static MeetingResponse from(Meeting meeting) {
		return new MeetingResponse(
				meeting.getId(),
				meeting.getCommunity().getId(),
				meeting.getName(),
				meeting.getMeetingDate(),
				meeting.getStatus(),
				meeting.getCreatedAt()
		);
	}
}
