package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.enums.MeetingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	List<Meeting> findByCommunityIdOrderByMeetingDateDesc(Long communityId);

	Optional<Meeting> findFirstByCommunityIdOrderByMeetingDateDesc(Long communityId);

	Optional<Meeting> findFirstByCommunityIdAndStatusOrderByMeetingDateDesc(Long communityId, MeetingStatus status);
}
