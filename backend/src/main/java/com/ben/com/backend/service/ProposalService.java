package com.ben.com.backend.service;

import com.ben.com.backend.domain.entity.Proposal;
import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.model.VoteOptionItem;
import com.ben.com.backend.exception.ConflictException;
import com.ben.com.backend.exception.ResourceNotFoundException;
import com.ben.com.backend.repository.OwnerRepository;
import com.ben.com.backend.repository.ProposalRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.repository.VoteRecordRepository;
import com.ben.com.backend.util.VoteOptionDefaults;
import com.ben.com.backend.web.dto.AdminProposalResultResponse;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.ProposalResponse;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.UpdateProposalRequest;
import com.ben.com.backend.web.dto.VoteOptionRequest;
import com.ben.com.backend.web.dto.VoteRecordResponse;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProposalService {

	private final ProposalRepository proposalRepository;
	private final VoteRecordRepository voteRecordRepository;
	private final UnitRepository unitRepository;
	private final OwnerRepository ownerRepository;
	private final CommunityService communityService;
	private final MeetingService meetingService;
	private final ProposalLifecycleService proposalLifecycleService;
	private final EntityManager entityManager;

	public ProposalService(
			ProposalRepository proposalRepository,
			VoteRecordRepository voteRecordRepository,
			UnitRepository unitRepository,
			OwnerRepository ownerRepository,
			CommunityService communityService,
			MeetingService meetingService,
			ProposalLifecycleService proposalLifecycleService,
			EntityManager entityManager
	) {
		this.proposalRepository = proposalRepository;
		this.voteRecordRepository = voteRecordRepository;
		this.unitRepository = unitRepository;
		this.ownerRepository = ownerRepository;
		this.communityService = communityService;
		this.meetingService = meetingService;
		this.proposalLifecycleService = proposalLifecycleService;
		this.entityManager = entityManager;
	}

	public List<ProposalResponse> listForAdmin(Long communityId) {
		communityService.getById(communityId);
		syncExpiredStatuses(communityId);
		return proposalRepository.findByCommunityIdWithMeeting(communityId).stream()
				.map(proposal -> ProposalResponse.from(proposal, false))
				.toList();
	}

	public List<ProposalResponse> listForVoter(Long communityId, Long ownerId) {
		syncExpiredStatuses(communityId);
		var proposals = proposalRepository.findVisibleByCommunityId(communityId);
		var proposalIds = proposals.stream().map(Proposal::getId).toList();
		Set<Long> votedIds = proposalIds.isEmpty()
				? Set.of()
				: new HashSet<>(voteRecordRepository.findVotedProposalIds(ownerId, proposalIds));
		return proposals.stream()
				.map(proposal -> ProposalResponse.from(proposal, votedIds.contains(proposal.getId())))
				.toList();
	}

	public ProposalResponse getForAdmin(Long communityId, Long proposalId) {
		syncExpiredStatuses(communityId);
		return ProposalResponse.from(findProposalInCommunity(communityId, proposalId), false);
	}

	public ProposalResponse getForVoter(Long communityId, Long proposalId, Long ownerId) {
		syncExpiredStatuses(communityId);
		var proposal = findProposalInCommunity(communityId, proposalId);
		ensureVoterVisible(proposal);
		return ProposalResponse.from(
				proposal,
				voteRecordRepository.existsByProposalIdAndOwnerId(proposalId, ownerId)
		);
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
		applyFields(proposal, request);
		proposal.setSortOrder(proposalRepository.maxSortOrderByCommunityId(communityId) + 1);
		proposalRepository.save(proposal);
		return ProposalResponse.from(proposal, false);
	}

	public ProposalResponse update(Long communityId, Long proposalId, UpdateProposalRequest request) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		proposal.setProposalNumber(request.getProposalNumber());
		proposal.setTitle(request.getTitle());
		proposal.setContent(request.getContent());
		proposal.setType(request.getType());
		applyFields(proposal, request);
		return ProposalResponse.from(proposal, false);
	}

	public void delete(Long communityId, Long proposalId) {
		var proposal = findProposalInCommunity(communityId, proposalId);
		voteRecordRepository.deleteByProposal_Id(proposalId);
		proposalRepository.delete(proposal);
	}

	public ProposalResponse start(Long communityId, Long proposalId) {
		return setVotingActive(communityId, proposalId, true);
	}

	public ProposalResponse stop(Long communityId, Long proposalId) {
		return setVotingActive(communityId, proposalId, false);
	}

	public ProposalResponse setVotingActive(Long communityId, Long proposalId, boolean active) {
		syncExpiredStatuses(communityId);
		var proposal = findProposalInCommunity(communityId, proposalId);
		var now = Instant.now();
		if (active) {
			if (proposal.getStatus() == ProposalStatus.ACTIVE) {
				return ProposalResponse.from(proposal, false);
			}
			// Admin manual start: clear expired end time so lifecycle sync won't immediately end it.
			if (proposal.getEndTime() != null && !proposal.getEndTime().isAfter(now)) {
				proposal.setEndTime(null);
			}
			proposal.setStatus(ProposalStatus.ACTIVE);
			proposal.setVisible(true);
			proposal.setStartTime(now);
		} else {
			if (proposal.getStatus() != ProposalStatus.ACTIVE) {
				if (proposal.getStatus() == ProposalStatus.ENDED) {
					return ProposalResponse.from(proposal, false);
				}
				throw new ConflictException("僅進行中的提案可以終止投票");
			}
			proposal.setStatus(ProposalStatus.ENDED);
			if (proposal.getEndTime() == null) {
				proposal.setEndTime(now);
			}
		}
		return ProposalResponse.from(proposal, false);
	}

	public void reorder(Long communityId, List<Long> orderedIds) {
		communityService.getById(communityId);
		var proposals = proposalRepository.findByCommunityIdWithMeeting(communityId);
		Map<Long, Proposal> byId = proposals.stream().collect(Collectors.toMap(Proposal::getId, p -> p));
		if (orderedIds.size() != proposals.size() || !byId.keySet().containsAll(orderedIds)) {
			throw new ConflictException("排序清單必須包含此社區的全部提案");
		}
		for (int i = 0; i < orderedIds.size(); i++) {
			byId.get(orderedIds.get(i)).setSortOrder(i);
		}
	}

	public void resetVotes(Long communityId, Long proposalId) {
		findProposalInCommunity(communityId, proposalId);
		voteRecordRepository.deleteByProposal_Id(proposalId);
	}

	public ProposalResultResponse getResult(Long communityId, Long proposalId) {
		syncExpiredStatuses(communityId);
		var proposal = findProposalInCommunity(communityId, proposalId);
		return computeResult(proposal);
	}

	public List<ProposalResultResponse> listResults(Long communityId) {
		syncExpiredStatuses(communityId);
		return proposalRepository.findByCommunityIdWithMeeting(communityId).stream()
				.map(this::computeResult)
				.toList();
	}

	public AdminProposalResultResponse getAdminResult(Long communityId, Long proposalId) {
		syncExpiredStatuses(communityId);
		var proposal = findProposalInCommunity(communityId, proposalId);
		var summary = computeResult(proposal);
		var options = VoteOptionDefaults.normalize(proposal.getVoteOptions());
		var voters = voteRecordRepository.findByProposalIdWithOwner(proposalId).stream()
				.map(record -> new VoteRecordResponse(
						record.getOwner().getId(),
						record.getOwner().getName(),
						record.getOwner().getUnit().getShortName(),
						record.getChoiceKey(),
						VoteOptionDefaults.labelFor(options, record.getChoiceKey()),
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

	private ProposalResultResponse computeResult(Proposal proposal) {
		var community = proposal.getMeeting().getCommunity();
		var communityId = community.getId();
		var totalCommunityArea = unitRepository.sumAreaByCommunityId(communityId);
		var attendedHouseholds = ownerRepository.countAttendedByCommunityId(communityId);
		var attendedWeight = ownerRepository.sumAttendedAreaByCommunityId(communityId);
		return ProposalResultCalculator.compute(
				proposal,
				community,
				totalCommunityArea,
				attendedHouseholds,
				attendedWeight,
				voteRecordRepository
		);
	}

	private void ensureVoterVisible(Proposal proposal) {
		if (!proposal.isVisible()) {
			throw new ResourceNotFoundException("找不到提案：" + proposal.getId());
		}
		if (proposal.getStatus() == ProposalStatus.DRAFT) {
			throw new ResourceNotFoundException("找不到提案：" + proposal.getId());
		}
	}

	private void applyFields(Proposal proposal, CreateProposalRequest request) {
		proposal.setStartTime(request.getStartTime());
		proposal.setEndTime(request.getEndTime());
		proposal.setVisible(request.isVisible());
		proposal.setVoteOptions(resolveVoteOptions(request.getVoteOptions()));
		proposal.setPassThresholdNumerator(Math.max(1, request.getPassThresholdNumerator()));
		proposal.setPassThresholdDenominator(Math.max(1, request.getPassThresholdDenominator()));
		proposal.setThresholdBase(request.getThresholdBase());
		proposal.setAllowRevote(request.isAllowRevote());
	}

	private void applyFields(Proposal proposal, UpdateProposalRequest request) {
		proposal.setStartTime(request.getStartTime());
		proposal.setEndTime(request.getEndTime());
		proposal.setVisible(request.isVisible());
		proposal.setVoteOptions(resolveVoteOptions(request.getVoteOptions()));
		proposal.setPassThresholdNumerator(Math.max(1, request.getPassThresholdNumerator()));
		proposal.setPassThresholdDenominator(Math.max(1, request.getPassThresholdDenominator()));
		proposal.setThresholdBase(request.getThresholdBase());
		proposal.setAllowRevote(request.isAllowRevote());
	}

	private List<VoteOptionItem> resolveVoteOptions(List<VoteOptionRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return new ArrayList<>(VoteOptionDefaults.standard());
		}
		var items = new ArrayList<VoteOptionItem>();
		for (int i = 0; i < requests.size(); i++) {
			items.add(requests.get(i).toItem(i));
		}
		return VoteOptionDefaults.normalize(items);
	}

	public void syncExpiredProposalsForCommunity(Long communityId) {
		syncExpiredStatuses(communityId);
	}

	private void syncExpiredStatuses(Long communityId) {
		proposalLifecycleService.syncExpiredProposals(communityId);
		entityManager.clear();
	}
}
