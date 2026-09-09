package com.bana.dotori.family.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "families")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "family_id")
	private Integer id;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Column(name = "created_by", nullable = false)
	private Integer createdBy;

	/**
	 * 온보딩 설정 3종 — 가족 생성 시 부모가 입력하는 "가족 기본값"이다.
	 * 금액 두 개는 원이 아니라 <b>포인트 단위</b>로 저장한다 (10원=1포인트 환산은 생성 시점에 끝냄).
	 * 자녀별 차등이 필요해지면 자녀 쪽 오버라이드를 두고, 소비처는
	 * {@code FamilySettingsReader} 를 통해서만 읽으므로 이 컬럼을 직접 참조하지 않는다.
	 */
	@Column(name = "weekly_allowance", nullable = false)
	private Integer weeklyAllowance;

	@Column(name = "weekly_reward_limit", nullable = false)
	private Integer weeklyRewardLimit;

	@Enumerated(EnumType.STRING)
	@Column(name = "financial_knowledge_level", length = 20, nullable = false)
	private FinancialKnowledgeLevel financialKnowledgeLevel;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Builder
	private Family(String name, Integer createdBy, Integer weeklyAllowance, Integer weeklyRewardLimit,
		FinancialKnowledgeLevel financialKnowledgeLevel) {
		this.name = name;
		this.createdBy = createdBy;
		this.weeklyAllowance = weeklyAllowance;
		this.weeklyRewardLimit = weeklyRewardLimit;
		this.financialKnowledgeLevel = financialKnowledgeLevel;
	}

	/**
	 * 가족 이름·온보딩 설정 부분 수정. null 인 필드는 그대로 둔다 (회원 정보 수정과 같은 방식).
	 * 금액 두 개는 이미 포인트로 환산된 값을 받는다 — 원→포인트 환산·검증은 서비스가 한다.
	 */
	public void updateSettings(String name, Integer allowancePoints, Integer rewardLimitPoints,
		FinancialKnowledgeLevel financialKnowledgeLevel) {
		if (name != null) {
			this.name = name;
		}
		if (allowancePoints != null) {
			this.weeklyAllowance = allowancePoints;
		}
		if (rewardLimitPoints != null) {
			this.weeklyRewardLimit = rewardLimitPoints;
		}
		if (financialKnowledgeLevel != null) {
			this.financialKnowledgeLevel = financialKnowledgeLevel;
		}
	}

	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
	}
}
