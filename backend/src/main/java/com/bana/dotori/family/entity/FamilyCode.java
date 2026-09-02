package com.bana.dotori.family.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "family_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "family_code_id")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id", nullable = false)
	private Family family;

	@Column(name = "code", length = 100, nullable = false)
	private String code;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "expired_at", nullable = false)
	private OffsetDateTime expiredAt;

	@Builder
	private FamilyCode(Family family, String code, OffsetDateTime expiredAt) {
		this.family = family;
		this.code = code;
		this.expiredAt = expiredAt;
	}

	public boolean isExpired() {
		return expiredAt.isBefore(OffsetDateTime.now());
	}
}
