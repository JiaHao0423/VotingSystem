package com.ben.com.backend.domain.entity;

import com.ben.com.backend.domain.enums.BuildingType;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
		name = "units",
		uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "short_name"})
)
@Getter
@Setter
@NoArgsConstructor
public class Unit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "community_id", nullable = false)
	private Community community;

	@Column(name = "short_name", nullable = false, length = 20)
	private String shortName;

	@Column(name = "full_address", nullable = false, length = 255)
	private String fullAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "building_type", nullable = false, length = 10)
	private BuildingType buildingType;

	@Column
	private Integer floor;

	@Column(name = "unit_no")
	private Integer unitNo;

	@Column(name = "shop_no")
	private Integer shopNo;

	@Column(precision = 10, scale = 2)
	private BigDecimal area;

	@Column(name = "ownership_ratio", precision = 8, scale = 4)
	private BigDecimal ownershipRatio;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Unit(
			Community community,
			String shortName,
			String fullAddress,
			BuildingType buildingType,
			Integer floor,
			Integer unitNo,
			Integer shopNo,
			BigDecimal area,
			BigDecimal ownershipRatio
	) {
		this.community = community;
		this.shortName = shortName;
		this.fullAddress = fullAddress;
		this.buildingType = buildingType;
		this.floor = floor;
		this.unitNo = unitNo;
		this.shopNo = shopNo;
		this.area = area;
		this.ownershipRatio = ownershipRatio;
	}
}
