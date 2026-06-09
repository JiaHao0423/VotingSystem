package com.ben.com.backend.domain.entity;

import com.ben.com.backend.domain.enums.ProposalStatus;
import com.ben.com.backend.domain.enums.ProposalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "proposals")
@Getter
@Setter
@NoArgsConstructor
public class Proposal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false)
	private Meeting meeting;

	@Column(name = "proposal_number", nullable = false, length = 50)
	private String proposalNumber;

	@Column(nullable = false, length = 300)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProposalType type = ProposalType.GENERAL;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProposalStatus status = ProposalStatus.DRAFT;

	@Column(name = "start_time")
	private Instant startTime;

	@Column(name = "end_time")
	private Instant endTime;

	@Column(nullable = false)
	private boolean visible = false;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Proposal(Meeting meeting, String proposalNumber, String title, String content, ProposalType type) {
		this.meeting = meeting;
		this.proposalNumber = proposalNumber;
		this.title = title;
		this.content = content;
		this.type = type;
		this.status = ProposalStatus.DRAFT;
	}
}
