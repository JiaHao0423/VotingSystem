package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.CommunityService;
import com.ben.com.backend.service.MeetingService;
import com.ben.com.backend.web.dto.CommunityResponse;
import com.ben.com.backend.web.dto.MeetingResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommunityController {

	private final CommunityService communityService;
	private final MeetingService meetingService;

	public CommunityController(CommunityService communityService, MeetingService meetingService) {
		this.communityService = communityService;
		this.meetingService = meetingService;
	}

	@GetMapping("/community")
	public CommunityResponse getDefaultCommunity() {
		return CommunityResponse.from(communityService.getDefaultCommunity());
	}

	@GetMapping("/communities/{communityId}/meetings")
	public List<MeetingResponse> listMeetings(@PathVariable Long communityId) {
		return meetingService.listByCommunity(communityId).stream()
				.map(MeetingResponse::from)
				.toList();
	}
}
