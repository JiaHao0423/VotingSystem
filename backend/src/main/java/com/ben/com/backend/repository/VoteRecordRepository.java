package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.VoteRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NullMarked
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {

	boolean existsByProposalIdAndOwnerId(Long proposalId, Long ownerId);

	Optional<VoteRecord> findByProposalIdAndOwnerId(Long proposalId, Long ownerId);

	@Query("""
			SELECT v.proposal.id FROM VoteRecord v
			WHERE v.owner.id = :ownerId AND v.proposal.id IN :proposalIds
			""")
	List<Long> findVotedProposalIds(
			@Param("ownerId") Long ownerId,
			@Param("proposalIds") Collection<Long> proposalIds
	);

	@Query("""
			SELECT v.choiceKey, COUNT(v), COALESCE(SUM(v.voteWeight), 0)
			FROM VoteRecord v
			WHERE v.proposal.id = :proposalId
			GROUP BY v.choiceKey
			""")
	List<Object[]> aggregateByProposalId(@Param("proposalId") Long proposalId);

	@Query("""
			SELECT COUNT(v) FROM VoteRecord v
			WHERE v.proposal.id = :proposalId AND v.choiceKey IN :passKeys
			""")
	long countPassVotes(
			@Param("proposalId") Long proposalId,
			@Param("passKeys") Collection<String> passKeys
	);

	@Query("""
			SELECT COALESCE(SUM(v.voteWeight), 0) FROM VoteRecord v
			WHERE v.proposal.id = :proposalId AND v.choiceKey IN :passKeys
			""")
	java.math.BigDecimal sumPassVoteWeight(
			@Param("proposalId") Long proposalId,
			@Param("passKeys") Collection<String> passKeys
	);

	long countByProposalId(Long proposalId);

	@Query("""
			SELECT COALESCE(SUM(v.voteWeight), 0) FROM VoteRecord v
			WHERE v.proposal.id = :proposalId
			""")
	java.math.BigDecimal sumWeightByProposalId(@Param("proposalId") Long proposalId);

	@Query("""
			SELECT v FROM VoteRecord v
			JOIN FETCH v.owner o
			JOIN FETCH o.unit u
			WHERE v.proposal.id = :proposalId
			ORDER BY u.buildingType, u.floor, u.unitNo, u.shopNo
			""")
	List<VoteRecord> findByProposalIdWithOwner(@Param("proposalId") Long proposalId);

	void deleteByOwner_IdIn(Collection<Long> ownerIds);

	void deleteByOwner_Unit_Community_Id(Long communityId);

	void deleteByProposal_Id(Long proposalId);
}
