package com.ben.com.backend.repository;

import com.ben.com.backend.domain.entity.Community;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, Long> {

	Optional<Community> findByName(String name);
}
