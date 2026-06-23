package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.ProposalService;
import com.ben.com.backend.web.dto.AdminProposalResultResponse;
import com.ben.com.backend.web.dto.CreateProposalRequest;
import com.ben.com.backend.web.dto.ProposalResponse;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.ReorderProposalsRequest;
import com.ben.com.backend.web.dto.ToggleVotingRequest;
import com.ben.com.backend.web.dto.UpdateProposalRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/communities/{communityId}/proposals")
public class AdminProposalController {

	private final ProposalService proposalService;

	public AdminProposalController(ProposalService proposalService) {
		this.proposalService = proposalService;
	}

	@GetMapping
	public List<ProposalResponse> list(@PathVariable Long communityId) {
		return proposalService.listForAdmin(communityId);
	}

	@GetMapping("/{proposalId}")
	public ProposalResponse get(@PathVariable Long communityId, @PathVariable Long proposalId) {
		return proposalService.getForAdmin(communityId, proposalId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProposalResponse create(@PathVariable Long communityId, @Valid @RequestBody CreateProposalRequest request) {
		return proposalService.create(communityId, request);
	}

	@PutMapping("/{proposalId}")
	public ProposalResponse update(
			@PathVariable Long communityId,
			@PathVariable Long proposalId,
			@Valid @RequestBody UpdateProposalRequest request
	) {
		return proposalService.update(communityId, proposalId, request);
	}

	@DeleteMapping("/{proposalId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long communityId, @PathVariable Long proposalId) {
		proposalService.delete(communityId, proposalId);
	}

	@PostMapping("/reorder")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reorder(
			@PathVariable Long communityId,
			@Valid @RequestBody ReorderProposalsRequest request
	) {
		proposalService.reorder(communityId, request.orderedIds());
	}

	@PostMapping("/{proposalId}/start")
	public ProposalResponse start(@PathVariable Long communityId, @PathVariable Long proposalId) {
		return proposalService.start(communityId, proposalId);
	}

	@PostMapping("/{proposalId}/stop")
	public ProposalResponse stop(@PathVariable Long communityId, @PathVariable Long proposalId) {
		return proposalService.stop(communityId, proposalId);
	}

	@PutMapping("/{proposalId}/voting")
	public ProposalResponse toggleVoting(
			@PathVariable Long communityId,
			@PathVariable Long proposalId,
			@Valid @RequestBody ToggleVotingRequest request
	) {
		return proposalService.setVotingActive(communityId, proposalId, Boolean.TRUE.equals(request.active()));
	}

	@PostMapping("/{proposalId}/reset-votes")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetVotes(@PathVariable Long communityId, @PathVariable Long proposalId) {
		proposalService.resetVotes(communityId, proposalId);
	}

	@GetMapping("/results")
	public List<ProposalResultResponse> listResults(@PathVariable Long communityId) {
		return proposalService.listResults(communityId);
	}

	@GetMapping("/{proposalId}/results")
	public ProposalResultResponse results(@PathVariable Long communityId, @PathVariable Long proposalId) {
		return proposalService.getResult(communityId, proposalId);
	}

	@GetMapping("/{proposalId}/results/detail")
	public AdminProposalResultResponse resultsDetail(@PathVariable Long communityId, @PathVariable Long proposalId) {
		return proposalService.getAdminResult(communityId, proposalId);
	}
}
