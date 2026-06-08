package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.enums.MeetingStatus;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.MeetingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingService {

	private final MeetingRepository meetingRepository;
	private final CommunityService communityService;

	public MeetingService(MeetingRepository meetingRepository, CommunityService communityService) {
		this.meetingRepository = meetingRepository;
		this.communityService = communityService;
	}

	public List<Meeting> listByCommunity(Long communityId) {
		communityService.getById(communityId);
		return meetingRepository.findByCommunityIdOrderByMeetingDateDesc(communityId);
	}

	public Meeting getActiveMeeting(Long communityId) {
		return meetingRepository.findFirstByCommunityIdAndStatusOrderByMeetingDateDesc(communityId, MeetingStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("目前沒有進行中的區權會"));
	}

	public Meeting getById(Long id) {
		return meetingRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到會議：" + id));
	}
}
