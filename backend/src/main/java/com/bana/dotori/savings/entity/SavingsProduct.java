package com.bana.dotori.savings.entity;

import java.math.BigDecimal;
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

/**
 * 6개 고정 예·적금 상품의 DB 행. {@link SavingsProductType} 을 그대로 반영한 seed 데이터일 뿐이고,
 * 부모나 관리자가 새로 만들거나 고치지 않는다 — {@code SavingsProductSeeder} 가 앱 기동 시마다
 * 코드 값과 동기화한다.
 */
@Entity
@Table(name = "savings_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "savings_product_id")
	private Integer id;

	@Column(name = "name", length = 100, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_type", nullable = false)
	private SavingsProductType productType;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "interest_rate", nullable = false)
	private int interestRate;

	@Column(name = "reward_ratio", nullable = false)
	private BigDecimal rewardRatio;

	@Enumerated(EnumType.STRING)
	@Column(name = "contribution_style", nullable = false)
	private ContributionStyle contributionStyle;

	@Enumerated(EnumType.STRING)
	@Column(name = "payout_timing", nullable = false)
	private PayoutTiming payoutTiming;

	@Column(name = "term_weeks")
	private Integer termWeeks;

	@Column(name = "installment_count")
	private Integer installmentCount;

	@Column(name = "education_goal", length = 255)
	private String educationGoal;

	@Column(name = "pre_signup_message", length = 500)
	private String preSignupMessage;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Builder
	private SavingsProduct(SavingsProductType productType, String name, String description) {
		this.productType = productType;
		this.name = name;
		this.description = description;
		this.active = true;
		applyType(productType);
	}

	/**
	 * {@link SavingsProductType} 상수 값으로 스펙 컬럼을 채운다. 시더가 기동 시마다 호출해서
	 * 코드(상수)와 DB 행이 항상 같은 값을 갖게 한다.
	 */
	public void applyType(SavingsProductType productType) {
		this.productType = productType;
		this.interestRate = productType.getInterestRate();
		this.rewardRatio = BigDecimal.valueOf(productType.getRewardRatio());
		this.contributionStyle = productType.getContributionStyle();
		this.payoutTiming = productType.getPayoutTiming();
		this.termWeeks = productType.getTermWeeks();
		this.installmentCount = productType.getInstallmentCount();
		this.educationGoal = productType.getEducationGoal();
		this.preSignupMessage = productType.getPreSignupMessage();
	}
}
