package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.enums.AdminRole;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@NullMarked
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	Optional<AdminUser> findByUsername(String username);

	boolean existsByUsername(String username);

	long countByRole(AdminRole role);

	@Query("""
			SELECT a FROM AdminUser a
			LEFT JOIN FETCH a.community
			ORDER BY a.role ASC, a.id ASC
			""")
	List<AdminUser> findAllWithCommunity();

	@Query("""
			SELECT a FROM AdminUser a
			LEFT JOIN FETCH a.community
			WHERE a.username = :username
			""")
	Optional<AdminUser> findByUsernameWithCommunity(String username);

	void deleteByCommunity_Id(Long communityId);
}
