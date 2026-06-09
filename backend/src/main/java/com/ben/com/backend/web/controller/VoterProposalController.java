package com.ben.com.backend.web.controller;

import com.ben.com.backend.service.ProposalService;
import com.ben.com.backend.service.VoterAuthService;
import com.ben.com.backend.service.VoteService;
import com.ben.com.backend.web.dto.ProposalResponse;
import com.ben.com.backend.web.dto.ProposalResultResponse;
import com.ben.com.backend.web.dto.SubmitVoteRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proposals")
public class VoterProposalController {

	private final ProposalService proposalService;
	private final VoteService voteService;
	private final VoterAuthService voterAuthService;

	public VoterProposalController(
			ProposalService proposalService,
			VoteService voteService,
			VoterAuthService voterAuthService
	) {
		this.proposalService = proposalService;
		this.voteService = voteService;
		this.voterAuthService = voterAuthService;
	}

	@GetMapping
	public List<ProposalResponse> list() {
		var voter = voterAuthService.getCurrentPrincipal();
		return proposalService.listForVoter(voter.communityId(), voter.ownerId());
	}

	@GetMapping("/{proposalId}")
	public ProposalResponse get(@PathVariable Long proposalId) {
		var voter = voterAuthService.getCurrentPrincipal();
		return proposalService.getForVoter(proposalId, voter.ownerId());
	}

	@PostMapping("/{proposalId}/votes")
	@ResponseStatus(HttpStatus.CREATED)
	public ProposalResultResponse vote(@PathVariable Long proposalId, @Valid @RequestBody SubmitVoteRequest request) {
		var voter = voterAuthService.getCurrentPrincipal();
		return voteService.submitVote(proposalId, voter, request);
	}

	@GetMapping("/{proposalId}/results")
	public ProposalResultResponse results(@PathVariable Long proposalId) {
		var voter = voterAuthService.getCurrentPrincipal();
		return voteService.getResultForVoter(proposalId, voter);
	}
}
