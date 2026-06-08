package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.CommunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommunityService {

	private final CommunityRepository communityRepository;

	public CommunityService(CommunityRepository communityRepository) {
		this.communityRepository = communityRepository;
	}

	public Community getDefaultCommunity() {
		return communityRepository.findByName("鴻邑晴川硯")
				.orElseThrow(() -> new ResourceNotFoundException("尚未初始化社區資料"));
	}

	public Community getById(Long id) {
		return communityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到社區：" + id));
	}
}
