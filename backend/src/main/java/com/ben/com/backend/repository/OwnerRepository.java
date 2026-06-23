package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.Owner;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NullMarked
public interface OwnerRepository extends JpaRepository<Owner, Long> {

	@Query("""
			SELECT o FROM Owner o
			JOIN FETCH o.unit u
			JOIN FETCH u.community
			WHERE u.community.id = :communityId
			ORDER BY u.buildingType, u.floor, u.unitNo, u.shopNo
			""")
	List<Owner> findByCommunityIdWithUnit(@Param("communityId") Long communityId);

	@Query("""
			SELECT o FROM Owner o
			JOIN FETCH o.unit u
			JOIN FETCH u.community
			WHERE o.id = :id
			""")
	Optional<Owner> findByIdWithUnit(@Param("id") Long id);

	@Query("""
			SELECT o FROM Owner o
			JOIN FETCH o.unit u
			JOIN FETCH u.community
			WHERE o.qrToken = :token
			""")
	Optional<Owner> findByQrTokenWithUnit(@Param("token") String token);

	boolean existsByUnitId(Long unitId);

	void deleteByUnit_Community_Id(Long communityId);

	@Query("""
			SELECT o FROM Owner o
			JOIN FETCH o.unit u
			WHERE u.community.id = :communityId AND u.shortName = :shortName
			""")
	Optional<Owner> findByCommunityIdAndUnitShortName(
			@Param("communityId") Long communityId,
			@Param("shortName") String shortName
	);

	@Query("""
			SELECT COUNT(o) FROM Owner o
			WHERE o.unit.community.id = :communityId AND o.attended = true
			""")
	long countAttendedByCommunityId(@Param("communityId") Long communityId);

	@Query("""
			SELECT COALESCE(SUM(u.area), 0) FROM Owner o
			JOIN o.unit u
			WHERE u.community.id = :communityId AND o.attended = true
			""")
	java.math.BigDecimal sumAttendedAreaByCommunityId(@Param("communityId") Long communityId);
}
