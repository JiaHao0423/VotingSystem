package com.ben.com.backend.domain.entity;

import com.ben.com.backend.domain.enums.VoteChoice;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
		name = "vote_records",
		uniqueConstraints = @UniqueConstraint(columnNames = {"proposal_id", "owner_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class VoteRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposal_id", nullable = false)
	private Proposal proposal;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private Owner owner;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VoteChoice choice;

	@Column(name = "vote_weight", precision = 10, scale = 2, nullable = false)
	private BigDecimal voteWeight;

	@CreationTimestamp
	@Column(name = "voted_at", nullable = false, updatable = false)
	private Instant votedAt;

	public VoteRecord(Proposal proposal, Owner owner, VoteChoice choice, BigDecimal voteWeight) {
		this.proposal = proposal;
		this.owner = owner;
		this.choice = choice;
		this.voteWeight = voteWeight;
	}
}
