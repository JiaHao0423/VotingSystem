package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NullMarked
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

	@Query("""
			SELECT p FROM Proposal p
			JOIN FETCH p.meeting m
			WHERE m.community.id = :communityId
			ORDER BY p.sortOrder, p.id
			""")
	List<Proposal> findByCommunityIdWithMeeting(@Param("communityId") Long communityId);

	@Query("""
			SELECT p FROM Proposal p
			JOIN FETCH p.meeting m
			JOIN FETCH m.community
			WHERE p.id = :id
			""")
	Optional<Proposal> findByIdWithMeeting(@Param("id") Long id);

	@Query("""
			SELECT p FROM Proposal p
			JOIN FETCH p.meeting m
			WHERE m.community.id = :communityId
			AND p.visible = true
			AND p.status IN :statuses
			ORDER BY p.sortOrder, p.id
			""")
	List<Proposal> findVisibleByCommunityIdAndStatusIn(
			@Param("communityId") Long communityId,
			@Param("statuses") List<ProposalStatus> statuses
	);

	void deleteByMeeting_Community_Id(Long communityId);

	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE Proposal p SET p.status = 'ENDED'
			WHERE p.status = 'ACTIVE'
			AND p.endTime IS NOT NULL
			AND p.endTime <= :now
			AND p.meeting.community.id = :communityId
			""")
	void endActiveProposalsPastEndTime(@Param("communityId") Long communityId, @Param("now") Instant now);

	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE Proposal p SET p.status = 'ENDED'
			WHERE p.status = 'ACTIVE'
			AND p.endTime IS NOT NULL
			AND p.endTime <= :now
			""")
	void endAllActiveProposalsPastEndTime(@Param("now") Instant now);
}
