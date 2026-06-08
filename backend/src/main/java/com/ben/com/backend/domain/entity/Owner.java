package com.ben.com.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
		name = "owners",
		uniqueConstraints = @UniqueConstraint(columnNames = "unit_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Owner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "unit_id", nullable = false)
	private Unit unit;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 20)
	private String phone;

	@Column(name = "auth_code_hash", nullable = false, length = 100)
	private String authCodeHash;

	@Column(name = "qr_token", nullable = false, unique = true, length = 64)
	private String qrToken;

	@Column(nullable = false)
	private boolean attended = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Owner(Unit unit, String name, String phone, String authCodeHash, String qrToken) {
		this.unit = unit;
		this.name = name;
		this.phone = phone;
		this.authCodeHash = authCodeHash;
		this.qrToken = qrToken;
	}
}
