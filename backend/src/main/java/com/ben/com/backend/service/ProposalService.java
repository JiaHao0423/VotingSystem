package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.ProposalRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.web.dto.AdminProposalResultResponse;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.ProposalResponse;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.UpdateProposalRequest;
import com.ben.com.backend.web.dto.VoteRecordResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProposalService {

	private final ProposalRepository proposalRepository;
	private final VoteRecordRepository voteRecordRepository;
	private final UnitRepository unitRepository;
	private final CommunityService communityService;
	private final MeetingService meetingService;

	public ProposalService(
			ProposalRepository proposalRepository,
			VoteRecordRepository voteRecordRepository,
			UnitRepository unitRepository,
			CommunityService communityService,
			MeetingService meetingService
	) {
		this.proposalRepository = proposalRepository;
		this.voteRecordRepository = voteRecordRepository;
		this.unitRepository = unitRepository;
		this.communityService = communityService;
		this.meetingService = meetingService;
	}

	@Transactional(readOnly = true)
	public List<ProposalResponse> listForAdmin(Long communityId) {
		communityService.getById(communityId);
		return proposalRepository.findByCommunityIdWithMeeting(communityId).stream()
				.map(proposal -> ProposalResponse.from(proposal, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ProposalResponse> listForVoter(Long communityId, Long ownerId) {
		var visibleStatuses = List.of(ProposalStatus.ACTIVE, ProposalStatus.SCHEDULED, ProposalStatus.ENDED);
		return proposalRepository.findVisibleByCommunityIdAndStatusIn(communityId, visibleStatuses).stream()
				.map(proposal -> ProposalResponse.from(
						proposal,
						voteRecordRepository.existsByProposalIdAndOwnerId(proposal.getId(), ownerId)
				))
				.toList();
	}

	@Transactional(readOnly = true)
	public ProposalResponse getForAdmin(Long communityId, Long proposalId) {
		return ProposalResponse.from(findProposalInCommunity(communityId, proposalId), false);
	}

	@Transactional(readOnly = true)
	public ProposalResponse getForVoter(Long communityId, Long proposalId, Long ownerId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		ensureVoterVisible(proposal);
		return ProposalResponse.from(proposal, voteRecordRepository.existsByProposalIdAndOwnerId(proposalId, ownerId));
	}

	public ProposalResponse create(Long communityId, CreateProposalRequest request) {
		var meeting = meetingService.getDefaultMeeting(communityId);
		var proposal = new Proposal(
				meeting,
				request.getProposalNumber(),
				request.getTitle(),
				request.getContent(),
				request.getType()
		);
		applyFields(proposal, request.getStartTime(), request.getEndTime(), request.isVisible(), request.getSortOrder());
		proposalRepository.save(proposal);
		return ProposalResponse.from(proposal, false);
	}

	public ProposalResponse update(Long communityId, Long proposalId, UpdateProposalRequest request) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		proposal.setProposalNumber(request.getProposalNumber());
		proposal.setTitle(request.getTitle());
		proposal.setContent(request.getContent());
		proposal.setType(request.getType());
		applyFields(proposal, request.getStartTime(), request.getEndTime(), request.isVisible(), request.getSortOrder());
		return ProposalResponse.from(proposal, false);
	}

	public void delete(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		voteRecordRepository.deleteByProposal_Id(proposalId);
		proposalRepository.delete(proposal);
	}

	public ProposalResponse start(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		if (proposal.getStatus() == ProposalStatus.ACTIVE) {
			return ProposalResponse.from(proposal, false);
		}
		if (proposal.getStatus() == ProposalStatus.ENDED) {
			throw new ConflictException("已結束的提案無法重新啟動");
		}
		proposal.setStatus(ProposalStatus.ACTIVE);
		proposal.setVisible(true);
		if (proposal.getStartTime() == null) {
			proposal.setStartTime(java.time.Instant.now());
		}
		return ProposalResponse.from(proposal, false);
	}

	public ProposalResponse stop(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		if (proposal.getStatus() != ProposalStatus.ACTIVE) {
			throw new ConflictException("僅進行中的提案可以終止投票");
		}
		proposal.setStatus(ProposalStatus.ENDED);
		if (proposal.getEndTime() == null) {
			proposal.setEndTime(java.time.Instant.now());
		}
		return ProposalResponse.from(proposal, false);
	}

	@Transactional(readOnly = true)
	public ProposalResultResponse getResult(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		var community = proposal.getMeeting().getCommunity();
		var totalCommunityArea = unitRepository.sumAreaByCommunityId(community.getId());
		return ProposalResultCalculator.compute(proposal, community, totalCommunityArea, voteRecordRepository);
	}

	@Transactional(readOnly = true)
	public AdminProposalResultResponse getAdminResult(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		var community = proposal.getMeeting().getCommunity();
		var totalCommunityArea = unitRepository.sumAreaByCommunityId(community.getId());
		var summary = ProposalResultCalculator.compute(proposal, community, totalCommunityArea, voteRecordRepository);
		var voters = voteRecordRepository.findByProposalIdWithOwner(proposalId).stream()
				.map(record -> new VoteRecordResponse(
						record.getOwner().getId(),
						record.getOwner().getName(),
						record.getOwner().getUnit().getShortName(),
						record.getChoice(),
						ProposalResultCalculator.label(record.getChoice()),
						record.getVotedAt()
				))
				.toList();
		return new AdminProposalResultResponse(summary, voters);
	}

	Proposal findProposal(Long id) {
		return proposalRepository.findByIdWithMeeting(id)
				.orElseThrow(() -> new ResourceNotFoundException("找不到提案：" + id));
	}

	Proposal findProposalInCommunity(Long communityId, Long id) {
		var proposal = findProposal(id);
		if (!proposal.getMeeting().getCommunity().getId().equals(communityId)) {
			throw new ResourceNotFoundException("找不到提案：" + id);
		}
		return proposal;
	}

	private void ensureVoterVisible(Proposal proposal) {
		if (!proposal.isVisible()) {
			throw new ResourceNotFoundException("找不到提案：" + proposal.getId());
		}
		if (proposal.getStatus() == ProposalStatus.DRAFT) {
			throw new ResourceNotFoundException("找不到提案：" + proposal.getId());
		}
	}

	private void applyFields(Proposal proposal, java.time.Instant startTime, java.time.Instant endTime, boolean visible, int sortOrder) {
		proposal.setStartTime(startTime);
		proposal.setEndTime(endTime);
		proposal.setVisible(visible);
		proposal.setSortOrder(sortOrder);
	}
}
