package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitRepository extends JpaRepository<Unit, Long> {

	List<Unit> findByCommunityIdOrderByBuildingTypeAscFloorAscUnitNoAscShopNoAsc(Long communityId);

	List<Unit> findByCommunityIdAndBuildingTypeOrderByFloorAscUnitNoAsc(Long communityId, BuildingType buildingType);

	Optional<Unit> findByCommunityIdAndShortName(Long communityId, String shortName);

	boolean existsByCommunityIdAndShortName(Long communityId, String shortName);

	@Query("""
			SELECT u FROM Unit u
			WHERE u.community.id = :communityId
			AND u.id NOT IN (SELECT o.unit.id FROM Owner o)
			ORDER BY u.buildingType, u.floor, u.unitNo, u.shopNo
			""")
	List<Unit> findUnassignedByCommunityId(@Param("communityId") Long communityId);

	long countByCommunityId(Long communityId);
}
